package dev.miklires.chatutils.mixin.client;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import dev.miklires.chatutils.client.privacy.NoReports;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

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
