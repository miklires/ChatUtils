package dev.miklires.chatutils.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import dev.miklires.chatutils.client.render.ChatHeads;
import dev.miklires.chatutils.client.render.HeadOwner;
import dev.miklires.chatutils.client.render.MessageStart;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Draws the head beside a chat line and shifts that line's text out of its way.
 *
 * <p>Targets the anonymous line consumer inside {@code ChatComponent}, which is the only place that
 * knows both the line being drawn and the y and opacity it is being drawn at. The head goes at the
 * left edge; the text is moved right by translating the chat's own pose, then moved back, so the
 * shift applies to this line and nothing else.
 */
@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$1")
public abstract class ChatLineMixin {

    private static final String HANDLE_MESSAGE =
            "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;"
                    + "handleMessage(IFLnet/minecraft/util/FormattedCharSequence;)Z";

    private static final String HANDLE_TAG_ICON =
            "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;"
                    + "handleTagIcon(IIZLnet/minecraft/client/multiplayer/chat/GuiMessageTag;"
                    + "Lnet/minecraft/client/multiplayer/chat/GuiMessageTag$Icon;)V";

    @ModifyArgs(method = "accept", at = @At(value = "INVOKE", target = HANDLE_MESSAGE))
    private void chatutils$drawHeadAndShiftText(Args args,
                                                @Local(argsOnly = true) GuiMessage.Line line,
                                                @Share("offset") LocalIntRef offset) {
        offset.set(0);
        if (!ChatHeads.enabled()) {
            return;
        }

        PlayerInfo owner = ((HeadOwner) (Object) line).chatutils$getOwner();
        offset.set(ChatHeads.offsetFor(owner));

        // Every line of a wrapped message is indented, but only the one that starts it gets a head —
        // otherwise a three-line message would show the same face three times.
        boolean drawHead = ((MessageStart) (Object) line).chatutils$isMessageStart();
        if (drawHead && owner != null && ChatHeads.graphics != null) {
            int y = args.get(0);
            float opacity = args.get(1);
            ChatHeads.render(ChatHeads.graphics, 0, y, owner, opacity);
        }
        shift(offset.get());
    }

    @Inject(method = "accept",
            at = @At(value = "INVOKE", target = HANDLE_MESSAGE, shift = At.Shift.AFTER))
    private void chatutils$unshiftText(CallbackInfo ci, @Share("offset") LocalIntRef offset) {
        shift(-offset.get());
    }

    /** Message tags sit alongside the text, so they need the same shift or they land on the head. */
    @Inject(method = "accept",
            at = @At(value = "INVOKE", target = HANDLE_TAG_ICON), require = 0)
    private void chatutils$shiftTagIcon(CallbackInfo ci, @Share("offset") LocalIntRef offset) {
        shift(offset.get());
    }

    @Inject(method = "accept",
            at = @At(value = "INVOKE", target = HANDLE_TAG_ICON, shift = At.Shift.AFTER), require = 0)
    private void chatutils$unshiftTagIcon(CallbackInfo ci, @Share("offset") LocalIntRef offset) {
        shift(-offset.get());
    }

    private static void shift(int amount) {
        if (amount == 0 || ChatHeads.chatAccess == null) {
            return;
        }
        ChatHeads.chatAccess.updatePose(pose -> pose.translate(amount, 0));
    }
}
