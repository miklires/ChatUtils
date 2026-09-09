package dev.miklires.chatutils.client.render;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

public final class ChatShift {

    private static float applied;

    private ChatShift() {
    }

    public static float begin() {
        applied = ChatAnimator.begin() - hudLift();
        return applied;
    }

    public static float applied() {
        return applied;
    }

    private static float hudLift() {
        ChatUtilsConfig config = ChatUtilsConfig.get();
        if (config.chatHudOffset <= 0) {
            return 0.0f;
        }
        return Minecraft.getInstance().gui.screen() instanceof ChatScreen ? 0.0f : config.chatHudOffset;
    }
}
