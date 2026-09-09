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

public final class ChatHistory {

    public record Entry(long time, Component component, String plain, @Nullable String author) {
    }

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

    public static void clear() {
        entries.clear();
    }
}
