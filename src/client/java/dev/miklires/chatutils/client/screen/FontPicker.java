package dev.miklires.chatutils.client.screen;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import dev.miklires.chatutils.client.config.ChatFont;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Picks the Unicode font outgoing messages are written in.
 *
 * <p>Each entry is labelled with its own alphabet rather than with a name, because the name of a
 * font tells you far less than seeing it — and because a font whose glyphs your resource pack lacks
 * shows up here as boxes before you send anything in it. The translated name is the tooltip.
 *
 * <p>The choice is saved as it is made: picking a font and then having to remember to confirm it
 * somewhere would be a strange way to run a toggle that lives next to the chat input.
 */
public final class FontPicker {

    private static final int CELL = SymbolPalette.CELL;
    private static final int GAP = SymbolPalette.GAP;
    private static final int MARGIN = SymbolPalette.MARGIN;

    private static final int COLUMNS = 2;
    private static final int ENTRY_WIDTH = 64;

    private final List<AbstractWidget> panel = new ArrayList<>();
    private final List<FlatButton> entries = new ArrayList<>();

    private FlatButton toggle;
    private boolean open;

    /**
     * @param slot how many buttons already sit to the right of this one, so the row lines up
     */
    public void build(int inputY, int screenWidth, int slot, Consumer<AbstractWidget> register) {
        if (!ChatUtilsConfig.get().chatFontButton) {
            return;
        }

        ChatFont[] fonts = ChatFont.values();
        int rows = (fonts.length + COLUMNS - 1) / COLUMNS;

        int top = inputY - CELL - MARGIN;
        int x = screenWidth - MARGIN - CELL - slot * (CELL + GAP);

        int panelWidth = COLUMNS * (ENTRY_WIDTH + GAP) - GAP;
        int panelHeight = rows * (CELL + GAP) - GAP;
        int panelLeft = Math.max(MARGIN, x + CELL - panelWidth);
        int panelTop = Math.max(MARGIN, top - MARGIN - panelHeight);

        // Registered before the entries so it sits behind them. Inactive, so it swallows no clicks.
        FlatButton background = new FlatButton(panelLeft - 1, panelTop - 1,
                panelWidth + 2, panelHeight + 2, Component.empty(), () -> {
        });
        background.active = false;
        panel.add(background);
        register.accept(background);

        for (int i = 0; i < fonts.length; i++) {
            ChatFont font = fonts[i];
            FlatButton entry = new FlatButton(
                    panelLeft + (i % COLUMNS) * (ENTRY_WIDTH + GAP),
                    panelTop + (i / COLUMNS) * (CELL + GAP),
                    ENTRY_WIDTH, CELL,
                    Component.literal(font == ChatFont.NONE ? "—" : font.apply("Abc")),
                    () -> select(font));
            entry.setTooltip(Tooltip.create(Component.translatable(font.translationKey())));
            entries.add(entry);
            panel.add(entry);
            register.accept(entry);
        }

        toggle = new FlatButton(x, top, CELL, CELL, Component.empty(), this::toggle);
        register.accept(toggle);

        refresh();
        setVisible(false);
    }

    private void select(ChatFont font) {
        ChatUtilsConfig.get().chatFont = font;
        ChatUtilsConfig.save();
        refresh();
    }

    private void toggle() {
        open = !open;
        setVisible(open);
    }

    private void setVisible(boolean visible) {
        for (AbstractWidget widget : panel) {
            widget.visible = visible;
        }
    }

    /** The toggle wears the active font's own "A", so the current choice is readable at a glance. */
    private void refresh() {
        ChatFont active = ChatUtilsConfig.get().chatFont;
        toggle.setMessage(Component.literal(active == ChatFont.NONE ? "A" : active.apply("A")));
        toggle.setTooltip(Tooltip.create(Component.translatable("chatutils.font.button")
                .append(": ").append(Component.translatable(active.translationKey()))));

        ChatFont[] fonts = ChatFont.values();
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setSelected(fonts[i] == active);
        }
    }
}
