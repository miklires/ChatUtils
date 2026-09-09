package dev.miklires.chatutils.client.config;

import dev.isxander.yacl3.api.NameableEnum;
import net.minecraft.network.chat.Component;

public enum FilterMode implements NameableEnum {
    CENSOR("chatutils.filter_mode.censor"),
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
