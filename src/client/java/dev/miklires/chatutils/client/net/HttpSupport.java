package dev.miklires.chatutils.client.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class HttpSupport {
    public static final int MAX_JSON_BYTES = 1_048_576;

    private HttpSupport() {
    }

    public static URI translationEndpoint(String value) {
        URI uri = URI.create(value);
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null || uri.getUserInfo() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("invalid translation endpoint");
        }
        boolean secure = scheme.equalsIgnoreCase("https");
        boolean localHttp = scheme.equalsIgnoreCase("http") && isLoopback(host);
        if (!secure && !localHttp) {
            throw new IllegalArgumentException("translation endpoint must use HTTPS");
        }
        return uri;
    }

    public static String readUtf8(HttpResponse<InputStream> response, int maxBytes) throws IOException {
        try (InputStream input = response.body()) {
            byte[] bytes = input.readNBytes(maxBytes + 1);
            if (bytes.length > maxBytes) {
                throw new IOException("response is too large");
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    public static String safeReason(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ")
                .replace('\r', ' ').replace('\n', ' ').strip();
        return cleaned.substring(0, Math.min(cleaned.length(), 240));
    }

    private static boolean isLoopback(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        return normalized.equals("localhost") || normalized.equals("::1") || normalized.startsWith("127.");
    }
}
