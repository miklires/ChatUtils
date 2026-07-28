package dev.miklires.chatutils.client.render;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

/**
 * How far the whole chat is moved this frame, as one number.
 *
 * <p>Two things want to move it: the arrival animation slides it down and lets it ease back, and the
 * HUD offset lifts it clear of the armour bar. They are applied together because the chat is
 * translated once, before its lines are extracted, and untranslated by exactly the same amount
 * afterwards — time moves between those two points, so the value has to be remembered rather than
 * recomputed.
 *
 * <p>The HUD offset deliberately does <em>not</em> apply while the chat is open. Moving the chat
 * moves what you see but not where the game thinks its lines are, so a link would stop being where
 * it looks. With the chat closed there is nothing to click, so the offset is free; with the chat
 * open it sits back down where clicking works. That also happens to be when you want it: the offset
 * exists to clear the armour bar while you are playing, not while you are typing.
 */
public final class ChatShift {

    private static float applied;

    private ChatShift() {
    }

    /** Starts an extraction pass: works out the shift and remembers it. Positive is downwards. */
    public static float begin() {
        applied = ChatAnimator.begin() - hudLift();
        return applied;
    }

    /** The shift applied by the current pass, for undoing it. */
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
