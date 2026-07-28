package dev.miklires.chatutils.client.chat;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * The mod's own record of everything that reached the chat.
 *
 * <p>Kept separately from the chat widget because the widget stores lines, not messages: text is
 * already wrapped, styled and stripped of any notion of who sent it. Searching and copying want the
 * message as it was — one entry, whole, with its author.
 *
 * <p>Messages the pipeline dropped never get here. A blocked player's messages staying searchable
 * would rather defeat the point of blocking them.
 */
public final class ChatHistory {

    /**
     * @param plain  the message with formatting stripped, for matching and for the clipboard
     * @param author who sent it, when that could be worked out
     */
    public record Entry(long time, Component component, String plain, @Nullable String author) {
    }

    /** Safe in a file name on every platform, and sorts chronologically in a directory listing. */
    private static final DateTimeFormatter FILE_STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());

    private static final DateTimeFormatter LINE_STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private static final Deque<Entry> entries = new ArrayDeque<>();

    private ChatHistory() {
    }

    public static void record(Component component, String plain, @Nullable String author) {
        entries.addLast(new Entry(System.currentTimeMillis(), component, plain, author));

        int limit = Math.max(100, ChatUtilsConfig.get().historySize);
        while (entries.size() > limit) {
            entries.removeFirst();
        }
    }

    /**
     * Entries matching {@code query}, newest first.
     *
     * <p>Matches the author as well as the text, so "Steve" finds what Steve said and not only the
     * messages that happen to mention him.
     */
    public static List<Entry> search(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Entry> matches = new ArrayList<>();

        for (Entry entry : entries) {
            if (needle.isEmpty()
                    || entry.plain().toLowerCase(Locale.ROOT).contains(needle)
                    || (entry.author() != null && entry.author().toLowerCase(Locale.ROOT).contains(needle))) {
                matches.add(entry);
            }
        }

        java.util.Collections.reverse(matches);
        return matches;
    }

    /**
     * Writes {@code selection} to a timestamped text file and returns its path.
     *
     * <p>Goes to {@code chat-logs/} beside the game's own {@code screenshots/} rather than into the
     * config directory: this is something the player made and will want to find, not a setting.
     */
    public static Path export(List<Entry> selection) throws IOException {
        Path directory = FabricLoader.getInstance().getGameDir().resolve("chat-logs");
        Files.createDirectories(directory);

        Path file = directory.resolve("chat-" + FILE_STAMP.format(Instant.now()) + ".txt");
        List<String> lines = new ArrayList<>(selection.size());
        for (Entry entry : selection) {
            lines.add("[" + LINE_STAMP.format(Instant.ofEpochMilli(entry.time())) + "] " + entry.plain());
        }

        Files.write(file, lines, StandardCharsets.UTF_8);
        return file;
    }

    public static int size() {
        return entries.size();
    }

    /**
     * Empties the log. Vanilla's F3+D clears the chat widget but never reaches here — the whole
     * point of this log is that it outlives the widget — so this is the only way to drop it.
     */
    public static void clear() {
        entries.clear();
    }
}
