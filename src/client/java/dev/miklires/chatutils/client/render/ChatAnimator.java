package dev.miklires.chatutils.client.render;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.Minecraft;

/**
 * Slides the chat up when a message arrives instead of snapping it.
 *
 * <p>Vanilla redraws the chat one line higher the instant a message lands. Here the whole chat
 * starts one line lower and eases into place, so the new line appears to rise from the bottom and
 * the older ones drift up with it.
 *
 * <p>The offset is a function of elapsed wall-clock time rather than a value stepped per frame:
 * frames are not evenly spaced, and a per-frame decay would run at different speeds depending on
 * frame rate.
 */
public final class ChatAnimator {

    private static long startedAt;
    private static boolean rebuilding;

    private ChatAnimator() {
    }

    /**
     * The chat screen opened. The chat grows from the few lines it shows while you play to the full
     * focused height, and vanilla does that between one frame and the next; easing it in reuses the
     * same slide the arrival animation uses.
     */
    public static void onChatOpened() {
        if (ChatUtilsConfig.get().chatOpenAnimation) {
            startedAt = System.currentTimeMillis();
        }
    }

    /**
     * A message reached the chat, so start the slide.
     *
     * <p>Ignored while the chat is being rebuilt: a resize re-adds every message it already had,
     * and animating that would send the whole backlog sliding for no reason.
     */
    public static void onMessageAdded() {
        if (!rebuilding && ChatUtilsConfig.get().chatAnimationEnabled) {
            startedAt = System.currentTimeMillis();
        }
    }

    public static void setRebuilding(boolean rebuilding) {
        ChatAnimator.rebuilding = rebuilding;
    }

    /** This frame's slide. Remembering it for the undo is {@link ChatShift}'s job. */
    public static float begin() {
        return offset();
    }

    /** How far down the chat should currently be drawn, in pixels; 0 once the slide has finished. */
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
        // Ease out: quick off the mark, gentle as it settles. A linear slide reads as mechanical.
        float eased = 1.0f - (1.0f - progress) * (1.0f - progress) * (1.0f - progress);
        return lineHeight() * (1.0f - eased);
    }

    /**
     * Height of one chat line, matching vanilla's own calculation. Read from the options rather than
     * from the chat widget, whose accessor is not public.
     */
    private static float lineHeight() {
        double spacing = Minecraft.getInstance().options.chatLineSpacing().get();
        return (float) (9.0 * (spacing + 1.0));
    }

    public static void reset() {
        startedAt = 0L;
        rebuilding = false;
    }
}
