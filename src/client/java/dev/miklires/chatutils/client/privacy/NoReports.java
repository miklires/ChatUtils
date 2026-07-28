package dev.miklires.chatutils.client.privacy;

import dev.miklires.chatutils.client.chat.ChatDelivery;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import dev.miklires.chatutils.mixin.client.ClientPacketListenerAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

/**
 * State and messaging for the "no chat reports" feature.
 *
 * <p>The actual work is one mixin that blanks the signature attached to an outgoing chat packet.
 * Unsigned messages cannot be bundled into a Mojang report, because there is nothing to prove you
 * sent them — which is also why servers running {@code enforce-secure-profile=true} refuse them.
 *
 * <p>This class exists mostly to keep the feature honest. A signature-stripping mixin is exactly the
 * kind of thing that quietly stops matching after a game update, and a privacy feature that silently
 * stopped working is worse than one that is plainly off. So the first message of each session is
 * checked, and the user is told if nothing was actually stripped.
 */
public final class NoReports {

    private static boolean signatureStripped;
    private static boolean verificationPending;
    private static boolean verified;

    private NoReports() {
    }

    /** Called from the mixin every time a signature is actually removed. */
    public static void onSignatureStripped() {
        signatureStripped = true;
    }

    /** Clears the conflicting vanilla filter, and warns when the server will reject unsigned chat. */
    public static void onJoin() {
        signatureStripped = false;
        verificationPending = false;
        verified = false;

        ChatUtilsConfig config = ChatUtilsConfig.get();
        if (!config.noReportsEnabled) {
            return;
        }

        // Not gated on the warning preference: that setting is about the server, this is a conflict
        // inside the client's own options and would break chat whatever the player prefers to hear.
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

    /**
     * Clears the vanilla "only show secure chat" option while unsigned chat is on.
     *
     * <p>The two settings contradict each other: that option hides every message whose sender cannot
     * be verified, which now includes your own. Left on, it looks like the mod swallowed your chat.
     * It is turned off rather than left alone, and said so out loud, so the cause is never a mystery.
     */
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

    /** Called when the player sends a chat message. */
    public static void onMessageSent() {
        if (!verified && ChatUtilsConfig.get().noReportsEnabled) {
            verificationPending = true;
        }
    }

    /**
     * Runs a tick after the first message went out — by then the mixin has either fired or it has
     * not, and either way the answer is known.
     */
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
