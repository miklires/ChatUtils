package dev.miklires.chatutils.mixin.client;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import dev.miklires.chatutils.client.privacy.NoReports;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Strips the cryptographic signature from outgoing chat.
 *
 * <p>Without a signature the server can still show the message, but it can no longer hand Mojang
 * proof that this account sent it — which is what a chat report is built on. Servers that set
 * {@code enforce-secure-profile=true} reject unsigned chat outright; {@link NoReports} warns about
 * that on join.
 *
 * <p>{@code require = 0} keeps a failed match from bricking the mod on a future game version. The
 * cost is that the injection could silently stop applying, so {@link NoReports} verifies at runtime
 * that a signature really was stripped and tells the user when it was not.
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @ModifyVariable(method = "sendChat", at = @At("STORE"), require = 0, expect = 0)
    private MessageSignature chatutils$stripSignature(MessageSignature signature) {
        if (!ChatUtilsConfig.get().noReportsEnabled) {
            return signature;
        }
        NoReports.onSignatureStripped();
        return null;
    }
}
