package dev.miklires.chatutils.client.chat;

import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.UUID;

public record ChatAuthor(String name, UUID id, PlayerInfo info) {

    public static final ChatAuthor UNKNOWN = new ChatAuthor(null, null, null);

    public boolean isKnown() {
        return name != null;
    }
}
