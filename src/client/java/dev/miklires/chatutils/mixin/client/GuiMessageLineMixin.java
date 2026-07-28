package dev.miklires.chatutils.mixin.client;

import dev.miklires.chatutils.client.render.ChatHeads;
import dev.miklires.chatutils.client.render.HeadOwner;
import dev.miklires.chatutils.client.render.MessageStart;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Carries the sender down to a single display line.
 *
 * <p>A message is split into lines when it is queued and again whenever the chat is rebuilt — on a
 * resize, say — so the owner is taken from whichever message is being split at the time rather than
 * from the message that happened to arrive last.
 */
@Mixin(GuiMessage.Line.class)
public class GuiMessageLineMixin implements HeadOwner, MessageStart {

    @Unique
    @Nullable
    private PlayerInfo chatutils$owner;

    @Unique
    private boolean chatutils$messageStart;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void chatutils$captureOwner(CallbackInfo ci) {
        chatutils$owner = ChatHeads.currentOwner();
        chatutils$messageStart = ChatHeads.consumeMessageStart();
    }

    @Override
    public boolean chatutils$isMessageStart() {
        return chatutils$messageStart;
    }

    @Override
    public void chatutils$setMessageStart(boolean messageStart) {
        this.chatutils$messageStart = messageStart;
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
