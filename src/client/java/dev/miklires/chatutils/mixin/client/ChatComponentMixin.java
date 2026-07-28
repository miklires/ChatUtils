package dev.miklires.chatutils.mixin.client;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Constant;

/**
 * The two things about chat history that cannot be done through Fabric's message events: how much
 * of it is kept, and when it is thrown away.
 *
 * <p>Everything else — filtering, highlighting, timestamps — happens at packet level in
 * {@code MessagePipeline}, which is far less sensitive to game updates than injecting here.
 */
@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    /**
     * Vanilla trims its backlog to 100 lines. Raising that is the whole "long chat history" feature.
     *
     * <p>{@code require = 0} on purpose: if a future version stops using a literal 100, the mod
     * should carry on with vanilla's backlog rather than refuse to load.
     */
    @ModifyConstant(method = "addMessage", constant = @Constant(intValue = 100), require = 0, expect = 0)
    private int chatutils$historySize(int vanilla) {
        return Math.max(vanilla, ChatUtilsConfig.get().historySize);
    }

    /**
     * Keeps the backlog when leaving a world.
     *
     * <p>The flag distinguishes the two callers: leaving a world clears the sent-message history too
     * ({@code true}), while the F3+D shortcut does not ({@code false}). Cancelling only the former
     * means the chat survives a trip to the main menu but F3+D still wipes it on demand.
     */
    @Inject(method = "clearMessages", at = @At("HEAD"), cancellable = true)
    private void chatutils$keepHistory(boolean clearSentMsgHistory, CallbackInfo ci) {
        if (clearSentMsgHistory && ChatUtilsConfig.get().keepHistoryOnDisconnect) {
            ci.cancel();
        }
    }
}
