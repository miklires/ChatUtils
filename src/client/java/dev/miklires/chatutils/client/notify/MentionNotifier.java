package dev.miklires.chatutils.client.notify;

import dev.miklires.chatutils.client.chat.ChatAuthor;
import dev.miklires.chatutils.client.chat.MentionRule;
import org.jetbrains.annotations.Nullable;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class MentionNotifier {

    private static long lastNotifiedAt;

    private MentionNotifier() {
    }

    public static void notifyMention(ChatAuthor author, @Nullable MentionRule rule) {
        ChatUtilsConfig config = ChatUtilsConfig.get();

        long now = System.currentTimeMillis();
        if (now - lastNotifiedAt < Math.max(0, config.mentionCooldownMs)) {
            return;
        }
        lastNotifiedAt = now;

        if (config.mentionSound) {
            String sound = rule != null && rule.soundId() != null ? rule.soundId() : config.mentionSoundId;
            SoundPlayer.play(sound, config.mentionSoundVolume, config.mentionSoundPitch);
        }

        if (config.mentionHotbarText) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gui != null) {
                minecraft.gui.hud.setOverlayMessage(Component.literal(hotbarText(config, author)), false);
            }
        }
    }

    private static String hotbarText(ChatUtilsConfig config, ChatAuthor author) {
        String format = config.mentionHotbarFormat == null || config.mentionHotbarFormat.isBlank()
                ? "%s mentioned you"
                : config.mentionHotbarFormat;
        String name = author.isKnown() ? author.name() : "?";
        try {
            return String.format(format, name);
        } catch (java.util.IllegalFormatException badFormat) {
            return name + " mentioned you";
        }
    }

    public static void reset() {
        lastNotifiedAt = 0L;
    }
}
