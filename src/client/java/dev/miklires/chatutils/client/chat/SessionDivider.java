package dev.miklires.chatutils.client.chat;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * A line naming the world or server you just joined.
 *
 * <p>Only useful because the backlog survives leaving a world: without a divider, yesterday's
 * conversation on one server and today's on another run together as one wall of text with nothing
 * marking the seam.
 *
 * <p>Deliberately not drawn on the first join of a session — there is nothing above it to separate
 * it from.
 */
public final class SessionDivider {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    /** Padding either side of the label, wide enough to read as a rule rather than as a message. */
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

    /** The server's name or address; singleplayer has no address to show. */
    private static Component destination() {
        ServerData server = Minecraft.getInstance().getCurrentServer();
        if (server == null) {
            return Component.translatable("chatutils.divider.singleplayer");
        }
        return Component.literal(
                server.name == null || server.name.isBlank() ? server.ip : server.name);
    }
}
