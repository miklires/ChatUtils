package dev.miklires.chatutils.client.chat.processor;

import dev.miklires.chatutils.client.chat.ChatMessage;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

/**
 * Makes every word in a chat line copyable by clicking it.
 *
 * <p>Chat is full of things you want out of the game and cannot select with a mouse: coordinates,
 * server addresses, someone's name spelled in a way you would get wrong by hand. Copying the whole
 * line is rarely what you want; a word is.
 *
 * <p>Runs early in the pipeline on purpose. Later processors set their own click behaviour on the
 * ranges they care about — the filter offers to reveal a censored word, the linkifier opens a URL —
 * and those must win over plain copying.
 */
public final class WordCopyProcessor implements ChatProcessor {

    /** Below this a "word" is punctuation, and a click target smaller than the cursor is a trap. */
    private static final int MIN_LENGTH = 2;

    @Override
    public void apply(ChatMessage message, ChatUtilsConfig config) {
        if (!config.clickToCopy) {
            return;
        }

        String plain = message.plain();
        int start = -1;

        for (int i = 0; i <= plain.length(); i++) {
            boolean word = i < plain.length() && !Character.isWhitespace(plain.charAt(i));
            if (word && start < 0) {
                start = i;
            } else if (!word && start >= 0) {
                copyable(message, plain, start, i);
                start = -1;
            }
        }
    }

    private static void copyable(ChatMessage message, String plain, int start, int end) {
        if (end - start < MIN_LENGTH) {
            return;
        }

        String word = plain.substring(start, end);
        message.styleRange(start, end, style -> style
                .withClickEvent(new ClickEvent.CopyToClipboard(word))
                .withHoverEvent(new HoverEvent.ShowText(
                        Component.translatable("chatutils.copy.hint", word))));
    }
}
