package dev.miklires.chatutils.mixin.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Reaches the connection's secure-chat flag, which is private.
 *
 * <p>Needed to warn the player when a server demands signed chat while unsigned chat is turned on.
 * Without the warning the setting is a silent trap: the server simply refuses every message.
 */
@Mixin(ClientPacketListener.class)
public interface ClientPacketListenerAccessor {

    @Invoker("enforcesSecureChat")
    boolean chatutils$enforcesSecureChat();
}
