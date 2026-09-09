package dev.miklires.chatutils.client.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatUtilsConfigTest {
    @Test void normalizesUnsafeManualValues() {
        ChatUtilsConfig config = new ChatUtilsConfig();
        config.historySize = Integer.MAX_VALUE;
        config.streamMaxPerTick = -5;
        config.messageSoundVolume = Float.NaN;
        config.chatScale = Double.POSITIVE_INFINITY;
        config.filterMode = null;
        config.mentionKeywords = new ArrayList<>(Arrays.asList(null, "  name  ", "name", ""));

        config.normalize();

        assertEquals(10_000, config.historySize);
        assertEquals(1, config.streamMaxPerTick);
        assertEquals(0.0f, config.messageSoundVolume);
        assertEquals(0.0, config.chatScale);
        assertEquals(FilterMode.CENSOR, config.filterMode);
        assertEquals(java.util.List.of("name"), config.mentionKeywords);
    }

    @Test void limitsListAndEntrySizes() {
        ChatUtilsConfig config = new ChatUtilsConfig();
        config.customMacros = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            config.customMacros.add("key" + i + "=" + "x".repeat(2000));
        }

        config.normalize();

        assertEquals(256, config.customMacros.size());
        assertTrue(config.customMacros.stream().allMatch(value -> value.length() <= 1024));
    }
}
