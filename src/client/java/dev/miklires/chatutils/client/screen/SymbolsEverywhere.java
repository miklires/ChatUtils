package dev.miklires.chatutils.client.screen;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import dev.miklires.chatutils.mixin.client.ScreenWidgetsInvoker;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SymbolsEverywhere {

    private static final Set<String> SCREENS = Set.of(
            "AnvilScreen", "SignEditScreen", "HangingSignEditScreen", "BookEditScreen");

    private static final int CELL = SymbolPalette.CELL;
    private static final int GAP = SymbolPalette.GAP;
    private static final int COLUMNS = 12;
    private static final int ROWS = 6;

    private SymbolsEverywhere() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!ChatUtilsConfig.get().symbolsEverywhere
                    || !SCREENS.contains(screen.getClass().getSimpleName())) {
                return;
            }
            new Panel(screen).build(scaledWidth, scaledHeight);
        });
    }

    private static final class Panel {

        private final Screen screen;
        private final List<FlatButton> cells = new ArrayList<>();
        private final List<AbstractWidget> panel = new ArrayList<>();

        private List<SymbolLibrary.Symbol> view = List.of();
        private int categoryIndex;
        private int page;
        private boolean open;
        private FlatButton pageLabel;

        private Panel(Screen screen) {
            this.screen = screen;
        }

        private void build(int screenWidth, int screenHeight) {
            int panelWidth = COLUMNS * (CELL + GAP) - GAP;
            int left = screenWidth - panelWidth - CELL - GAP * 3;
            int top = Math.max(GAP, screenHeight / 2 - (ROWS + 2) * (CELL + GAP) / 2);

            FlatButton background = new FlatButton(left - 1, top - 1, panelWidth + 2,
                    (CELL + GAP) * (ROWS + 2) + 1, Component.empty(), () -> {
                    });
            background.active = false;
            add(background, true);

            List<SymbolLibrary.Category> categories = SymbolLibrary.categories();
            for (int i = 0; i < categories.size() && i < COLUMNS; i++) {
                int index = i;
                FlatButton tab = new FlatButton(left + i * (CELL + GAP), top, CELL, CELL,
                        Component.literal(categories.get(i).tabLabel()), () -> {
                    categoryIndex = index;
                    page = 0;
                    refresh();
                });
                tab.setTooltip(Tooltip.create(Component.translatable(categories.get(i).translationKey())));
                add(tab, true);
            }

            int gridTop = top + (CELL + GAP) * 2;
            for (int i = 0; i < COLUMNS * ROWS; i++) {
                int index = i;
                FlatButton cell = new FlatButton(
                        left + (i % COLUMNS) * (CELL + GAP),
                        gridTop + (i / COLUMNS) * (CELL + GAP),
                        CELL, CELL, Component.empty(), () -> insert(index));
                cells.add(cell);
                add(cell, true);
            }

            int footer = gridTop + ROWS * (CELL + GAP);
            add(new FlatButton(left, footer, CELL, CELL,
                    Component.literal("<"), () -> turnPage(-1)), true);
            add(new FlatButton(left + panelWidth - CELL, footer, CELL, CELL,
                    Component.literal(">"), () -> turnPage(1)), true);
            pageLabel = new FlatButton(left + CELL + GAP, footer, panelWidth - (CELL + GAP) * 2, CELL,
                    Component.empty(), () -> {
                    });
            pageLabel.active = false;
            add(pageLabel, true);

            FlatButton toggle = new FlatButton(screenWidth - CELL - GAP * 2, top, CELL, CELL,
                    Component.literal("☺"), () -> {
                open = !open;
                setVisible(open);
            });
            toggle.setTooltip(Tooltip.create(Component.translatable("chatutils.symbols.search")));
            add(toggle, false);

            refresh();
            setVisible(false);
        }

        private void add(AbstractWidget widget, boolean hideable) {
            if (hideable) {
                panel.add(widget);
            }
            ((ScreenWidgetsInvoker) screen).chatutils$addRenderableWidget(widget);
        }

        private void setVisible(boolean visible) {
            for (AbstractWidget widget : panel) {
                widget.visible = visible;
            }
        }

        private void turnPage(int delta) {
            List<SymbolLibrary.Symbol> source = SymbolLibrary.categories().get(categoryIndex).symbols();
            int pages = Math.max(1, (source.size() + COLUMNS * ROWS - 1) / (COLUMNS * ROWS));
            page = Math.floorMod(page + delta, pages);
            refresh();
        }

        private void refresh() {
            List<SymbolLibrary.Symbol> source = SymbolLibrary.categories().get(categoryIndex).symbols();
            int perPage = COLUMNS * ROWS;
            int pages = Math.max(1, (source.size() + perPage - 1) / perPage);
            page = Math.min(page, pages - 1);

            int from = page * perPage;
            view = source.subList(from, Math.min(source.size(), from + perPage));

            for (int i = 0; i < cells.size(); i++) {
                FlatButton cell = cells.get(i);
                boolean filled = i < view.size();
                cell.setMessage(filled ? Component.literal(view.get(i).text()) : Component.empty());
                cell.setTooltip(filled ? Tooltip.create(Component.literal(view.get(i).name())) : null);
                cell.active = filled;
            }
            pageLabel.setMessage(Component.literal((page + 1) + " / " + pages));
        }

        private void insert(int index) {
            if (index >= view.size()) {
                return;
            }
            String symbol = view.get(index).text();

            EditBox box = textBox();
            if (box != null) {
                box.insertText(symbol);
            } else {
                Minecraft.getInstance().keyboardHandler.setClipboard(symbol);
                pageLabel.setMessage(Component.translatable("chatutils.symbols.copied"));
            }
        }

        @Nullable
        private EditBox textBox() {
            for (Object child : screen.children()) {
                if (child instanceof EditBox box) {
                    return box;
                }
            }
            return null;
        }
    }
}
