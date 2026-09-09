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

public final class FilterProcessor implements ChatProcessor {

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

    private static void offerReveal(ChatMessage message, String original, int start, int end) {
        String id = String.valueOf(nextId++);
        originals.put(id, original);

        message.styleRange(start, end, style -> style
                .withClickEvent(new ClickEvent.RunCommand("/chatutils reveal " + id))
                .withHoverEvent(new HoverEvent.ShowText(
                        Component.translatable("chatutils.filter.reveal_hint"))));
    }

    @Nullable
    public static String reveal(String id) {
        return originals.get(id);
    }
}
