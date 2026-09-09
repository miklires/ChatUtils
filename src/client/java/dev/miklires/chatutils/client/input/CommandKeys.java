package dev.miklires.chatutils.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CommandKeys {

    private record Binding(int code, String payload) {
    }

    private static final Set<Integer> held = new HashSet<>();

    @Nullable
    private static String lastCommand;

    private CommandKeys() {
    }

    public static void rememberCommand(String command) {
        if (command != null && !command.isBlank()) {
            lastCommand = command;
        }
    }

    public static void repeatLast() {
        send(lastCommand == null ? null : "/" + lastCommand);
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null) {
            held.clear();
            return;
        }

        var window = minecraft.getWindow();
        Set<Integer> pressedNow = new HashSet<>();

        for (Binding binding : bindings(ChatUtilsConfig.get())) {
            if (!InputConstants.isKeyDown(window, binding.code())) {
                continue;
            }
            pressedNow.add(binding.code());
            if (held.add(binding.code())) {
                send(binding.payload());
            }
        }

        held.retainAll(pressedNow);
    }

    private static List<Binding> bindings(ChatUtilsConfig config) {
        List<Binding> bindings = new ArrayList<>();
        for (String entry : config.commandKeys) {
            if (entry == null) {
                continue;
            }
            int split = entry.indexOf('|');
            if (split <= 0) {
                continue;
            }

            int code = KeyNames.codeOf(entry.substring(0, split));
            String payload = entry.substring(split + 1).trim();
            if (code != KeyNames.UNKNOWN && !payload.isEmpty()) {
                bindings.add(new Binding(code, payload));
            }
        }
        return bindings;
    }

    private static void send(@Nullable String entry) {
        if (entry == null || entry.isBlank()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null) {
            return;
        }

        String text = entry.trim();
        if (text.startsWith("/")) {
            connection.sendCommand(text.substring(1));
        } else {
            connection.sendChat(text);
        }
    }

    public static Component formatHint() {
        return Component.translatable("chatutils.option.command_keys.format", KeyNames.examples());
    }

    public static void reset() {
        lastCommand = null;
        held.clear();
    }
}
