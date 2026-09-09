package dev.miklires.chatutils.client.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;

import java.util.Locale;

public final class AuthorResolver {

    private static final String[] SEPARATORS = {"» ", "> ", ": ", "]: ", " - "};

    private static final int HEADER_FALLBACK_LENGTH = 48;

    private AuthorResolver() {
    }

    public static ChatAuthor resolve(String plainText) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null || plainText.isEmpty()) {
            return ChatAuthor.UNKNOWN;
        }

        int headerEnd = headerEnd(plainText);
        String header = plainText.substring(0, headerEnd).toLowerCase(Locale.ROOT);

        PlayerInfo best = null;
        int bestIndex = Integer.MAX_VALUE;
        int bestLength = 0;

        for (PlayerInfo info : connection.getOnlinePlayers()) {
            String name = info.getProfile().name();
            if (name == null || name.isEmpty()) {
                continue;
            }
            int index = header.indexOf(name.toLowerCase(Locale.ROOT));
            if (index < 0) {
                continue;
            }
            if (index < bestIndex || (index == bestIndex && name.length() > bestLength)) {
                best = info;
                bestIndex = index;
                bestLength = name.length();
            }
        }

        if (best == null) {
            return ChatAuthor.UNKNOWN;
        }
        return new ChatAuthor(best.getProfile().name(), best.getProfile().id(), best);
    }

    public static int bodyStart(String text) {
        int end = -1;
        for (String separator : SEPARATORS) {
            int index = text.indexOf(separator);
            if (index >= 0 && (end < 0 || index < end)) {
                end = index + separator.length();
            }
        }
        return end < 0 ? 0 : Math.min(end, text.length());
    }

    private static int headerEnd(String text) {
        int body = bodyStart(text);
        return body > 0 ? body : Math.min(text.length(), HEADER_FALLBACK_LENGTH);
    }

    public static boolean isLocalPlayer(ChatAuthor author) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (!author.isKnown() || player == null) {
            return false;
        }
        if (author.id() != null && author.id().equals(player.getUUID())) {
            return true;
        }
        return author.name() != null && author.name().equalsIgnoreCase(player.getGameProfile().name());
    }
}
