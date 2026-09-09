package dev.miklires.chatutils.client.chat.processor;

import dev.miklires.chatutils.client.chat.ChatMessage;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

public final class WordCopyProcessor implements ChatProcessor {

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
