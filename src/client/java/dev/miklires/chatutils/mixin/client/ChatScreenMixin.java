package dev.miklires.chatutils.mixin.client;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import dev.miklires.chatutils.client.render.ChatAnimator;
import dev.miklires.chatutils.client.screen.FlatButton;
import dev.miklires.chatutils.client.screen.InlineSearch;
import dev.miklires.chatutils.client.screen.FontPicker;
import dev.miklires.chatutils.client.screen.SymbolPalette;
import dev.miklires.chatutils.client.screen.SymbolSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {

    @Shadow
    protected EditBox input;

    @Unique
    private SymbolPalette chatutils$palette;

    @Unique
    private FontPicker chatutils$fonts;

    @Unique
    private InlineSearch chatutils$search;

    @Unique
    private SymbolSuggestions chatutils$suggestions;

    private ChatScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void chatutils$onOpen(CallbackInfo ci) {
        ChatUtilsConfig config = ChatUtilsConfig.get();
        int slot = 0;

        chatutils$palette = new SymbolPalette();
        chatutils$palette.build(this.font, this.input, this.width, slot, this::addRenderableWidget);
        if (config.symbolBarEnabled) {
            slot++;
        }

        if (config.chatSearchButton) {
            chatutils$addSearchButton(slot);
            slot++;
        }

        chatutils$fonts = new FontPicker();
        chatutils$fonts.build(this.input.getY(), this.width, slot, this::addRenderableWidget);
        if (config.chatFontButton) {
            slot++;
        }

        chatutils$search = new InlineSearch();
        chatutils$search.build(this.font, this.input.getY(), this.width,
                slot * (SymbolPalette.CELL + SymbolPalette.GAP), this::addRenderableWidget);

        chatutils$suggestions = new SymbolSuggestions();
        chatutils$suggestions.build(this.input, this::addRenderableWidget);

        ChatAnimator.onChatOpened();
    }

    @Unique
    private void chatutils$addSearchButton(int slot) {
        int cell = SymbolPalette.CELL;
        int x = this.width - SymbolPalette.MARGIN - cell - slot * (cell + SymbolPalette.GAP);

        FlatButton button = new FlatButton(x, this.input.getY() - cell - SymbolPalette.MARGIN,
                cell, cell, Component.literal("🔍"),
                () -> chatutils$search.toggle());
        button.setTooltip(Tooltip.create(Component.translatable("chatutils.search.hint")));
        addRenderableWidget(button);
    }
}
