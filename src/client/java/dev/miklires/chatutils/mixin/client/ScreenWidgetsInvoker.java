package dev.miklires.chatutils.mixin.client;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Lets the mod add a widget to a screen it does not own.
 *
 * <p>Fabric's screen API used to hand out the widget list for exactly this, but the accessor is
 * gone from this version. Reaching for {@code Screen}'s own method instead needs no guess about a
 * class that may have moved: {@code Screen} is as fixed a point as the game has, and the mod already
 * calls this method from inside the chat screen, so the name is known to be right.
 *
 * <p>Typed to the erasure of the real signature — the generic bound is
 * {@code <T extends GuiEventListener & Renderable & NarratableEntry>}, which erases to its first
 * bound — because an invoker has to match the compiled descriptor, not the source one.
 */
@Mixin(Screen.class)
public interface ScreenWidgetsInvoker {

    @Invoker("addRenderableWidget")
    GuiEventListener chatutils$addRenderableWidget(GuiEventListener widget);
}
