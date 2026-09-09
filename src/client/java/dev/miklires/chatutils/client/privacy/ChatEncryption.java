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

public final class ChatEncryption {

    public static final String MARKER = "##";

    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    private static final byte[] SALT = "chatutils-chat-encryption-v1".getBytes(StandardCharsets.UTF_8);

    private static final int ITERATIONS = 200_000;
    private static final int MAX_TOKEN_LENGTH = 4096;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static byte[] cachedFingerprint;
    private static SecretKey cachedKey;

    private ChatEncryption() {
    }

    public static class EncryptionException extends RuntimeException {
        public EncryptionException(String message) {
            super(message);
        }
    }

    public static boolean looksEncrypted(String token) {
        return token != null && token.startsWith(MARKER) && token.length() > MARKER.length();
    }

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
        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException(e.getClass().getSimpleName());
        }
    }

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
        } catch (Exception e) {
            return null;
        }
    }

    public static int maxPlaintextLength(int chatLimit) {
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
        } catch (Exception e) {
            Arrays.fill(fingerprint, (byte) 0);
            throw new EncryptionException(e.getClass().getSimpleName());
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
        } catch (Exception e) {
            throw new EncryptionException(e.getClass().getSimpleName());
        }
    }
}
