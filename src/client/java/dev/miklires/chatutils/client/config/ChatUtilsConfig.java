package dev.miklires.chatutils.client.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.miklires.chatutils.Chatutils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Every user-facing setting of the mod. Serialised to {@code config/chatutils.json} by YACL.
 *
 * <p>Colours are stored as plain {@code 0xRRGGBB} ints rather than {@code java.awt.Color} so the
 * JSON stays readable and hand-editable; {@link ConfigScreen} converts them for YACL's colour
 * picker.
 */
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

    // ------------------------------------------------------------------ mentions

    @SerialEntry
    public boolean mentionEnabled = true;

    /** Treat the player's own account name as a keyword without having to type it in. */
    @SerialEntry
    public boolean mentionUseOwnName = true;

    /** Extra words that trigger a mention: clan tags, nicknames, "help", whatever. */
    @SerialEntry
    public List<String> mentionKeywords = new ArrayList<>();

    /**
     * Read every keyword as a regular expression instead of as literal text.
     *
     * <p>Whole-word matching is ignored in this mode: a pattern already says for itself where it
     * starts and ends, and quietly wrapping it in word boundaries would change what it means.
     */
    @SerialEntry
    public boolean mentionRegex = false;

    /** Messages that must <em>not</em> match, checked after a keyword hit. Literal text, or regex. */
    @SerialEntry
    public List<String> mentionExclusions = new ArrayList<>();

    @SerialEntry
    public boolean mentionCaseSensitive = false;

    /** Only fire when the keyword stands alone, so "Mik" does not match "Mikhail". */
    @SerialEntry
    public boolean mentionWholeWord = true;

    /** Do not notify for messages the player sent themselves. */
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

    /**
     * Either a vanilla sound id ({@code minecraft:entity.experience_orb.pickup}) or the name of a
     * {@code .wav} file inside {@code config/chatutils/sounds/}.
     */
    @SerialEntry
    public String mentionSoundId = "minecraft:entity.experience_orb.pickup";

    @SerialEntry
    public float mentionSoundVolume = 1.0f;

    @SerialEntry
    public float mentionSoundPitch = 1.0f;

    @SerialEntry
    public boolean mentionHotbarText = true;

    /** {@code %s} is replaced with the name of whoever mentioned you (or "?" when unknown). */
    @SerialEntry
    public String mentionHotbarFormat = "%s mentioned you";

    /** Minimum gap between two mention notifications, so a spamming player cannot ear-rape you. */
    @SerialEntry
    public int mentionCooldownMs = 2000;

    // ------------------------------------------------------------------ message sounds

    @SerialEntry
    public boolean messageSoundEnabled = false;

    @SerialEntry
    public String messageSoundId = "minecraft:ui.button.click";

    @SerialEntry
    public float messageSoundVolume = 0.5f;

    @SerialEntry
    public float messageSoundPitch = 1.0f;

    /** Only play the sound for messages that came from a player, not for server spam. */
    @SerialEntry
    public boolean messageSoundOnlyPlayers = true;

    // ------------------------------------------------------------------ word filter

    @SerialEntry
    public boolean filterEnabled = true;

    @SerialEntry
    public FilterMode filterMode = FilterMode.CENSOR;

    @SerialEntry
    public List<String> filteredWords = new ArrayList<>();

    /** Treat entries in the word list as regular expressions instead of plain substrings. */
    @SerialEntry
    public boolean filterRegex = false;

    @SerialEntry
    public String filterCensorChar = "*";

    /**
     * Let a censored word be clicked to see what it said, privately.
     *
     * <p>You wrote the word list, so this keeps no secret from you — it makes a false positive
     * diagnosable instead of mysterious. Turn it off if someone else watches your screen.
     */
    @SerialEntry
    public boolean filterRevealable = true;

    // ------------------------------------------------------------------ player blacklist

    @SerialEntry
    public boolean blacklistEnabled = false;

    /** Messages authored by these players never reach the chat. */
    @SerialEntry
    public List<String> blacklistedPlayers = new ArrayList<>();

    // ------------------------------------------------------------------ friends

    @SerialEntry
    public boolean friendsEnabled = false;

    @SerialEntry
    public List<String> friends = new ArrayList<>();

    @SerialEntry
    public int friendColor = 0x55FF55;

    @SerialEntry
    public boolean friendBold = false;

    // ------------------------------------------------------------------ appearance

    @SerialEntry
    public TimestampMode timestampMode = TimestampMode.OFF;

    @SerialEntry
    public String timestampFormat = "HH:mm";

    @SerialEntry
    public int timestampColor = 0x808080;

    /** Draw the sender's head beside their message. */
    @SerialEntry
    public boolean chatHeadsEnabled = true;

    /**
     * Indent lines that have no head too, so every line starts at the same x. Without it, server
     * messages and player messages zig-zag against each other.
     */
    @SerialEntry
    public boolean chatHeadsOffsetAllLines = true;

    /** Slide the chat up when a message arrives instead of snapping it into place. */
    @SerialEntry
    public boolean chatAnimationEnabled = true;

    @SerialEntry
    public int chatAnimationDurationMs = 200;

    /** Ease the chat in when you open it, instead of it appearing at full height at once. */
    @SerialEntry
    public boolean chatOpenAnimation = true;

    /**
     * Pixels to lift the chat off the bottom of the screen, to clear the armour bar.
     *
     * <p>Only while the chat is closed. Moving the chat moves what you see but not where the game
     * thinks its lines are, so a lifted chat would have links that do not click where they look —
     * with the chat closed there is nothing to click, which is also exactly when you want the space.
     */
    @SerialEntry
    public int chatHudOffset = 0;

    // ------------------------------------------------------------------ chat bubbles

    /**
     * Show what a player said above their head, for a few seconds.
     *
     * <p>Off by default. It draws in the world rather than in the interface, which is the one part of
     * the game that changes most between versions — and a feature that can look wrong is better asked
     * for than assumed.
     */
    @SerialEntry
    public boolean chatBubbles = false;

    /** How long a bubble stays up. */
    @SerialEntry
    public int chatBubbleMs = 6000;

    /** Lines kept per player; older ones drop off the top. */
    @SerialEntry
    public int chatBubbleLines = 3;

    /** Characters per line before it is cut — a wall of text over someone's head is not a bubble. */
    @SerialEntry
    public int chatBubbleLength = 48;

    /** Do not draw your own bubble; you already know what you said, and it sits in your face. */
    @SerialEntry
    public boolean chatBubbleHideSelf = true;

    /** Click a word in chat to copy it. Links and censored words keep their own click behaviour. */
    @SerialEntry
    public boolean clickToCopy = true;

    @SerialEntry
    public boolean linkifyEnabled = true;

    /**
     * Domains whose links may be clicked, e.g. {@code imgur.com}. Subdomains are covered, so
     * {@code i.imgur.com} needs no separate entry.
     *
     * <p>Empty means every link is clickable. With anything in it, links elsewhere are still shown
     * and still readable — they simply stop being clickable, which is the point: a chat link is
     * someone else's text, and one misread character is all a lookalike domain needs.
     */
    @SerialEntry
    public List<String> linkAllowedDomains = new ArrayList<>();

    /** Collapse identical consecutive messages into one line with an {@code (x4)} counter. */
    @SerialEntry
    public boolean compactSpamEnabled = false;

    /**
     * How long a message waits before being drawn, so identical repeats can be merged into it.
     * Higher values catch more spam but make chat feel laggy.
     */
    @SerialEntry
    public int compactSpamWindowMs = 250;

    @SerialEntry
    public int compactSpamCounterColor = 0xAAAAAA;

    /**
     * Ignore the flood of empty lines servers send to "clear" your chat. A run longer than
     * {@link #antiClearThreshold} blank messages is dropped entirely.
     */
    @SerialEntry
    public boolean antiClearEnabled = true;

    @SerialEntry
    public int antiClearThreshold = 5;

    // ------------------------------------------------------------------ chat window

    /** Push the values below into the vanilla chat options on join. */
    /**
     * Drive the game's own chat options from this config.
     *
     * <p>On by default, now that this mod's settings are where the "Chat Settings…" button leads:
     * the defaults below match vanilla's, so turning it on changes nothing until you move a slider.
     */
    @SerialEntry
    public boolean overrideChatWindow = true;

    /** Send the game's "Chat Settings…" button here instead of to its own screen. */
    @SerialEntry
    public boolean replaceChatSettings = true;

    /** Vanilla's own chat options, brought across so they live in one place. */
    @SerialEntry
    public boolean chatColors = true;

    @SerialEntry
    public boolean chatLinks = true;

    /** Ask before opening a link from chat in a browser. */
    @SerialEntry
    public boolean chatLinksPrompt = true;

    /** Seconds a message waits before appearing. Vanilla's "chat delay"; 0 is off. */
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

    // ------------------------------------------------------------------ history

    /**
     * How many messages the chat keeps. Vanilla stores 100; this raises the cap so scrolling back
     * actually gets you somewhere.
     */
    @SerialEntry
    public int historySize = 2000;

    /** Keep the backlog when leaving a world, so rejoining does not wipe the chat. */
    @SerialEntry
    public boolean keepHistoryOnDisconnect = true;

    /**
     * Draw a divider naming the world or server when you join one.
     *
     * <p>Only meaningful alongside the kept backlog: once two sessions share one chat, there is
     * otherwise nothing to say where the old one ended and the new one began.
     */
    @SerialEntry
    public boolean sessionDivider = true;

    @SerialEntry
    public int sessionDividerColor = 0x555555;

    /** Open the search screen straight from the chat, without closing it and pressing the key. */
    @SerialEntry
    public boolean chatSearchButton = true;

    // ------------------------------------------------------------------ outgoing text

    /**
     * Unicode look-alike alphabet applied to messages you send — see {@link ChatFont}.
     *
     * <p>Never applied to commands: {@code /ᵍᵃᵐᵉᵐᵒᵈᵉ} is not a command.
     */
    @SerialEntry
    public ChatFont chatFont = ChatFont.NONE;

    /** The picker beside the chat input. On by default: a font nobody can find is a font that does not work. */
    @SerialEntry
    public boolean chatFontButton = true;

    // ------------------------------------------------------------------ name hiding

    /**
     * Mask your own account name wherever it appears in chat — for streaming and screenshots.
     *
     * <p>Masked with a repeated character, not swapped for an alias: the pipeline edits the line in
     * place and the replacement has to be the same length.
     */
    @SerialEntry
    public boolean hideOwnName = false;

    @SerialEntry
    public String hideOwnNameChar = "*";

    /** Anything else to keep out of the chat: an alt's name, a clan tag, a real name. */
    @SerialEntry
    public List<String> hiddenNames = new ArrayList<>();

    // ------------------------------------------------------------------ command hotkeys

    /**
     * Hotkeys, as {@code KEY | what to send} — for example {@code G | /home}.
     *
     * <p>An entry starting with {@code /} is sent as a command, anything else as a chat message.
     * Sent exactly as though it had been typed, so macros, the font and encryption still apply.
     */
    @SerialEntry
    public List<String> commandKeys = new ArrayList<>();

    // ------------------------------------------------------------------ auto-reply

    /**
     * Answer phrases automatically: someone says {@code gg}, you say {@code ggwp}.
     *
     * <p>Off with an empty list, and worth leaving that way unless you want it. Two clients with
     * mirrored rules would talk to each other forever, and a server cannot tell an eager player from
     * a bot — people are banned for this. The brakes in {@code AutoResponder} exist for that reason,
     * and one of them is a ceiling no setting can raise.
     */
    @SerialEntry
    public boolean autoReplyEnabled = false;

    /** Entries are {@code phrase | response}, e.g. {@code gg | ggwp}. Whole words only. */
    @SerialEntry
    public List<String> autoReplyRules = new ArrayList<>();

    /** Minimum gap before the same phrase may be answered again. */
    @SerialEntry
    public int autoReplyCooldownMs = 15000;

    // ------------------------------------------------------------------ translation

    /** Off until you have somewhere to translate through. */
    @SerialEntry
    public boolean translateEnabled = false;

    /**
     * A LibreTranslate-compatible endpoint.
     *
     * <p>Deliberately empty out of the box. Shipping a working public address would have every copy
     * of this mod pointed at one stranger's server by default, and would quietly make "translate"
     * mean "send this line to a third party" for anyone who never read the setting. Your own
     * instance, or a service you chose, or nothing.
     */
    @SerialEntry
    public String translateEndpoint = "";

    /** Whatever your endpoint wants, if it wants one. */
    @SerialEntry
    public String translateApiKey = "";

    @SerialEntry
    public TranslateLanguage translateSource = TranslateLanguage.AUTO;

    @SerialEntry
    public TranslateLanguage translateTarget = TranslateLanguage.AUTO;

    // ------------------------------------------------------------------ stream chat

    /**
     * Show Twitch and YouTube live chat in the Minecraft chat.
     *
     * <p>Read-only in both directions: nothing typed in Minecraft is ever sent to a stream.
     */
    @SerialEntry
    public boolean streamChatEnabled = false;

    /** Twitch channel to read, without the {@code #}. Connects anonymously — no token needed. */
    @SerialEntry
    public String twitchChannel = "";

    /** Video id of a live stream, i.e. the {@code v=} part of the watch URL. */
    @SerialEntry
    public String youtubeVideoId = "";

    /**
     * YouTube Data API v3 key. Unlike Twitch, YouTube has no anonymous read endpoint.
     *
     * <p>Stored in this file in plain text, like every other setting — treat {@code chatutils.json}
     * as something not to share once a key is in it, and use a key restricted to the YouTube Data
     * API so a leak costs no more than the quota.
     */
    @SerialEntry
    public String youtubeApiKey = "";

    /** Messages injected per platform per tick. The brake that keeps a busy channel readable. */
    @SerialEntry
    public int streamMaxPerTick = 2;

    /** Fire the usual mention alert when a stream message trips one of the mention keywords. */
    @SerialEntry
    public boolean streamMentions = true;

    @SerialEntry
    public int twitchTagColor = 0x9146FF;

    @SerialEntry
    public int youtubeTagColor = 0xFF0000;

    // ------------------------------------------------------------------ hidden chat

    /**
     * Stop drawing the chat while you play. Messages still arrive and still go into the history —
     * they are simply not on screen until you open the chat or hold the peek key.
     */
    @SerialEntry
    public boolean hideChatEnabled = false;

    /** Show the chat for a moment when someone mentions you, even while it is hidden. */
    @SerialEntry
    public boolean hideChatShowMentions = true;

    @SerialEntry
    public int hideChatRevealMs = 4000;

    // ------------------------------------------------------------------ macros

    @SerialEntry
    public boolean macrosEnabled = true;

    /**
     * Evaluate arithmetic typed into the chat: a message starting with the prefix is answered
     * privately, and {@code [=1+2]} inside a message is replaced before sending.
     */
    @SerialEntry
    public boolean calculatorEnabled = true;

    @SerialEntry
    public String calculatorPrefix = "=";

    /** Custom expansions in {@code name=text} form, expanded as {@code [name]}. */
    @SerialEntry
    public List<String> customMacros = new ArrayList<>();

    // ------------------------------------------------------------------ symbol bar

    @SerialEntry
    public boolean symbolBarEnabled = true;

    /** Offer symbols as you type, after a colon: {@code :cro} suggests a croissant, a crown, a crocodile. */
    @SerialEntry
    public boolean symbolSuggestions = true;

    /** Put the symbol button on signs, books and the anvil as well as the chat. */
    @SerialEntry
    public boolean symbolsEverywhere = true;

    // ------------------------------------------------------------------ privacy

    /**
     * Send chat messages without a cryptographic signature, so they cannot be attached to a Mojang
     * report. Servers running {@code enforce-secure-profile=true} reject unsigned chat.
     */
    /**
     * Encrypt chat between people who share a passphrase.
     *
     * <p>The server relays your messages but cannot read them. Everyone in the conversation needs
     * this mod and the same phrase; to everyone else the line is a run of Base64. That cost is the
     * whole reason this is off by default.
     */
    @SerialEntry
    public boolean encryptionEnabled = false;

    /**
     * The shared secret. Anyone who has it can read every message sent with it, so it is worth as
     * much as the conversation is — and it sits in {@code chatutils.json} in plain text.
     */
    @SerialEntry
    public String encryptionPassphrase = "";

    @SerialEntry
    public boolean noReportsEnabled = false;

    /** Warn in chat when joining a server that demands signed messages. */
    @SerialEntry
    public boolean noReportsWarnOnSecureServer = true;

    // ------------------------------------------------------------------ helpers

    public static void save() {
        HANDLER.save();
    }
}
