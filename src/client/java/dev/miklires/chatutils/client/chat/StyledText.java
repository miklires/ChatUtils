package dev.miklires.chatutils.client.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * A chat message flattened to one style per character.
 *
 * <p>Chat components are trees, which makes "colour the word at index 12..17" awkward: the word may
 * straddle several siblings. Flattening to a parallel {@code char[]}/{@code Style[]} pair lets every
 * processor work in plain-string coordinates and hands the regrouping problem to
 * {@link #toComponent()}.
 *
 * <p>Edits must preserve length. That covers everything the mod needs — censoring swaps characters
 * one-for-one, highlighting only touches styles — and keeps the index maths honest for processors
 * that run later in the pipeline.
 */
public final class StyledText {

    private final char[] chars;
    private final Style[] styles;
    private String cachedPlain;

    private StyledText(char[] chars, Style[] styles) {
        this.chars = chars;
        this.styles = styles;
    }

    public static StyledText of(Component component) {
        StringBuilder text = new StringBuilder();
        java.util.List<Style> collected = new java.util.ArrayList<>();

        component.visit((style, string) -> {
            text.append(string);
            for (int i = 0; i < string.length(); i++) {
                collected.add(style);
            }
            return Optional.empty();
        }, Style.EMPTY);

        return new StyledText(text.toString().toCharArray(), collected.toArray(new Style[0]));
    }

    public int length() {
        return chars.length;
    }

    public boolean isEmpty() {
        return chars.length == 0;
    }

    /** The message with all formatting stripped. Cached, so repeated matching stays cheap. */
    public String plain() {
        if (cachedPlain == null) {
            cachedPlain = new String(chars);
        }
        return cachedPlain;
    }

    /** Applies {@code op} to the style of every character in {@code [from, to)}. */
    public void styleRange(int from, int to, UnaryOperator<Style> op) {
        int start = Math.max(0, from);
        int end = Math.min(chars.length, to);
        for (int i = start; i < end; i++) {
            styles[i] = op.apply(styles[i]);
        }
    }

    /** Overwrites every character in {@code [from, to)} with {@code fill}, keeping styles intact. */
    public void fillRange(int from, int to, char fill) {
        int start = Math.max(0, from);
        int end = Math.min(chars.length, to);
        for (int i = start; i < end; i++) {
            chars[i] = fill;
        }
        cachedPlain = null;
    }

    /** Rebuilds a component, merging neighbouring characters that ended up with the same style. */
    public Component toComponent() {
        MutableComponent result = Component.empty();
        if (chars.length == 0) {
            return result;
        }

        int runStart = 0;
        for (int i = 1; i <= chars.length; i++) {
            boolean boundary = i == chars.length || !styles[i].equals(styles[runStart]);
            if (boundary) {
                result.append(Component.literal(new String(chars, runStart, i - runStart))
                        .setStyle(styles[runStart]));
                runStart = i;
            }
        }
        return result;
    }
}
