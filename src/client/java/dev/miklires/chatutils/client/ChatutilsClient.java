package dev.miklires.chatutils.client;

import dev.miklires.chatutils.client.calc.ChatCalculator;
import dev.miklires.chatutils.client.chat.MessagePipeline;
import dev.miklires.chatutils.client.chat.AutoResponder;
import dev.miklires.chatutils.client.chat.ChatWindowOptions;
import dev.miklires.chatutils.client.chat.SessionDivider;
import dev.miklires.chatutils.client.chat.SpamCompactor;
import dev.miklires.chatutils.client.command.ChatUtilsCommands;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import dev.miklires.chatutils.client.input.ChatUtilsKeys;
import dev.miklires.chatutils.client.input.CommandKeys;
import dev.miklires.chatutils.client.macro.MacroExpander;
import dev.miklires.chatutils.client.notify.MentionNotifier;
import dev.miklires.chatutils.client.notify.SoundPlayer;
import dev.miklires.chatutils.client.privacy.EncryptedChat;
import dev.miklires.chatutils.client.privacy.NoReports;
import dev.miklires.chatutils.client.render.BubbleRenderer;
import dev.miklires.chatutils.client.render.ChatBubbles;
import dev.miklires.chatutils.client.render.ChatHeads;
import dev.miklires.chatutils.client.render.ChatVisibility;
import dev.miklires.chatutils.client.screen.SymbolSuggestions;
import dev.miklires.chatutils.client.screen.SymbolsEverywhere;
import dev.miklires.chatutils.client.screen.VanillaChatSettings;
import dev.miklires.chatutils.client.stream.StreamChat;
import dev.miklires.chatutils.client.translate.Translator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;

/** Wires the mod's pieces to the client's events. */
public class ChatutilsClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("chatutils");

    @Override
    public void onInitializeClient() {
        ChatUtilsConfig.HANDLER.load();
        createSoundsDirectory();

        ChatUtilsKeys.register();
        ChatUtilsCommands.register();
        VanillaChatSettings.register();
        SymbolsEverywhere.register();
        BubbleRenderer.register();
        registerIncomingChat();
        registerOutgoingChat();
        registerLifecycle();
    }

    /**
     * Incoming messages go through Fabric's receive events rather than a chat mixin: they fire at
     * packet level, hand us the verified sender, and survive game updates far better.
     *
     * <p>Player chat has no modify hook — rewriting a signed message would contradict its signature
     * — so {@code MessagePipeline} re-posts the lines it changed itself. System messages have both
     * hooks and are swapped in place. Overlay messages are the text above the hotbar, not chat, so
     * they are left alone.
     */
    private void registerIncomingChat() {
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, params, timestamp) ->
                MessagePipeline.allowPlayerChat(message, sender));

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
                overlay || MessagePipeline.allowSystemMessage(message));
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) ->
                overlay ? message : MessagePipeline.modifySystemMessage(message));
    }

    private void registerOutgoingChat() {
        // Before anything else: a message that is a sum is an answer to yourself, not something to
        // broadcast, so it is intercepted rather than expanded and sent.
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> !ChatCalculator.handleOutgoing(message));
        // Length is checked against what will actually be sent, not what was typed: a placeholder
        // can expand well past the packet limit, and an encrypted message clipped by that limit
        // fails to decrypt entirely rather than arriving short.
        ClientSendMessageEvents.ALLOW_CHAT.register(
                message -> EncryptedChat.allowOutgoing(finalPlaintext(message)));

        // Macros expand first so that what a placeholder turned into is written in the same alphabet
        // as the rest of the line; encryption comes last, since everything after it would be working
        // on ciphertext. Commands get the macros but neither the font nor the encryption — the
        // server has to be able to read a command.
        ClientSendMessageEvents.MODIFY_CHAT.register(
                message -> EncryptedChat.encryptOutgoing(finalPlaintext(message)));
        // Recorded here rather than through a separate send event: this listener already sees the
        // final text of every command, which is exactly what the repeat key has to replay.
        ClientSendMessageEvents.MODIFY_COMMAND.register(command -> {
            String expanded = MacroExpander.expand(command);
            CommandKeys.rememberCommand(expanded);
            return expanded;
        });
        ClientSendMessageEvents.CHAT.register(message -> NoReports.onMessageSent());
    }

    /** Everything the mod does to a message before it is sealed. */
    private static String finalPlaintext(String message) {
        return ChatUtilsConfig.get().chatFont.apply(MacroExpander.expand(message));
    }

    private void registerLifecycle() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ChatWindowOptions.apply();
            // Before anything the mod might say about this session, so the divider stays a heading.
            SessionDivider.onJoin();
            NoReports.onJoin();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SpamCompactor.reset();
            MessagePipeline.reset();
            AutoResponder.reset();
            MentionNotifier.reset();
            CommandKeys.reset();
            ChatHeads.reset();
            ChatVisibility.reset();
            // Leaving a world should not leave a socket open to someone's stream.
            StreamChat.stopAll();
            ChatBubbles.clear();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SpamCompactor.tick();
            ChatUtilsKeys.tick();
            NoReports.tick();
            StreamChat.tick();
            SymbolSuggestions.tick();
            Translator.tick();
            AutoResponder.tick();
        });
    }

    /** Makes the drop-in folder for custom sounds exist, so the user has somewhere obvious to look. */
    private void createSoundsDirectory() {
        try {
            Files.createDirectories(SoundPlayer.soundsDirectory());
        } catch (IOException failure) {
            LOGGER.warn("Could not create the custom sounds directory: {}", failure.toString());
        }
    }
}
