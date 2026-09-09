package dev.miklires.chatutils.client.chat;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.network.chat.Component;

public final class SpamCompactor {

    private static final int MAX_WINDOWS = 8;

    private static String pendingKey;
    private static ChatDelivery.Line pending;
    private static int pendingCount;
    private static long pendingSince;
    private static long pendingFirstAt;

    private SpamCompactor() {
    }

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

    public static void tick() {
        if (pendingKey == null) {
            return;
        }
        long window = Math.max(0, ChatUtilsConfig.get().compactSpamWindowMs);
        long now = System.currentTimeMillis();
        if (now - pendingSince >= window || now - pendingFirstAt >= window * MAX_WINDOWS) {
            flush();
        }
    }

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

    public static void reset() {
        pendingKey = null;
        pending = null;
        pendingCount = 0;
        pendingFirstAt = 0L;
    }
}
