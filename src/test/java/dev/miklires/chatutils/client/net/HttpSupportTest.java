package dev.miklires.chatutils.client.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpSupportTest {
    @Test void acceptsSecureAndLocalEndpoints() {
        assertEquals("https", HttpSupport.translationEndpoint("https://example.org/translate").getScheme());
        assertEquals("http", HttpSupport.translationEndpoint("http://localhost:5000/translate").getScheme());
        assertEquals("http", HttpSupport.translationEndpoint("http://127.0.0.1:5000/translate").getScheme());
    }

    @Test void rejectsUnsafeEndpoints() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpSupport.translationEndpoint("http://example.org/translate"));
        assertThrows(IllegalArgumentException.class,
                () -> HttpSupport.translationEndpoint("https://user@example.org/translate"));
        assertThrows(IllegalArgumentException.class,
                () -> HttpSupport.translationEndpoint("file:///tmp/translate"));
    }

    @Test void sanitizesRemoteErrorText() {
        assertEquals("bad key", HttpSupport.safeReason("bad\nkey"));
        assertEquals(240, HttpSupport.safeReason("x".repeat(300)).length());
    }
}
