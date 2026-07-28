package dev.miklires.chatutils.client.chat.processor;

import dev.miklires.chatutils.client.chat.ChatMessage;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;

/** Drops messages written by players on the blacklist. */
public final class BlacklistProcessor implements ChatProcessor {

    @Override
    public void apply(ChatMessage message, ChatUtilsConfig config) {
        if (!config.blacklistEnabled || config.blacklistedPlayers.isEmpty() || !message.author().isKnown()) {
            return;
        }

        String author = message.author().name();
        for (String blocked : config.blacklistedPlayers) {
            if (blocked != null && blocked.equalsIgnoreCase(author)) {
                message.cancel();
                return;
            }
        }
    }
}
