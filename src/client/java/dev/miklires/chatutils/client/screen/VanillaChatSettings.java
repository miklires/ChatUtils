package dev.miklires.chatutils.client.screen;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import dev.miklires.chatutils.client.config.ConfigScreen;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

/**
 * Sends the game's own "Chat Settings…" button to this mod's settings instead.
 *
 * <p>Every option that screen offered now lives in the mod's config — size, opacity, scale, line
 * spacing, colours, links, delay — alongside everything the mod adds. Two chat settings screens, one
 * a strict subset of the other, is a worse answer than one.
 *
 * <p>Matched on the class's simple name through Fabric's screen event rather than with a mixin. A
 * mixin needs the exact class to exist at load time, and being wrong about a remapped name is a
 * crash on startup; being wrong here is a button that keeps its old behaviour.
 */
public final class VanillaChatSettings {

    private static final String TARGET = "ChatOptionsScreen";

    /** The screen open before this one, so Esc goes back where it came from. */
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
            // Swapped on the next tick: the game is part-way through opening this screen, and a
            // screen set from inside that would be closed again as it finishes.
            Minecraft.getInstance().execute(() ->
                    Minecraft.getInstance().gui.setScreen(ConfigScreen.create(parent)));
        });
    }
}
