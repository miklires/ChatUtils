package dev.miklires.chatutils.client.chat;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.network.chat.Component;

/**
 * Merges identical consecutive messages into one line with an {@code (x4)} counter.
 *
 * <p>Rather than editing lines already on screen — which means reaching into vanilla's private
 * message lists and breaks on every chat refactor — a message is held back for a short window. Any
 * identical repeat that lands inside the window bumps the counter instead of adding a line, and the
 * whole thing is drawn once the window closes. Ordering is preserved because a *different* message
 * flushes the pending one before taking its place.
 */
public final class SpamCompactor {

    /**
     * How many windows a message may be held for while repeats keep arriving. Without a cap, a line
     * repeating faster than the window keeps pushing its own deadline back and never appears at all
     * — the exact case the feature exists for.
     */
    private static final int MAX_WINDOWS = 8;

    private static String pendingKey;
    private static ChatDelivery.Line pending;
    private static int pendingCount;
    private static long pendingSince;
    private static long pendingFirstAt;

    private SpamCompactor() {
    }

    /** Takes ownership of a line, merging it into the pending one when it is a repeat. */
    public static void hold(String key, ChatDelivery.Line line) {
        long now = System.currentTimeMillis();
        if (pendingKey != null && pendingKey.equals(key)) {
            pendingCount++;
            pendingSince = now;
            return;
        }

        flush();
        pendingKey = key;
        pending = line;
        pendingCount = 1;
        pendingSince = now;
        pendingFirstAt = now;
    }

    /** Called every client tick: releases the pending message once its window has elapsed. */
    public static void tick() {
        if (pendingKey == null) {
            return;
        }
        long window = Math.max(0, ChatUtilsConfig.get().compactSpamWindowMs);
        long now = System.currentTimeMillis();
        // Either the repeats stopped, or they have gone on long enough that the player deserves to
        // see what is being spammed rather than a chat that stays silent while it happens.
        if (now - pendingSince >= window || now - pendingFirstAt >= window * MAX_WINDOWS) {
            flush();
        }
    }

    /** Draws the pending message, with a counter when it was repeated. */
    public static void flush() {
        if (pendingKey == null) {
            return;
        }

        ChatDelivery.Line line = pending;
        if (pendingCount > 1) {
            int color = ChatUtilsConfig.get().compactSpamCounterColor;
            Component counted = Component.empty()
                    .append(line.component())
                    .append(Component.literal(" (x" + pendingCount + ")")
                            .withStyle(style -> style.withColor(color)));
            line = new ChatDelivery.Line(counted, line.mention(), line.source(), line.owner());
        }

        reset();
        ChatDelivery.deliver(line);
    }

    /** Drops anything pending without drawing it — used when the world unloads. */
    public static void reset() {
        pendingKey = null;
        pending = null;
        pendingCount = 0;
        pendingFirstAt = 0L;
    }
}
