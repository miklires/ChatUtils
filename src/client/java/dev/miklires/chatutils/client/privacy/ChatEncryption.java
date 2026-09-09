package dev.miklires.chatutils.client.privacy;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Encrypts chat between people who share a passphrase.
 *
 * <p>What this is for: the server relays your messages but cannot read them, and neither can anyone
 * without the passphrase. What it is <em>not</em>: privacy from the people you gave the passphrase
 * to, or from anyone who obtains it — there is one key, shared by everyone in the conversation, and
 * a chat message you can decrypt is a chat message they can decrypt. It is a curtain, not a vault.
 *
 * <p>Everyone in the conversation needs this mod and the same passphrase. To everyone else the line
 * is a run of Base64, which is the honest cost and the reason this is off by default.
 *
 * <p>AES-GCM, so a tampered message fails to decrypt rather than decrypting to something else. A
 * fresh random nonce per message travels in front of the ciphertext; the key comes from the
 * passphrase through PBKDF2, which is what makes a human-typed phrase costly to guess at.
 *
 * <p>Deliberately plain Base64 with a visible marker rather than something disguised as normal text.
 * A server that forbids encrypted chat should be able to see that this is encrypted chat and say so,
 * instead of being quietly worked around.
 */
public final class ChatEncryption {

    /** Not in the Base64 alphabet, so the boundary is never ambiguous. */
    public static final String MARKER = "##";

    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    /**
     * Fixed, and not a secret. A salt exists to stop one precomputed table from covering every user
     * of every program; it does not need to be unpredictable, and it cannot be random here because
     * both sides derive the same key from the passphrase alone with nothing to exchange.
     */
    private static final byte[] SALT = "chatutils-chat-encryption-v1".getBytes(StandardCharsets.UTF_8);

    private static final int ITERATIONS = 200_000;
    private static final int MAX_TOKEN_LENGTH = 4096;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static byte[] cachedFingerprint;
    private static SecretKey cachedKey;

    private ChatEncryption() {
    }

    /** Thrown with a message meant to be shown to the player. */
    public static class EncryptionException extends RuntimeException {
        public EncryptionException(String message) {
            super(message);
        }
    }

    public static boolean looksEncrypted(String token) {
        return token != null && token.startsWith(MARKER) && token.length() > MARKER.length();
    }

    /** @return the marker followed by Base64 of nonce and ciphertext */
    public static String encrypt(String plaintext, String passphrase) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new EncryptionException("nothing to encrypt");
        }

        try {
            byte[] nonce = new byte[NONCE_BYTES];
            RANDOM.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(passphrase), new GCMParameterSpec(TAG_BITS, nonce));
            byte[] plain = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext;
            try {
                ciphertext = cipher.doFinal(plain);
            } finally {
                Arrays.fill(plain, (byte) 0);
            }

            byte[] joined = new byte[nonce.length + ciphertext.length];
            System.arraycopy(nonce, 0, joined, 0, nonce.length);
            System.arraycopy(ciphertext, 0, joined, nonce.length, ciphertext.length);

            return MARKER + Base64.getEncoder().withoutPadding().encodeToString(joined);
        } catch (EncryptionException known) {
            throw known;
        } catch (Exception failure) {
            throw new EncryptionException(failure.getClass().getSimpleName());
        }
    }

    /**
     * @return the plaintext, or null when this is not ours to read — a message from someone using a
     *         different passphrase is indistinguishable from noise, and that is not an error
     */
    public static String decrypt(String token, String passphrase) {
        if (!looksEncrypted(token)) {
            return null;
        }
        if (token.length() > MAX_TOKEN_LENGTH) {
            return null;
        }

        try {
            byte[] joined = Base64.getDecoder().decode(token.substring(MARKER.length()));
            if (joined.length <= NONCE_BYTES) {
                return null;
            }

            byte[] nonce = new byte[NONCE_BYTES];
            System.arraycopy(joined, 0, nonce, 0, NONCE_BYTES);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(passphrase), new GCMParameterSpec(TAG_BITS, nonce));
            byte[] plain = cipher.doFinal(joined, NONCE_BYTES, joined.length - NONCE_BYTES);
            try {
                return new String(plain, StandardCharsets.UTF_8);
            } finally {
                Arrays.fill(plain, (byte) 0);
            }
        } catch (Exception notForUs) {
            // Wrong key, truncated by a chat limit, or never encrypted at all. All the same to us.
            return null;
        }
    }

    /**
     * How long a message may be before its encrypted form will not fit in a chat packet.
     *
     * <p>Worth knowing in advance: an encrypted message cut off at the limit does not arrive
     * damaged, it fails to decrypt entirely, and the sender would have no idea why.
     */
    public static int maxPlaintextLength(int chatLimit) {
        // Base64 is four characters per three bytes, and the payload is the nonce, the plaintext and
        // GCM's authentication tag.
        int payload = (chatLimit - MARKER.length()) / 4 * 3;
        return Math.max(0, payload - NONCE_BYTES - TAG_BITS / 8);
    }

    public static boolean fits(String plaintext, int chatLimit) {
        if (plaintext == null || chatLimit < MARKER.length()) {
            return false;
        }
        int payloadBytes = NONCE_BYTES + TAG_BITS / 8
                + plaintext.getBytes(StandardCharsets.UTF_8).length;
        int encodedLength = (payloadBytes * 4 + 2) / 3;
        return MARKER.length() + encodedLength <= chatLimit;
    }

    private static synchronized SecretKey key(String passphrase) {
        if (passphrase == null || passphrase.isBlank()) {
            throw new EncryptionException("no passphrase set");
        }
        byte[] fingerprint = fingerprint(passphrase);
        if (cachedKey != null && MessageDigest.isEqual(fingerprint, cachedFingerprint)) {
            return cachedKey;
        }

        PBEKeySpec spec = null;
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            spec = new PBEKeySpec(passphrase.toCharArray(), SALT, ITERATIONS, 256);
            cachedKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
            if (cachedFingerprint != null) {
                Arrays.fill(cachedFingerprint, (byte) 0);
            }
            cachedFingerprint = fingerprint;
            return cachedKey;
        } catch (Exception failure) {
            Arrays.fill(fingerprint, (byte) 0);
            throw new EncryptionException(failure.getClass().getSimpleName());
        } finally {
            if (spec != null) {
                spec.clearPassword();
            }
        }
    }

    private static byte[] fingerprint(String passphrase) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(passphrase.getBytes(StandardCharsets.UTF_8));
        } catch (Exception failure) {
            throw new EncryptionException(failure.getClass().getSimpleName());
        }
    }
}
