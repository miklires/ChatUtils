package dev.miklires.chatutils.client.config;

import dev.isxander.yacl3.api.NameableEnum;
import net.minecraft.network.chat.Component;

/** What the word filter does with a message that contains a blocked word. */
public enum FilterMode implements NameableEnum {
    /** Replace the matched word with the censor character, keep the message. */
    CENSOR("chatutils.filter_mode.censor"),
    /** Drop the whole message so it never reaches the chat. */
    HIDE("chatutils.filter_mode.hide");

    private final String key;

    FilterMode(String key) {
        this.key = key;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(key);
    }
}
