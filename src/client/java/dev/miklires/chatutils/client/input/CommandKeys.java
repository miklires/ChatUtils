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

/**
 * Hotkeys that send a command or a message without opening the chat, bound in this mod's own
 * settings as {@code KEY | what to send}.
 *
 * <p>The key lives in the same line as what it sends, so one list is the whole feature: no numbered
 * slots to line up against a second screen, and a binding you add takes effect the moment you close
 * the settings rather than at the next restart. The price is that these do not appear in the game's
 * controls screen and so cannot warn you about clashing with an existing binding — hence keys are
 * read only while no screen is open, and anything typed goes nowhere near a text box.
 *
 * <p>An entry beginning with {@code /} goes out as a command, anything else as an ordinary chat
 * message. Everything is sent through the same path as typing it, so macros, the calculator prefix,
 * the message font and encryption all still apply — a hotkey is a shortcut for typing, not a way
 * around it.
 */
public final class CommandKeys {

    /** @param code GLFW key code, {@code payload} the command or message it sends */
    private record Binding(int code, String payload) {
    }

    /** Keys held down last tick, so a held key fires once rather than every tick. */
    private static final Set<Integer> held = new HashSet<>();

    /** The last command actually sent, for the repeat key. */
    @Nullable
    private static String lastCommand;

    private CommandKeys() {
    }

    /** Records what went out, so the repeat key has something to repeat. */
    public static void rememberCommand(String command) {
        if (command != null && !command.isBlank()) {
            lastCommand = command;
        }
    }

    /** Re-sends the last command, with the slash the recorded form does not carry. */
    public static void repeatLast() {
        send(lastCommand == null ? null : "/" + lastCommand);
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        // Never while a screen is open: the keys are raw, and a text box has first claim on them.
        if (minecraft.player == null || minecraft.gui.screen() != null) {
            held.clear();
            return;
        }

        // The window object itself, not its handle: the raw descriptor accessor is gone and
        // isKeyDown takes the window now. Declared with var so this depends on no package either.
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

    /** Entries are {@code KEY | payload}; anything without a key this understands is skipped. */
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

    /**
     * Sends one entry. A blank one is silently ignored rather than announced: pressing a key you
     * never configured should do nothing at all, not tell you off.
     */
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

    /** Shown under the list in the settings screen, so the format is not something to work out. */
    public static Component formatHint() {
        return Component.translatable("chatutils.option.command_keys.format", KeyNames.examples());
    }

    public static void reset() {
        lastCommand = null;
        held.clear();
    }
}
