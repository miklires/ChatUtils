package dev.miklires.chatutils.client.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;

import java.util.Locale;

/**
 * Works out who wrote a chat line.
 *
 * <p>There is no reliable structured author on the client: servers rewrite chat into arbitrary text
 * ({@code <Nick> hi}, {@code [VIP] Nick » hi}, {@code Nick: hi}), and plugin formats vary per
 * server. So we match against the names the client already knows — the tab list — and only accept a
 * hit that lands in the *header* of the line, before the separator that introduces the message body.
 * That stops "hello Steve" from being attributed to Steve.
 */
public final class AuthorResolver {

    /** Separators that mark the end of the "who said it" part of a line. */
    private static final String[] SEPARATORS = {"» ", "> ", ": ", "]: ", " - "};

    /** Fallback window when the line has no recognisable separator at all. */
    private static final int HEADER_FALLBACK_LENGTH = 48;

    private AuthorResolver() {
    }

    public static ChatAuthor resolve(String plainText) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null || plainText.isEmpty()) {
            return ChatAuthor.UNKNOWN;
        }

        int headerEnd = headerEnd(plainText);
        String header = plainText.substring(0, headerEnd).toLowerCase(Locale.ROOT);

        PlayerInfo best = null;
        int bestIndex = Integer.MAX_VALUE;
        int bestLength = 0;

        for (PlayerInfo info : connection.getOnlinePlayers()) {
            String name = info.getProfile().name();
            if (name == null || name.isEmpty()) {
                continue;
            }
            int index = header.indexOf(name.toLowerCase(Locale.ROOT));
            if (index < 0) {
                continue;
            }
            // Earliest match wins; on a tie prefer the longer name so "Mik" does not beat "Mikliisr".
            if (index < bestIndex || (index == bestIndex && name.length() > bestLength)) {
                best = info;
                bestIndex = index;
                bestLength = name.length();
            }
        }

        if (best == null) {
            return ChatAuthor.UNKNOWN;
        }
        return new ChatAuthor(best.getProfile().name(), best.getProfile().id(), best);
    }

    /**
     * Index at which the message body starts, just past the "who said it" header.
     *
     * <p>Returns {@code 0} when the line has no recognisable header, so callers scanning the body
     * fall back to scanning everything rather than silently skipping the start of a system message.
     */
    public static int bodyStart(String text) {
        int end = -1;
        for (String separator : SEPARATORS) {
            int index = text.indexOf(separator);
            if (index >= 0 && (end < 0 || index < end)) {
                // Include the separator itself so a name sitting right against it still matches.
                end = index + separator.length();
            }
        }
        return end < 0 ? 0 : Math.min(end, text.length());
    }

    private static int headerEnd(String text) {
        int body = bodyStart(text);
        return body > 0 ? body : Math.min(text.length(), HEADER_FALLBACK_LENGTH);
    }

    /**
     * True when the line was written by the player running the game.
     *
     * <p>Checks the name as well as the id: on offline-mode and cracked servers the profile behind a
     * chat message does not always carry the same uuid as the client's own player entity, and a
     * mismatch there would quietly turn every one of your own messages into a self-mention.
     */
    public static boolean isLocalPlayer(ChatAuthor author) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (!author.isKnown() || player == null) {
            return false;
        }
        if (author.id() != null && author.id().equals(player.getUUID())) {
            return true;
        }
        return author.name() != null && author.name().equalsIgnoreCase(player.getGameProfile().name());
    }
}
