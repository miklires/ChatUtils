package dev.miklires.chatutils.mixin.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPacketListener.class)
public interface ClientPacketListenerAccessor {

    @Invoker("enforcesSecureChat")
    boolean chatutils$enforcesSecureChat();
}
