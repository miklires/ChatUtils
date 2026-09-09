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

public final class BubbleRenderer {

    private static final double HEIGHT = 0.7;

    private static final int LINE_HEIGHT = 10;

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
        Vec3 right = look.cross(up);

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

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
            if (depth < 0.1) {
                continue;
            }

            int x = (int) (width / 2.0 + delta.dot(right) / depth * focal);
            int y = (int) (height / 2.0 - delta.dot(up) / depth * focal);

            draw(graphics, minecraft.font, bubbles, x, y);
        }
    }

    private static void draw(GuiGraphicsExtractor graphics, Font font,
                             List<ChatBubbles.Bubble> bubbles, int x, int y) {
        int top = y - LINE_HEIGHT * bubbles.size();

        for (int i = 0; i < bubbles.size(); i++) {
            String text = bubbles.get(i).text();
            int lineY = top + i * LINE_HEIGHT;
            int half = font.width(text) / 2;

            graphics.fill(x - half - PADDING, lineY - 1, x + half + PADDING, lineY + LINE_HEIGHT - 1,
                    BACKGROUND);
            graphics.centeredText(font, text, x, lineY, TEXT);
        }
    }
}
