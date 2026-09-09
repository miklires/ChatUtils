package dev.miklires.chatutils.client.privacy;

import dev.miklires.chatutils.client.chat.ChatDelivery;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class EncryptedChat {

    private static final int CHAT_LIMIT = 256;

    private EncryptedChat() {
    }

    public static boolean isOn() {
        ChatUtilsConfig config = ChatUtilsConfig.get();
        return config.encryptionEnabled && !config.encryptionPassphrase.isBlank();
    }

    public static boolean allowOutgoing(String finalPlaintext) {
        if (!isOn()) {
            return true;
        }

        int max = ChatEncryption.maxPlaintextLength(CHAT_LIMIT);
        if (ChatEncryption.fits(finalPlaintext, CHAT_LIMIT)) {
            return true;
        }

        warn(Component.translatable("chatutils.encrypt.too_long", max));
        return false;
    }

    public static String encryptOutgoing(String message) {
        if (!isOn() || message.isBlank()) {
            return message;
        }
        try {
            return ChatEncryption.encrypt(message, ChatUtilsConfig.get().encryptionPassphrase);
        } catch (ChatEncryption.EncryptionException e) {
            warn(Component.translatable("chatutils.encrypt.failed", e.getMessage()));
            return "";
        }
    }

    public static Component decryptIncoming(Component incoming) {
        if (!isOn()) {
            return incoming;
        }

        String plain = incoming.getString();
        int start = plain.indexOf(ChatEncryption.MARKER);
        if (start < 0) {
            return incoming;
        }

        int end = start;
        while (end < plain.length() && !Character.isWhitespace(plain.charAt(end))) {
            end++;
        }

        String opened = ChatEncryption.decrypt(plain.substring(start, end),
                ChatUtilsConfig.get().encryptionPassphrase);
        if (opened == null) {
            return incoming;
        }

        return Component.literal(plain.substring(0, start))
                .append(Component.translatable("chatutils.encrypt.marker")
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(opened).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(plain.substring(end)));
    }

    private static void warn(Component message) {
        ChatDelivery.sendSystem(message.copy().withStyle(ChatFormatting.RED));
    }
}
