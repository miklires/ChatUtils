package dev.miklires.chatutils.client.screen;

import dev.miklires.chatutils.client.chat.ChatHistory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * A searchable log of everything that reached the chat, with click-to-copy.
 *
 * <p>Searching the chat widget itself would mean filtering lines that vanilla has already wrapped,
 * styled and stripped of their sender — and mapping a click back onto one of them means undoing the
 * chat's own transform. A screen of the mod's own rows sidesteps both: the entries are whole
 * messages, and a row is a widget that knows perfectly well when it was clicked.
 */
public class ChatHistoryScreen extends Screen {

    private static final int ROW_HEIGHT = 12;
    private static final int GAP = 1;
    private static final int MARGIN = 8;
    private static final int HEADER = 22;
    private static final int FOOTER = 24;

    private static final Logger LOGGER = LoggerFactory.getLogger("chatutils/history");

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final java.util.List<FlatButton> rows = new java.util.ArrayList<>();

    /** Where to go back to on Esc; null returns to the game, which is right for the hotkey. */
    private final Screen parent;

    private EditBox search;
    private FlatButton pageLabel;
    private List<ChatHistory.Entry> view = List.of();
    private int page;
    private int rowCount;

    /** Query to start with, so a command can open the screen already filtered. */
    private final String initialQuery;

    public ChatHistoryScreen() {
        this(null, "");
    }

    public ChatHistoryScreen(Screen parent) {
        this(parent, "");
    }

    public ChatHistoryScreen(Screen parent, String initialQuery) {
        super(Component.translatable("chatutils.history.title"));
        this.parent = parent;
        this.initialQuery = initialQuery == null ? "" : initialQuery;
    }

    @Override
    protected void init() {
        rows.clear();

        int width = this.width - MARGIN * 2;
        search = new EditBox(this.font, MARGIN, MARGIN, width, ROW_HEIGHT + 2,
                Component.translatable("chatutils.history.search"));
        search.setHint(Component.translatable("chatutils.history.search"));
        search.setMaxLength(256);
        search.setValue(initialQuery);
        search.setResponder(query -> {
            page = 0;
            refresh();
        });
        addRenderableWidget(search);
        setInitialFocus(search);

        rowCount = Math.max(1, (this.height - HEADER - FOOTER) / (ROW_HEIGHT + GAP));
        for (int i = 0; i < rowCount; i++) {
            int index = i;
            FlatButton row = new FlatButton(MARGIN, HEADER + i * (ROW_HEIGHT + GAP), width, ROW_HEIGHT,
                    Component.empty(), () -> copy(index));
            row.setLeftAligned(true);
            rows.add(row);
            addRenderableWidget(row);
        }

        int footerY = this.height - FOOTER + 4;
        int arrow = 20;
        int button = 70;
        addRenderableWidget(new FlatButton(MARGIN, footerY, arrow, ROW_HEIGHT,
                Component.literal("<"), () -> turnPage(-1)));
        addRenderableWidget(new FlatButton(this.width - MARGIN - arrow, footerY, arrow, ROW_HEIGHT,
                Component.literal(">"), () -> turnPage(1)));

        FlatButton export = new FlatButton(this.width - MARGIN - arrow - GAP - button, footerY,
                button, ROW_HEIGHT, Component.translatable("chatutils.history.save"), this::export);
        export.setTooltip(Tooltip.create(Component.translatable("chatutils.history.save_hint")));
        addRenderableWidget(export);

        // F3+D clears the chat widget but never reaches this log — that is the whole point of it —
        // so emptying it has to be possible from the screen that shows it.
        FlatButton clear = new FlatButton(
                this.width - MARGIN - arrow - GAP - button - GAP - button, footerY,
                button, ROW_HEIGHT, Component.translatable("chatutils.history.clear"), this::clearAll);
        clear.setTooltip(Tooltip.create(Component.translatable("chatutils.history.clear_hint")));
        addRenderableWidget(clear);

        pageLabel = new FlatButton(MARGIN + arrow + GAP, footerY,
                width - (arrow + GAP) * 2 - (button + GAP) * 2, ROW_HEIGHT, Component.empty(), () -> {
        });
        pageLabel.active = false;
        addRenderableWidget(pageLabel);

        refresh();
    }

    /** Copies the clicked row's message, so it can be pasted somewhere that is not Minecraft. */
    private void copy(int index) {
        if (index >= view.size()) {
            return;
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(view.get(index).plain());
        pageLabel.setMessage(Component.translatable("chatutils.history.copied"));
    }

    /**
     * Writes everything the current query matches, not just the visible page — otherwise the search
     * box would be useless for pulling out a conversation.
     */
    private void export() {
        List<ChatHistory.Entry> matches = ChatHistory.search(search.getValue());
        if (matches.isEmpty()) {
            return;
        }

        try {
            Path file = ChatHistory.export(matches);
            pageLabel.setMessage(Component.translatable("chatutils.history.saved",
                    file.getFileName().toString()));
        } catch (IOException failure) {
            LOGGER.warn("Could not export the chat log: {}", failure.toString());
            pageLabel.setMessage(Component.translatable("chatutils.history.save_failed"));
        }
    }

    /**
     * Empties the log. Deliberately one click with no confirmation dialog: the log is a convenience,
     * not a document, and it refills the moment anyone speaks.
     */
    private void clearAll() {
        int cleared = ChatHistory.size();
        ChatHistory.clear();
        page = 0;
        refresh();
        pageLabel.setMessage(Component.translatable("chatutils.history.cleared", cleared));
    }

    private void turnPage(int delta) {
        List<ChatHistory.Entry> matches = ChatHistory.search(search.getValue());
        int pages = Math.max(1, (matches.size() + rowCount - 1) / rowCount);
        page = Math.floorMod(page + delta, pages);
        refresh();
    }

    private void refresh() {
        List<ChatHistory.Entry> matches = ChatHistory.search(search.getValue());
        int pages = Math.max(1, (matches.size() + rowCount - 1) / rowCount);
        page = Math.min(page, pages - 1);

        int from = page * rowCount;
        view = matches.subList(from, Math.min(matches.size(), from + rowCount));

        for (int i = 0; i < rows.size(); i++) {
            FlatButton row = rows.get(i);
            if (i < view.size()) {
                ChatHistory.Entry entry = view.get(i);
                row.setMessage(Component.literal(label(entry, row.getWidth())));
                row.setTooltip(Tooltip.create(Component.translatable("chatutils.history.copy_hint")));
                row.active = true;
            } else {
                row.setMessage(Component.empty());
                row.setTooltip(null);
                row.active = false;
            }
        }

        pageLabel.setMessage(matches.isEmpty()
                ? Component.translatable("chatutils.history.empty")
                : Component.literal((page + 1) + " / " + pages + "   (" + matches.size() + ")"));
    }

    /** Prefixes the arrival time and trims to what the row can actually show. */
    private String label(ChatHistory.Entry entry, int width) {
        String text = "[" + TIME.format(Instant.ofEpochMilli(entry.time())) + "] " + entry.plain();
        int available = width - 8;
        if (this.font.width(text) <= available) {
            return text;
        }

        String ellipsis = "…";
        int limit = available - this.font.width(ellipsis);
        int end = text.length();
        while (end > 0 && this.font.width(text.substring(0, end)) > limit) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    /**
     * Opened from the chat, closing goes back to the chat — the search is a detour, not a
     * destination, and it should not cost the half-typed message that was in the input.
     */
    @Override
    public void onClose() {
        if (parent == null) {
            super.onClose();
            return;
        }
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
