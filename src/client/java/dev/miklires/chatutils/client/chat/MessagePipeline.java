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

/**
 * Runs every incoming chat line through the processors and decides what the game should do with it.
 *
 * <p>Driven by Fabric's {@code ClientReceiveMessageEvents}, which fire at packet level. The two
 * kinds of message are handled differently because the API offers different hooks:
 *
 * <ul>
 *   <li><b>System messages</b> have both {@code ALLOW_GAME} and {@code MODIFY_GAME}, so the line can
 *       be dropped or swapped in place. The verdict is computed in {@code ALLOW} and reused by
 *       {@code MODIFY}.
 *   <li><b>Player chat</b> only has {@code ALLOW_CHAT} — there is no modify hook, since rewriting a
 *       signed message would contradict its signature. So a line we changed is dropped and re-posted
 *       by us instead. A line we did <em>not</em> change is allowed through untouched, which keeps
 *       its signature and tag intact for the messages that do not need us.
 * </ul>
 *
 * <p>Features that take over delivery — spam compaction and hidden chat — also drop the line and
 * push it into the chat themselves. That path goes straight to {@code ChatComponent}, below the
 * events, so it cannot loop back around.
 */
public final class MessagePipeline {

    /** @param line the finished line, ready to post; null when the message was dropped */
    private record Result(Component component, boolean cancelled, @Nullable ChatDelivery.Line line) {
    }

    private static final List<ChatProcessor> PROCESSORS = List.of(
            new BlacklistProcessor(),
            // Before the filter and the linkifier: both overwrite the click on the ranges they own,
            // and "reveal this word" and "open this link" must beat "copy this word".
            new WordCopyProcessor(),
            new FilterProcessor(),
            new FriendProcessor(),
            new MentionProcessor(),
            // After the mention: masking the name first would mean never being told it was said.
            new NameHiderProcessor(),
            new LinkProcessor(),
            new TimestampProcessor()
    );

    private static Component lastInput;
    private static Result lastResult;

    /** Consecutive blank lines seen so far, for the anti-clear-chat guard. */
    private static int blankRun;
    private static long lastBlankAt;

    private MessagePipeline() {
    }

    /**
     * Handles a player chat message, which can only be allowed or dropped.
     *
     * @param sender the verified sender the server signed the message with
     * @return {@code false} to stop the line from reaching the chat
     */
    public static boolean allowPlayerChat(Component message, @Nullable GameProfile sender) {
        Result result = compute(message, sender);
        if (result.cancelled()) {
            return false;
        }
        if (result.component() == message) {
            // Untouched: let vanilla add it so the signature and message tag survive.
            return true;
        }
        ChatDelivery.deliver(result.line());
        return false;
    }

    /** {@code ALLOW_GAME}: computes the verdict that {@link #modifySystemMessage} then reuses. */
    public static boolean allowSystemMessage(Component message) {
        return !remember(message, compute(message, null)).cancelled();
    }

    /**
     * {@code MODIFY_GAME}: the transformed line, reusing the verdict from the allow phase.
     *
     * <p>Never recomputes. {@code ALLOW} always runs first for the same message, so a miss means
     * another mod replaced the instance in between — and running the pipeline again would notify
     * twice and log the message twice. Passing it through unchanged loses this mod's formatting on a
     * rare line; recomputing would corrupt the history on it.
     */
    public static Component modifySystemMessage(Component message) {
        if (message == lastInput && lastResult != null) {
            return lastResult.component();
        }
        return message;
    }

    private static Result compute(Component incoming, @Nullable GameProfile sender) {
        ChatUtilsConfig config = ChatUtilsConfig.get();

        // Opened before anything else reads the text, so filters, mentions and the log all see what
        // was said rather than the Base64 it travelled as.
        incoming = EncryptedChat.decryptIncoming(incoming);

        ChatMessage message = new ChatMessage(incoming);
        message.setAuthor(resolveAuthor(message, sender));

        // A signed sender means this really was a player talking; anything else came from the server.
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

        // The chat has no notion of a sender, so the head has to be announced before the message
        // reaches it. Cleared on the cancelled paths above so a dropped line cannot lend its head
        // to whatever arrives next.
        ChatHeads.setPendingOwner(message.author().info());

        // Bubbles and the auto-reply both want the message without the sender's name in front of
        // it; the author is only known here, and the pipeline is the one place that has both halves.
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

    /**
     * A message that never reaches the chat. Clears the pending head owner too, or the dropped line
     * would lend its head to whatever arrives next.
     */
    private static Result dropped(Component incoming) {
        ChatHeads.setPendingOwner(null);
        return new Result(incoming, true, null);
    }

    /**
     * Prefers the signed sender the server gave us; only guesses from the text when there is none,
     * which is the case for system messages and for servers that rewrite chat as plain text.
     */
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

    /**
     * Detects the wall of empty lines servers use to wipe your chat. A stray blank line is normal
     * formatting, so only a fast run of them counts as a clear.
     */
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

    /** Clears per-session state when leaving a world. */
    public static void reset() {
        lastInput = null;
        lastResult = null;
        blankRun = 0;
    }
}
