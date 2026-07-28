package dev.miklires.chatutils.client.screen;

import java.util.ArrayList;
import java.util.List;

/**
 * The characters offered by the symbol picker, grouped into tabs.
 *
 * <p>Built from Unicode blocks rather than a hand-written list: typing out a few dozen favourites by
 * hand caps the picker at whatever someone bothered to enumerate, while the blocks give thousands
 * and stay correct as Unicode grows. Anything unassigned, invisible or combining is filtered out —
 * a combining mark on its own renders as a stray accent and is never what the player wanted.
 *
 * <p>Minecraft ships Unifont, which covers these blocks in monochrome, so emoji code points do draw
 * — just without colour. A character missing from the font shows as a hollow box; the search box is
 * usually a faster way past those than scrolling.
 *
 * <p>Names come from {@link Character#getName}, so the search box works without shipping a name
 * table. Resolving a few thousand of them costs a moment, so the whole library is built once, on
 * first use.
 */
public final class SymbolLibrary {

    /** One character, with the Unicode name the search box matches against. */
    public record Symbol(String text, String name) {
    }

    /**
     * A tab in the picker.
     *
     * @param wide entries too long for a 14px cell, laid out as full-width rows instead of a grid
     */
    public record Category(String translationKey, String tabLabel, List<Symbol> symbols, boolean wide) {
    }

    private static List<Category> categories;

    private SymbolLibrary() {
    }

    /** Built on first call; afterwards this is a plain field read. */
    public static synchronized List<Category> categories() {
        if (categories == null) {
            categories = build();
        }
        return categories;
    }

    private static List<Category> build() {
        List<Category> built = new ArrayList<>();
        built.add(category("faces", "☺",
                0x1F600, 0x1F64F, 0x1F910, 0x1F92F, 0x1F970, 0x1F97F, 0x1F440, 0x1F463));
        built.add(category("people", "✌",
                0x1F464, 0x1F487, 0x1F930, 0x1F93E, 0x1F9D0, 0x1F9DF));
        built.add(category("nature", "❀",
                0x1F300, 0x1F32F, 0x1F330, 0x1F37F, 0x1F400, 0x1F43F, 0x1F980, 0x1F9AF));
        built.add(category("objects", "✉",
                0x1F4A0, 0x1F4FF, 0x1F5A0, 0x1F5FF, 0x1F680, 0x1F6FF, 0x1FA70, 0x1FAFF));
        built.add(category("symbols", "★",
                0x2600, 0x26FF, 0x2700, 0x27BF, 0x1F500, 0x1F53F, 0x1F650, 0x1F67F));
        built.add(category("arrows", "→",
                0x2190, 0x21FF, 0x27F0, 0x27FF, 0x2B00, 0x2BFF, 0x1F800, 0x1F8FF));
        built.add(category("math", "±",
                0x2200, 0x22FF, 0x2A00, 0x2AFF, 0x27C0, 0x27EF));
        built.add(category("shapes", "■",
                0x25A0, 0x25FF, 0x2580, 0x259F, 0x2500, 0x257F, 0x1F780, 0x1F7FF));
        built.add(category("punctuation", "«",
                0x2000, 0x206F, 0x00A1, 0x00BF, 0x2E00, 0x2E4F));
        built.add(category("numbers", "½",
                0x2150, 0x218F, 0x2460, 0x24FF, 0x20A0, 0x20BF, 0x1F100, 0x1F1AD));
        built.add(category("greek", "α",
                0x0370, 0x03FF, 0x1F00, 0x1FFF));
        built.add(kaomoji());
        return built;
    }

    /**
     * Faces built out of punctuation. Not generated from a block like the rest — a kaomoji is a
     * little composition, not a code point, so this is the one list that has to be written out.
     *
     * <p>Flagged wide: {@code ¯\_(ツ)_/¯} in a 14px cell would be a smear.
     */
    private static Category kaomoji() {
        String[] faces = {
                "¯\\_(ツ)_/¯", "(╯°□°)╯︵ ┻━┻", "┬─┬ ノ( ゜-゜ノ)", "(ノಠ益ಠ)ノ彡┻━┻",
                "(⌐■_■)", "( ͡° ͜ʖ ͡°)", "(╥﹏╥)", "(◕‿◕)", "(¬‿¬)", "(°ロ°)",
                "(ง'̀-'́)ง", "ヽ(⌐■_■)ノ", "(づ｡◕‿‿◕｡)づ", "(๑>‿<๑)", "(；一_一)",
                "(・_・;)", "(＾▽＾)", "(≧◡≦)", "(ಠ_ಠ)", "(╬ಠ益ಠ)",
                "ヽ(o_o)ﾉ", "(=^･ω･^=)", "( ˘▽˘)っ♨", "(*^▽^*)", "orz",
                "(-_-)zzz", "\\(^o^)/", "(T_T)", "(>_<)", "(o.O)"
        };

        List<Symbol> symbols = new ArrayList<>(faces.length);
        for (String face : faces) {
            // The face is its own search term: there is no Unicode name for a composition.
            symbols.add(new Symbol(face, face));
        }
        return new Category("chatutils.symbols.kaomoji", "^-^", List.copyOf(symbols), true);
    }

    /** @param ranges inclusive {@code from, to} code point pairs */
    private static Category category(String key, String tabLabel, int... ranges) {
        List<Symbol> symbols = new ArrayList<>();
        for (int i = 0; i + 1 < ranges.length; i += 2) {
            for (int codePoint = ranges[i]; codePoint <= ranges[i + 1]; codePoint++) {
                if (isUsable(codePoint)) {
                    symbols.add(new Symbol(new String(Character.toChars(codePoint)), name(codePoint)));
                }
            }
        }
        return new Category("chatutils.symbols." + key, tabLabel, List.copyOf(symbols), false);
    }

    /** Excludes anything that would not show up as a standalone glyph the player can click. */
    private static boolean isUsable(int codePoint) {
        if (!Character.isDefined(codePoint)) {
            return false;
        }
        return switch (Character.getType(codePoint)) {
            case Character.UNASSIGNED, Character.CONTROL, Character.FORMAT, Character.PRIVATE_USE,
                 Character.SURROGATE, Character.NON_SPACING_MARK, Character.ENCLOSING_MARK,
                 Character.COMBINING_SPACING_MARK, Character.LINE_SEPARATOR,
                 Character.PARAGRAPH_SEPARATOR, Character.SPACE_SEPARATOR -> false;
            default -> true;
        };
    }

    private static String name(int codePoint) {
        String name = Character.getName(codePoint);
        return name == null ? String.format("U+%04X", codePoint) : name;
    }
}
