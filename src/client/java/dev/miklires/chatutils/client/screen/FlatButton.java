package dev.miklires.chatutils.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * A button drawn like the chat itself: a flat translucent panel, no border, no vanilla texture.
 *
 * <p>Extends {@link AbstractWidget} rather than {@code Button} because {@code AbstractButton} marks
 * its render method final and always draws the raised stone sprite — which reads as a settings menu,
 * not as part of the chat. The cost is handling the click here, which is one method.
 */
public class FlatButton extends AbstractWidget {

    private static final int BACKGROUND = 0x90000000;
    private static final int HOVERED = 0xC0454545;
    private static final int SELECTED = 0xC02C5D8C;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DISABLED = 0xFF555555;

    private static final int PADDING = 3;

    private final Runnable onPress;
    private boolean selected;
    private boolean leftAligned;

    public FlatButton(int x, int y, int width, int height, Component message, Runnable onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    /** Marks this as the active tab, so it stays lit while the pointer is elsewhere. */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /** Left-aligns the label, for rows of text where centring would look arbitrary. */
    public void setLeftAligned(boolean leftAligned) {
        this.leftAligned = leftAligned;
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubled) {
        onPress.run();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getX() + this.width, getY() + this.height, background());

        FormattedCharSequence text = getMessage().getVisualOrderText();
        Font font = Minecraft.getInstance().font;
        int x = leftAligned ? getX() + PADDING : getX() + (this.width - font.width(text)) / 2;
        graphics.text(font, text, x,
                getY() + (this.height - font.lineHeight) / 2 + 1,
                this.active ? TEXT : TEXT_DISABLED,
                true);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    private int background() {
        if (selected) {
            return SELECTED;
        }
        return this.active && isHovered() ? HOVERED : BACKGROUND;
    }
}
