package dev.miklires.chatutils.client.chat.processor;

import dev.miklires.chatutils.client.chat.ChatMessage;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;

@FunctionalInterface
public interface ChatProcessor {

    void apply(ChatMessage message, ChatUtilsConfig config);
}
