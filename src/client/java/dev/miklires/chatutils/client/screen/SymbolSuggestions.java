package dev.miklires.chatutils.client.screen;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Suggests symbols as you type, the way every other chat app does: {@code :cro} offers a croissant,
 * a crown, a crocodile.
 *
 * <p>Faster than the picker for anything you can name, and it teaches the names — the picker is a
 * grid of thousands of glyphs, and finding the same one twice means remembering which tab it was on.
 *
 * <p>Driven from the client tick rather than from the text box's responder. Vanilla already owns
 * that responder for its own command suggestions, and taking it would break command completion for
 * a feature that does not need to react any faster than a tick.
 */
public final class SymbolSuggestions {

    private static final int CELL = SymbolPalette.CELL;
    private static final int GAP = SymbolPalette.GAP;
    private static final int MARGIN = SymbolPalette.MARGIN;

    private static final int SLOTS = 8;
    private static final int WIDTH = 22;

    /** Below this the query matches half of Unicode and the list is noise. */
    private static final int MIN_QUERY = 2;

    /** The chat screen currently showing, so the tick handler can find it. */
    @Nullable
    private static SymbolSuggestions active;

    private final List<FlatButton> slots = new ArrayList<>();

    private EditBox input;
    private List<SymbolLibrary.Symbol> view = List.of();

    /** Start of the {@code :query} being completed, or -1 when there is none. */
    private int tokenStart = -1;

    private String lastValue = "";

    public void build(EditBox input, Consumer<AbstractWidget> register) {
        if (!ChatUtilsConfig.get().symbolSuggestions) {
            return;
        }

        this.input = input;
        int top = input.getY() - CELL - MARGIN;

        for (int i = 0; i < SLOTS; i++) {
            int index = i;
            FlatButton slot = new FlatButton(MARGIN + i * (WIDTH + GAP), top, WIDTH, CELL,
                    Component.empty(), () -> insert(index));
            slot.visible = false;
            slots.add(slot);
            register.accept(slot);
        }

        active = this;
        refresh();
    }

    /**
     * Called every client tick.
     *
     * <p>Also notices when the chat screen has gone, rather than being told: a screen closing is
     * only reliably observable from the outside, and polling one tick longer than needed costs a
     * null check.
     */
    public static void tick() {
        SymbolSuggestions current = active;
        if (current == null) {
            return;
        }
        if (!(Minecraft.getInstance().gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen)) {
            active = null;
            return;
        }
        current.refresh();
    }

    private void refresh() {
        if (input == null) {
            return;
        }

        String value = input.getValue();
        if (value.equals(lastValue)) {
            return;
        }
        lastValue = value;

        view = matches(value);
        for (int i = 0; i < slots.size(); i++) {
            FlatButton slot = slots.get(i);
            if (i < view.size()) {
                SymbolLibrary.Symbol symbol = view.get(i);
                slot.setMessage(Component.literal(symbol.text()));
                slot.setTooltip(Tooltip.create(Component.literal(symbol.name())));
                slot.visible = true;
                slot.active = true;
            } else {
                slot.setMessage(Component.empty());
                slot.setTooltip(null);
                slot.visible = false;
                slot.active = false;
            }
        }
    }

    /**
     * Symbols whose Unicode name contains the word after the last {@code :}.
     *
     * <p>The colon has to start a word, so a time or an emoticon is not read as the start of a
     * query, and the text after it has to be plain letters — a space ends it.
     */
    private List<SymbolLibrary.Symbol> matches(String value) {
        tokenStart = -1;

        int colon = value.lastIndexOf(':');
        if (colon < 0 || (colon > 0 && !Character.isWhitespace(value.charAt(colon - 1)))) {
            return List.of();
        }

        String query = value.substring(colon + 1);
        if (query.length() < MIN_QUERY) {
            return List.of();
        }
        for (int i = 0; i < query.length(); i++) {
            if (!Character.isLetterOrDigit(query.charAt(i))) {
                return List.of();
            }
        }

        tokenStart = colon;
        String needle = query.toLowerCase(Locale.ROOT);

        List<SymbolLibrary.Symbol> found = new ArrayList<>();
        for (SymbolLibrary.Category category : SymbolLibrary.categories()) {
            for (SymbolLibrary.Symbol symbol : category.symbols()) {
                if (symbol.name().toLowerCase(Locale.ROOT).contains(needle)) {
                    found.add(symbol);
                    if (found.size() >= SLOTS) {
                        return found;
                    }
                }
            }
        }
        return found;
    }

    /** Swaps the {@code :query} for the chosen symbol, leaving the rest of the line alone. */
    private void insert(int index) {
        if (index >= view.size() || tokenStart < 0 || input == null) {
            return;
        }

        String value = input.getValue();
        input.setValue(value.substring(0, tokenStart) + view.get(index).text());
        lastValue = input.getValue();
        refresh();
    }
}
