package dev.miklires.chatutils.client.chat;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public record MentionRule(String keyword, int color, @Nullable String soundId) {

    public static final int INHERIT = -1;

    public static MentionRule parse(String entry, boolean regex) {
        if (regex) {
            return new MentionRule(entry.trim(), INHERIT, null);
        }

        String[] parts = entry.split("\\|", 3);
        String keyword = parts[0].trim();

        int color = parts.length > 1 ? parseColor(parts[1]) : INHERIT;
        String sound = parts.length > 2 && !parts[2].isBlank() ? parts[2].trim() : null;

        return new MentionRule(keyword, color, sound);
    }

    public boolean hasColor() {
        return color != INHERIT;
    }

    private static int parseColor(String value) {
        String hex = value.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() != 6) {
            return INHERIT;
        }
        try {
            return Integer.parseInt(hex.toLowerCase(Locale.ROOT), 16);
        } catch (NumberFormatException notAColour) {
            return INHERIT;
        }
    }
}
