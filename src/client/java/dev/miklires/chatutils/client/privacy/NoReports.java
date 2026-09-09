package dev.miklires.chatutils.client.privacy;

import dev.miklires.chatutils.client.chat.ChatDelivery;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import dev.miklires.chatutils.mixin.client.ClientPacketListenerAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

public final class NoReports {

    private static boolean signatureStripped;
    private static boolean verificationPending;
    private static boolean verified;

    private NoReports() {
    }

    public static void onSignatureStripped() {
        signatureStripped = true;
    }

    public static void onJoin() {
        signatureStripped = false;
        verificationPending = false;
        verified = false;

        ChatUtilsConfig config = ChatUtilsConfig.get();
        if (!config.noReportsEnabled) {
            return;
        }

        turnOffSecureChatFilter();

        if (!config.noReportsWarnOnSecureServer) {
            return;
        }

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null && ((ClientPacketListenerAccessor) connection).chatutils$enforcesSecureChat()) {
            send(Component.translatable("chatutils.noreports.secure_server")
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static void turnOffSecureChatFilter() {
        Options options = Minecraft.getInstance().options;
        if (!options.onlyShowSecureChat().get()) {
            return;
        }

        options.onlyShowSecureChat().set(false);
        options.save();
        send(Component.translatable("chatutils.noreports.secure_filter_off")
                .withStyle(ChatFormatting.YELLOW));
    }

    public static void onMessageSent() {
        if (!verified && ChatUtilsConfig.get().noReportsEnabled) {
            verificationPending = true;
        }
    }

    public static void tick() {
        if (!verificationPending) {
            return;
        }
        verificationPending = false;
        verified = true;

        if (!signatureStripped) {
            send(Component.translatable("chatutils.noreports.not_working")
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static void send(Component message) {
        ChatDelivery.sendSystem(message);
    }
}
