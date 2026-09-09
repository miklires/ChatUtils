package dev.miklires.chatutils.mixin.client;

import dev.miklires.chatutils.client.render.ChatHeads;
import dev.miklires.chatutils.client.render.HeadOwner;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMessage.class)
public class GuiMessageMixin implements HeadOwner {

    @Unique
    @Nullable
    private PlayerInfo chatutils$owner;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void chatutils$captureOwner(CallbackInfo ci) {
        chatutils$owner = ChatHeads.takePendingOwner();
    }

    @Override
    @Nullable
    public PlayerInfo chatutils$getOwner() {
        return chatutils$owner;
    }

    @Override
    public void chatutils$setOwner(@Nullable PlayerInfo owner) {
        this.chatutils$owner = owner;
    }
}
