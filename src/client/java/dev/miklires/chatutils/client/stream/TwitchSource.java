package dev.miklires.chatutils.client.stream;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class TwitchSource extends StreamSource {

    private static final URI GATEWAY = URI.create("wss://irc-ws.chat.twitch.tv:443");

    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(6);
    private static final int MAX_PARTIAL_CHARS = 65_536;

    private final String channel;
    private final StringBuilder partial = new StringBuilder();
    private final CountDownLatch finished = new CountDownLatch(1);

    private volatile WebSocket socket;
    private volatile long lastActivity;

    public TwitchSource(String channel) {
        super(normalise(channel));
        this.channel = normalise(channel);
    }

    public static String normalise(String channel) {
        if (channel == null) {
            return "";
        }
        String normalized = channel.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        return normalized.matches("[a-z0-9_]{1,25}") ? normalized : "";
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

        send("CAP REQ :twitch.tv/tags");
        send("NICK justinfan" + (10000 + (int) (Math.random() * 89999)));
        send("JOIN #" + channel);

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
            open.abort();
        }
    }

    private void send(String line) {
        WebSocket open = socket;
        if (open != null) {
            open.sendText(line + "\r\n", true);
        }
    }

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

    private static int parseColor(String value) {
        if (value == null || !value.startsWith("#") || value.length() != 7) {
            return 0;
        }
        try {
            return Integer.parseInt(value.substring(1), 16);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private final class Listener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            partial.append(data);
            if (partial.length() > MAX_PARTIAL_CHARS) {
                partial.setLength(0);
                webSocket.abort();
                finished.countDown();
                return null;
            }
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
