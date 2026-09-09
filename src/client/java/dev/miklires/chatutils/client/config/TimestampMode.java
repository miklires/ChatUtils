package dev.miklires.chatutils.client.config;

import dev.isxander.yacl3.api.NameableEnum;
import net.minecraft.network.chat.Component;

public enum TimestampMode implements NameableEnum {
    OFF("chatutils.timestamp_mode.off"),
    INLINE("chatutils.timestamp_mode.inline"),
    HOVER("chatutils.timestamp_mode.hover");

    private final String key;

    TimestampMode(String key) {
        this.key = key;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(key);
    }
}
