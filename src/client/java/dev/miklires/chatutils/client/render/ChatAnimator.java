package dev.miklires.chatutils.client.render;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.Minecraft;

public final class ChatAnimator {

    private static long startedAt;
    private static boolean rebuilding;

    private ChatAnimator() {
    }

    public static void onChatOpened() {
        if (ChatUtilsConfig.get().chatOpenAnimation) {
            startedAt = System.currentTimeMillis();
        }
    }

    public static void onMessageAdded() {
        if (!rebuilding && ChatUtilsConfig.get().chatAnimationEnabled) {
            startedAt = System.currentTimeMillis();
        }
    }

    public static void setRebuilding(boolean rebuilding) {
        ChatAnimator.rebuilding = rebuilding;
    }

    public static float begin() {
        return offset();
    }

    private static float offset() {
        ChatUtilsConfig config = ChatUtilsConfig.get();
        if (!config.chatAnimationEnabled || startedAt == 0L) {
            return 0.0f;
        }

        int duration = Math.max(1, config.chatAnimationDurationMs);
        long elapsed = System.currentTimeMillis() - startedAt;
        if (elapsed >= duration) {
            startedAt = 0L;
            return 0.0f;
        }

        float progress = (float) elapsed / duration;
        float eased = 1.0f - (1.0f - progress) * (1.0f - progress) * (1.0f - progress);
        return lineHeight() * (1.0f - eased);
    }

    private static float lineHeight() {
        double spacing = Minecraft.getInstance().options.chatLineSpacing().get();
        return (float) (9.0 * (spacing + 1.0));
    }

    public static void reset() {
        startedAt = 0L;
        rebuilding = false;
    }
}
