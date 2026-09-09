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

public class ChatutilsClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("chatutils");

    @Override
    public void onInitializeClient() {
        ChatUtilsConfig.HANDLER.load();
        ChatUtilsConfig.get().normalize();
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

    private void registerIncomingChat() {
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, params, timestamp) ->
                MessagePipeline.allowPlayerChat(message, sender));

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
                overlay || MessagePipeline.allowSystemMessage(message));
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) ->
                overlay ? message : MessagePipeline.modifySystemMessage(message));
    }

    private void registerOutgoingChat() {
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> !ChatCalculator.handleOutgoing(message));
        ClientSendMessageEvents.ALLOW_CHAT.register(
                message -> EncryptedChat.allowOutgoing(finalPlaintext(message)));

        ClientSendMessageEvents.MODIFY_CHAT.register(
                message -> EncryptedChat.encryptOutgoing(finalPlaintext(message)));
        ClientSendMessageEvents.MODIFY_COMMAND.register(command -> {
            String expanded = MacroExpander.expand(command);
            CommandKeys.rememberCommand(expanded);
            return expanded;
        });
        ClientSendMessageEvents.CHAT.register(message -> NoReports.onMessageSent());
    }

    private static String finalPlaintext(String message) {
        return ChatUtilsConfig.get().chatFont.apply(MacroExpander.expand(message));
    }

    private void registerLifecycle() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ChatWindowOptions.apply();
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

    private void createSoundsDirectory() {
        try {
            Files.createDirectories(SoundPlayer.soundsDirectory());
        } catch (IOException e) {
            LOGGER.warn("Could not create the custom sounds directory: {}", e.toString());
        }
    }
}
