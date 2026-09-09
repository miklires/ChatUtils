package dev.miklires.chatutils.client.render;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.Nullable;

public final class ChatHeads {

    public static final int HEAD_SIZE = 8;
    public static final int TEXT_OFFSET = HEAD_SIZE + 2;

    private static final float FACE_U = 8.0f;
    private static final float HAT_U = 40.0f;
    private static final float LAYER_V = 8.0f;
    private static final int SKIN_SIZE = 64;

    @Nullable
    public static GuiGraphicsExtractor graphics;

    @Nullable
    public static ChatComponent.ChatGraphicsAccess chatAccess;

    @Nullable
    private static PlayerInfo pendingOwner;

    @Nullable
    private static PlayerInfo currentOwner;

    private static boolean messageStartPending;

    private ChatHeads() {
    }

    public static boolean enabled() {
        return ChatUtilsConfig.get().chatHeadsEnabled;
    }

    public static void setPendingOwner(@Nullable PlayerInfo owner) {
        pendingOwner = owner;
    }

    @Nullable
    public static PlayerInfo takePendingOwner() {
        PlayerInfo owner = pendingOwner;
        pendingOwner = null;
        return owner;
    }

    public static void setCurrentOwner(@Nullable PlayerInfo owner) {
        currentOwner = owner;
        messageStartPending = true;
    }

    @Nullable
    public static PlayerInfo currentOwner() {
        return currentOwner;
    }

    public static boolean consumeMessageStart() {
        boolean start = messageStartPending;
        messageStartPending = false;
        return start;
    }

    public static int offsetFor(@Nullable PlayerInfo owner) {
        if (!enabled()) {
            return 0;
        }
        if (owner == null) {
            return ChatUtilsConfig.get().chatHeadsOffsetAllLines ? TEXT_OFFSET : 0;
        }
        return TEXT_OFFSET;
    }

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

    public static void reset() {
        pendingOwner = null;
        currentOwner = null;
        messageStartPending = false;
        graphics = null;
        chatAccess = null;
    }
}
