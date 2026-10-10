package dev.miklires.chatutils.client.input;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class KeyNames {

    public static final int UNKNOWN = InputConstants.UNKNOWN.getValue();

    private static final Map<String, Integer> BY_NAME = build();

    private KeyNames() {
    }

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

    public static String examples() {
        return "A-Z, 0-9, F1-F12, NUMPAD0-9, UP, DOWN, LEFT, RIGHT, "
                + "SPACE, ENTER, TAB, INSERT, DELETE, HOME, END, PAGE_UP, PAGE_DOWN, "
                + "LEFT_SHIFT, LEFT_CONTROL, LEFT_ALT, COMMA, PERIOD, SLASH, GRAVE_ACCENT";
    }

    private static Map<String, Integer> build() {
        Map<String, Integer> names = new LinkedHashMap<>();

        for (char c = 'A'; c <= 'Z'; c++) {
            names.put(String.valueOf(c), InputConstants.KEY_A + (c - 'A'));
        }
        int[] digits = {
                InputConstants.KEY_0, InputConstants.KEY_1, InputConstants.KEY_2, InputConstants.KEY_3,
                InputConstants.KEY_4, InputConstants.KEY_5, InputConstants.KEY_6, InputConstants.KEY_7,
                InputConstants.KEY_8, InputConstants.KEY_9
        };
        int[] numpad = {
                InputConstants.KEY_NUMPAD0, InputConstants.KEY_NUMPAD1, InputConstants.KEY_NUMPAD2,
                InputConstants.KEY_NUMPAD3, InputConstants.KEY_NUMPAD4, InputConstants.KEY_NUMPAD5,
                InputConstants.KEY_NUMPAD6, InputConstants.KEY_NUMPAD7, InputConstants.KEY_NUMPAD8,
                InputConstants.KEY_NUMPAD9
        };
        for (int i = 0; i <= 9; i++) {
            names.put(String.valueOf(i), digits[i]);
            names.put("NUMPAD" + i, numpad[i]);
        }
        for (int i = 1; i <= 12; i++) {
            names.put("F" + i, InputConstants.KEY_F1 + i - 1);
        }

        names.put("SPACE", InputConstants.KEY_SPACE);
        names.put("ENTER", InputConstants.KEY_RETURN);
        names.put("TAB", InputConstants.KEY_TAB);
        names.put("BACKSPACE", InputConstants.KEY_BACKSPACE);
        names.put("INSERT", InputConstants.KEY_INSERT);
        names.put("DELETE", InputConstants.KEY_DELETE);
        names.put("HOME", InputConstants.KEY_HOME);
        names.put("END", InputConstants.KEY_END);
        names.put("PAGE_UP", InputConstants.KEY_PAGEUP);
        names.put("PAGE_DOWN", InputConstants.KEY_PAGEDOWN);
        names.put("UP", InputConstants.KEY_UP);
        names.put("DOWN", InputConstants.KEY_DOWN);
        names.put("LEFT", InputConstants.KEY_LEFT);
        names.put("RIGHT", InputConstants.KEY_RIGHT);
        names.put("LEFT_SHIFT", InputConstants.KEY_LSHIFT);
        names.put("RIGHT_SHIFT", InputConstants.KEY_RSHIFT);
        names.put("LEFT_CONTROL", InputConstants.KEY_LCONTROL);
        names.put("RIGHT_CONTROL", InputConstants.KEY_RCONTROL);
        names.put("LEFT_ALT", InputConstants.KEY_LALT);
        names.put("RIGHT_ALT", InputConstants.KEY_RALT);
        names.put("CAPS_LOCK", InputConstants.KEY_CAPSLOCK);
        names.put("MINUS", InputConstants.KEY_MINUS);
        names.put("EQUAL", InputConstants.KEY_EQUALS);
        names.put("LEFT_BRACKET", InputConstants.KEY_LBRACKET);
        names.put("RIGHT_BRACKET", InputConstants.KEY_RBRACKET);
        names.put("SEMICOLON", InputConstants.KEY_SEMICOLON);
        names.put("APOSTROPHE", InputConstants.KEY_APOSTROPHE);
        names.put("COMMA", InputConstants.KEY_COMMA);
        names.put("PERIOD", InputConstants.KEY_PERIOD);
        names.put("SLASH", InputConstants.KEY_SLASH);
        names.put("BACKSLASH", InputConstants.KEY_BACKSLASH);
        names.put("GRAVE_ACCENT", InputConstants.KEY_GRAVE);

        names.put("CTRL", InputConstants.KEY_LCONTROL);
        names.put("SHIFT", InputConstants.KEY_LSHIFT);
        names.put("ALT", InputConstants.KEY_LALT);
        names.put("ESC", InputConstants.KEY_ESCAPE);
        names.put("ESCAPE", InputConstants.KEY_ESCAPE);
        names.put("PGUP", InputConstants.KEY_PAGEUP);
        names.put("PGDN", InputConstants.KEY_PAGEDOWN);

        return Map.copyOf(names);
    }
}
