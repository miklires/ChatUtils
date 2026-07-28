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

/**
 * Replies to phrases automatically: someone says {@code gg}, you say {@code ggwp}.
 *
 * <p>The obvious feature; the dangerous one. Two clients with mirrored rules will talk to each other
 * forever, and a server cannot tell an eager player from a bot — people are banned for this. So the
 * matching is the small half of this class and the brakes are the large one, and it is off by
 * default with an empty list.
 *
 * <p>Four independent brakes, because any one of them alone has a hole:
 *
 * <ul>
 *   <li>never answer yourself, which is the instant loop;
 *   <li>never answer a line that is one of your own recent replies, which is the loop with another
 *       player standing in the middle;
 *   <li>a per-rule cooldown, so one phrase repeated does not become a stream;
 *   <li>a hard ceiling per minute across all rules, which no setting can raise — the brake that
 *       still holds when the first three are outwitted by a combination nobody thought of.
 * </ul>
 *
 * <p>The decision half takes the time as an argument and keeps its own state, so all of it can be
 * tested without a game running.
 */
public final class AutoResponder {

    /** @param response what to send when {@code trigger} appears in a message */
    public record Rule(String trigger, String response) {
    }

    /** No setting raises this. It is the brake of last resort, not a preference. */
    private static final int MAX_PER_MINUTE = 4;

    private static final long MINUTE = 60_000L;

    /** Replies we sent recently, so one coming back at us is recognised and ignored. */
    private static final int REMEMBERED_REPLIES = 8;

    /** Sent from the tick, not from inside the receive event that produced it. */
    @Nullable
    private static String pending;

    private static final Map<String, Long> lastFired = new HashMap<>();
    private static final Deque<Long> sentAt = new ArrayDeque<>();
    private static final Deque<String> recentReplies = new ArrayDeque<>();

    private AutoResponder() {
    }

    /** Entries are {@code phrase | response}; anything without both halves is skipped. */
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

    /**
     * The reply a message earns, or null for silence.
     *
     * @param body   the message with the sender's name already stripped
     * @param fromMe whether this is the player's own message, which is never answered
     */
    @Nullable
    public static String replyTo(String body, boolean fromMe, List<Rule> rules, long now, int cooldownMs) {
        if (fromMe || body == null || body.isBlank() || rules.isEmpty()) {
            return null;
        }

        // Our own words coming back at us, relayed by whoever we were talking to. This is the loop
        // that the self-check cannot see, and the one that actually happens.
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

    /**
     * Whole words only, so a rule for {@code gg} does not fire on "logging" — the difference between
     * a reply and a mystery.
     */
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

    /**
     * Sends whatever the last message earned.
     *
     * <p>From the tick rather than from the pipeline: replying inside the event that is still
     * handling an incoming message means sending while the game is mid-receive, and it goes out
     * through the same path as typing, so macros, the font and encryption all still apply.
     */
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

    /** Reads the config, decides, and queues. Called by the pipeline for messages from others. */
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
