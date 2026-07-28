package dev.miklires.chatutils.client.stream;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Reads a Twitch channel's chat over Twitch's IRC-over-WebSocket gateway.
 *
 * <p>Anonymously: Twitch lets any client read a public channel by logging in as {@code justinfan}
 * plus digits, with no password and no OAuth app. That is the whole reason this works out of the
 * box — there is no token to obtain, nothing to paste into the config, and no credential of the
 * player's for the mod to hold. The flip side is that it is strictly read-only, which is the right
 * amount of power for a Minecraft chat mod to have over someone's stream.
 *
 * <p>{@code twitch.tv/tags} is requested only for the display name and the chatter's colour; the
 * badge and emote metadata that comes with it is ignored.
 */
public final class TwitchSource extends StreamSource {

    private static final URI GATEWAY = URI.create("wss://irc-ws.chat.twitch.tv:443");

    /** Twitch drops a silent connection; it pings first, and expects the pong within five minutes. */
    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(6);

    private final String channel;
    private final StringBuilder partial = new StringBuilder();
    private final CountDownLatch finished = new CountDownLatch(1);

    private volatile WebSocket socket;
    private volatile long lastActivity;

    public TwitchSource(String channel) {
        // Normalised before it becomes the target, so "Channel", "channel" and "#channel" are one
        // target and not three — otherwise editing the config's casing would force a reconnect.
        super(normalise(channel));
        this.channel = normalise(channel);
    }

    /**
     * Public because the reconciler compares a running source's target against the config to decide
     * whether to reconnect. It has to compare like for like, and the rule lives here.
     */
    public static String normalise(String channel) {
        return channel == null ? "" : channel.trim().toLowerCase(Locale.ROOT).replace("#", "");
    }

    @Override
    public String name() {
        return "twitch";
    }

    @Override
    protected void run() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        socket = client.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .buildAsync(GATEWAY, new Listener())
                .get(20, TimeUnit.SECONDS);

        lastActivity = System.currentTimeMillis();

        // Anonymous login. The digits only have to be unique-ish; Twitch does not check them.
        send("CAP REQ :twitch.tv/tags");
        send("NICK justinfan" + (10000 + (int) (Math.random() * 89999)));
        send("JOIN #" + channel);

        // Park until the connection ends or the game asks to stop. The listener does the work.
        while (shouldRun() && finished.getCount() > 0) {
            if (finished.await(1, TimeUnit.SECONDS)) {
                break;
            }
            if (System.currentTimeMillis() - lastActivity > IDLE_TIMEOUT.toMillis()) {
                throw new IllegalStateException("no traffic from Twitch for " + IDLE_TIMEOUT.toMinutes() + " minutes");
            }
        }
    }

    @Override
    protected void close() {
        finished.countDown();
        WebSocket open = socket;
        socket = null;
        if (open != null) {
            // Abort rather than a polite close: this is called from the game thread and must return
            // at once, and there is nothing to flush on a read-only connection.
            open.abort();
        }
    }

    private void send(String line) {
        WebSocket open = socket;
        if (open != null) {
            open.sendText(line + "\r\n", true);
        }
    }

    /**
     * Handles one IRC line.
     *
     * <p>Only {@code PING} and {@code PRIVMSG} matter. Everything else — the welcome banner, joins,
     * parts, the capability acknowledgement — is traffic that proves the connection is alive, which
     * is all it is used for.
     */
    private void handle(String line) {
        lastActivity = System.currentTimeMillis();

        if (line.startsWith("PING")) {
            send("PONG" + line.substring(4));
            return;
        }

        String tags = "";
        String rest = line;
        if (rest.startsWith("@")) {
            int space = rest.indexOf(' ');
            if (space < 0) {
                return;
            }
            tags = rest.substring(1, space);
            rest = rest.substring(space + 1);
        }

        // :nick!nick@nick.tmi.twitch.tv PRIVMSG #channel :the message
        if (!rest.startsWith(":")) {
            return;
        }
        int space = rest.indexOf(' ');
        if (space < 0) {
            return;
        }
        String prefix = rest.substring(1, space);
        String command = rest.substring(space + 1);
        if (!command.startsWith("PRIVMSG ")) {
            return;
        }

        int body = command.indexOf(" :");
        if (body < 0) {
            return;
        }
        String text = command.substring(body + 2);
        if (text.isBlank()) {
            return;
        }

        String author = tagValue(tags, "display-name");
        if (author.isBlank()) {
            int bang = prefix.indexOf('!');
            author = bang > 0 ? prefix.substring(0, bang) : prefix;
        }

        deliver(new StreamMessage(StreamMessage.Platform.TWITCH, author, text, parseColor(tagValue(tags, "color"))));
    }

    /** IRCv3 tags are {@code key=value;key=value}, with escapes for the few characters that clash. */
    private static String tagValue(String tags, String key) {
        for (String tag : tags.split(";")) {
            int equals = tag.indexOf('=');
            if (equals > 0 && tag.substring(0, equals).equals(key)) {
                return unescape(tag.substring(equals + 1));
            }
        }
        return "";
    }

    private static String unescape(String value) {
        if (value.indexOf('\\') < 0) {
            return value;
        }
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\' || i + 1 >= value.length()) {
                result.append(c);
                continue;
            }
            char next = value.charAt(++i);
            switch (next) {
                case 's' -> result.append(' ');
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case ':' -> result.append(';');
                case '\\' -> result.append('\\');
                default -> result.append(next);
            }
        }
        return result.toString();
    }

    /** @return {@code 0xRRGGBB}, or 0 when the chatter never picked a colour */
    private static int parseColor(String value) {
        if (value == null || !value.startsWith("#") || value.length() != 7) {
            return 0;
        }
        try {
            return Integer.parseInt(value.substring(1), 16);
        } catch (NumberFormatException malformed) {
            return 0;
        }
    }

    /**
     * A WebSocket frame is not an IRC line: frames arrive split at arbitrary points and several
     * lines can share one frame, so text is buffered and cut on newlines.
     */
    private final class Listener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                int newline;
                while ((newline = partial.indexOf("\n")) >= 0) {
                    String line = partial.substring(0, newline).replace("\r", "");
                    partial.delete(0, newline + 1);
                    if (!line.isEmpty()) {
                        handle(line);
                    }
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            finished.countDown();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            finished.countDown();
        }
    }
}
