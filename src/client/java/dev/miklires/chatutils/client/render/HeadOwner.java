package dev.miklires.chatutils.client.render;

import net.minecraft.client.multiplayer.PlayerInfo;
import org.jetbrains.annotations.Nullable;

public interface HeadOwner {

    @Nullable
    PlayerInfo chatutils$getOwner();

    void chatutils$setOwner(@Nullable PlayerInfo owner);
}
