package dev.miklires.chatutils.client.privacy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatEncryptionTest {
    @Test void roundTripsUtf8WithFreshNonces() {
        String first = ChatEncryption.encrypt("Привет, world", "correct horse battery staple");
        String second = ChatEncryption.encrypt("Привет, world", "correct horse battery staple");
        assertNotEquals(first, second);
        assertEquals("Привет, world", ChatEncryption.decrypt(first, "correct horse battery staple"));
    }

    @Test void rejectsWrongKeyAndTampering() {
        String token = ChatEncryption.encrypt("secret", "key one");
        assertNull(ChatEncryption.decrypt(token, "key two"));
        int changedIndex = ChatEncryption.MARKER.length() + 5;
        char replacement = token.charAt(changedIndex) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, changedIndex) + replacement + token.substring(changedIndex + 1);
        assertNull(ChatEncryption.decrypt(tampered, "key one"));
    }

    @Test void calculatesPacketLimitAndRejectsBlankInputs() {
        int limit = ChatEncryption.maxPlaintextLength(256);
        assertTrue(ChatEncryption.encrypt("x".repeat(limit), "passphrase").length() <= 256);
        assertTrue(ChatEncryption.fits("x".repeat(limit), 256));
        assertFalse(ChatEncryption.fits("😀".repeat(limit), 256));
        assertNull(ChatEncryption.decrypt("##" + "A".repeat(5000), "passphrase"));
        assertThrows(ChatEncryption.EncryptionException.class, () -> ChatEncryption.encrypt("", "passphrase"));
        assertThrows(ChatEncryption.EncryptionException.class, () -> ChatEncryption.encrypt("text", ""));
    }
}
