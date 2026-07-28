package dev.miklires.chatutils.client.chat.processor;

import dev.miklires.chatutils.client.chat.AuthorResolver;
import dev.miklires.chatutils.client.chat.ChatMessage;
import dev.miklires.chatutils.client.chat.MentionRule;
import dev.miklires.chatutils.client.chat.TextMatcher;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects mentions and highlights them.
 *
 * <p>Only flags the message; the actual sound and hotbar text are fired by
 * {@code MentionNotifier} once the whole pipeline has run, so a message that a later processor
 * cancels never notifies.
 */
public final class MentionProcessor implements ChatProcessor {

    @Override
    public void apply(ChatMessage message, ChatUtilsConfig config) {
        if (!config.mentionEnabled) {
            return;
        }
        if (config.mentionIgnoreSelf && AuthorResolver.isLocalPlayer(message.author())) {
            return;
        }

        List<MentionRule> rules = rules(config);
        if (rules.isEmpty()) {
            return;
        }

        // Search the body only. A name in the "<Nick>" header says who is talking, not who is being
        // talked to — matching there would make every one of your own messages a self-mention, and
        // would fire on any line a friend merely happened to send.
        String plain = message.plain();
        int bodyStart = AuthorResolver.bodyStart(plain);
        String body = plain.substring(bodyStart);

        // Exclusions are checked before anything is highlighted: a line the player asked to ignore
        // should look exactly as it would have with the mention feature off.
        if (isExcluded(body, config)) {
            return;
        }

        MentionRule fired = null;

        for (MentionRule rule : rules) {
            List<TextMatcher.Match> matches = find(body, rule.keyword(), config);
            if (matches.isEmpty()) {
                continue;
            }
            // First match wins the sound: two alarms at once is worse than either alone.
            if (fired == null) {
                fired = rule;
            }

            if (config.mentionHighlight) {
                int color = rule.hasColor() ? rule.color() : config.mentionHighlightColor;
                for (TextMatcher.Match match : matches) {
                    message.styleRange(bodyStart + match.start(), bodyStart + match.end(), style -> {
                        var styled = style.withColor(color);
                        return config.mentionHighlightBold ? styled.withBold(true) : styled;
                    });
                }
            }
        }

        if (fired != null) {
            message.markMention(fired);
        }
    }

    private static List<TextMatcher.Match> find(String body, String keyword, ChatUtilsConfig config) {
        return config.mentionRegex
                ? TextMatcher.findRegex(body, keyword, config.mentionCaseSensitive)
                : TextMatcher.findLiteral(body, keyword, config.mentionCaseSensitive, config.mentionWholeWord);
    }

    /** True when any exclusion matches, which vetoes the whole message. */
    private static boolean isExcluded(String body, ChatUtilsConfig config) {
        for (String exclusion : config.mentionExclusions) {
            if (exclusion == null || exclusion.isBlank()) {
                continue;
            }
            if (!find(body, exclusion, config).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The configured rules, plus the player's own name when that option is on.
     *
     * <p>The own name is quoted in regex mode: an account name is literal text, and a player whose
     * nick contains a {@code .} or a {@code +} should not have it read as a pattern.
     */
    private static List<MentionRule> rules(ChatUtilsConfig config) {
        List<MentionRule> rules = new ArrayList<>();
        for (String entry : config.mentionKeywords) {
            if (entry != null && !entry.isBlank()) {
                MentionRule rule = MentionRule.parse(entry, config.mentionRegex);
                if (!rule.keyword().isBlank()) {
                    rules.add(rule);
                }
            }
        }

        if (config.mentionUseOwnName) {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                String name = player.getGameProfile().name();
                rules.add(new MentionRule(
                        config.mentionRegex ? java.util.regex.Pattern.quote(name) : name,
                        MentionRule.INHERIT, null));
            }
        }
        return rules;
    }
}
