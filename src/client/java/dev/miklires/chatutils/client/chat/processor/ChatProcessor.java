package dev.miklires.chatutils.client.chat.processor;

import dev.miklires.chatutils.client.chat.ChatMessage;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;

/** One transformation step applied to an incoming chat line. */
@FunctionalInterface
public interface ChatProcessor {

    void apply(ChatMessage message, ChatUtilsConfig config);
}
