package dev.miklires.chatutils.client.render;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ChatBubbles {

    public record Bubble(String text, long shownAt) {
    }

    private static final Map<UUID, List<Bubble>> byPlayer = new LinkedHashMap<>();

    private ChatBubbles() {
    }

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

    private static String trim(String text, int limit) {
        String line = text.strip();
        int max = Math.max(8, limit);
        return line.length() <= max ? line : line.substring(0, max - 1) + "…";
    }

    public static String stripAuthor(String plain, String author) {
        if (author == null || author.isBlank()) {
            return plain;
        }
        int name = plain.toLowerCase(Locale.ROOT).indexOf(author.toLowerCase(Locale.ROOT));
        if (name < 0) {
            return plain;
        }

        int i = name + author.length();
        while (i < plain.length() && (plain.charAt(i) == '>' || plain.charAt(i) == ':'
                || plain.charAt(i) == ']' || Character.isWhitespace(plain.charAt(i)))) {
            i++;
        }
        return i >= plain.length() ? plain : plain.substring(i);
    }
}
