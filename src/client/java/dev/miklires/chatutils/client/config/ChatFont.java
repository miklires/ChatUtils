package dev.miklires.chatutils.client.config;

import dev.isxander.yacl3.api.NameableEnum;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A Unicode look-alike alphabet for outgoing messages — 𝔣𝔯𝔞𝔨𝔱𝔲𝔯, 𝕕𝕠𝕦𝕓𝕝𝕖-𝕤𝕥𝕣𝕦𝕔𝕜, ᴋᴀᴘɪᴛᴀʟs and so on.
 *
 * <p>These are real characters, not formatting codes: everyone sees them, on any server, with or
 * without this mod. The flip side is that they are not letters as far as anything else is concerned,
 * so a server's search, autocomplete or word filter will not match text written in one.
 *
 * <p>An enum rather than a list of string keys, so the settings screen gets a real picker with
 * translated names instead of a text box you have to type {@code sans_bold_italic} into exactly.
 *
 * <p>Built from base code points rather than pasted glyphs. The blocks are contiguous {@code A-Z},
 * {@code a-z} and {@code 0-9} runs with a handful of holes: Unicode had already assigned some of
 * these letters elsewhere (ℎ, ℬ, ℭ, ℍ …) and left gaps where they would have gone. The exception
 * tables are exactly those gaps.
 */
public enum ChatFont implements NameableEnum {

    /** Send messages exactly as typed. */
    NONE(0, 0, 0),

    BOLD(0x1D400, 0x1D41A, 0x1D7CE),
    ITALIC(0x1D434, 0x1D44E, 0, 'h', 0x210E),
    BOLD_ITALIC(0x1D468, 0x1D482, 0),

    // Script and fraktur are the gappiest: eight capitals and three lower-case script letters were
    // already assigned as letterlike symbols before the block existed.
    SCRIPT(0x1D49C, 0x1D4B6, 0,
            'B', 0x212C, 'E', 0x2130, 'F', 0x2131, 'H', 0x210B, 'I', 0x2110,
            'L', 0x2112, 'M', 0x2133, 'R', 0x211B,
            'e', 0x212F, 'g', 0x210A, 'o', 0x2134),
    SCRIPT_BOLD(0x1D4D0, 0x1D4EA, 0),
    FRAKTUR(0x1D504, 0x1D51E, 0,
            'C', 0x212D, 'H', 0x210C, 'I', 0x2111, 'R', 0x211C, 'Z', 0x2128),
    FRAKTUR_BOLD(0x1D56C, 0x1D586, 0),

    DOUBLE_STRUCK(0x1D538, 0x1D552, 0x1D7D8,
            'C', 0x2102, 'H', 0x210D, 'N', 0x2115, 'P', 0x2119,
            'Q', 0x211A, 'R', 0x211D, 'Z', 0x2124),

    SANS(0x1D5A0, 0x1D5BA, 0x1D7E2),
    SANS_BOLD(0x1D5D4, 0x1D5EE, 0x1D7EC),
    SANS_ITALIC(0x1D608, 0x1D622, 0),
    SANS_BOLD_ITALIC(0x1D63C, 0x1D656, 0),
    MONOSPACE(0x1D670, 0x1D68A, 0x1D7F6),
    FULLWIDTH(0xFF21, 0xFF41, 0xFF10),

    /** Letters run cleanly; the digits do not, so circled zero is listed with the exceptions. */
    CIRCLED(0x24B6, 0x24D0, 0, circledDigits()),

    /**
     * Small capitals were never a block — they are phonetic letters collected one at a time over
     * decades, so every one is an exception and {@code q} and {@code x} were never encoded at all.
     * Those two are left as ordinary letters, which is what every other tool does.
     */
    SMALL_CAPS(0, 0, 0, smallCapitals());

    private final int upper;
    private final int lower;
    private final int digit;
    private final Map<Character, Integer> exceptions;

    ChatFont(int upper, int lower, int digit) {
        this(upper, lower, digit, Map.of());
    }

    ChatFont(int upper, int lower, int digit, Object... pairs) {
        this(upper, lower, digit, pairsToMap(pairs));
    }

    ChatFont(int upper, int lower, int digit, Map<Character, Integer> exceptions) {
        this.upper = upper;
        this.lower = lower;
        this.digit = digit;
        this.exceptions = exceptions;
    }

    @Override
    public Component getDisplayName() {
        // The name in words, then the alphabet itself: seeing "𝔄𝔞" beats reading "Fraktur", and a
        // font your resource pack cannot draw shows as boxes here rather than after you send in it.
        String label = Component.translatable(translationKey()).getString();
        return Component.literal(this == NONE ? label : label + "  " + apply("Abc"));
    }

