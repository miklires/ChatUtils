package dev.miklires.chatutils.client.stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Reads a YouTube live stream's chat through the official Data API.
 *
 * <p>Unlike Twitch, YouTube has no anonymous read endpoint, so this needs an API key the player
 * creates in Google Cloud themselves. That is a real cost and it is why this half is off by default:
 * a key is a credential, it lands in {@code chatutils.json} in plain text like every other setting,
 * and it counts against a daily quota that live-chat polling eats quickly.
 *
 * <p>The alternative — scraping the page's internal chat endpoint the way most desktop chat clients
 * do — needs no key, but it is undocumented, breaks whenever YouTube changes it, and is exactly the
 * kind of thing that gets an IP blocked. The supported API is the honest option even though it asks
 * more of the player.
 *
 * <p>The server dictates the poll interval and this obeys it. Polling faster than told is how a key
 * gets its quota burned before the stream is over.
 */
public final class YouTubeSource extends StreamSource {

    private static final String API = "https://www.googleapis.com/youtube/v3/";

    /** Used until the API states its own interval, and as a floor if it ever asks for less. */
    private static final long MIN_POLL_MS = 3000L;

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
        // The first page is the whole backlog since the stream started; skipping it keeps a join
        // from dumping thousands of old messages into the Minecraft chat.
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
                wait = Math.max(MIN_POLL_MS, interval.getAsLong());
            }
            Thread.sleep(wait);
        }
    }

    @Override
    protected void close() {
        closed = true;
    }

    /** Turns a video id into the live chat id the messages endpoint wants. */
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

        // Superchats, memberships and moderation events all live in this feed; only plain text
        // messages carry displayMessage, so anything without it is skipped rather than guessed at.
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

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();

        if (response.statusCode() != 200) {
            // Google's error bodies are far more useful than the status code, but they can also echo
            // the request — so only the message is taken, never the whole body, to keep the key out
            // of the log and out of the chat.
            JsonObject error = object(body, "error");
            String reason = error == null ? null : string(error, "message");
            throw new IllegalStateException("YouTube API " + response.statusCode()
                    + (reason == null ? "" : ": " + reason));
        }
        return body;
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
