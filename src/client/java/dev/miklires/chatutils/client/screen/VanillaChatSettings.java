package dev.miklires.chatutils.client.screen;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import dev.miklires.chatutils.client.config.ConfigScreen;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class VanillaChatSettings {

    private static final String TARGET = "ChatOptionsScreen";

    @Nullable
    private static Screen previous;

    private VanillaChatSettings() {
    }

    public static void register() {
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!screen.getClass().getSimpleName().equals(TARGET)) {
                previous = screen;
                return;
            }
            if (!ChatUtilsConfig.get().replaceChatSettings) {
                return;
            }

            Screen parent = previous;
            Minecraft.getInstance().execute(() ->
                    Minecraft.getInstance().gui.setScreen(ConfigScreen.create(parent)));
        });
    }
}
