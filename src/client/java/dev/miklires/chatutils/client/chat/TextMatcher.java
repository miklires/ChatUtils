package dev.miklires.chatutils.client.chat;

import dev.miklires.chatutils.client.config.ChatFont;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class TextMatcher {
    private static final int MAX_REGEX_TEXT_LENGTH = 4096;
    private static final int MAX_MATCHES = 256;

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
        }
        return matches;
    }

    private static Match translate(ChatFont.Folded folded, int start, int end) {
        if (folded.isPlain()) {
            return new Match(start, end);
        }
        return new Match(folded.toOriginal()[start], folded.toOriginal()[end]);
    }

    private static boolean isStandalone(String text, int start, int end) {
        boolean leftClear = start == 0 || !isWordChar(text.charAt(start - 1));
        boolean rightClear = end >= text.length() || !isWordChar(text.charAt(end));
        return leftClear && rightClear;
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
