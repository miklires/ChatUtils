package dev.miklires.chatutils.client.translate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.miklires.chatutils.client.chat.ChatDelivery;
import dev.miklires.chatutils.client.chat.ChatHistory;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import dev.miklires.chatutils.client.net.HttpSupport;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Translates a chat message on demand, through a LibreTranslate-compatible endpoint.
 *
 * <p>On demand rather than automatically: translating every line would send the whole chat to a
 * third party and burn whatever quota the endpoint has within minutes. A key and the last message
 * are almost always what you actually wanted.
 *
 * <p>LibreTranslate rather than a big provider's API because it is a documented, open protocol with
 * public and self-hosted instances — the endpoint is a config field, so anyone can point it at their
 * own server and send nothing to anybody. The one thing this will not do is scrape a provider's
 * private web endpoint: that breaks without warning and gets IPs blocked.
 *
 * <p>The request runs on a daemon thread and the answer is posted from the client tick. A slow or
 * unreachable endpoint can only ever mean "no translation yet".
 */
public final class Translator {

    private static final Logger LOGGER = LoggerFactory.getLogger("chatutils/translate");

    /** Finished translations and failures, waiting for the game thread to show them. */
    private static final ConcurrentLinkedQueue<Component> answers = new ConcurrentLinkedQueue<>();

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /** One request at a time: a held key must not open twenty connections. */
    private static final AtomicBoolean busy = new AtomicBoolean();

    private Translator() {
    }

    /** Translates the newest message in the log. Bound to a key, and to {@code /chatutils translate}. */
    public static void translateLast() {
        List<ChatHistory.Entry> recent = ChatHistory.search("");
        if (recent.isEmpty()) {
            report(Component.translatable("chatutils.translate.nothing"));
            return;
        }
        translate(recent.get(0).plain());
    }

    public static void translate(String text) {
        ChatUtilsConfig config = ChatUtilsConfig.get();
        String endpoint = config.translateEndpoint == null ? "" : config.translateEndpoint.trim();

        if (!config.translateEnabled) {
            report(Component.translatable("chatutils.translate.off").withStyle(ChatFormatting.RED));
            return;
        }
        if (endpoint.isEmpty()) {
            report(Component.translatable("chatutils.translate.no_endpoint").withStyle(ChatFormatting.RED));
            return;
        }
        if (text == null || text.isBlank()) {
            return;
        }
        URI endpointUri;
        try {
            endpointUri = HttpSupport.translationEndpoint(endpoint);
        } catch (IllegalArgumentException e) {
            report(Component.translatable("chatutils.translate.failed", e.getMessage())
                    .withStyle(ChatFormatting.RED));
            return;
        }
        if (!busy.compareAndSet(false, true)) {
            return;
        }

        String body = request(text, config);
        Thread worker = new Thread(() -> {
            try {
                answers.add(translated(endpointUri, body));
            } catch (Exception failure) {
                LOGGER.warn("Translation failed: {}", failure.toString());
                answers.add(Component.translatable("chatutils.translate.failed", failure.getMessage())
                        .withStyle(ChatFormatting.RED));
            } finally {
                busy.set(false);
            }
        }, "chatutils-translate");
        worker.setDaemon(true);
        worker.start();
    }

    /** Called every client tick; posts whatever came back. */
    public static void tick() {
        Component answer;
        while ((answer = answers.poll()) != null) {
            ChatDelivery.sendSystem(answer);
        }
    }

    private static String request(String text, ChatUtilsConfig config) {
        JsonObject json = new JsonObject();
        json.addProperty("q", text);
        json.addProperty("source", config.translateSource.sourceCode());
        json.addProperty("target", config.translateTarget.targetCode());
        json.addProperty("format", "text");

        String key = config.translateApiKey == null ? "" : config.translateApiKey.trim();
        if (!key.isEmpty()) {
            json.addProperty("api_key", key);
        }
        return json.toString();
    }

    private static Component translated(URI endpoint, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        String responseBody = HttpSupport.readUtf8(response, HttpSupport.MAX_JSON_BYTES);

        if (response.statusCode() != 200) {
            // The endpoint's own message is far more useful than the status code, but never the whole
            // body — the request is echoed back by some instances, key included.
            String reason = null;
            try {
                JsonElement error = JsonParser.parseString(responseBody).getAsJsonObject().get("error");
                reason = error != null && error.isJsonPrimitive()
                        ? HttpSupport.safeReason(error.getAsString()) : null;
            } catch (RuntimeException ignored) {
            }
            throw new IllegalStateException(response.statusCode() + (reason == null ? "" : ": " + reason));
        }

        JsonObject answer = JsonParser.parseString(responseBody).getAsJsonObject();

        JsonElement text = answer.get("translatedText");
        if (text == null || !text.isJsonPrimitive()) {
            throw new IllegalStateException("no translation in the reply");
        }

        return Component.translatable("chatutils.translate.prefix").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(text.getAsString()).withStyle(ChatFormatting.WHITE));
    }

    private static void report(Component message) {
        answers.add(message.copy().withStyle(ChatFormatting.GRAY));
    }

}
