package dev.miklires.chatutils.client.chat;

import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

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

        options.chatColors().set(config.chatColors);
        options.chatLinks().set(config.chatLinks);
        options.chatLinksPrompt().set(config.chatLinksPrompt);
        options.chatDelay().set(Math.max(0.0, Math.min(6.0, config.chatDelaySeconds)));

        options.save();
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
