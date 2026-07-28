package dev.miklires.chatutils.client.chat.processor;

import dev.miklires.chatutils.client.chat.ChatMessage;
import dev.miklires.chatutils.client.chat.TextMatcher;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Masks your own account name wherever it appears in chat.
 *
 * <p>For streaming and screenshots: the chat is the one place your real account name turns up in
 * every other player's sentences, where no privacy setting reaches it.
 *
 * <p>Masked with a repeated character rather than replaced with an alias. The pipeline edits a
 * flattened copy of the line in place, which keeps every style and every index valid but fixes the
 * length — an alias of a different length would shift everything after it. A row of stars is also
 * honest about what happened, where an alias reads as if that were really the name used.
 *
 * <p>Runs after the mention processor on purpose: hiding the name first would mean never being
 * notified when someone says it, which is the opposite of what anyone wants.
 */
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

        // Snapshot once: masking only overwrites characters, so indices stay valid, and matching
        // against the snapshot stops one name from matching the stars left by another.
        String plain = message.plain();
        for (String name : names) {
            for (TextMatcher.Match match : TextMatcher.findLiteral(plain, name, false, false)) {
                message.fillRange(match.start(), match.end(), mask);
            }
        }
    }

    /** The player's own name, plus whatever else they asked to keep out of the chat. */
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
