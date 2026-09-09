package dev.miklires.chatutils.client.chat.processor;

import dev.miklires.chatutils.client.chat.ChatMessage;
import dev.miklires.chatutils.client.chat.TextMatcher;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public final class NameHiderProcessor implements ChatProcessor {

    @Override
    public void apply(ChatMessage message, ChatUtilsConfig config) {
        if (!config.hideOwnName) {
            return;
        }

        List<String> names = names(config);
        if (names.isEmpty()) {
            return;
        }

        char mask = config.hideOwnNameChar == null || config.hideOwnNameChar.isEmpty()
                ? '*'
                : config.hideOwnNameChar.charAt(0);

        String plain = message.plain();
        for (String name : names) {
            for (TextMatcher.Match match : TextMatcher.findLiteral(plain, name, false, false)) {
                message.fillRange(match.start(), match.end(), mask);
            }
        }
    }

    private static List<String> names(ChatUtilsConfig config) {
        List<String> names = new ArrayList<>(config.hiddenNames);

        var player = Minecraft.getInstance().player;
        if (player != null) {
            names.add(player.getGameProfile().name());
        }

        names.removeIf(name -> name == null || name.isBlank());
        return names;
    }
}
