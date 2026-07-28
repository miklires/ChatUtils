package dev.miklires.chatutils.client.render;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the sender's head beside their chat line.
 *
 * <p>26.x builds the GUI by extracting render state rather than drawing immediately, so this cannot
 * be one injection. Three things have to line up:
 *
 * <ol>
 *   <li>the {@code GuiGraphicsExtractor} and {@code ChatGraphicsAccess} in use while the chat is
 *       being extracted — neither is reachable from where the line is drawn, so both are captured
 *       into {@link #graphics} and {@link #chatAccess} for the duration;
 *   <li>which player a given line belongs to, which the chat widget does not track — the mod's own
 *       author resolution supplies it, handed down through {@link #pendingOwner};
 *   <li>the head itself, drawn at the line's y with the line's fade opacity, plus a matching shift
 *       of the text so the two do not overlap.
 * </ol>
 *
 * <p>Everything here is static and single-threaded: chat extraction happens on the render thread,
 * one line at a time.
 */
public final class ChatHeads {

    /** Width of a head in pixels, plus the gap before the text. */
    public static final int HEAD_SIZE = 8;
    public static final int TEXT_OFFSET = HEAD_SIZE + 2;

    /** Where a skin's face and hat sit in the 64x64 skin texture. */
    private static final float FACE_U = 8.0f;
    private static final float HAT_U = 40.0f;
    private static final float LAYER_V = 8.0f;
    private static final int SKIN_SIZE = 64;

    @Nullable
    public static GuiGraphicsExtractor graphics;

    @Nullable
    public static ChatComponent.ChatGraphicsAccess chatAccess;

    /** Owner of the message about to be added, set by the pipeline just before it reaches the chat. */
    @Nullable
    private static PlayerInfo pendingOwner;

    /** Owner of the message currently being split into lines. */
    @Nullable
    private static PlayerInfo currentOwner;

    /** Whether the next line built belongs at the start of that message. */
    private static boolean messageStartPending;

    private ChatHeads() {
    }

    public static boolean enabled() {
        return ChatUtilsConfig.get().chatHeadsEnabled;
    }

    /**
     * Records who wrote the message that is about to be handed to the chat.
     *
     * <p>The chat widget keeps no notion of a sender, so this is the only moment the connection
     * exists. It is deliberately fed from the mod's own author resolution rather than the signed
     * sender alone, so heads also appear on servers that rewrite chat as plain text.
     */
    public static void setPendingOwner(@Nullable PlayerInfo owner) {
        pendingOwner = owner;
    }

    /** Consumed once, as the message is turned into a {@code GuiMessage}. */
    @Nullable
    public static PlayerInfo takePendingOwner() {
        PlayerInfo owner = pendingOwner;
        pendingOwner = null;
        return owner;
    }

    /**
     * Set as a message is split into display lines, so every line of it inherits the owner. Also
     * covers the chat being rebuilt on resize, which re-splits messages that were added long ago.
     */
    public static void setCurrentOwner(@Nullable PlayerInfo owner) {
        currentOwner = owner;
        messageStartPending = true;
    }

    @Nullable
    public static PlayerInfo currentOwner() {
        return currentOwner;
    }

    /**
     * True for the first line built out of the current message, false for its continuations.
     *
     * <p>Vanilla wraps a message into lines in reading order and inserts each at the head of its
     * list, so the first line built is the one drawn at the top — the one that should carry the
     * head. If a head ever shows up against the bottom line of a wrapped message instead, that
     * insertion order is what changed.
     */
    public static boolean consumeMessageStart() {
        boolean start = messageStartPending;
        messageStartPending = false;
        return start;
    }

    /** How far to push a line's text right. */
    public static int offsetFor(@Nullable PlayerInfo owner) {
        if (!enabled()) {
            return 0;
        }
        if (owner == null) {
            // Indenting headless lines too keeps every line starting at the same x. Left ragged,
            // server messages and player messages would zig-zag against each other.
            return ChatUtilsConfig.get().chatHeadsOffsetAllLines ? TEXT_OFFSET : 0;
        }
        return TEXT_OFFSET;
    }

    /** Draws a head at the left edge of a chat line, faded to match the line. */
    public static void render(GuiGraphicsExtractor graphics, int x, int y, PlayerInfo owner, float opacity) {
        Identifier skin = owner.getSkin().body().texturePath();
        int color = ARGB.white(opacity);

        blit(graphics, skin, x, y, FACE_U, color);
        if (owner.showHat()) {
            blit(graphics, skin, x, y, HAT_U, color);
        }
    }

    private static void blit(GuiGraphicsExtractor graphics, Identifier skin, int x, int y, float u, int color) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, u, LAYER_V,
                HEAD_SIZE, HEAD_SIZE, HEAD_SIZE, HEAD_SIZE, SKIN_SIZE, SKIN_SIZE, color);
    }

    /** Clears per-session state when leaving a world. */
    public static void reset() {
        pendingOwner = null;
        currentOwner = null;
        messageStartPending = false;
        graphics = null;
        chatAccess = null;
    }
}
