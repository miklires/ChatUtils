package dev.miklires.chatutils.client.stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.miklires.chatutils.client.net.HttpSupport;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class YouTubeSource extends StreamSource {

    private static final String API = "https://www.googleapis.com/youtube/v3/";

    private static final long MIN_POLL_MS = 3000L;
    private static final long MAX_POLL_MS = 60_000L;

    private final String videoId;
    private final String apiKey;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private volatile boolean closed;

    public YouTubeSource(String videoId, String apiKey) {
        super(videoId);
        this.videoId = videoId.trim();
        this.apiKey = apiKey.trim();
        if (!this.videoId.matches("[A-Za-z0-9_-]{6,64}")) {
            throw new IllegalArgumentException("invalid YouTube video id");
        }
        if (this.apiKey.isEmpty() || this.apiKey.length() > 4096) {
            throw new IllegalArgumentException("invalid YouTube API key");
        }
    }

    @Override
    public String name() {
        return "youtube";
    }

    @Override
    protected void run() throws Exception {
        String liveChatId = resolveLiveChatId();
        if (liveChatId == null) {
            throw new IllegalStateException("video " + videoId + " has no active live chat");
        }

        String pageToken = null;
        boolean firstPage = true;

        while (shouldRun() && !closed) {
            JsonObject response = get(API + "liveChat/messages"
                    + "?liveChatId=" + encode(liveChatId)
                    + "&part=snippet,authorDetails"
                    + "&maxResults=200"
                    + (pageToken == null ? "" : "&pageToken=" + encode(pageToken))
                    + "&key=" + encode(apiKey));

            pageToken = string(response, "nextPageToken");

            if (!firstPage) {
                JsonElement items = response.get("items");
                if (items != null && items.isJsonArray()) {
                    for (JsonElement item : items.getAsJsonArray()) {
                        emit(item);
                    }
                }
            }
            firstPage = false;

            long wait = MIN_POLL_MS;
            JsonElement interval = response.get("pollingIntervalMillis");
            if (interval != null && interval.isJsonPrimitive()) {
                wait = Math.clamp(interval.getAsLong(), MIN_POLL_MS, MAX_POLL_MS);
            }
            Thread.sleep(wait);
        }
    }

    @Override
    protected void close() {
        closed = true;
    }

    private String resolveLiveChatId() throws Exception {
        JsonObject response = get(API + "videos?part=liveStreamingDetails&id="
                + encode(videoId) + "&key=" + encode(apiKey));

        JsonElement items = response.get("items");
        if (items == null || !items.isJsonArray() || items.getAsJsonArray().isEmpty()) {
            throw new IllegalStateException("no video with id " + videoId);
        }

        JsonArray array = items.getAsJsonArray();
        JsonObject details = object(array.get(0).getAsJsonObject(), "liveStreamingDetails");
        return details == null ? null : string(details, "activeLiveChatId");
    }

    private void emit(JsonElement item) {
        if (!item.isJsonObject()) {
            return;
        }
        JsonObject snippet = object(item.getAsJsonObject(), "snippet");
        JsonObject author = object(item.getAsJsonObject(), "authorDetails");
        if (snippet == null || author == null) {
            return;
        }

        String text = string(snippet, "displayMessage");
        String name = string(author, "displayName");
        if (text == null || text.isBlank() || name == null || name.isBlank()) {
            return;
        }

        deliver(new StreamMessage(StreamMessage.Platform.YOUTUBE, name, text, 0));
    }

    private JsonObject get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        String responseBody = HttpSupport.readUtf8(response, HttpSupport.MAX_JSON_BYTES);

        if (response.statusCode() != 200) {
            String reason = null;
            try {
                JsonObject error = object(JsonParser.parseString(responseBody).getAsJsonObject(), "error");
                reason = error == null ? null : HttpSupport.safeReason(string(error, "message"));
            } catch (RuntimeException ignored) {
            }
            throw new IllegalStateException("YouTube API " + response.statusCode()
                    + (reason == null ? "" : ": " + reason));
        }
        return JsonParser.parseString(responseBody).getAsJsonObject();
    }

    private static JsonObject object(JsonObject parent, String key) {
        JsonElement element = parent.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String string(JsonObject parent, String key) {
        JsonElement element = parent.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
