package dev.miklires.chatutils.client.chat;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * One keyword, optionally with its own colour and sound.
 *
 * <p>Written as {@code keyword|#RRGGBB|sound.id} in the keyword list, both extras optional:
 * {@code boss} is a plain keyword, {@code boss|#FF0000} colours that one differently, and
 * {@code boss|#FF0000|minecraft:entity.wither.spawn} gives it its own alarm too.
 *
 * <p>Encoded in the string rather than modelled as a list of objects because the settings screen
 * edits a list of strings. Nested editors for a handful of optional fields would be a screen of
 * their own to build and to learn, and this stays readable in the config file — where anyone who
 * wants twenty rules will end up editing them anyway.
 *
 * <p>A malformed suffix falls back to the global settings rather than being rejected. Getting a
 * colour wrong should cost you the colour, not the alert.
 *
 * <p>Not available in regex mode, where {@code |} is alternation: splitting {@code raid|event} into
 * a keyword and a colour would quietly break a working pattern, which is far worse than not offering
 * a per-rule colour there. In that mode the entry is the pattern, whole.
 */
public record MentionRule(String keyword, int color, @Nullable String soundId) {

    /** Colour value meaning "use the one from the settings screen". */
    public static final int INHERIT = -1;

    /** @param regex whether keywords are patterns, in which case {@code |} belongs to the pattern */
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

    /** @return {@code 0xRRGGBB}, or {@link #INHERIT} when the text is not a colour */
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
