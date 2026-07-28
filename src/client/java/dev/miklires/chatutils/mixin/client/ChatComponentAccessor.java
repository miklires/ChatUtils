package dev.miklires.chatutils.mixin.client;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Posts a message into the chat widget.
 *
 * <p>26.2 made {@code addMessage} private and dropped the one-argument convenience overload, so
 * there is no public way left to put a locally produced line into the chat. Everything the mod
 * delivers itself — hidden-chat flushes, collapsed spam, its own notices, and player messages the
 * processors rewrote — goes through here.
 */
@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {

    @Invoker("addMessage")
    void chatutils$addMessage(Component message, MessageSignature signature,
                              GuiMessageSource source, GuiMessageTag tag);
}
