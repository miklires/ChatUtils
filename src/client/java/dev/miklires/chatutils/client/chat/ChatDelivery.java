package dev.miklires.chatutils.client.chat;

import dev.miklires.chatutils.client.render.ChatHeads;
import dev.miklires.chatutils.mixin.client.ChatComponentAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class ChatDelivery {

    public record Line(Component component, boolean mention, GuiMessageSource source,
                       @Nullable PlayerInfo owner) {
    }

    private ChatDelivery() {
    }

    public static void deliver(Line line) {
        push(line);
    }

    public static void sendSystem(Component component) {
        push(new Line(component, false, GuiMessageSource.SYSTEM_CLIENT, null));
    }

    private static void push(Line line) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui == null) {
            return;
        }
        ChatHeads.setPendingOwner(line.owner());
        ((ChatComponentAccessor) minecraft.gui.hud.getChat())
                .chatutils$addMessage(line.component(), null, line.source(), null);
    }
}
