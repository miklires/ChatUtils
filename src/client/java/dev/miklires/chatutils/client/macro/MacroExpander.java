package dev.miklires.chatutils.client.macro;

import dev.miklires.chatutils.client.calc.ChatCalculator;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands {@code [placeholder]} shorthands in outgoing messages — type {@code my xyz: [xyz]} and it
 * goes out as {@code my xyz: 128, 64, -310}.
 *
 * <p>Deliberately no {@code [tps]}: the client never learns the server's real tick rate, so any
 * number shown would be a guess dressed up as a fact.
 */
public final class MacroExpander {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\[([a-zA-Z0-9_]+)]");

    /** {@code [=1+2]} anywhere in a message. Kept separate: expressions are not plain names. */
    private static final Pattern INLINE_MATH = Pattern.compile("\\[=([^\\[\\]]+)]");

    private static final Map<String, Supplier<String>> BUILT_INS = new LinkedHashMap<>();

    static {
        BUILT_INS.put("xyz", () -> {
            BlockPos pos = player().blockPosition();
            return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        });
        BUILT_INS.put("x", () -> String.valueOf(player().blockPosition().getX()));
        BUILT_INS.put("y", () -> String.valueOf(player().blockPosition().getY()));
        BUILT_INS.put("z", () -> String.valueOf(player().blockPosition().getZ()));
        BUILT_INS.put("dir", () -> player().getDirection().getName());
        BUILT_INS.put("ping", () -> {
            ClientPacketListener connection = connection();
            PlayerInfo info = connection.getPlayerInfo(player().getUUID());
            return info == null ? "?" : info.getLatency() + "ms";
        });
        BUILT_INS.put("xp", () -> String.valueOf(player().experienceLevel));
        BUILT_INS.put("fps", () -> String.valueOf(Minecraft.getInstance().getFps()));
        BUILT_INS.put("hp", () -> String.valueOf((int) Math.ceil(player().getHealth())));
        BUILT_INS.put("food", () -> String.valueOf(player().getFoodData().getFoodLevel()));
        BUILT_INS.put("item", () -> player().getMainHandItem().getHoverName().getString());
        BUILT_INS.put("dur", () -> {
            ItemStack stack = player().getMainHandItem();
            if (!stack.isDamageableItem()) {
                return "-";
            }
            return (stack.getMaxDamage() - stack.getDamageValue()) + "/" + stack.getMaxDamage();
        });
        BUILT_INS.put("biome", () -> {
            ClientLevel level = level();
            return level.getBiome(player().blockPosition()).unwrapKey()
                    .map(key -> key.identifier().getPath())
                    .orElse("?");
        });
        BUILT_INS.put("dim", () -> level().dimension().identifier().getPath());
        BUILT_INS.put("time", () -> {
            // 26.1 replaced Level#getDayTime with the world clock. The overworld clock is the right
            // one to read anyway: it is the time other players mean when they ask, even in the Nether.
            // Minecraft day starts at 06:00, so shift before splitting into hours and minutes.
            long ticks = (level().getOverworldClockTime() % 24000L + 6000L) % 24000L;
            return String.format(Locale.ROOT, "%02d:%02d", ticks / 1000L, (ticks % 1000L) * 60L / 1000L);
        });
        BUILT_INS.put("rt", () -> LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        BUILT_INS.put("name", () -> player().getGameProfile().name());
        BUILT_INS.put("server", () -> {
            var server = Minecraft.getInstance().getCurrentServer();
            return server == null ? "singleplayer" : server.ip;
        });
    }

    private MacroExpander() {
    }

    /** Every built-in placeholder name, for the help command and the config screen. */
    public static java.util.Set<String> builtInNames() {
        return BUILT_INS.keySet();
    }

    public static String expand(String message) {
        ChatUtilsConfig config = ChatUtilsConfig.get();
        if (message == null || message.indexOf('[') < 0) {
            return message;
        }

        // Maths first: an expression can contain anything, so matching it before the name pattern
        // keeps a stray bracket inside it from being read as a placeholder.
        if (config.calculatorEnabled) {
            message = expandInlineMath(message);
        }
        // Each feature answers for itself: turning macros off must not silently take [=1+2] with it.
        if (!config.macrosEnabled) {
            return message;
        }

        Map<String, String> custom = parseCustomMacros(config);
        Matcher matcher = PLACEHOLDER.matcher(message);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group(1).toLowerCase(Locale.ROOT);
            String replacement = custom.get(key);
            if (replacement == null) {
                replacement = evaluate(key);
            }
            // No such placeholder: leave the original text alone rather than eating it.
            matcher.appendReplacement(result, Matcher.quoteReplacement(
                    replacement == null ? matcher.group() : replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String expandInlineMath(String message) {
        Matcher matcher = INLINE_MATH.matcher(message);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(result,
                    Matcher.quoteReplacement(ChatCalculator.evaluateInline(matcher.group(1))));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /** Returns {@code null} for an unknown key, {@code "?"} when the value cannot be read right now. */
    private static String evaluate(String key) {
        Supplier<String> supplier = BUILT_INS.get(key);
        if (supplier == null) {
            return null;
        }
        try {
            return supplier.get();
        } catch (RuntimeException unavailable) {
            // No world, no player, item without durability — a placeholder must never break sending.
            return "?";
        }
    }

    private static Map<String, String> parseCustomMacros(ChatUtilsConfig config) {
        Map<String, String> macros = new LinkedHashMap<>();
        for (String entry : config.customMacros) {
            if (entry == null) {
                continue;
            }
            int separator = entry.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = entry.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            if (!key.isEmpty()) {
                macros.put(key, entry.substring(separator + 1));
            }
        }
        return macros;
    }

    private static LocalPlayer player() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            throw new IllegalStateException("no player");
        }
        return player;
    }

    private static ClientLevel level() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            throw new IllegalStateException("no level");
        }
        return level;
    }

    private static ClientPacketListener connection() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            throw new IllegalStateException("not connected");
        }
        return connection;
    }
}
