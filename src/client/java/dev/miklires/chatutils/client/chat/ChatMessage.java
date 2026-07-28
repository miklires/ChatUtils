package dev.miklires.chatutils.client.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.function.UnaryOperator;

/**
 * One chat line travelling through the processor pipeline.
 *
 * <p>Processors mutate the flattened {@link StyledText} in place and may attach a prefix, which is
 * how the timestamp gets in front. {@link #build()} assembles the final component once, at the end —
 * and returns the untouched original when nothing changed, so unaffected messages keep their vanilla
 * component tree.
 */
public final class ChatMessage {

    private final Component original;
    private final StyledText text;
    private ChatAuthor author = ChatAuthor.UNKNOWN;
    private boolean mention;
    private MentionRule mentionRule;
    private boolean cancelled;
    private boolean modified;
    private Component prefix;
    private UnaryOperator<Style> wholeLineStyle;

    public ChatMessage(Component original) {
        this.original = original;
        this.text = StyledText.of(original);
    }

    /** Read-only view of the flattened text. Use the mutators below to change it. */
    public StyledText text() {
        return text;
    }

    public String plain() {
        return text.plain();
    }

    public ChatAuthor author() {
        return author;
    }

    public void setAuthor(ChatAuthor author) {
        this.author = author;
    }

    public boolean isMention() {
        return mention;
    }

    /** @param rule the keyword that matched, whose colour and sound override the defaults */
    public void markMention(MentionRule rule) {
        this.mention = true;
        this.mentionRule = rule;
    }

    /** The rule that fired, or null when the message is not a mention. */
    public MentionRule mentionRule() {
        return mentionRule;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    /** Drops the line: it never reaches the chat at all. */
    public void cancel() {
        this.cancelled = true;
    }

    public void styleRange(int from, int to, UnaryOperator<Style> op) {
        text.styleRange(from, to, op);
        modified = true;
    }

    public void fillRange(int from, int to, char fill) {
        text.fillRange(from, to, fill);
        modified = true;
    }

    public void setPrefix(Component prefix) {
        this.prefix = prefix;
        modified = true;
    }

    /** Applies a style to the finished line as a whole, including prefix and suffix. */
    public void setWholeLineStyle(UnaryOperator<Style> op) {
        this.wholeLineStyle = op;
        modified = true;
    }

    public Component build() {
        if (!modified) {
            return original;
        }

        MutableComponent result = Component.empty();
        if (prefix != null) {
            result.append(prefix);
        }
        result.append(text.toComponent());
        if (wholeLineStyle != null) {
            result.setStyle(wholeLineStyle.apply(result.getStyle()));
        }
        return result;
    }
}
