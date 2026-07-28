package dev.miklires.chatutils.client.render;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

/**
 * Decides whether the chat is drawn at all.
 *
 * <p>"Hidden chat" suppresses the drawing, not the message. Messages still arrive, still go into the
 * history and can still be scrolled back through — they are simply not on screen while you play.
 * That is what makes peeking possible: there is nothing to release, the chat is just drawn again for
 * as long as the key is held.
 *
 * <p>Opening the chat always shows it. Hiding the chat you deliberately opened would be absurd.
 */
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

    /** Held state of the peek key, refreshed every tick. */
    public static void setPeeking(boolean peeking) {
        ChatVisibility.peeking = peeking;
    }

    /**
     * Shows the chat for a moment because the player was mentioned.
     *
     * <p>Hiding chat is about muting background noise, not about missing someone talking to you.
     */
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
