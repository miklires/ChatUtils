package dev.miklires.chatutils.client.chat.processor;

import dev.miklires.chatutils.client.chat.ChatMessage;
import dev.miklires.chatutils.client.chat.DomainMatcher;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns bare URLs in chat into clickable links.
 *
 * <p>Vanilla only does this for messages the client itself formats; anything a server sends as plain
 * text stays dead. Links keep whatever style they already had and just gain the underline, colour
 * and click handler.
 *
 * <p>An allowed-domain list narrows that: with one configured, only links to those domains become
 * clickable and everything else is left as inert text. The text is never removed — you can still
 * read the address and decide for yourself — it simply stops being one click away, which is the
 * useful half of the protection. An empty list means every link is clickable, as before.
 */
public final class LinkProcessor implements ChatProcessor {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)\\b(?:https?://|www\\.)[-a-z0-9+&@#/%?=~_|!:,.;()]*[-a-z0-9+&@#/%=~_|()]");

    @Override
    public void apply(ChatMessage message, ChatUtilsConfig config) {
        if (!config.linkifyEnabled) {
            return;
        }

        Matcher matcher = URL_PATTERN.matcher(message.plain());
        while (matcher.find()) {
            String raw = matcher.group();
            URI uri = parse(raw);
            if (uri == null) {
                continue;
            }

            if (!DomainMatcher.isAllowed(host(uri), config.linkAllowedDomains)) {
                blocked(message, matcher.start(), matcher.end());
                continue;
            }

            message.styleRange(matcher.start(), matcher.end(), style -> style
                    .withColor(ChatFormatting.AQUA)
                    .withUnderlined(true)
                    .withClickEvent(new ClickEvent.OpenUrl(uri))
                    .withHoverEvent(new HoverEvent.ShowText(
                            Component.translatable("chatutils.link.open", raw))));
        }
    }

    /**
     * Marks a link that is not on the allowed list: still readable, still copyable from the history
     * screen, but with no click event and a hover that says why.
     */
    private static void blocked(ChatMessage message, int start, int end) {
        message.styleRange(start, end, style -> style
                .withColor(ChatFormatting.GRAY)
                .withUnderlined(false)
                .withHoverEvent(new HoverEvent.ShowText(
                        Component.translatable("chatutils.link.blocked"))));
    }

    /**
     * {@code URI#getHost} returns null whenever the authority is not strictly legal — which chat
     * links often are not — so fall back to reading the authority out of the raw string.
     */
    private static String host(URI uri) {
        String host = uri.getHost();
        if (host != null) {
            return host;
        }
        String authority = uri.getAuthority();
        return authority != null ? authority : uri.getSchemeSpecificPart();
    }

    private static URI parse(String raw) {
        try {
            return URI.create(raw.startsWith("www.") ? "http://" + raw : raw);
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }
}
