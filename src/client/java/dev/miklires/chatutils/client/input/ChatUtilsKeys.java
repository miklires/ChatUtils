package dev.miklires.chatutils.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.miklires.chatutils.Chatutils;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import dev.miklires.chatutils.client.render.ChatVisibility;
import dev.miklires.chatutils.client.screen.ChatHistoryScreen;
import dev.miklires.chatutils.client.translate.Translator;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ChatUtilsKeys {

    private static KeyMapping.Category category;
    private static KeyMapping peek;
    private static KeyMapping toggleHide;
    private static KeyMapping openHistory;
    private static KeyMapping translate;
    private static KeyMapping repeatCommand;

    private ChatUtilsKeys() {
    }

    public static void register() {
        category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(Chatutils.MOD_ID, "main"));

        peek = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.chatutils.peek", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, category));
        toggleHide = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.chatutils.toggle_hide", InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(), category));
        openHistory = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.chatutils.history", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, category));

        translate = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.chatutils.translate", InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(), category));
        repeatCommand = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.chatutils.repeat_command", InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(), category));

    }

    public static void tick() {
        if (peek == null) {
            return;
        }

        while (toggleHide.consumeClick()) {
            ChatUtilsConfig config = ChatUtilsConfig.get();
            config.hideChatEnabled = !config.hideChatEnabled;
            ChatUtilsConfig.save();
            status(config.hideChatEnabled
                    ? Component.translatable("chatutils.hide.on")
                    : Component.translatable("chatutils.hide.off"));
        }

        while (openHistory.consumeClick()) {
            Minecraft.getInstance().gui.setScreen(new ChatHistoryScreen());
        }

        while (translate.consumeClick()) {
            Translator.translateLast();
        }

        while (repeatCommand.consumeClick()) {
            CommandKeys.repeatLast();
        }

        CommandKeys.tick();

        ChatVisibility.setPeeking(peek.isDown());
    }

    private static void status(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui != null) {
            minecraft.gui.hud.setOverlayMessage(message, false);
        }
    }
}
