package dev.miklires.chatutils.client.screen;

import dev.miklires.chatutils.client.chat.ChatHistory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Searching the chat without leaving it: a query line above the chat input, with matches listed
 * above that.
 *
 * <p>The full screen is still there behind the {@code H} key and {@code /chatsearch}, and it is the
 * better tool for reading a conversation back. This is for the other case — you are mid-sentence,
 * you need the coordinates someone posted a minute ago, and closing the chat to go looking would
 * lose what you were typing. Clicking a match copies it, so it can go straight back into the line.
 *
 * <p>Matches come from the mod's own log rather than from the chat widget, for the same reason the
 * full screen does: the widget holds wrapped, styled lines with no notion of who sent them.
 */
public final class InlineSearch {

    private static final int CELL = SymbolPalette.CELL;
    private static final int GAP = SymbolPalette.GAP;
    private static final int MARGIN = SymbolPalette.MARGIN;

    private static final int MAX_ROWS = 8;
    private static final int MIN_ROWS = 2;

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final List<AbstractWidget> panel = new ArrayList<>();
    private final List<FlatButton> rows = new ArrayList<>();

    private Font font;
    private EditBox query;
    private FlatButton status;
    private List<ChatHistory.Entry> view = List.of();
    private boolean open;

    /**
     * @param reserved width taken by the button row to the right, so the query line stops short of it
     */
    public void build(Font font, int inputY, int screenWidth, int reserved,
                      Consumer<AbstractWidget> register) {
        this.font = font;

        int queryTop = inputY - CELL - MARGIN;
        int width = Math.max(80, screenWidth - MARGIN * 2 - reserved);

        int available = queryTop - MARGIN * 2;
        int rowCount = Math.max(MIN_ROWS, Math.min(MAX_ROWS, available / (CELL + GAP) - 1));

        int listBottom = queryTop - GAP;
        int listTop = listBottom - rowCount * (CELL + GAP);

        FlatButton background = new FlatButton(MARGIN - 1, listTop - 1, width + 2,
                listBottom - listTop + CELL + 2, Component.empty(), () -> {
                });
        background.active = false;
        panel.add(background);
        register.accept(background);

        for (int i = 0; i < rowCount; i++) {
            int index = i;
            FlatButton row = new FlatButton(MARGIN, listTop + i * (CELL + GAP), width, CELL,
                    Component.empty(), () -> copy(index));
            row.setLeftAligned(true);
            rows.add(row);
            panel.add(row);
            register.accept(row);
        }

        status = new FlatButton(MARGIN, listBottom, width, CELL, Component.empty(), () -> {
        });
        status.active = false;
        status.setLeftAligned(true);
        panel.add(status);
        register.accept(status);

        query = new EditBox(font, MARGIN, queryTop, width, CELL,
                Component.translatable("chatutils.search.hint"));
        query.setBordered(false);
        query.setHint(Component.translatable("chatutils.search.hint"));
        query.setMaxLength(256);
        query.setResponder(text -> refresh());
        panel.add(query);
        register.accept(query);

        refresh();
        setVisible(false);
    }

    /** Called by the search button. */
    public void toggle() {
        open = !open;
        setVisible(open);
        if (open) {
            // Typing should land in the query the moment it opens, not in the chat line.
            var screen = Minecraft.getInstance().gui.screen();
            if (screen != null) {
                screen.setFocused(query);
            }
            refresh();
        }
    }

    private void setVisible(boolean visible) {
        for (AbstractWidget widget : panel) {
            widget.visible = visible;
        }
    }

    /** @param row index of the clicked row, which {@link #refresh} has already filled in */
    private void copy(int row) {
        if (row < view.size() && view.get(row) != null) {
            Minecraft.getInstance().keyboardHandler.setClipboard(view.get(row).plain());
            status.setMessage(Component.translatable("chatutils.history.copied"));
        }
    }

    private void refresh() {
        // An empty query lists the most recent messages rather than nothing: opening the panel and
        // being shown the tail of the conversation is useful on its own.
        List<ChatHistory.Entry> matches = ChatHistory.search(query.getValue());

        // The log is newest first; the rows read top to bottom like the chat above them, so the
        // newest match belongs on the bottom row. Padding goes at the top, for the same reason.
        List<ChatHistory.Entry> page = new ArrayList<>(matches.subList(0, Math.min(matches.size(), rows.size())));
        java.util.Collections.reverse(page);
        while (page.size() < rows.size()) {
            page.add(0, null);
        }
        view = page;

        for (int i = 0; i < rows.size(); i++) {
            FlatButton row = rows.get(i);
            ChatHistory.Entry entry = view.get(i);
            if (entry != null) {
                row.setMessage(Component.literal(label(entry, row.getWidth())));
                row.setTooltip(Tooltip.create(Component.translatable("chatutils.history.copy_hint")));
                row.active = true;
            } else {
                row.setMessage(Component.empty());
                row.setTooltip(null);
                row.active = false;
            }
        }

        status.setMessage(matches.isEmpty()
                ? Component.translatable("chatutils.history.empty")
                : Component.translatable("chatutils.search.found", matches.size()));
    }

    private String label(ChatHistory.Entry entry, int width) {
        String text = "[" + TIME.format(Instant.ofEpochMilli(entry.time())) + "] " + entry.plain();
        int available = width - 8;
        if (font.width(text) <= available) {
            return text;
        }

        int limit = available - font.width("…");
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end)) > limit) {
            end--;
        }
        return text.substring(0, end) + "…";
    }
}
