package dev.miklires.chatutils.client.chat.processor;

import dev.miklires.chatutils.client.chat.ChatMessage;
import dev.miklires.chatutils.client.chat.TextMatcher;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import dev.miklires.chatutils.client.config.FilterMode;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Censors — or hides outright — messages containing a blocked word.
 *
 * <p>A censored word can be clicked to see what it really said. The filter is there so the word does
 * not hit you unasked, not to keep a secret from you: you wrote the list, and being unable to check
 * what was matched makes a false positive impossible to diagnose. The original goes to you alone and
 * is never sent anywhere.
 *
 * <p>Matching runs through {@code TextMatcher}, which folds the mod's Unicode alphabets back to
 * plain letters first — otherwise picking a font would walk straight past the filter.
 */
public final class FilterProcessor implements ChatProcessor {

    /**
     * Originals of recently censored words, keyed by the id in their click event.
     *
     * <p>Bounded and in memory only. Nothing here is ever written to disk: the whole point of the
     * filter is that these words are unwelcome, and a file of them would be worse than the messages.
     */
    private static final int REMEMBERED = 128;

    private static final Map<String, String> originals = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > REMEMBERED;
        }
    };

    private static int nextId;

    @Override
    public void apply(ChatMessage message, ChatUtilsConfig config) {
        if (!config.filterEnabled || config.filteredWords.isEmpty()) {
            return;
        }

        char censor = config.filterCensorChar == null || config.filterCensorChar.isEmpty()
                ? '*'
                : config.filterCensorChar.charAt(0);
        // Snapshot the text once: censoring only overwrites characters, so indices stay valid, and
        // matching against the snapshot keeps one blocked word from matching the stars of another.
        String plain = message.plain();

        for (String word : config.filteredWords) {
            List<TextMatcher.Match> matches = config.filterRegex
                    ? TextMatcher.findRegex(plain, word, false)
                    : TextMatcher.findLiteral(plain, word, false, false);
            if (matches.isEmpty()) {
                continue;
            }
            if (config.filterMode == FilterMode.HIDE) {
                message.cancel();
                return;
            }
            for (TextMatcher.Match match : matches) {
                message.fillRange(match.start(), match.end(), censor);
                if (config.filterRevealable) {
                    offerReveal(message, plain.substring(match.start(), match.end()),
                            match.start(), match.end());
                }
            }
        }
    }

    /** Hangs a "show me what this said" click event on the stars that replaced the word. */
    private static void offerReveal(ChatMessage message, String original, int start, int end) {
        String id = String.valueOf(nextId++);
        originals.put(id, original);

        message.styleRange(start, end, style -> style
                .withClickEvent(new ClickEvent.RunCommand("/chatutils reveal " + id))
                .withHoverEvent(new HoverEvent.ShowText(
                        Component.translatable("chatutils.filter.reveal_hint"))));
    }

    /** @return what the word said, or null once it has aged out of the buffer */
    @Nullable
    public static String reveal(String id) {
        return originals.get(id);
    }
}
