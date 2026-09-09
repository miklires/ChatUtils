package dev.miklires.chatutils.client.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dev.miklires.chatutils.client.chat.ChatDelivery;
import dev.miklires.chatutils.client.chat.ChatHistory;
import dev.miklires.chatutils.client.chat.processor.FilterProcessor;
import dev.miklires.chatutils.client.config.ChatFont;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import dev.miklires.chatutils.client.config.ConfigScreen;
import dev.miklires.chatutils.client.screen.ChatHistoryScreen;
import dev.miklires.chatutils.client.translate.Translator;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ChatUtilsCommands {

    private ChatUtilsCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            dispatcher.register(literal("chatutils")
                    .executes(context -> openSettings())
                    .then(literal("settings").executes(context -> openSettings()))
                    .then(literal("help").executes(context -> help()))
                    .then(literal("hidechat").executes(context -> toggle("hide_chat",
                            () -> ChatUtilsConfig.get().hideChatEnabled,
                            value -> ChatUtilsConfig.get().hideChatEnabled = value)))
                    .then(literal("bubbles").executes(context -> toggle("chat_bubbles",
                            () -> ChatUtilsConfig.get().chatBubbles,
                            value -> ChatUtilsConfig.get().chatBubbles = value)))
                    .then(literal("heads").executes(context -> toggle("chat_heads",
                            () -> ChatUtilsConfig.get().chatHeadsEnabled,
                            value -> ChatUtilsConfig.get().chatHeadsEnabled = value)))
                    .then(literal("filter").executes(context -> toggle("filter_enabled",
                            () -> ChatUtilsConfig.get().filterEnabled,
                            value -> ChatUtilsConfig.get().filterEnabled = value)))
                    .then(literal("mentions").executes(context -> toggle("mention_enabled",
                            () -> ChatUtilsConfig.get().mentionEnabled,
                            value -> ChatUtilsConfig.get().mentionEnabled = value)))
                    .then(literal("autoreply").executes(context -> toggle("auto_reply",
                            () -> ChatUtilsConfig.get().autoReplyEnabled,
                            value -> ChatUtilsConfig.get().autoReplyEnabled = value)))
                    .then(literal("encrypt").executes(context -> toggle("encryption",
                            () -> ChatUtilsConfig.get().encryptionEnabled,
                            value -> ChatUtilsConfig.get().encryptionEnabled = value)))
                    .then(literal("streams").executes(context -> toggle("stream_chat",
                            () -> ChatUtilsConfig.get().streamChatEnabled,
                            value -> ChatUtilsConfig.get().streamChatEnabled = value)))
                    .then(literal("history").executes(context -> openHistory("")))
                    .then(literal("clear").executes(context -> clearHistory()))
                    .then(literal("font")
                            .executes(context -> reportFont())
                            .then(argument("name", StringArgumentType.word())
                                    .executes(context -> setFont(StringArgumentType.getString(context, "name")))))
                    .then(literal("search")
                            .then(argument("query", StringArgumentType.greedyString())
                                    .executes(context -> openHistory(StringArgumentType.getString(context, "query")))))
                    .then(literal("translate")
                            .executes(context -> translateLast())
                            .then(argument("text", StringArgumentType.greedyString())
                                    .executes(context -> translate(
                                            StringArgumentType.getString(context, "text")))))
                    .then(literal("reveal")
                            .then(argument("id", StringArgumentType.word())
                                    .executes(context -> reveal(StringArgumentType.getString(context, "id"))))));

            dispatcher.register(literal("hidechat").executes(context -> toggle("hide_chat",
                    () -> ChatUtilsConfig.get().hideChatEnabled,
                    value -> ChatUtilsConfig.get().hideChatEnabled = value)));
            dispatcher.register(literal("chatsearch")
                    .executes(context -> openHistory(""))
                    .then(argument("query", StringArgumentType.greedyString())
                            .executes(context -> openHistory(StringArgumentType.getString(context, "query")))));
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    private static <T> RequiredArgumentBuilder<FabricClientCommandSource, T> argument(
            String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    private static int openSettings() {
        later(() -> Minecraft.getInstance().gui.setScreen(ConfigScreen.create(null)));
        return 1;
    }

    private static int openHistory(String query) {
        later(() -> Minecraft.getInstance().gui.setScreen(new ChatHistoryScreen(null, query)));
        return 1;
    }

    private static int toggle(String optionKey, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        boolean now = !getter.get();
        setter.accept(now);
        ChatUtilsConfig.save();

        reply(Component.translatable("chatutils.command.toggled",
                Component.translatable("chatutils.option." + optionKey),
                Component.translatable(now ? "chatutils.command.on" : "chatutils.command.off")));
        return 1;
    }

    private static int help() {
        reply(Component.translatable("chatutils.command.help_title"));
        for (String line : new String[]{"toggles", "screens", "text", "keys"}) {
            reply(Component.translatable("chatutils.command.help_" + line));
        }
        return 1;
    }

    private static int clearHistory() {
        int cleared = ChatHistory.size();
        ChatHistory.clear();
        reply(Component.translatable("chatutils.history.cleared", cleared));
        return 1;
    }

    private static int reportFont() {
        ChatFont active = ChatUtilsConfig.get().chatFont;
        reply(Component.translatable("chatutils.command.font_is")
                .append(" ").append(Component.translatable(active.translationKey())));
        return 1;
    }

    private static int setFont(String name) {
        for (ChatFont font : ChatFont.values()) {
            if (font.name().equalsIgnoreCase(name)) {
                ChatUtilsConfig.get().chatFont = font;
                ChatUtilsConfig.save();
                return reportFont();
            }
        }
        reply(Component.translatable("chatutils.command.no_such_font", name)
                .withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int translateLast() {
        Translator.translateLast();
        return 1;
    }

    private static int translate(String text) {
        Translator.translate(text);
        return 1;
    }

    private static int reveal(String id) {
        String original = FilterProcessor.reveal(id);
        if (original == null) {
            reply(Component.translatable("chatutils.filter.gone").withStyle(ChatFormatting.GRAY));
            return 0;
        }
        reply(Component.translatable("chatutils.filter.revealed")
                .append(" ").append(Component.literal(original).withStyle(ChatFormatting.WHITE)));
        return 1;
    }

    private static void later(Runnable action) {
        Minecraft.getInstance().execute(action);
    }

    private static void reply(Component message) {
        ChatDelivery.sendSystem(message.copy().withStyle(ChatFormatting.GRAY));
    }
}
