package dev.miklires.chatutils.client.render;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * What each player said recently, for the bubbles drawn above their head.
 *
 * <p>Kept apart from the drawing on purpose. This half is plain Java with no rendering in it at all,
 * so it can be reasoned about and changed without touching the part that has to know how the game
 * draws text in the world — which is the part that changes between versions.
 *
 * <p>Bounded twice: a few lines per player, and only for as long as they are fresh. A bubble is a
 * glance at what was just said, not a log; the chat is the log.
 */
public final class ChatBubbles {

    /** @param text what was said, already stripped of the sender's name */
    public record Bubble(String text, long shownAt) {
    }

    private static final Map<UUID, List<Bubble>> byPlayer = new LinkedHashMap<>();

    private ChatBubbles() {
    }

    /** Records a line for a player. Ignored when the feature is off, so nothing accumulates unused. */
    public static void record(UUID player, String text) {
        ChatUtilsConfig config = ChatUtilsConfig.get();
        if (!config.chatBubbles || player == null || text == null || text.isBlank()) {
            return;
        }

        List<Bubble> lines = byPlayer.computeIfAbsent(player, key -> new ArrayList<>());
        lines.add(new Bubble(trim(text, config.chatBubbleLength), System.currentTimeMillis()));

        while (lines.size() > Math.max(1, config.chatBubbleLines)) {
            lines.remove(0);
        }
    }

    /** A player's live bubbles, oldest first, with the expired ones dropped as a side effect. */
    public static List<Bubble> forPlayer(UUID player) {
        List<Bubble> lines = byPlayer.get(player);
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }

        long cutoff = System.currentTimeMillis() - Math.max(1000L, ChatUtilsConfig.get().chatBubbleMs);
        lines.removeIf(bubble -> bubble.shownAt() < cutoff);
        if (lines.isEmpty()) {
            byPlayer.remove(player);
            return List.of();
        }
        return List.copyOf(lines);
    }

    public static void clear() {
        byPlayer.clear();
    }

    /** Long messages are cut rather than wrapped: a wall of text over someone's head is not a bubble. */
    private static String trim(String text, int limit) {
        String line = text.strip();
        int max = Math.max(8, limit);
        return line.length() <= max ? line : line.substring(0, max - 1) + "…";
    }

    /**
     * Whether a line looks like it came from this player rather than about them.
     *
     * <p>Used by the pipeline, which knows the author but not always where their name ends in a
     * server's own formatting.
     */
    public static String stripAuthor(String plain, String author) {
        if (author == null || author.isBlank()) {
            return plain;
        }
        int name = plain.toLowerCase(Locale.ROOT).indexOf(author.toLowerCase(Locale.ROOT));
        if (name < 0) {
            return plain;
        }

        // Past the name and whatever punctuation the server puts between it and the message.
        int i = name + author.length();
        while (i < plain.length() && (plain.charAt(i) == '>' || plain.charAt(i) == ':'
                || plain.charAt(i) == ']' || Character.isWhitespace(plain.charAt(i)))) {
            i++;
        }
        return i >= plain.length() ? plain : plain.substring(i);
    }
}