    public String translationKey() {
        return "chatutils.font." + name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Letters and digits become their look-alikes; everything else is left exactly as it is. */
    public String apply(String text) {
        if (this == NONE || text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int mapped = map(c);
            if (mapped == 0) {
                result.append(c);
            } else {
                result.appendCodePoint(mapped);
            }
        }
        return result.toString();
    }

    /** @return the replacement code point, or 0 to keep the character as it is */
    private int map(char c) {
        Integer exception = exceptions.get(c);
        if (exception != null) {
            return exception;
        }
        if (c >= 'A' && c <= 'Z') {
            return upper == 0 ? 0 : upper + (c - 'A');
        }
        if (c >= 'a' && c <= 'z') {
            return lower == 0 ? 0 : lower + (c - 'a');
        }
        if (c >= '0' && c <= '9') {
            return digit == 0 ? 0 : digit + (c - '0');
        }
        return 0;
    }

    private static Map<Character, Integer> circledDigits() {
        Map<Character, Integer> map = new LinkedHashMap<>();
        // Circled zero sits alone at U+24EA, a long way from one to nine.
        map.put('0', 0x24EA);
        for (int i = 1; i <= 9; i++) {
            map.put((char) ('0' + i), 0x2460 + i - 1);
        }
        return Map.copyOf(map);
    }

    private static Map<Character, Integer> smallCapitals() {
        int[] codePoints = {
                0x1D00, 0x0299, 0x1D04, 0x1D05, 0x1D07, 0xA730, 0x0262, 0x029C, 0x026A, 0x1D0A,
                0x1D0B, 0x029F, 0x1D0D, 0x0274, 0x1D0F, 0x1D18, 0, 0x0280, 0xA731, 0x1D1B,
                0x1D1C, 0x1D20, 0x1D21, 0, 0x028F, 0x1D22
        };

        Map<Character, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < codePoints.length; i++) {
            if (codePoints[i] == 0) {
                continue;
            }
            // Upper case maps to the same glyphs: the whole point is that everything is a capital.
            map.put((char) ('a' + i), codePoints[i]);
            map.put((char) ('A' + i), codePoints[i]);
        }
        return Map.copyOf(map);
    }

    /**
     * Every look-alike code point any font can produce, mapped back to the plain letter it stands
     * for. Built once from the same tables that produce them, so a font can never be added without
     * the filters learning to see through it.
     */
    private static final Map<Integer, Character> FOLD = buildFold();

    /**
     * Rewrites look-alike letters back to plain ASCII, so a word filter, a keyword or a blocked
     * name still matches text written in one of these alphabets.
     *
     * <p>Folding shortens the text — a supplementary code point is two chars and its plain letter is
     * one — and the callers use the result to style or mask a range of the <em>original</em>. So the
     * mapping back is returned alongside it rather than left for the caller to guess.
     */
    public record Folded(String text, int[] toOriginal) {

        /** Length of the text this was folded from. */
        public int originalLength() {
            return toOriginal[text.length()];
        }

        /** Nothing was folded, so indices already line up and need no translation. */
        public boolean isPlain() {
            return text.length() == originalLength();
        }
    }

    public static Folded fold(String text) {
        StringBuilder folded = new StringBuilder(text.length());
        // One more entry than characters: the last marks where the folded text ends in the original.
        int[] toOriginal = new int[text.length() + 1];

        int i = 0;
        while (i < text.length()) {
            int codePoint = text.codePointAt(i);
            Character plain = FOLD.get(codePoint);

            toOriginal[folded.length()] = i;
            if (plain != null) {
                folded.append(plain.charValue());
            } else {
                folded.appendCodePoint(codePoint);
                // A character that is not folded may still be a surrogate pair, and both of its
                // chars need an entry or a match ending inside it would map to the wrong place.
                for (int extra = 1; extra < Character.charCount(codePoint); extra++) {
                    toOriginal[folded.length() - extra] = i;
                }
            }
            i += Character.charCount(codePoint);
        }

        toOriginal[folded.length()] = text.length();
        return new Folded(folded.toString(), java.util.Arrays.copyOf(toOriginal, folded.length() + 1));
    }

    private static Map<Integer, Character> buildFold() {
        Map<Integer, Character> fold = new java.util.HashMap<>();
        for (ChatFont font : values()) {
            if (font == NONE) {
                continue;
            }
            for (char c = 'a'; c <= 'z'; c++) {
                record(fold, font.map(c), c);
            }
            for (char c = 'A'; c <= 'Z'; c++) {
                record(fold, font.map(c), Character.toLowerCase(c));
            }
            for (char c = '0'; c <= '9'; c++) {
                record(fold, font.map(c), c);
            }
        }
        return Map.copyOf(fold);
    }

    /**
     * First font to claim a code point keeps it. Only small capitals collide — they use one glyph
     * for both cases — and either answer folds to the same letter, since matching is
     * case-insensitive by default anyway.
     */
    private static void record(Map<Integer, Character> fold, int codePoint, char plain) {
        if (codePoint != 0) {
            fold.putIfAbsent(codePoint, plain);
        }
    }

    private static Map<Character, Integer> pairsToMap(Object... pairs) {
        Map<Character, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put((Character) pairs[i], (Integer) pairs[i + 1]);
        }
        return Map.copyOf(map);
    }
}
