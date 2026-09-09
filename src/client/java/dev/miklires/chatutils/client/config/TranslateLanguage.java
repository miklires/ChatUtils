package dev.miklires.chatutils.client.config;

import dev.isxander.yacl3.api.NameableEnum;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;

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

    public String sourceCode() {
        return code;
    }

    public String targetCode() {
        if (this != AUTO) {
            return code;
        }

        String game = Minecraft.getInstance().options.languageCode;
        if (game == null || game.length() < 2) {
            return ENGLISH.code;
        }
        return game.substring(0, 2).toLowerCase(Locale.ROOT);
    }
}
