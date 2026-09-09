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

@Mixin(ChatComponent.class)
public class ChatComponentGraphicsMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At("HEAD"))
    private static void chatutils$captureGraphics(CallbackInfo ci,
                                                  @Local(argsOnly = true) GuiGraphicsExtractor graphics) {
        ChatHeads.graphics = graphics;
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
            at = @At("HEAD"), cancellable = true)
    private static void chatutils$captureChatAccess(CallbackInfo ci,
                                                    @Local(argsOnly = true) ChatComponent.ChatGraphicsAccess access) {
        if (!ChatVisibility.shouldRender()) {
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

    @Inject(method = "addMessageToDisplayQueue", at = @At("HEAD"))
    private void chatutils$carryOwnerToLines(GuiMessage message, CallbackInfo ci) {
        ChatHeads.setCurrentOwner(((HeadOwner) (Object) message).chatutils$getOwner());
        ChatAnimator.onMessageAdded();
    }

    @Inject(method = "refreshTrimmedMessages", at = @At("HEAD"))
    private void chatutils$startRebuild(CallbackInfo ci) {
        ChatAnimator.setRebuilding(true);
    }

    @Inject(method = "refreshTrimmedMessages", at = @At("RETURN"))
    private void chatutils$finishRebuild(CallbackInfo ci) {
        ChatAnimator.setRebuilding(false);
    }
}
