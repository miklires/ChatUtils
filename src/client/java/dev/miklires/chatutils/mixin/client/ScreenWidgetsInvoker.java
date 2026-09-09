package dev.miklires.chatutils.mixin.client;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenWidgetsInvoker {

    @Invoker("addRenderableWidget")
    GuiEventListener chatutils$addRenderableWidget(GuiEventListener widget);
}
