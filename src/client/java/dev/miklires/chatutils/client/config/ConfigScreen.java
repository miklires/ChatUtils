package dev.miklires.chatutils.client.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.miklires.chatutils.client.chat.ChatWindowOptions;
import dev.miklires.chatutils.client.input.CommandKeys;
import dev.miklires.chatutils.client.macro.MacroExpander;
import dev.miklires.chatutils.client.notify.SoundPlayer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Builds the YACL settings screen shown from Mod Menu. */
public final class ConfigScreen {

    private ConfigScreen() {
    }

    public static Screen create(Screen parent) {
        ChatUtilsConfig config = ChatUtilsConfig.get();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("chatutils.title"))
                .category(mentions(config))
                .category(appearance(config))
                .category(window(config))
                .category(filters(config))
                .category(history(config))
                .category(macros(config))
                .category(streams(config))
                .category(translation(config))
                .category(privacy(config))
                .save(() -> {
                    ChatUtilsConfig.save();
                    // Sound files may have been swapped out and the window settings changed while
                    // the screen was open; both need to take effect without a restart.
                    SoundPlayer.clearCache();
                    ChatWindowOptions.apply();
                })
                .build()
                .generateScreen(parent);
    }

    // ------------------------------------------------------------------ categories

    private static ConfigCategory mentions(ChatUtilsConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("chatutils.category.mentions"))
                .option(bool("mention_enabled", true,
                        () -> config.mentionEnabled, value -> config.mentionEnabled = value))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.mention_triggers"))
                        .option(bool("mention_own_name", true,
                                () -> config.mentionUseOwnName, value -> config.mentionUseOwnName = value))
                        .option(bool("mention_whole_word", true,
                                () -> config.mentionWholeWord, value -> config.mentionWholeWord = value))
                        .option(bool("mention_case_sensitive", false,
                                () -> config.mentionCaseSensitive, value -> config.mentionCaseSensitive = value))
                        .option(bool("mention_ignore_self", true,
                                () -> config.mentionIgnoreSelf, value -> config.mentionIgnoreSelf = value))
                        .option(bool("mention_regex", false,
                                () -> config.mentionRegex, value -> config.mentionRegex = value))
                        .build())
                .group(stringList("mention_keywords",
                        () -> config.mentionKeywords, value -> config.mentionKeywords = value))
                .group(stringList("mention_exclusions",
                        () -> config.mentionExclusions, value -> config.mentionExclusions = value))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.mention_highlight"))
                        .option(bool("mention_highlight", true,
                                () -> config.mentionHighlight, value -> config.mentionHighlight = value))
                        .option(color("mention_highlight_color", 0xFFD966,
                                () -> config.mentionHighlightColor, value -> config.mentionHighlightColor = value))
                        .option(bool("mention_highlight_bold", true,
                                () -> config.mentionHighlightBold, value -> config.mentionHighlightBold = value))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.mention_alerts"))
                        .option(bool("mention_sound", true,
                                () -> config.mentionSound, value -> config.mentionSound = value))
                        .option(soundIdOption("mention_sound_id", "minecraft:entity.experience_orb.pickup",
                                () -> config.mentionSoundId, value -> config.mentionSoundId = value))
                        .option(floatSlider("mention_sound_volume", 1.0f, 0.0f, 1.0f, 0.05f,
                                () -> config.mentionSoundVolume, value -> config.mentionSoundVolume = value))
                        .option(floatSlider("mention_sound_pitch", 1.0f, 0.5f, 2.0f, 0.05f,
                                () -> config.mentionSoundPitch, value -> config.mentionSoundPitch = value))
                        .option(bool("mention_hotbar", true,
                                () -> config.mentionHotbarText, value -> config.mentionHotbarText = value))
                        .option(string("mention_hotbar_format", "%s mentioned you",
                                () -> config.mentionHotbarFormat, value -> config.mentionHotbarFormat = value))
                        .option(intSlider("mention_cooldown", 2000, 0, 10000, 250,
                                () -> config.mentionCooldownMs, value -> config.mentionCooldownMs = value))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.message_sound"))
                        .option(bool("message_sound", false,
                                () -> config.messageSoundEnabled, value -> config.messageSoundEnabled = value))
                        .option(soundIdOption("message_sound_id", "minecraft:ui.button.click",
                                () -> config.messageSoundId, value -> config.messageSoundId = value))
                        .option(floatSlider("message_sound_volume", 0.5f, 0.0f, 1.0f, 0.05f,
                                () -> config.messageSoundVolume, value -> config.messageSoundVolume = value))
                        .option(floatSlider("message_sound_pitch", 1.0f, 0.5f, 2.0f, 0.05f,
                                () -> config.messageSoundPitch, value -> config.messageSoundPitch = value))
                        .option(bool("message_sound_players_only", true,
                                () -> config.messageSoundOnlyPlayers, value -> config.messageSoundOnlyPlayers = value))
                        .build())
                .build();
    }

    private static ConfigCategory appearance(ChatUtilsConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("chatutils.category.appearance"))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.timestamps"))
                        .option(enumOption("timestamp_mode", TimestampMode.OFF, TimestampMode.class,
                                () -> config.timestampMode, value -> config.timestampMode = value))
                        .option(string("timestamp_format", "HH:mm",
                                () -> config.timestampFormat, value -> config.timestampFormat = value))
                        .option(color("timestamp_color", 0x808080,
                                () -> config.timestampColor, value -> config.timestampColor = value))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.heads"))
                        .option(bool("chat_heads", true,
                                () -> config.chatHeadsEnabled, value -> config.chatHeadsEnabled = value))
                        .option(bool("chat_heads_offset_all", true,
                                () -> config.chatHeadsOffsetAllLines,
                                value -> config.chatHeadsOffsetAllLines = value))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.animation"))
                        .option(bool("chat_animation", true,
                                () -> config.chatAnimationEnabled, value -> config.chatAnimationEnabled = value))
                        .option(intSlider("chat_animation_duration", 200, 50, 800, 25,
                                () -> config.chatAnimationDurationMs,
                                value -> config.chatAnimationDurationMs = value))
                        .option(bool("chat_open_animation", true,
                                () -> config.chatOpenAnimation, value -> config.chatOpenAnimation = value))
                        .option(intSlider("chat_hud_offset", 0, 0, 60, 1,
                                () -> config.chatHudOffset, value -> config.chatHudOffset = value))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.links"))
                        .option(bool("click_to_copy", true,
                                () -> config.clickToCopy, value -> config.clickToCopy = value))
                        .option(bool("linkify", true,
                                () -> config.linkifyEnabled, value -> config.linkifyEnabled = value))
                        .build())
                .group(stringList("link_domains",
                        () -> config.linkAllowedDomains, value -> config.linkAllowedDomains = value))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.bubbles"))
                        .option(bool("chat_bubbles", false,
                                () -> config.chatBubbles, value -> config.chatBubbles = value))
                        .option(intSlider("chat_bubble_ms", 6000, 1000, 20000, 500,
                                () -> config.chatBubbleMs, value -> config.chatBubbleMs = value))
                        .option(intSlider("chat_bubble_lines", 3, 1, 6, 1,
                                () -> config.chatBubbleLines, value -> config.chatBubbleLines = value))
                        .option(intSlider("chat_bubble_length", 48, 16, 96, 4,
                                () -> config.chatBubbleLength, value -> config.chatBubbleLength = value))
                        .option(bool("chat_bubble_hide_self", true,
                                () -> config.chatBubbleHideSelf, value -> config.chatBubbleHideSelf = value))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.symbols"))
                        .option(bool("symbol_bar", true,
                                () -> config.symbolBarEnabled, value -> config.symbolBarEnabled = value))
                        .option(bool("symbol_suggestions", true,
                                () -> config.symbolSuggestions, value -> config.symbolSuggestions = value))
                        .option(bool("symbols_everywhere", true,
                                () -> config.symbolsEverywhere, value -> config.symbolsEverywhere = value))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.fonts"))
                        .option(enumOption("chat_font", ChatFont.NONE, ChatFont.class,
                                () -> config.chatFont, value -> config.chatFont = value))
                        .option(bool("chat_font_button", true,
                                () -> config.chatFontButton, value -> config.chatFontButton = value))
                        .build())
                .build();
    }

    private static ConfigCategory window(ChatUtilsConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("chatutils.category.window"))
                .option(bool("override_window", false,
                        () -> config.overrideChatWindow, value -> config.overrideChatWindow = value))
                .option(doubleSlider("chat_scale", 1.0,
                        () -> config.chatScale, value -> config.chatScale = value))
                .option(doubleSlider("chat_width", 1.0,
                        () -> config.chatWidth, value -> config.chatWidth = value))
                .option(doubleSlider("chat_height_focused", 1.0,
                        () -> config.chatHeightFocused, value -> config.chatHeightFocused = value))
                .option(doubleSlider("chat_height_unfocused", 0.44,
                        () -> config.chatHeightUnfocused, value -> config.chatHeightUnfocused = value))
                .option(doubleSlider("chat_opacity", 1.0,
                        () -> config.chatOpacity, value -> config.chatOpacity = value))
                .option(doubleSlider("chat_background_opacity", 0.5,
                        () -> config.chatTextBackgroundOpacity, value -> config.chatTextBackgroundOpacity = value))
                .option(doubleSlider("chat_line_spacing", 0.0,
                        () -> config.chatLineSpacing, value -> config.chatLineSpacing = value))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.vanilla_chat"))
                        .option(doubleSlider("chat_delay", 0.0, 0.0, 6.0, 0.1,
                                () -> config.chatDelaySeconds, value -> config.chatDelaySeconds = value))
                        .option(bool("chat_colors", true,
                                () -> config.chatColors, value -> config.chatColors = value))
                        .option(bool("chat_links", true,
                                () -> config.chatLinks, value -> config.chatLinks = value))
                        .option(bool("chat_links_prompt", true,
                                () -> config.chatLinksPrompt, value -> config.chatLinksPrompt = value))
                        .option(bool("replace_chat_settings", true,
                                () -> config.replaceChatSettings, value -> config.replaceChatSettings = value))
                        .build())
                .build();
    }

    private static ConfigCategory filters(ChatUtilsConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("chatutils.category.filters"))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.word_filter"))
                        .option(bool("filter_enabled", true,
                                () -> config.filterEnabled, value -> config.filterEnabled = value))
                        .option(enumOption("filter_mode", FilterMode.CENSOR, FilterMode.class,
                                () -> config.filterMode, value -> config.filterMode = value))
                        .option(bool("filter_regex", false,
                                () -> config.filterRegex, value -> config.filterRegex = value))
                        .option(bool("filter_revealable", true,
                                () -> config.filterRevealable, value -> config.filterRevealable = value))
                        .option(string("filter_censor_char", "*",
                                () -> config.filterCensorChar, value -> config.filterCensorChar = value))
                        .build())
                .group(stringList("filtered_words",
                        () -> config.filteredWords, value -> config.filteredWords = value))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.blacklist"))
                        .option(bool("blacklist_enabled", false,
                                () -> config.blacklistEnabled, value -> config.blacklistEnabled = value))
                        .build())
                .group(stringList("blacklisted_players",
                        () -> config.blacklistedPlayers, value -> config.blacklistedPlayers = value))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.hide_name"))
                        .option(bool("hide_own_name", false,
                                () -> config.hideOwnName, value -> config.hideOwnName = value))
                        .option(string("hide_name_char", "*",
                                () -> config.hideOwnNameChar, value -> config.hideOwnNameChar = value))
                        .build())
                .group(stringList("hidden_names",
                        () -> config.hiddenNames, value -> config.hiddenNames = value))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.friends"))
                        .option(bool("friends_enabled", false,
                                () -> config.friendsEnabled, value -> config.friendsEnabled = value))
                        .option(color("friend_color", 0x55FF55,
                                () -> config.friendColor, value -> config.friendColor = value))
                        .option(bool("friend_bold", false,
                                () -> config.friendBold, value -> config.friendBold = value))
                        .build())
                .group(stringList("friends", () -> config.friends, value -> config.friends = value))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.noise"))
                        .option(bool("compact_spam", false,
                                () -> config.compactSpamEnabled, value -> config.compactSpamEnabled = value))
                        .option(intSlider("compact_spam_window", 250, 50, 1000, 50,
                                () -> config.compactSpamWindowMs, value -> config.compactSpamWindowMs = value))
                        .option(color("compact_spam_color", 0xAAAAAA,
                                () -> config.compactSpamCounterColor, value -> config.compactSpamCounterColor = value))
                        .option(bool("anti_clear", true,
                                () -> config.antiClearEnabled, value -> config.antiClearEnabled = value))
                        .option(intSlider("anti_clear_threshold", 5, 2, 30, 1,
                                () -> config.antiClearThreshold, value -> config.antiClearThreshold = value))
                        .build())
                .build();
    }

    private static ConfigCategory history(ChatUtilsConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("chatutils.category.history"))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.history"))
                        .option(intSlider("history_size", 2000, 100, 10000, 100,
                                () -> config.historySize, value -> config.historySize = value))
                        .option(bool("keep_history", true,
                                () -> config.keepHistoryOnDisconnect, value -> config.keepHistoryOnDisconnect = value))
                        .option(bool("session_divider", true,
                                () -> config.sessionDivider, value -> config.sessionDivider = value))
                        .option(color("session_divider_color", 0x555555,
                                () -> config.sessionDividerColor, value -> config.sessionDividerColor = value))
                        .option(bool("chat_search_button", true,
                                () -> config.chatSearchButton, value -> config.chatSearchButton = value))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.hide_chat"))
                        .option(bool("hide_chat", false,
                                () -> config.hideChatEnabled, value -> config.hideChatEnabled = value))
                        .option(bool("hide_chat_mentions", true,
                                () -> config.hideChatShowMentions, value -> config.hideChatShowMentions = value))
                        .option(intSlider("hide_chat_reveal", 4000, 500, 15000, 500,
                                () -> config.hideChatRevealMs, value -> config.hideChatRevealMs = value))
                        .build())
                .build();
    }

    private static ConfigCategory macros(ChatUtilsConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("chatutils.category.macros"))
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("chatutils.option.macros_enabled"))
                        .description(OptionDescription.of(
                                Component.translatable("chatutils.option.macros_enabled.desc"),
                                Component.literal(String.join(", ", macroList()))))
                        .binding(true, () -> config.macrosEnabled, value -> config.macrosEnabled = value)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .group(stringList("custom_macros",
                        () -> config.customMacros, value -> config.customMacros = value))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.calculator"))
                        .option(bool("calculator", true,
                                () -> config.calculatorEnabled, value -> config.calculatorEnabled = value))
                        .option(string("calculator_prefix", "=",
                                () -> config.calculatorPrefix, value -> config.calculatorPrefix = value))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.auto_reply"))
                        .option(bool("auto_reply", false,
                                () -> config.autoReplyEnabled, value -> config.autoReplyEnabled = value))
                        .option(intSlider("auto_reply_cooldown", 15000, 3000, 120000, 1000,
                                () -> config.autoReplyCooldownMs, value -> config.autoReplyCooldownMs = value))
                        .build())
                .group(stringList("auto_reply_rules",
                        () -> config.autoReplyRules, value -> config.autoReplyRules = value))
                .group(ListOption.<String>createBuilder()
                        .name(Component.translatable("chatutils.option.command_keys"))
                        .description(OptionDescription.of(
                                Component.translatable("chatutils.option.command_keys.desc"),
                                CommandKeys.formatHint()))
                        .binding(new ArrayList<>(),
                                () -> config.commandKeys, value -> config.commandKeys = value)
                        .controller(StringControllerBuilder::create)
                        .initial("")
                        .build())
                .build();
    }

    private static ConfigCategory streams(ChatUtilsConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("chatutils.category.streams"))
                .option(bool("stream_chat", false,
                        () -> config.streamChatEnabled, value -> config.streamChatEnabled = value))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.twitch"))
                        .option(string("twitch_channel", "",
                                () -> config.twitchChannel, value -> config.twitchChannel = value))
                        .option(color("twitch_tag_color", 0x9146FF,
                                () -> config.twitchTagColor, value -> config.twitchTagColor = value))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.youtube"))
                        .option(string("youtube_video", "",
                                () -> config.youtubeVideoId, value -> config.youtubeVideoId = value))
                        .option(string("youtube_key", "",
                                () -> config.youtubeApiKey, value -> config.youtubeApiKey = value))
                        .option(color("youtube_tag_color", 0xFF0000,
                                () -> config.youtubeTagColor, value -> config.youtubeTagColor = value))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.stream_flow"))
                        .option(intSlider("stream_rate", 2, 1, 20, 1,
                                () -> config.streamMaxPerTick, value -> config.streamMaxPerTick = value))
                        .option(bool("stream_mentions", true,
                                () -> config.streamMentions, value -> config.streamMentions = value))
                        .build())
                .build();
    }

    private static ConfigCategory translation(ChatUtilsConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("chatutils.category.translation"))
                .option(bool("translate_enabled", false,
                        () -> config.translateEnabled, value -> config.translateEnabled = value))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.translation"))
                        .option(string("translate_endpoint", "",
                                () -> config.translateEndpoint, value -> config.translateEndpoint = value))
                        .option(string("translate_key", "",
                                () -> config.translateApiKey, value -> config.translateApiKey = value))
                        .option(enumOption("translate_source", TranslateLanguage.AUTO, TranslateLanguage.class,
                                () -> config.translateSource, value -> config.translateSource = value))
                        .option(enumOption("translate_target", TranslateLanguage.AUTO, TranslateLanguage.class,
                                () -> config.translateTarget, value -> config.translateTarget = value))
                        .build())
                .build();
    }

    private static ConfigCategory privacy(ChatUtilsConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("chatutils.category.privacy"))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("chatutils.group.encryption"))
                        .option(bool("encryption", false,
                                () -> config.encryptionEnabled, value -> config.encryptionEnabled = value))
                        .option(string("encryption_passphrase", "",
                                () -> config.encryptionPassphrase,
                                value -> config.encryptionPassphrase = value))
                        .build())
                .option(bool("no_reports", false,
                        () -> config.noReportsEnabled, value -> config.noReportsEnabled = value))
                .option(bool("no_reports_warn", true,
                        () -> config.noReportsWarnOnSecureServer, value -> config.noReportsWarnOnSecureServer = value))
                .build();
    }

    /** Built-in placeholder names, shown in the macro description so they are discoverable. */
    private static List<String> macroList() {
        List<String> names = new ArrayList<>();
        for (String name : MacroExpander.builtInNames()) {
            names.add("[" + name + "]");
        }
        return names;
    }

    // ------------------------------------------------------------------ option helpers

    private static Option<Boolean> bool(String key, boolean def, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(name(key))
                .description(description(key))
                .binding(def, getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }

    private static Option<Integer> intSlider(String key, int def, int min, int max, int step,
                                             Supplier<Integer> getter, Consumer<Integer> setter) {
        return Option.<Integer>createBuilder()
                .name(name(key))
                .description(description(key))
                .binding(def, getter, setter)
                .controller(option -> IntegerSliderControllerBuilder.create(option)
                        .range(min, max)
                        .step(step))
                .build();
    }

    private static Option<Float> floatSlider(String key, float def, float min, float max, float step,
                                             Supplier<Float> getter, Consumer<Float> setter) {
        return Option.<Float>createBuilder()
                .name(name(key))
                .description(description(key))
                .binding(def, getter, setter)
                .controller(option -> FloatSliderControllerBuilder.create(option)
                        .range(min, max)
                        .step(step))
                .build();
    }

    /** Vanilla chat options are all 0..1 fractions, so every slider here shares that range. */
    /** Ranged variant, for the values vanilla does not express as a 0..1 fraction. */
    private static Option<Double> doubleSlider(String key, double def, double min, double max,
                                               double step, Supplier<Double> getter,
                                               Consumer<Double> setter) {
        return Option.<Double>createBuilder()
                .name(name(key))
                .description(description(key))
                .binding(def, getter, setter)
                .controller(option -> DoubleSliderControllerBuilder.create(option)
                        .range(min, max)
                        .step(step))
                .build();
    }

    private static Option<Double> doubleSlider(String key, double def,
                                               Supplier<Double> getter, Consumer<Double> setter) {
        return Option.<Double>createBuilder()
                .name(name(key))
                .description(description(key))
                .binding(def, getter, setter)
                .controller(option -> DoubleSliderControllerBuilder.create(option)
                        .range(0.0, 1.0)
                        .step(0.01))
                .build();
    }

    private static Option<String> string(String key, String def,
                                         Supplier<String> getter, Consumer<String> setter) {
        return Option.<String>createBuilder()
                .name(name(key))
                .description(description(key))
                .binding(def, getter, setter)
                .controller(StringControllerBuilder::create)
                .build();
    }

    /** A plain string field, but described so the user knows a {@code .wav} file name works too. */
    private static Option<String> soundIdOption(String key, String def,
                                                Supplier<String> getter, Consumer<String> setter) {
        return Option.<String>createBuilder()
                .name(name(key))
                .description(OptionDescription.of(
                        Component.translatable(key(key) + ".desc"),
                        Component.literal(SoundPlayer.soundsDirectory().toString())))
                .binding(def, getter, setter)
                .controller(StringControllerBuilder::create)
                .build();
    }

    private static <E extends Enum<E>> Option<E> enumOption(String key, E def, Class<E> type,
                                                            Supplier<E> getter, Consumer<E> setter) {
        return Option.<E>createBuilder()
                .name(name(key))
                .description(description(key))
                .binding(def, getter, setter)
                .controller(option -> EnumControllerBuilder.create(option).enumClass(type))
                .build();
    }

    /**
     * Colours live in the config as {@code 0xRRGGBB} ints so the JSON stays hand-editable; YACL's
     * picker works in {@link Color}, so translate at the boundary and drop the alpha channel.
     */
    private static Option<Color> color(String key, int def,
                                       Supplier<Integer> getter, Consumer<Integer> setter) {
        return Option.<Color>createBuilder()
                .name(name(key))
                .description(description(key))
                .binding(new Color(def),
                        () -> new Color(getter.get()),
                        value -> setter.accept(value.getRGB() & 0xFFFFFF))
                .controller(ColorControllerBuilder::create)
                .build();
    }

    private static ListOption<String> stringList(String key,
                                                 Supplier<List<String>> getter, Consumer<List<String>> setter) {
        return ListOption.<String>createBuilder()
                .name(name(key))
                .description(description(key))
                .binding(new ArrayList<>(), getter, setter)
                .controller(StringControllerBuilder::create)
                .initial("")
                .build();
    }

    private static String key(String key) {
        return "chatutils.option." + key;
    }

    private static Component name(String key) {
        return Component.translatable(key(key));
    }

    private static OptionDescription description(String key) {
        return OptionDescription.of(Component.translatable(key(key) + ".desc"));
    }
}
