package dev.miklires.chatutils.client.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.miklires.chatutils.Chatutils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class ChatUtilsConfig {

    public static final ConfigClassHandler<ChatUtilsConfig> HANDLER = ConfigClassHandler.createBuilder(ChatUtilsConfig.class)
            .id(Identifier.fromNamespaceAndPath(Chatutils.MOD_ID, "config"))
            .serializer(handler -> GsonConfigSerializerBuilder.create(handler)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("chatutils.json"))
                    .build())
            .build();

    public static ChatUtilsConfig get() {
        return HANDLER.instance();
    }

    public void normalize() {
        mentionKeywords = cleanList(mentionKeywords, 256, 256);
        mentionExclusions = cleanList(mentionExclusions, 256, 256);
        filteredWords = cleanList(filteredWords, 512, 256);
        blacklistedPlayers = cleanList(blacklistedPlayers, 512, 64);
        friends = cleanList(friends, 512, 64);
        linkAllowedDomains = cleanList(linkAllowedDomains, 256, 253);
        hiddenNames = cleanList(hiddenNames, 512, 64);
        commandKeys = cleanList(commandKeys, 128, 512);
        autoReplyRules = cleanList(autoReplyRules, 256, 1024);
        customMacros = cleanList(customMacros, 256, 1024);

        mentionSoundId = fallback(mentionSoundId, "minecraft:entity.experience_orb.pickup", 256);
        messageSoundId = fallback(messageSoundId, "minecraft:ui.button.click", 256);
        mentionHotbarFormat = fallback(mentionHotbarFormat, "%s mentioned you", 256);
        filterCensorChar = fallback(filterCensorChar, "*", 8);
        timestampFormat = fallback(timestampFormat, "HH:mm", 64);
        hideOwnNameChar = fallback(hideOwnNameChar, "*", 8);
        calculatorPrefix = fallback(calculatorPrefix, "=", 16);
        translateEndpoint = trim(translateEndpoint, 2048);
        translateApiKey = trim(translateApiKey, 4096);
        twitchChannel = trim(twitchChannel, 64);
        youtubeVideoId = trim(youtubeVideoId, 128);
        youtubeApiKey = trim(youtubeApiKey, 4096);
        encryptionPassphrase = trim(encryptionPassphrase, 1024);

        mentionCooldownMs = clamp(mentionCooldownMs, 0, 10_000);
        chatAnimationDurationMs = clamp(chatAnimationDurationMs, 50, 800);
        chatHudOffset = clamp(chatHudOffset, 0, 60);
        chatBubbleMs = clamp(chatBubbleMs, 1_000, 20_000);
        chatBubbleLines = clamp(chatBubbleLines, 1, 6);
        chatBubbleLength = clamp(chatBubbleLength, 16, 96);
        compactSpamWindowMs = clamp(compactSpamWindowMs, 50, 1_000);
        antiClearThreshold = clamp(antiClearThreshold, 2, 30);
        historySize = clamp(historySize, 100, 10_000);
        hideChatRevealMs = clamp(hideChatRevealMs, 500, 15_000);
        autoReplyCooldownMs = clamp(autoReplyCooldownMs, 3_000, 120_000);
        streamMaxPerTick = clamp(streamMaxPerTick, 1, 20);

        mentionSoundVolume = clamp(mentionSoundVolume, 0.0f, 1.0f);
        mentionSoundPitch = clamp(mentionSoundPitch, 0.5f, 2.0f);
        messageSoundVolume = clamp(messageSoundVolume, 0.0f, 1.0f);
        messageSoundPitch = clamp(messageSoundPitch, 0.5f, 2.0f);
        chatDelaySeconds = clamp(chatDelaySeconds, 0.0, 6.0);
        chatScale = clamp(chatScale, 0.0, 1.0);
        chatWidth = clamp(chatWidth, 0.0, 1.0);
        chatHeightFocused = clamp(chatHeightFocused, 0.0, 1.0);
        chatHeightUnfocused = clamp(chatHeightUnfocused, 0.0, 1.0);
        chatOpacity = clamp(chatOpacity, 0.0, 1.0);
        chatTextBackgroundOpacity = clamp(chatTextBackgroundOpacity, 0.0, 1.0);
        chatLineSpacing = clamp(chatLineSpacing, 0.0, 1.0);

        filterMode = filterMode == null ? FilterMode.CENSOR : filterMode;
        timestampMode = timestampMode == null ? TimestampMode.OFF : timestampMode;
        chatFont = chatFont == null ? ChatFont.NONE : chatFont;
        translateSource = translateSource == null ? TranslateLanguage.AUTO : translateSource;
        translateTarget = translateTarget == null ? TranslateLanguage.AUTO : translateTarget;
    }

    @SerialEntry
    public boolean mentionEnabled = true;

    @SerialEntry
    public boolean mentionUseOwnName = true;

    @SerialEntry
    public List<String> mentionKeywords = new ArrayList<>();

    @SerialEntry
    public boolean mentionRegex = false;

    @SerialEntry
    public List<String> mentionExclusions = new ArrayList<>();

    @SerialEntry
    public boolean mentionCaseSensitive = false;

    @SerialEntry
    public boolean mentionWholeWord = true;

    @SerialEntry
    public boolean mentionIgnoreSelf = true;

    @SerialEntry
    public boolean mentionHighlight = true;

    @SerialEntry
    public int mentionHighlightColor = 0xFFD966;

    @SerialEntry
    public boolean mentionHighlightBold = true;

    @SerialEntry
    public boolean mentionSound = true;

    @SerialEntry
    public String mentionSoundId = "minecraft:entity.experience_orb.pickup";

    @SerialEntry
    public float mentionSoundVolume = 1.0f;

    @SerialEntry
    public float mentionSoundPitch = 1.0f;

    @SerialEntry
    public boolean mentionHotbarText = true;

    @SerialEntry
    public String mentionHotbarFormat = "%s mentioned you";

    @SerialEntry
    public int mentionCooldownMs = 2000;

    @SerialEntry
    public boolean messageSoundEnabled = false;

    @SerialEntry
    public String messageSoundId = "minecraft:ui.button.click";

    @SerialEntry
    public float messageSoundVolume = 0.5f;

    @SerialEntry
    public float messageSoundPitch = 1.0f;

    @SerialEntry
    public boolean messageSoundOnlyPlayers = true;

    @SerialEntry
    public boolean filterEnabled = true;

    @SerialEntry
    public FilterMode filterMode = FilterMode.CENSOR;

    @SerialEntry
    public List<String> filteredWords = new ArrayList<>();

    @SerialEntry
    public boolean filterRegex = false;

    @SerialEntry
    public String filterCensorChar = "*";

    @SerialEntry
    public boolean filterRevealable = true;

    @SerialEntry
    public boolean blacklistEnabled = false;

    @SerialEntry
    public List<String> blacklistedPlayers = new ArrayList<>();

    @SerialEntry
    public boolean friendsEnabled = false;

    @SerialEntry
    public List<String> friends = new ArrayList<>();

    @SerialEntry
    public int friendColor = 0x55FF55;

    @SerialEntry
    public boolean friendBold = false;

    @SerialEntry
    public TimestampMode timestampMode = TimestampMode.OFF;

    @SerialEntry
    public String timestampFormat = "HH:mm";

    @SerialEntry
    public int timestampColor = 0x808080;

    @SerialEntry
    public boolean chatHeadsEnabled = true;

    @SerialEntry
    public boolean chatHeadsOffsetAllLines = true;

    @SerialEntry
    public boolean chatAnimationEnabled = true;

    @SerialEntry
    public int chatAnimationDurationMs = 200;

    @SerialEntry
    public boolean chatOpenAnimation = true;

    @SerialEntry
    public int chatHudOffset = 0;

    @SerialEntry
    public boolean chatBubbles = false;

    @SerialEntry
    public int chatBubbleMs = 6000;

    @SerialEntry
    public int chatBubbleLines = 3;

    @SerialEntry
    public int chatBubbleLength = 48;

    @SerialEntry
    public boolean chatBubbleHideSelf = true;

    @SerialEntry
    public boolean clickToCopy = true;

    @SerialEntry
    public boolean linkifyEnabled = true;

    @SerialEntry
    public List<String> linkAllowedDomains = new ArrayList<>();

    @SerialEntry
    public boolean compactSpamEnabled = false;

    @SerialEntry
    public int compactSpamWindowMs = 250;

    @SerialEntry
    public int compactSpamCounterColor = 0xAAAAAA;

    @SerialEntry
    public boolean antiClearEnabled = true;

    @SerialEntry
    public int antiClearThreshold = 5;

    @SerialEntry
    public boolean overrideChatWindow = true;

    @SerialEntry
    public boolean replaceChatSettings = true;

    @SerialEntry
    public boolean chatColors = true;

    @SerialEntry
    public boolean chatLinks = true;

    @SerialEntry
    public boolean chatLinksPrompt = true;

    @SerialEntry
    public double chatDelaySeconds = 0.0;

    @SerialEntry
    public double chatScale = 1.0;

    @SerialEntry
    public double chatWidth = 1.0;

    @SerialEntry
    public double chatHeightFocused = 1.0;

    @SerialEntry
    public double chatHeightUnfocused = 0.44;

    @SerialEntry
    public double chatOpacity = 1.0;

    @SerialEntry
    public double chatTextBackgroundOpacity = 0.5;

    @SerialEntry
    public double chatLineSpacing = 0.0;

    @SerialEntry
    public int historySize = 2000;

    @SerialEntry
    public boolean keepHistoryOnDisconnect = true;

    @SerialEntry
    public boolean sessionDivider = true;

    @SerialEntry
    public int sessionDividerColor = 0x555555;

    @SerialEntry
    public boolean chatSearchButton = true;

    @SerialEntry
    public ChatFont chatFont = ChatFont.NONE;

    @SerialEntry
    public boolean chatFontButton = true;

    @SerialEntry
    public boolean hideOwnName = false;

    @SerialEntry
    public String hideOwnNameChar = "*";

    @SerialEntry
    public List<String> hiddenNames = new ArrayList<>();

    @SerialEntry
    public List<String> commandKeys = new ArrayList<>();

    @SerialEntry
    public boolean autoReplyEnabled = false;

    @SerialEntry
    public List<String> autoReplyRules = new ArrayList<>();

    @SerialEntry
    public int autoReplyCooldownMs = 15000;

    @SerialEntry
    public boolean translateEnabled = false;

    @SerialEntry
    public String translateEndpoint = "";

    @SerialEntry
    public String translateApiKey = "";

    @SerialEntry
    public TranslateLanguage translateSource = TranslateLanguage.AUTO;

    @SerialEntry
    public TranslateLanguage translateTarget = TranslateLanguage.AUTO;

    @SerialEntry
    public boolean streamChatEnabled = false;

    @SerialEntry
    public String twitchChannel = "";

    @SerialEntry
    public String youtubeVideoId = "";

    @SerialEntry
    public String youtubeApiKey = "";

    @SerialEntry
    public int streamMaxPerTick = 2;

    @SerialEntry
    public boolean streamMentions = true;

    @SerialEntry
    public int twitchTagColor = 0x9146FF;

    @SerialEntry
    public int youtubeTagColor = 0xFF0000;

    @SerialEntry
    public boolean hideChatEnabled = false;

    @SerialEntry
    public boolean hideChatShowMentions = true;

    @SerialEntry
    public int hideChatRevealMs = 4000;

    @SerialEntry
    public boolean macrosEnabled = true;

    @SerialEntry
    public boolean calculatorEnabled = true;

    @SerialEntry
    public String calculatorPrefix = "=";

    @SerialEntry
    public List<String> customMacros = new ArrayList<>();

    @SerialEntry
    public boolean symbolBarEnabled = true;

    @SerialEntry
    public boolean symbolSuggestions = true;

    @SerialEntry
    public boolean symbolsEverywhere = true;

    @SerialEntry
    public boolean encryptionEnabled = false;

    @SerialEntry
    public String encryptionPassphrase = "";

    @SerialEntry
    public boolean noReportsEnabled = false;

    @SerialEntry
    public boolean noReportsWarnOnSecureServer = true;

    public static void save() {
        get().normalize();
        HANDLER.save();
    }

    private static List<String> cleanList(List<String> values, int maxEntries, int maxLength) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            String cleaned = trim(value, maxLength);
            if (!cleaned.isEmpty()) {
                unique.add(cleaned);
            }
            if (unique.size() == maxEntries) {
                break;
            }
        }
        return new ArrayList<>(unique);
    }

    private static String fallback(String value, String defaultValue, int maxLength) {
        String cleaned = trim(value, maxLength);
        return cleaned.isEmpty() ? defaultValue : cleaned;
    }

    private static String trim(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String cleaned = value.strip();
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
    }

    private static double clamp(double value, double min, double max) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
    }
}
