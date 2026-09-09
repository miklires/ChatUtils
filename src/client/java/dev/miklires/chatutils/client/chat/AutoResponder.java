package dev.miklires.chatutils.client.chat;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AutoResponder {

    public record Rule(String trigger, String response) {
    }

    private static final int MAX_PER_MINUTE = 4;

    private static final long MINUTE = 60_000L;

    private static final int REMEMBERED_REPLIES = 8;

    @Nullable
    private static String pending;

    private static final Map<String, Long> lastFired = new HashMap<>();
    private static final Deque<Long> sentAt = new ArrayDeque<>();
    private static final Deque<String> recentReplies = new ArrayDeque<>();

    private AutoResponder() {
    }

    public static List<Rule> parse(List<String> entries) {
        List<Rule> rules = new ArrayList<>();
        if (entries == null) {
            return rules;
        }

        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            int split = entry.indexOf('|');
            if (split <= 0) {
                continue;
            }
            String trigger = entry.substring(0, split).trim();
            String response = entry.substring(split + 1).trim();
            if (!trigger.isEmpty() && !response.isEmpty()) {
                rules.add(new Rule(trigger, response));
            }
        }
        return rules;
    }

    @Nullable
    public static String replyTo(String body, boolean fromMe, List<Rule> rules, long now, int cooldownMs) {
        if (fromMe || body == null || body.isBlank() || rules.isEmpty()) {
            return null;
        }

        String normalised = body.trim().toLowerCase(Locale.ROOT);
        for (String recent : recentReplies) {
            if (normalised.contains(recent)) {
                return null;
            }
        }

        for (Rule rule : rules) {
            if (!mentions(body, rule.trigger())) {
                continue;
            }

            Long previous = lastFired.get(rule.trigger().toLowerCase(Locale.ROOT));
            if (previous != null && now - previous < Math.max(0, cooldownMs)) {
                return null;
            }
            if (!withinCeiling(now)) {
                return null;
            }

            record(rule, now);
            return rule.response();
        }
        return null;
    }

    private static boolean mentions(String body, String trigger) {
        return !TextMatcher.findLiteral(body, trigger, false, true).isEmpty();
    }

    private static boolean withinCeiling(long now) {
        while (!sentAt.isEmpty() && now - sentAt.peekFirst() > MINUTE) {
            sentAt.removeFirst();
        }
        return sentAt.size() < MAX_PER_MINUTE;
    }

    private static void record(Rule rule, long now) {
        lastFired.put(rule.trigger().toLowerCase(Locale.ROOT), now);
        sentAt.addLast(now);

        recentReplies.addLast(rule.response().trim().toLowerCase(Locale.ROOT));
        while (recentReplies.size() > REMEMBERED_REPLIES) {
            recentReplies.removeFirst();
        }
    }

    public static void tick() {
        String reply = pending;
        pending = null;
        if (reply == null) {
            return;
        }

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null && Minecraft.getInstance().player != null) {
            connection.sendChat(reply);
        }
    }

    public static void consider(String body, boolean fromMe) {
        ChatUtilsConfig config = ChatUtilsConfig.get();
        if (!config.autoReplyEnabled) {
            return;
        }

        String reply = replyTo(body, fromMe, parse(config.autoReplyRules),
                System.currentTimeMillis(), config.autoReplyCooldownMs);
        if (reply != null) {
            pending = reply;
        }
    }

    public static void reset() {
        pending = null;
        lastFired.clear();
        sentAt.clear();
        recentReplies.clear();
    }
}
