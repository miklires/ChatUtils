package dev.miklires.chatutils.mixin.client;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Constant;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    @ModifyConstant(method = "addMessage", constant = @Constant(intValue = 100), require = 0, expect = 0)
    private int chatutils$historySize(int vanilla) {
        return Math.max(vanilla, ChatUtilsConfig.get().historySize);
    }

    @Inject(method = "clearMessages", at = @At("HEAD"), cancellable = true)
    private void chatutils$keepHistory(boolean clearSentMsgHistory, CallbackInfo ci) {
        if (clearSentMsgHistory && ChatUtilsConfig.get().keepHistoryOnDisconnect) {
            ci.cancel();
        }
    }
}
