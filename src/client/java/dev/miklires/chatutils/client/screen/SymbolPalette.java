package dev.miklires.chatutils.client.screen;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class SymbolPalette {

    public static final int CELL = 14;
    public static final int GAP = 1;
    public static final int MARGIN = 2;

    private static final int COLUMNS = 16;
    private static final int MAX_ROWS = 10;
    private static final int MIN_ROWS = 2;

    private static final int FIXED_ROWS = 3;

    private static final int WIDE_COLUMNS = 2;

    private static final int SEPARATOR = 4;

    private final List<SymbolLibrary.Category> categories = new ArrayList<>();
    private final List<FlatButton> cells = new ArrayList<>();
    private final List<FlatButton> wideCells = new ArrayList<>();
    private final List<FlatButton> tabs = new ArrayList<>();
    private final List<AbstractWidget> panel = new ArrayList<>();

    private EditBox search;
    private FlatButton pageLabel;
    private List<SymbolLibrary.Symbol> view = List.of();
    private int categoryIndex;
    private int page;
    private int rows = MAX_ROWS;
    private boolean open;

    public void build(Font font, EditBox input, int screenWidth, int slot,
                      Consumer<AbstractWidget> register) {
        if (!ChatUtilsConfig.get().symbolBarEnabled) {
            return;
        }

        categories.addAll(SymbolLibrary.categories());
        if (categories.isEmpty()) {
            return;
        }

        int toggleTop = input.getY() - CELL - MARGIN;
        register.accept(new FlatButton(screenWidth - MARGIN - CELL - slot * (CELL + GAP),
                toggleTop, CELL, CELL, Component.literal("☺"), this::toggle));

        int panelWidth = COLUMNS * (CELL + GAP) - GAP;
        int left = Math.max(MARGIN, screenWidth - MARGIN - panelWidth);

        int available = toggleTop - MARGIN * 2 - (CELL + GAP) * FIXED_ROWS - SEPARATOR;
        rows = Math.max(MIN_ROWS, Math.min(MAX_ROWS, available / (CELL + GAP)));

        int panelHeight = (CELL + GAP) * (FIXED_ROWS + rows) - GAP + SEPARATOR;
        int top = toggleTop - MARGIN - panelHeight;

        FlatButton background = new FlatButton(left - 1, top - 1, panelWidth + 2, panelHeight + 2,
                Component.empty(), () -> {
                });
        background.active = false;
        panel.add(background);
        register.accept(background);

        int tabsTop = top + CELL + GAP;
        int gridTop = tabsTop + CELL + GAP + SEPARATOR;

        buildSearch(font, left, top, panelWidth, register);
        buildTabs(left, tabsTop, panelWidth, register);
        buildGrid(input, left, gridTop, panelWidth, register);
        buildPaging(left, gridTop + (CELL + GAP) * rows, panelWidth, register);

        refresh();
        setVisible(false);
    }

    private void buildSearch(Font font, int left, int top, int width, Consumer<AbstractWidget> register) {
        search = new EditBox(font, left, top, width, CELL, Component.translatable("chatutils.symbols.search"));
        search.setBordered(false);
        search.setHint(Component.translatable("chatutils.symbols.search"));
        search.setResponder(query -> {
            page = 0;
            refresh();
        });
        panel.add(search);
        register.accept(search);
    }

    private void buildTabs(int left, int top, int panelWidth, Consumer<AbstractWidget> register) {
        FlatButton strip = new FlatButton(left - 1, top - 1, panelWidth + 2, CELL + 2,
                Component.empty(), () -> {
                });
        strip.active = false;
        panel.add(strip);
        register.accept(strip);

        for (int i = 0; i < categories.size() && i < COLUMNS; i++) {
            SymbolLibrary.Category category = categories.get(i);
            int index = i;
            FlatButton tab = new FlatButton(left + i * (CELL + GAP), top, CELL, CELL,
                    Component.literal(category.tabLabel()), () -> selectCategory(index));
            tab.setTooltip(Tooltip.create(Component.translatable(category.translationKey())));
            tabs.add(tab);
            panel.add(tab);
            register.accept(tab);
        }
    }

    private void buildGrid(EditBox input, int left, int top, int panelWidth,
                           Consumer<AbstractWidget> register) {
        for (int i = 0; i < COLUMNS * rows; i++) {
            cells.add(makeCell(input, cells,
                    left + (i % COLUMNS) * (CELL + GAP),
                    top + (i / COLUMNS) * (CELL + GAP),
                    CELL, i, register));
        }

        int wideWidth = (panelWidth - GAP * (WIDE_COLUMNS - 1)) / WIDE_COLUMNS;
        for (int i = 0; i < WIDE_COLUMNS * rows; i++) {
            wideCells.add(makeCell(input, wideCells,
                    left + (i % WIDE_COLUMNS) * (wideWidth + GAP),
                    top + (i / WIDE_COLUMNS) * (CELL + GAP),
                    wideWidth, i, register));
        }
    }

    private FlatButton makeCell(EditBox input, List<FlatButton> owner, int x, int y, int width,
                                int index, Consumer<AbstractWidget> register) {
        FlatButton cell = new FlatButton(x, y, width, CELL, Component.empty(), () -> {
            if (owner == activeCells() && index < view.size()) {
                input.insertText(view.get(index).text());
            }
        });
        panel.add(cell);
        register.accept(cell);
        return cell;
    }

    private List<FlatButton> activeCells() {
        boolean searching = search != null && !search.getValue().trim().isEmpty();
        return !searching && categories.get(categoryIndex).wide() ? wideCells : cells;
    }

    private void buildPaging(int left, int top, int width, Consumer<AbstractWidget> register) {
        FlatButton previous = new FlatButton(left, top, CELL, CELL,
                Component.literal("<"), () -> turnPage(-1));
        FlatButton next = new FlatButton(left + width - CELL, top, CELL, CELL,
                Component.literal(">"), () -> turnPage(1));
        pageLabel = new FlatButton(left + CELL + GAP, top, width - (CELL + GAP) * 2, CELL,
                Component.empty(), () -> {
                });
        pageLabel.active = false;

        for (FlatButton button : List.of(previous, pageLabel, next)) {
            panel.add(button);
            register.accept(button);
        }
    }

    private void toggle() {
        open = !open;
        setVisible(open);
    }

    private void setVisible(boolean visible) {
        for (AbstractWidget widget : panel) {
            widget.visible = visible;
        }
        if (visible) {
            refresh();
        }
    }

    private void selectCategory(int index) {
        categoryIndex = index;
        page = 0;
        search.setValue("");
        refresh();
    }

    private void turnPage(int delta) {
        page = Math.floorMod(page + delta, pageCount(source()));
        refresh();
    }

    private int pageCount(List<SymbolLibrary.Symbol> source) {
        int perPage = Math.max(1, activeCells().size());
        return Math.max(1, (source.size() + perPage - 1) / perPage);
    }

    private List<SymbolLibrary.Symbol> source() {
        String query = search == null ? "" : search.getValue().trim();
        if (query.isEmpty()) {
            return categories.get(categoryIndex).symbols();
        }

        String needle = query.toLowerCase(Locale.ROOT);
        List<SymbolLibrary.Symbol> matches = new ArrayList<>();
        for (SymbolLibrary.Category category : categories) {
            for (SymbolLibrary.Symbol symbol : category.symbols()) {
                if (symbol.name().toLowerCase(Locale.ROOT).contains(needle)) {
                    matches.add(symbol);
                }
            }
        }
        return matches;
    }

    private void refresh() {
        List<SymbolLibrary.Symbol> source = source();
        List<FlatButton> active = activeCells();
        int perPage = active.size();
        int pages = pageCount(source);
        page = Math.min(page, pages - 1);

        int from = page * perPage;
        view = source.subList(from, Math.min(source.size(), from + perPage));

        for (FlatButton cell : (active == cells ? wideCells : cells)) {
            cell.setMessage(Component.empty());
            cell.setTooltip(null);
            cell.active = false;
            cell.visible = false;
        }

        for (int i = 0; i < active.size(); i++) {
            FlatButton cell = active.get(i);
            cell.visible = open;
            if (i < view.size()) {
                SymbolLibrary.Symbol symbol = view.get(i);
                cell.setMessage(Component.literal(symbol.text()));
                cell.setTooltip(Tooltip.create(Component.literal(symbol.name())));
                cell.active = true;
            } else {
                cell.setMessage(Component.empty());
                cell.setTooltip(null);
                cell.active = false;
            }
        }

        boolean searching = !search.getValue().trim().isEmpty();
        for (int i = 0; i < tabs.size(); i++) {
            tabs.get(i).setSelected(!searching && i == categoryIndex);
        }

        pageLabel.setMessage(Component.literal(
                source.isEmpty() ? "—" : (page + 1) + " / " + pages));
    }
}
