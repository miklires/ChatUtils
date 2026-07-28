package dev.miklires.chatutils.client.chat;

import dev.miklires.chatutils.client.render.ChatHeads;
import dev.miklires.chatutils.mixin.client.ChatComponentAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The single place where a finished message is handed to the vanilla chat.
 *
 * <p>Hiding the chat is not handled here: a hidden chat still receives every message, it is simply
 * not drawn. Keeping the messages means the history stays complete and scrollback still works —
 * see {@code ChatVisibility}.
 */
public final class ChatDelivery {

    /**
     * Everything the chat needs to know about a line the mod is posting itself.
     *
     * @param mention whether this line mentions the player
     * @param source  what the line is: a player's message, or something the server or client said
     * @param owner   who wrote it, for the chat head; null when nobody in particular did
     */
    public record Line(Component component, boolean mention, GuiMessageSource source,
                       @Nullable PlayerInfo owner) {
    }

    private ChatDelivery() {
    }

    /** Sends a line to the chat. */
    public static void deliver(Line line) {
        push(line);
    }

    /** Shows a message the mod itself produced. */
    public static void sendSystem(Component component) {
        push(new Line(component, false, GuiMessageSource.SYSTEM_CLIENT, null));
    }

    /**
     * Pushes straight into the chat widget. This sits below Fabric's receive events, so a message we
     * deliver here never loops back through {@link MessagePipeline}.
     *
     * <p>Signature and tag are left null — both are nullable, and a line the mod re-posts carries no
     * signature of its own. The source is preserved rather than flattened to "client", so a rewritten
     * player message still reads as a player message to anything that inspects the chat.
     */
    private static void push(Line line) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui == null) {
            return;
        }
        // Announce the sender first: the chat builds its message object during addMessage, and that
        // is the only moment the head has to latch onto.
        ChatHeads.setPendingOwner(line.owner());
        ((ChatComponentAccessor) minecraft.gui.hud.getChat())
                .chatutils$addMessage(line.component(), null, line.source(), null);
    }
}
