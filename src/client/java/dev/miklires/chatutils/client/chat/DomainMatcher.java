package dev.miklires.chatutils.client.chat;

import java.util.List;
import java.util.Locale;

/**
 * Decides whether a link's host is on the allowed list.
 *
 * <p>A rule of {@code imgur.com} covers {@code imgur.com} and any subdomain of it, because
 * {@code i.imgur.com} is where the images actually live and nobody wants to list both. It does
 * <em>not</em> cover {@code notimgur.com} or {@code imgur.com.example.net} — a suffix test on the
 * bare string would match both, and both are exactly the trick this list exists to stop.
 */
public final class DomainMatcher {

    private DomainMatcher() {
    }

    /** An empty list allows everything: the feature is off until the player names a domain. */
    public static boolean isAllowed(String host, List<String> allowed) {
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }
        if (host == null || host.isBlank()) {
            return false;
        }

        String candidate = normalise(host);
        for (String rule : allowed) {
            if (rule == null || rule.isBlank()) {
                continue;
            }
            String domain = normalise(rule);
            // The dot matters: it is what makes this a subdomain test rather than a suffix test.
            if (candidate.equals(domain) || candidate.endsWith("." + domain)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lower-cases, drops a trailing dot and strips anything the player might have pasted around the
     * domain — a rule copied out of the address bar is a URL, not a host, and refusing to understand
     * that would just look broken.
     */
    private static String normalise(String value) {
        String result = value.trim().toLowerCase(Locale.ROOT);

        int scheme = result.indexOf("://");
        if (scheme >= 0) {
            result = result.substring(scheme + 3);
        }
        int slash = result.indexOf('/');
        if (slash >= 0) {
            result = result.substring(0, slash);
        }
        int at = result.indexOf('@');
        if (at >= 0) {
            result = result.substring(at + 1);
        }
        int colon = result.indexOf(':');
        if (colon >= 0) {
            result = result.substring(0, colon);
        }
        if (result.startsWith("www.")) {
            result = result.substring(4);
        }
        // A fully qualified name ends in a dot; "imgur.com." and "imgur.com" are the same host.
        while (result.endsWith(".")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
