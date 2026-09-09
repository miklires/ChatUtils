package dev.miklires.chatutils.client.chat;

import dev.miklires.chatutils.client.config.ChatFont;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Finding word occurrences in a chat line, with the options the config exposes.
 *
 * <p>Matching runs against a folded copy of the text, so a word written in one of the mod's Unicode
 * alphabets still counts as that word: 𝔟𝔞𝔡 is not "bad" to a plain string compare, and a filter
 * that could be walked straight past by picking a font would not be a filter. Folding shortens the
 * text, so every match is translated back to a range of the original before it is returned — the
 * callers use these ranges to mask or style the real line.
 */
public final class TextMatcher {
    private static final int MAX_REGEX_TEXT_LENGTH = 4096;
    private static final int MAX_MATCHES = 256;

    /** Half-open {@code [start, end)} range of a match. */
    public record Match(int start, int end) {
    }

    private TextMatcher() {
    }

    public static List<Match> findLiteral(String text, String needle, boolean caseSensitive, boolean wholeWord) {
        List<Match> matches = new ArrayList<>();
        if (needle == null || needle.isBlank() || text.isEmpty()) {
            return matches;
        }

        ChatFont.Folded folded = ChatFont.fold(text);
        String plain = folded.text();
        String haystack = caseSensitive ? plain : plain.toLowerCase(Locale.ROOT);
        String target = caseSensitive ? needle : needle.toLowerCase(Locale.ROOT);

        int index = 0;
        while ((index = haystack.indexOf(target, index)) >= 0) {
            int end = index + target.length();
            if (!wholeWord || isStandalone(plain, index, end)) {
                matches.add(translate(folded, index, end));
            }
            index = end;
        }
        return matches;
    }

    /**
     * Compiles {@code regex} and returns every match, or an empty list when the pattern is invalid —
     * a typo in the config should not spam the log on every single message.
     */
    public static List<Match> findRegex(String text, String regex, boolean caseSensitive) {
        List<Match> matches = new ArrayList<>();
        if (regex == null || regex.isBlank() || text.isEmpty()) {
            return matches;
        }
        try {
            String boundedText = text.length() <= MAX_REGEX_TEXT_LENGTH
                    ? text : text.substring(0, MAX_REGEX_TEXT_LENGTH);
            ChatFont.Folded folded = ChatFont.fold(boundedText);
            Pattern pattern = Pattern.compile(regex, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(folded.text());
            while (matcher.find()) {
                if (matcher.end() > matcher.start()) {
                    matches.add(translate(folded, matcher.start(), matcher.end()));
                    if (matches.size() == MAX_MATCHES) {
                        break;
                    }
                }
            }
        } catch (PatternSyntaxException ignored) {
            // Invalid user pattern: treat as "matches nothing".
        }
        return matches;
    }

    /** Turns a range of the folded text back into the range of the original it came from. */
    private static Match translate(ChatFont.Folded folded, int start, int end) {
        if (folded.isPlain()) {
            return new Match(start, end);
        }
        return new Match(folded.toOriginal()[start], folded.toOriginal()[end]);
    }

    /** A match counts as a whole word when neither neighbour is a letter or digit. */
    private static boolean isStandalone(String text, int start, int end) {
        boolean leftClear = start == 0 || !isWordChar(text.charAt(start - 1));
        boolean rightClear = end >= text.length() || !isWordChar(text.charAt(end));
        return leftClear && rightClear;
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
