package dev.miklires.chatutils.client.config;

import dev.isxander.yacl3.api.NameableEnum;
import net.minecraft.network.chat.Component;

/** How message timestamps are shown. */
public enum TimestampMode implements NameableEnum {
    /** No timestamps at all. */
    OFF("chatutils.timestamp_mode.off"),
    /** Prefix every line with the time. */
    INLINE("chatutils.timestamp_mode.inline"),
    /** Show the time in a tooltip when hovering the message. */
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
