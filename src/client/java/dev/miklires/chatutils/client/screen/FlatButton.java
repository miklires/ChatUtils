package dev.miklires.chatutils.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

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

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

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
