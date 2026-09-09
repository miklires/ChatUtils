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

public final class StreamChat {

    private static final Logger LOGGER = LoggerFactory.getLogger("chatutils/stream");

    private static final ConcurrentLinkedQueue<String> failures = new ConcurrentLinkedQueue<>();

    private static final class Slot {

        @Nullable
        private StreamSource source;

        private int retryDelay;
    }

    private static final Slot TWITCH = new Slot();
    private static final Slot YOUTUBE = new Slot();

    private StreamChat() {
    }

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

    public static void stopAll() {
        for (Slot slot : new Slot[]{TWITCH, YOUTUBE}) {
            stop(slot);
            slot.retryDelay = 0;
        }
        failures.clear();
    }

    static void reportFailure(StreamSource source, Throwable e) {
        LOGGER.warn("{} stream source stopped: {}", source.name(), e.toString());
        failures.add(source.name() + ": " + e.getMessage());
    }

    private static void reconcile(ChatUtilsConfig config) {
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

        String plain = "[" + message.platform().tag() + "] " + message.author() + ": " + message.text();
        ChatHistory.record(line, plain, message.author());

        if (mention) {
            MentionNotifier.notifyMention(new ChatAuthor(message.author(), null, null), null);
        }
    }

    private static boolean mentions(String text, ChatUtilsConfig config) {
        for (String entry : config.mentionKeywords) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
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
        String e;
        while ((e = failures.poll()) != null) {
            if (Minecraft.getInstance().level == null) {
                continue;
            }
            ChatDelivery.sendSystem(Component.translatable("chatutils.stream.failed", e)
                    .withStyle(ChatFormatting.RED));
        }
    }
}
