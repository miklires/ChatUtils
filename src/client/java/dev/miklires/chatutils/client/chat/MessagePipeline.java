package dev.miklires.chatutils.client.chat;

import com.mojang.authlib.GameProfile;
import dev.miklires.chatutils.client.chat.processor.BlacklistProcessor;
import dev.miklires.chatutils.client.chat.processor.ChatProcessor;
import dev.miklires.chatutils.client.chat.processor.FilterProcessor;
import dev.miklires.chatutils.client.chat.processor.FriendProcessor;
import dev.miklires.chatutils.client.chat.processor.LinkProcessor;
import dev.miklires.chatutils.client.chat.processor.MentionProcessor;
import dev.miklires.chatutils.client.chat.processor.NameHiderProcessor;
import dev.miklires.chatutils.client.chat.processor.TimestampProcessor;
import dev.miklires.chatutils.client.chat.processor.WordCopyProcessor;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import dev.miklires.chatutils.client.notify.MentionNotifier;
import dev.miklires.chatutils.client.privacy.EncryptedChat;
import dev.miklires.chatutils.client.notify.SoundPlayer;
import dev.miklires.chatutils.client.render.ChatBubbles;
import dev.miklires.chatutils.client.render.ChatHeads;
import dev.miklires.chatutils.client.render.ChatVisibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class MessagePipeline {

    private record Result(Component component, boolean cancelled, @Nullable ChatDelivery.Line line) {
    }

    private static final List<ChatProcessor> PROCESSORS = List.of(
            new BlacklistProcessor(),
            new WordCopyProcessor(),
            new FilterProcessor(),
            new FriendProcessor(),
            new MentionProcessor(),
            new NameHiderProcessor(),
            new LinkProcessor(),
            new TimestampProcessor()
    );

    private static Component lastInput;
    private static Result lastResult;

    private static int blankRun;
    private static long lastBlankAt;

    private MessagePipeline() {
    }

    public static boolean allowPlayerChat(Component message, @Nullable GameProfile sender) {
        Result result = compute(message, sender);
        if (result.cancelled()) {
            return false;
        }
        if (result.component() == message) {
            return true;
        }
        ChatDelivery.deliver(result.line());
        return false;
    }

    public static boolean allowSystemMessage(Component message) {
        return !remember(message, compute(message, null)).cancelled();
    }

    public static Component modifySystemMessage(Component message) {
        if (message == lastInput && lastResult != null) {
            return lastResult.component();
        }
        return message;
    }

    private static Result compute(Component incoming, @Nullable GameProfile sender) {
        ChatUtilsConfig config = ChatUtilsConfig.get();

        incoming = EncryptedChat.decryptIncoming(incoming);

        ChatMessage message = new ChatMessage(incoming);
        message.setAuthor(resolveAuthor(message, sender));

        GuiMessageSource source = sender == null ? GuiMessageSource.SYSTEM_SERVER : GuiMessageSource.PLAYER;

        if (isClearChatFlood(message.plain(), config)) {
            return dropped(incoming);
        }

        for (ChatProcessor processor : PROCESSORS) {
            processor.apply(message, config);
            if (message.isCancelled()) {
                return dropped(incoming);
            }
        }

        Component built = message.build();
        boolean mention = message.isMention();
        ChatDelivery.Line line = new ChatDelivery.Line(built, mention, source, message.author().info());
        ChatHistory.record(built, message.plain(), message.author().name());

        ChatHeads.setPendingOwner(message.author().info());

        String body = ChatBubbles.stripAuthor(message.plain(), message.author().name());
        if (message.author().id() != null) {
            ChatBubbles.record(message.author().id(), body);
        }
        if (message.author().isKnown()) {
            AutoResponder.consider(body, AuthorResolver.isLocalPlayer(message.author()));
        }

        if (mention) {
            MentionNotifier.notifyMention(message.author(), message.mentionRule());
            ChatVisibility.onMention();
        } else if (config.messageSoundEnabled
                && (!config.messageSoundOnlyPlayers || message.author().isKnown())) {
            SoundPlayer.play(config.messageSoundId, config.messageSoundVolume, config.messageSoundPitch);
        }

        if (config.compactSpamEnabled) {
            SpamCompactor.hold(message.plain(), line);
            return new Result(incoming, true, line);
        }
        return new Result(built, false, line);
    }

    private static Result dropped(Component incoming) {
        ChatHeads.setPendingOwner(null);
        return new Result(incoming, true, null);
    }

    private static ChatAuthor resolveAuthor(ChatMessage message, @Nullable GameProfile sender) {
        if (sender == null) {
            return AuthorResolver.resolve(message.plain());
        }
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return new ChatAuthor(
                sender.name(),
                sender.id(),
                connection == null ? null : connection.getPlayerInfo(sender.id()));
    }

    private static boolean isClearChatFlood(String plain, ChatUtilsConfig config) {
        if (!plain.isBlank()) {
            blankRun = 0;
            return false;
        }
        if (!config.antiClearEnabled) {
            return false;
        }

        long now = System.currentTimeMillis();
        blankRun = (now - lastBlankAt < 1000L) ? blankRun + 1 : 1;
        lastBlankAt = now;
        return blankRun > Math.max(1, config.antiClearThreshold);
    }

    private static Result remember(Component input, Result result) {
        lastInput = input;
        lastResult = result;
        return result;
    }

    public static void reset() {
        lastInput = null;
        lastResult = null;
        blankRun = 0;
    }
}
