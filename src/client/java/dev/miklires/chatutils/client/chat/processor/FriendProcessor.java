package dev.miklires.chatutils.client.chat.processor;

import dev.miklires.chatutils.client.chat.ChatMessage;
import dev.miklires.chatutils.client.chat.TextMatcher;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;

import java.util.List;

public final class FriendProcessor implements ChatProcessor {

    @Override
    public void apply(ChatMessage message, ChatUtilsConfig config) {
        if (!config.friendsEnabled || config.friends.isEmpty()) {
            return;
        }

        String plain = message.plain();
        for (String friend : config.friends) {
            List<TextMatcher.Match> matches = TextMatcher.findLiteral(plain, friend, false, true);
            for (TextMatcher.Match match : matches) {
                message.styleRange(match.start(), match.end(), style -> {
                    var styled = style.withColor(config.friendColor);
                    return config.friendBold ? styled.withBold(true) : styled;
                });
            }
        }
    }
}
