package dev.miklires.chatutils.client.chat;

import java.util.List;
import java.util.Locale;

public final class DomainMatcher {

    private DomainMatcher() {
    }

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
            if (candidate.equals(domain) || candidate.endsWith("." + domain)) {
                return true;
            }
        }
        return false;
    }

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
        while (result.endsWith(".")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
