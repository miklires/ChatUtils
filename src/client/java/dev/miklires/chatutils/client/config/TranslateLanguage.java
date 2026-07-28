package dev.miklires.chatutils.client.config;

import dev.isxander.yacl3.api.NameableEnum;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * A language for the translator, picked from a list rather than typed.
 *
 * <p>Two-letter codes are easy to get wrong and impossible to check: a typo produces a request the
 * endpoint rejects with something unhelpful, and the player has no way to know whether the code or
 * the key was the problem. A list cannot be misspelled.
 *
 * <p>{@link #AUTO} means different things on each side, and both are the useful default. As the
 * source it asks the endpoint to detect the language; as the target it follows the game's own
 * language setting, so the translation arrives in whatever you are already reading.
 */
public enum TranslateLanguage implements NameableEnum {

    AUTO("auto"),
    ENGLISH("en"),
    RUSSIAN("ru"),
    UKRAINIAN("uk"),
    GERMAN("de"),
    FRENCH("fr"),
    SPANISH("es"),
    PORTUGUESE("pt"),
    ITALIAN("it"),
    POLISH("pl"),
    CZECH("cs"),
    DUTCH("nl"),
    SWEDISH("sv"),
    TURKISH("tr"),
    ARABIC("ar"),
    CHINESE("zh"),
    JAPANESE("ja"),
    KOREAN("ko"),
    HINDI("hi"),
    INDONESIAN("id");

    private final String code;

    TranslateLanguage(String code) {
        this.code = code;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("chatutils.language." + code);
    }

    /** The code to send as the source: {@code auto} asks the endpoint to work it out. */
    public String sourceCode() {
        return code;
    }

    /**
     * The code to send as the target. {@code AUTO} resolves to the game's own language, since a
     * translation you cannot read is no translation at all.
     */
    public String targetCode() {
        if (this != AUTO) {
            return code;
        }

        String game = Minecraft.getInstance().options.languageCode;
        if (game == null || game.length() < 2) {
            return ENGLISH.code;
        }
        // Minecraft writes "ru_ru"; the endpoint wants "ru".
        return game.substring(0, 2).toLowerCase(Locale.ROOT);
    }
}
