package dev.miklires.chatutils.client.chat.processor;

import dev.miklires.chatutils.client.chat.AuthorResolver;
import dev.miklires.chatutils.client.chat.ChatMessage;
import dev.miklires.chatutils.client.chat.MentionRule;
import dev.miklires.chatutils.client.chat.TextMatcher;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

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

        String plain = message.plain();
        int bodyStart = AuthorResolver.bodyStart(plain);
        String body = plain.substring(bodyStart);

        if (isExcluded(body, config)) {
            return;
        }

        MentionRule fired = null;

        for (MentionRule rule : rules) {
            List<TextMatcher.Match> matches = find(body, rule.keyword(), config);
            if (matches.isEmpty()) {
                continue;
            }
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
