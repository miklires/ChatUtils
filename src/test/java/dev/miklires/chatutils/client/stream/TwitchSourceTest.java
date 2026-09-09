package dev.miklires.chatutils.client.stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TwitchSourceTest {
    @Test void normalizesValidChannelNames() {
        assertEquals("channel_name", TwitchSource.normalise("  #Channel_Name "));
    }

    @Test void rejectsMalformedChannelNames() {
        assertEquals("", TwitchSource.normalise("bad#channel"));
        assertEquals("", TwitchSource.normalise("channel/name"));
        assertEquals("", TwitchSource.normalise("x".repeat(26)));
    }
}
