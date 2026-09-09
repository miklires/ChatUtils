package dev.miklires.chatutils.client.chat;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class SessionDivider {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static final String RULE = "─────";

    private SessionDivider() {
    }

    public static void onJoin() {
        ChatUtilsConfig config = ChatUtilsConfig.get();
        if (!config.sessionDivider || ChatHistory.size() == 0) {
            return;
        }

        MutableComponent label = Component.literal(RULE + "  ")
                .append(destination())
                .append("  ·  " + LocalTime.now().format(TIME) + "  " + RULE);

        ChatDelivery.sendSystem(label.withStyle(style -> style.withColor(config.sessionDividerColor)));
    }

    private static Component destination() {
        ServerData server = Minecraft.getInstance().getCurrentServer();
        if (server == null) {
            return Component.translatable("chatutils.divider.singleplayer");
        }
        return Component.literal(
                server.name == null || server.name.isBlank() ? server.ip : server.name);
    }
}
