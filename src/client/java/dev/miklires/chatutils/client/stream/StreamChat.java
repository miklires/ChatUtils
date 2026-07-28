package dev.miklires.chatutils.client.stream;

import dev.miklires.chatutils.client.chat.ChatAuthor;
import dev.miklires.chatutils.client.chat.ChatDelivery;
import dev.miklires.chatutils.client.chat.ChatHistory;
import dev.miklires.chatutils.client.chat.MentionRule;
import dev.miklires.chatutils.client.chat.TextMatcher;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import dev.miklires.chatutils.client.notify.MentionNotifier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Brings Twitch and YouTube chat into the Minecraft chat — the Chatterino idea, without the second
 * window.
 *
 * <p>Everything the sources do happens on their own threads. This class is the only place the two
 * worlds touch, and it is driven entirely from the client tick: sources are started and stopped to
 * match the config, their queues are drained a few messages at a time, and each one is turned into a
 * chat line here on the game thread.
 *
 * <p>The rate cap is the whole reason the drain is bounded. A busy Twitch channel can outrun a
 * Minecraft chat by an order of magnitude, and a chat that is nothing but stream messages is worse
 * than no integration at all.
 *
 * <p>Reading only. Nothing typed in Minecraft is ever sent to a stream, and neither source is given
 * a way to do so.
 */
public final class StreamChat {

    private static final Logger LOGGER = LoggerFactory.getLogger("chatutils/stream");

    /** Failures raised on worker threads, surfaced on the next tick. */
    private static final ConcurrentLinkedQueue<String> failures = new ConcurrentLinkedQueue<>();

    /**
     * One platform's source and its own retry clock.
     *
     * <p>Per platform, not shared: a Twitch channel name with a typo in it must not hold YouTube's
     * connection back while it flaps, and vice versa.
     */
    private static final class Slot {

        @Nullable
        private StreamSource source;

        /** Ticks until the next attempt, so a dead source is not retried every single tick. */
        private int retryDelay;
    }

    private static final Slot TWITCH = new Slot();
    private static final Slot YOUTUBE = new Slot();

    private StreamChat() {
    }

    /** Called every client tick. Cheap when the feature is off, which is the common case. */
    public static void tick() {
        ChatUtilsConfig config = ChatUtilsConfig.get();

        if (!config.streamChatEnabled) {
            stopAll();
            return;
        }

        reportQueuedFailures();
        reconcile(config);
        deliver(config);
    }

    /** Stops everything — leaving a world should not leave sockets open to someone's stream. */
    public static void stopAll() {
        for (Slot slot : new Slot[]{TWITCH, YOUTUBE}) {
            stop(slot);
            slot.retryDelay = 0;
        }
        failures.clear();
    }

    /** Called from the worker threads; never touches the game from there. */
    static void reportFailure(StreamSource source, Throwable failure) {
        LOGGER.warn("{} stream source stopped: {}", source.name(), failure.toString());
        failures.add(source.name() + ": " + failure.getMessage());
    }

    // ------------------------------------------------------------------ lifecycle

    /**
     * Brings the running sources in line with the config.
     *
     * <p>A source whose target changed is torn down and rebuilt rather than reconfigured: a
     * connection is bound to the channel it joined, and pretending otherwise is how you end up
     * reading the wrong chat.
     */
    private static void reconcile(ChatUtilsConfig config) {
        // Normalised through the source's own rule: the comparison below is against a running
        // source's target, and comparing a raw config string to a normalised one would never match,
        // which would tear the connection down and rebuild it on every single tick.
        String channel = TwitchSource.normalise(config.twitchChannel);
        reconcileOne(TWITCH, channel, () -> new TwitchSource(channel));

        String video = config.youtubeVideoId == null ? "" : config.youtubeVideoId.trim();
        String key = config.youtubeApiKey == null ? "" : config.youtubeApiKey.trim();
        boolean youtubeReady = !video.isEmpty() && !key.isEmpty();
        reconcileOne(YOUTUBE, youtubeReady ? video : "", () -> new YouTubeSource(video, key));
    }

