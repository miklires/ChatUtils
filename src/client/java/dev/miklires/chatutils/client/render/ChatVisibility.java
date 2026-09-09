package dev.miklires.chatutils.client.render;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

public final class ChatVisibility {

    private static boolean peeking;
    private static long revealUntil;

    private ChatVisibility() {
    }

    public static boolean shouldRender() {
        ChatUtilsConfig config = ChatUtilsConfig.get();
        if (!config.hideChatEnabled || peeking) {
            return true;
        }
        if (System.currentTimeMillis() < revealUntil) {
            return true;
        }
        return Minecraft.getInstance().gui.screen() instanceof ChatScreen;
    }

    public static void setPeeking(boolean peeking) {
        ChatVisibility.peeking = peeking;
    }

    public static void onMention() {
        ChatUtilsConfig config = ChatUtilsConfig.get();
        if (config.hideChatEnabled && config.hideChatShowMentions) {
            revealUntil = System.currentTimeMillis() + Math.max(0, config.hideChatRevealMs);
        }
    }

    public static void reset() {
        peeking = false;
        revealUntil = 0L;
    }
}
