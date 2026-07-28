package dev.miklires.chatutils.client.chat;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

/**
 * Applies the mod's chat window settings by writing into vanilla's own options.
 *
 * <p>Size, opacity and scale already exist in the game — they are just buried in the settings menu
 * and capped conservatively. Driving those options directly means no rendering mixin, and the values
 * survive because the mod re-applies them on join.
 */
public final class ChatWindowOptions {

    private ChatWindowOptions() {
    }

    public static void apply() {
        ChatUtilsConfig config = ChatUtilsConfig.get();
        if (!config.overrideChatWindow) {
            return;
        }

        Options options = Minecraft.getInstance().options;
        options.chatScale().set(clamp(config.chatScale));
        options.chatWidth().set(clamp(config.chatWidth));
        options.chatHeightFocused().set(clamp(config.chatHeightFocused));
        options.chatHeightUnfocused().set(clamp(config.chatHeightUnfocused));
        options.chatOpacity().set(clamp(config.chatOpacity));
        options.textBackgroundOpacity().set(clamp(config.chatTextBackgroundOpacity));
        options.chatLineSpacing().set(clamp(config.chatLineSpacing));

        // Brought across from the game's own chat screen, which now leads here.
        options.chatColors().set(config.chatColors);
        options.chatLinks().set(config.chatLinks);
        options.chatLinksPrompt().set(config.chatLinksPrompt);
        options.chatDelay().set(Math.max(0.0, Math.min(6.0, config.chatDelaySeconds)));

        options.save();
    }

    /** Vanilla stores these as 0..1 fractions and misbehaves outside that range. */
    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