    private static void reconcileOne(Slot slot, String target,
                                     java.util.function.Supplier<StreamSource> factory) {
        if (target.isEmpty()) {
            stop(slot);
            return;
        }
        if (slot.source != null && slot.source.target().equals(target) && slot.source.isRunning()) {
            return;
        }
        if (slot.retryDelay > 0) {
            slot.retryDelay--;
            return;
        }
        if (slot.source != null) {
            stop(slot);
            // A source that just died gets a pause before the next attempt, so a wrong channel name
            // is a line in the log every few seconds rather than a reconnect storm.
            slot.retryDelay = 200;
            return;
        }

        slot.source = factory.get();
        slot.source.start();
    }

    private static void stop(Slot slot) {
        if (slot.source != null) {
            slot.source.stop();
            slot.source = null;
        }
    }

    // ------------------------------------------------------------------ delivery

    private static void deliver(ChatUtilsConfig config) {
        int budget = Math.max(1, config.streamMaxPerTick);
        List<StreamMessage> messages = new ArrayList<>();

        for (Slot slot : new Slot[]{TWITCH, YOUTUBE}) {
            if (slot.source != null) {
                messages.addAll(slot.source.drain(budget));
            }
        }

        for (StreamMessage message : messages) {
            show(message, config);
        }
    }

    private static void show(StreamMessage message, ChatUtilsConfig config) {
        boolean mention = config.streamMentions && mentions(message.text(), config);

        int tagColor = message.platform() == StreamMessage.Platform.TWITCH
                ? config.twitchTagColor
                : config.youtubeTagColor;
        int nameColor = message.color() != 0 ? message.color() : tagColor;

        // Every part states its own colour. A child with an unset colour inherits the parent's, so
        // an empty style here would paint the whole message in the platform's tag colour.
        Style body = mention
                ? Style.EMPTY.withColor(config.mentionHighlightColor).withBold(config.mentionHighlightBold)
                : Style.EMPTY.withColor(ChatFormatting.WHITE);

        MutableComponent line = Component.literal("[" + message.platform().tag() + "] ")
                .withStyle(style -> style.withColor(tagColor))
                .append(Component.literal(message.author())
                        .withStyle(style -> style.withColor(nameColor)))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(message.text()).withStyle(body));

        ChatDelivery.sendSystem(line);

        // Into the mod's own log too, so the search screen and the .txt export cover stream chat
        // the same way they cover the game's.
        String plain = "[" + message.platform().tag() + "] " + message.author() + ": " + message.text();
        ChatHistory.record(line, plain, message.author());

        if (mention) {
            MentionNotifier.notifyMention(new ChatAuthor(message.author(), null, null), null);
        }
    }

    /**
     * Whether the message trips one of the mention keywords.
     *
     * <p>Deliberately does not include the player's Minecraft name by default — the point of this is
     * to catch your stream handle being said, and those are rarely the same word. The keyword list
     * is shared with in-game mentions, which is what makes it configurable at all.
     */
    private static boolean mentions(String text, ChatUtilsConfig config) {
        for (String entry : config.mentionKeywords) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            // Entries carry an optional "|colour|sound" suffix; only the keyword part is matched.
            String keyword = MentionRule.parse(entry, config.mentionRegex).keyword();
            if (keyword.isBlank()) {
                continue;
            }
            boolean hit = config.mentionRegex
                    ? !TextMatcher.findRegex(text, keyword, config.mentionCaseSensitive).isEmpty()
                    : !TextMatcher.findLiteral(text, keyword, config.mentionCaseSensitive,
                            config.mentionWholeWord).isEmpty();
            if (hit) {
                return true;
            }
        }
        return false;
    }

    private static void reportQueuedFailures() {
        String failure;
        while ((failure = failures.poll()) != null) {
            if (Minecraft.getInstance().level == null) {
                continue;
            }
            ChatDelivery.sendSystem(Component.translatable("chatutils.stream.failed", failure)
                    .withStyle(ChatFormatting.RED));
        }
    }
}
