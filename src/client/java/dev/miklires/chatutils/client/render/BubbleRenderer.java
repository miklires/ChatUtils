package dev.miklires.chatutils.client.render;

import dev.miklires.chatutils.Chatutils;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Draws each player's recent messages above their head.
 *
 * <p>Drawn as a HUD element with the positions worked out by hand, rather than in the world. Fabric
 * has no world-render event in this version at all, and the engine's own world drawing has moved to
 * submitting nodes rather than batching vertices — so hooking it would mean a mixin into the
 * renderer's internals, the most version-fragile thing this mod could own.
 *
 * <p>The projection is a dozen lines of trigonometry over {@code Entity} and {@code Options} methods
 * that have been stable for years, and the drawing goes through the same {@code GuiGraphicsExtractor}
 * the chat heads already use. Nothing here reaches into the renderer.
 *
 * <p>Attached before the chat element, which puts bubbles over the world and under the chat — where
 * they belong, since the chat is the thing you are reading when both are on screen. It also means
 * the element inherits vanilla's own render condition, so F1 hides bubbles along with everything
 * else without this having to know how that is decided.
 *
 * <p>The honest cost is the camera: the maths assumes the view sits at the player's eyes, which is
 * true in first person and slightly off in third.
 */
public final class BubbleRenderer {

    /** Blocks above the player's head, clear of their nameplate. */
    private static final double HEIGHT = 0.7;

    private static final int LINE_HEIGHT = 10;

    /** Padding around the text of a bubble, and the fill behind it. */
    private static final int PADDING = 2;
    private static final int BACKGROUND = 0x80000000;
    private static final int TEXT = 0xFFFFFFFF;

    private BubbleRenderer() {
    }

    public static void register() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(Chatutils.MOD_ID, "chat_bubbles"),
                (graphics, deltaTracker) -> render(graphics));
    }

    private static void render(GuiGraphicsExtractor graphics) {
        ChatUtilsConfig config = ChatUtilsConfig.get();
        Minecraft minecraft = Minecraft.getInstance();

        if (!config.chatBubbles || minecraft.level == null || minecraft.player == null) {
            return;
        }

        Vec3 eye = minecraft.player.getEyePosition(1.0f);
        Vec3 look = minecraft.player.getViewVector(1.0f);
        Vec3 up = minecraft.player.getUpVector(1.0f);
        // At yaw 0 the player faces +Z and their right hand points -X, which is what this gives.
        Vec3 right = look.cross(up);

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        // Distance from the eye to the projection plane, in screen pixels, from the vertical field
        // of view: everything else is similar triangles.
        double focal = (height / 2.0) / Math.tan(Math.toRadians(minecraft.options.fov().get()) / 2.0);

        for (AbstractClientPlayer player : minecraft.level.players()) {
            if (config.chatBubbleHideSelf && player == minecraft.player) {
                continue;
            }
            if (!player.isAlive() || player.isInvisibleTo(minecraft.player)) {
                continue;
            }

            List<ChatBubbles.Bubble> bubbles = ChatBubbles.forPlayer(player.getUUID());
            if (bubbles.isEmpty()) {
                continue;
            }

            Vec3 head = new Vec3(player.getX(), player.getY() + player.getBbHeight() + HEIGHT, player.getZ());
            Vec3 delta = head.subtract(eye);

            double depth = delta.dot(look);
            // Behind the camera, or so close that the projection blows up.
            if (depth < 0.1) {
                continue;
            }

            int x = (int) (width / 2.0 + delta.dot(right) / depth * focal);
            // Screen y grows downwards, world up does not.
            int y = (int) (height / 2.0 - delta.dot(up) / depth * focal);

            draw(graphics, minecraft.font, bubbles, x, y);
        }
    }

    /** Oldest at the top, so the block grows upwards and the newest line sits nearest the head. */
    private static void draw(GuiGraphicsExtractor graphics, Font font,
                             List<ChatBubbles.Bubble> bubbles, int x, int y) {
        int top = y - LINE_HEIGHT * bubbles.size();

        for (int i = 0; i < bubbles.size(); i++) {
            String text = bubbles.get(i).text();
            int lineY = top + i * LINE_HEIGHT;
            int half = font.width(text) / 2;

            // Behind each line rather than behind the block, so a short line does not get a wide box.
            graphics.fill(x - half - PADDING, lineY - 1, x + half + PADDING, lineY + LINE_HEIGHT - 1,
                    BACKGROUND);
            graphics.centeredText(font, text, x, lineY, TEXT);
        }
    }
}
