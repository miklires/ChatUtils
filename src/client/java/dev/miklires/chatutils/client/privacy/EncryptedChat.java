package dev.miklires.chatutils.client.privacy;

import dev.miklires.chatutils.client.chat.ChatDelivery;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Wires {@link ChatEncryption} into the chat: sealing what you send, opening what you can read.
 *
 * <p>The two halves are separate on purpose. The cryptography knows nothing about Minecraft and is
 * tested on its own; this knows nothing about cryptography beyond calling it.
 */
public final class EncryptedChat {

    /** Vanilla's chat packet limit. Anything longer is refused before it can be truncated. */
    private static final int CHAT_LIMIT = 256;

    private EncryptedChat() {
    }

    public static boolean isOn() {
        ChatUtilsConfig config = ChatUtilsConfig.get();
        return config.encryptionEnabled && !config.encryptionPassphrase.isBlank();
    }

    /**
     * Checks a message before it is sent.
     *
     * <p>Refused rather than truncated: an encrypted message cut off at the packet limit does not
     * arrive damaged, it fails to decrypt entirely, and the recipient sees noise while the sender
     * sees a message they think went out fine.
     *
     * @return {@code false} to stop the message being sent
     */
    public static boolean allowOutgoing(String finalPlaintext) {
        if (!isOn()) {
            return true;
        }

        int max = ChatEncryption.maxPlaintextLength(CHAT_LIMIT);
        if (finalPlaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= max) {
            return true;
        }

        warn(Component.translatable("chatutils.encrypt.too_long", max));
        return false;
    }

    /** Seals an outgoing message. Never returns the plaintext on failure — it refuses instead. */
    public static String encryptOutgoing(String message) {
        if (!isOn() || message.isBlank()) {
            return message;
        }
        try {
            return ChatEncryption.encrypt(message, ChatUtilsConfig.get().encryptionPassphrase);
        } catch (ChatEncryption.EncryptionException failure) {
            // Sending the plaintext here would be the worst possible outcome: the player asked for
            // this to be unreadable. A message that visibly failed to send is the safe answer.
            warn(Component.translatable("chatutils.encrypt.failed", failure.getMessage()));
            return "";
        }
    }

    /**
     * Opens an incoming message, if it is ours to open.
     *
     * <p>Rebuilds the line rather than editing it: the plaintext is shorter than the ciphertext, and
     * the rest of the mod's pipeline works on a flattened copy whose length must not change. The
     * cost is the server's own formatting on that one line, which was wrapped around a run of Base64
     * anyway.
     *
     * @return the opened line, or the original when there is nothing here for us
     */
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
            // Someone else's passphrase, or not encrypted at all. Leave it exactly as it came.
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
