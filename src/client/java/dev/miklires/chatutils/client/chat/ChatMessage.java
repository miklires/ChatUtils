package dev.miklires.chatutils.client.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.function.UnaryOperator;

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

    public void markMention(MentionRule rule) {
        this.mention = true;
        this.mentionRule = rule;
    }

    public MentionRule mentionRule() {
        return mentionRule;
    }

    public boolean isCancelled() {
        return cancelled;
    }

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
