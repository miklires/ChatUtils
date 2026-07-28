package dev.miklires.chatutils.client.chat.processor;

import dev.miklires.chatutils.client.chat.ChatMessage;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import dev.miklires.chatutils.client.config.TimestampMode;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Stamps the local arrival time onto a message, inline or as a tooltip. */
public final class TimestampProcessor implements ChatProcessor {

    private static final DateTimeFormatter FALLBACK = DateTimeFormatter.ofPattern("HH:mm");

    private String cachedPattern;
    private DateTimeFormatter cachedFormatter;

    @Override
    public void apply(ChatMessage message, ChatUtilsConfig config) {
        if (config.timestampMode == TimestampMode.OFF) {
            return;
        }

        String time = LocalTime.now().format(formatter(config.timestampFormat));

        if (config.timestampMode == TimestampMode.INLINE) {
            message.setPrefix(Component.literal("[" + time + "] ")
                    .withStyle(style -> style.withColor(config.timestampColor)));
        } else {
            message.setWholeLineStyle(style -> style.withHoverEvent(
                    new HoverEvent.ShowText(Component.literal(time))));
        }
    }

    /** Compiling a {@link DateTimeFormatter} per message would be wasteful; the pattern rarely changes. */
    private DateTimeFormatter formatter(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return FALLBACK;
        }
        if (!pattern.equals(cachedPattern)) {
            cachedPattern = pattern;
            try {
                cachedFormatter = DateTimeFormatter.ofPattern(pattern);
            } catch (IllegalArgumentException | DateTimeParseException invalid) {
                cachedFormatter = FALLBACK;
            }
        }
        return cachedFormatter;
    }
}
