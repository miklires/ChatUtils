package dev.miklires.chatutils.client.input;

import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Turns a key's written name into the code the window reports, so a binding can live in a config
 * line rather than in a slot registered at startup.
 *
 * <p>Built from GLFW's own constants — the same ones the game uses — rather than parsed out of a
 * translation key, so nothing here depends on a name the game might rename.
 *
 * <p>Forgiving about how the name is written: {@code g}, {@code G}, {@code KEY_G} and
 * {@code page up} all arrive at the same key, because someone typing a binding into a text box
 * should not have to guess the spelling.
 */
public final class KeyNames {

    public static final int UNKNOWN = GLFW.GLFW_KEY_UNKNOWN;

    private static final Map<String, Integer> BY_NAME = build();

    private KeyNames() {
    }

    /** @return the GLFW key code, or {@link #UNKNOWN} when the name means nothing */
    public static int codeOf(String name) {
        if (name == null || name.isBlank()) {
            return UNKNOWN;
        }
        String cleaned = name.trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        if (cleaned.startsWith("KEY_")) {
            cleaned = cleaned.substring(4);
        }
        return BY_NAME.getOrDefault(cleaned, UNKNOWN);
    }

    /** Every name this understands, for the help text in the settings screen. */
    public static String examples() {
        return "A-Z, 0-9, F1-F12, NUMPAD0-9, UP, DOWN, LEFT, RIGHT, "
                + "SPACE, ENTER, TAB, INSERT, DELETE, HOME, END, PAGE_UP, PAGE_DOWN, "
                + "LEFT_SHIFT, LEFT_CONTROL, LEFT_ALT, COMMA, PERIOD, SLASH, GRAVE_ACCENT";
    }

    private static Map<String, Integer> build() {
        Map<String, Integer> names = new LinkedHashMap<>();

        for (char c = 'A'; c <= 'Z'; c++) {
            names.put(String.valueOf(c), GLFW.GLFW_KEY_A + (c - 'A'));
        }
        for (int i = 0; i <= 9; i++) {
            names.put(String.valueOf(i), GLFW.GLFW_KEY_0 + i);
            names.put("NUMPAD" + i, GLFW.GLFW_KEY_KP_0 + i);
        }
        for (int i = 1; i <= 12; i++) {
            names.put("F" + i, GLFW.GLFW_KEY_F1 + i - 1);
        }

        names.put("SPACE", GLFW.GLFW_KEY_SPACE);
        names.put("ENTER", GLFW.GLFW_KEY_ENTER);
        names.put("TAB", GLFW.GLFW_KEY_TAB);
        names.put("BACKSPACE", GLFW.GLFW_KEY_BACKSPACE);
        names.put("INSERT", GLFW.GLFW_KEY_INSERT);
        names.put("DELETE", GLFW.GLFW_KEY_DELETE);
        names.put("HOME", GLFW.GLFW_KEY_HOME);
        names.put("END", GLFW.GLFW_KEY_END);
        names.put("PAGE_UP", GLFW.GLFW_KEY_PAGE_UP);
        names.put("PAGE_DOWN", GLFW.GLFW_KEY_PAGE_DOWN);
        names.put("UP", GLFW.GLFW_KEY_UP);
        names.put("DOWN", GLFW.GLFW_KEY_DOWN);
        names.put("LEFT", GLFW.GLFW_KEY_LEFT);
        names.put("RIGHT", GLFW.GLFW_KEY_RIGHT);
        names.put("LEFT_SHIFT", GLFW.GLFW_KEY_LEFT_SHIFT);
        names.put("RIGHT_SHIFT", GLFW.GLFW_KEY_RIGHT_SHIFT);
        names.put("LEFT_CONTROL", GLFW.GLFW_KEY_LEFT_CONTROL);
        names.put("RIGHT_CONTROL", GLFW.GLFW_KEY_RIGHT_CONTROL);
        names.put("LEFT_ALT", GLFW.GLFW_KEY_LEFT_ALT);
        names.put("RIGHT_ALT", GLFW.GLFW_KEY_RIGHT_ALT);
        names.put("CAPS_LOCK", GLFW.GLFW_KEY_CAPS_LOCK);
        names.put("MINUS", GLFW.GLFW_KEY_MINUS);
        names.put("EQUAL", GLFW.GLFW_KEY_EQUAL);
        names.put("LEFT_BRACKET", GLFW.GLFW_KEY_LEFT_BRACKET);
        names.put("RIGHT_BRACKET", GLFW.GLFW_KEY_RIGHT_BRACKET);
        names.put("SEMICOLON", GLFW.GLFW_KEY_SEMICOLON);
        names.put("APOSTROPHE", GLFW.GLFW_KEY_APOSTROPHE);
        names.put("COMMA", GLFW.GLFW_KEY_COMMA);
        names.put("PERIOD", GLFW.GLFW_KEY_PERIOD);
        names.put("SLASH", GLFW.GLFW_KEY_SLASH);
        names.put("BACKSLASH", GLFW.GLFW_KEY_BACKSLASH);
        names.put("GRAVE_ACCENT", GLFW.GLFW_KEY_GRAVE_ACCENT);

        // Aliases for how people actually write these.
        names.put("CTRL", GLFW.GLFW_KEY_LEFT_CONTROL);
        names.put("SHIFT", GLFW.GLFW_KEY_LEFT_SHIFT);
        names.put("ALT", GLFW.GLFW_KEY_LEFT_ALT);
        names.put("ESC", GLFW.GLFW_KEY_ESCAPE);
        names.put("ESCAPE", GLFW.GLFW_KEY_ESCAPE);
        names.put("PGUP", GLFW.GLFW_KEY_PAGE_UP);
        names.put("PGDN", GLFW.GLFW_KEY_PAGE_DOWN);

        return Map.copyOf(names);
    }
}
