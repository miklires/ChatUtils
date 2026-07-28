package dev.miklires.chatutils.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import dev.miklires.chatutils.client.render.ChatAnimator;
import dev.miklires.chatutils.client.render.ChatShift;
import dev.miklires.chatutils.client.render.ChatHeads;
import dev.miklires.chatutils.client.render.ChatVisibility;
import dev.miklires.chatutils.client.render.HeadOwner;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures what the chat head renderer needs but cannot reach on its own.
 *
 * <p>The graphics object and the chat's own drawing access are parameters of two different
 * {@code extractRenderState} overloads, and neither is visible from the inner class that draws an
 * individual line. Both are held for the duration of chat extraction and dropped afterwards, so
 * nothing keeps a stale reference to a finished frame.
 */
@Mixin(ChatComponent.class)
public class ChatComponentGraphicsMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At("HEAD"))
    private static void chatutils$captureGraphics(CallbackInfo ci,
                                                  @Local(argsOnly = true) GuiGraphicsExtractor graphics) {
        ChatHeads.graphics = graphics;
    }

    /**
     * Captures the chat's drawing access and moves the whole chat by this frame's shift — the
     * arrival animation plus the HUD offset. Applied once here rather than per line so the entire
     * chat moves together.
     */
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
            at = @At("HEAD"), cancellable = true)
    private static void chatutils$captureChatAccess(CallbackInfo ci,
                                                    @Local(argsOnly = true) ChatComponent.ChatGraphicsAccess access) {
        // Hidden chat is a drawing decision, not a delivery one: skipping the extraction leaves the
        // messages, the history and the scrollback exactly as they were, just off screen.
        if (!ChatVisibility.shouldRender()) {
            // Cancelling here skips the RETURN handler as well, so release what the other overload
            // captured rather than holding a finished frame's extractor until the chat reappears.
            ChatHeads.graphics = null;
            ci.cancel();
            return;
        }

        ChatHeads.chatAccess = access;

        float shift = ChatShift.begin();
        if (shift != 0.0f) {
            access.updatePose(pose -> pose.translate(0.0f, shift));
        }
    }

    /**
     * Undoing the shift and dropping the captured references happen together, in one handler: split
     * across two RETURN injections their order would be unspecified, and the undo has to run while
     * the access is still held.
     */
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
            at = @At("RETURN"))
    private static void chatutils$releaseGraphics(CallbackInfo ci) {
        float shift = ChatShift.applied();
        if (shift != 0.0f && ChatHeads.chatAccess != null) {
            ChatHeads.chatAccess.updatePose(pose -> pose.translate(0.0f, -shift));
        }
        ChatHeads.graphics = null;
        ChatHeads.chatAccess = null;
    }

    /**
     * A message is about to be split into display lines; hand its owner down so each line inherits
     * it. This also fires when the chat is rebuilt, which is what keeps heads after a resize.
     */
    @Inject(method = "addMessageToDisplayQueue", at = @At("HEAD"))
    private void chatutils$carryOwnerToLines(GuiMessage message, CallbackInfo ci) {
        ChatHeads.setCurrentOwner(((HeadOwner) (Object) message).chatutils$getOwner());
        ChatAnimator.onMessageAdded();
    }

    /** A rebuild re-adds every message the chat already had; none of that should animate. */
    @Inject(method = "refreshTrimmedMessages", at = @At("HEAD"))
    private void chatutils$startRebuild(CallbackInfo ci) {
        ChatAnimator.setRebuilding(true);
    }

    @Inject(method = "refreshTrimmedMessages", at = @At("RETURN"))
    private void chatutils$finishRebuild(CallbackInfo ci) {
        ChatAnimator.setRebuilding(false);
    }
}
