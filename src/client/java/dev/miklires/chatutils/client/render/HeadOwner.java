package dev.miklires.chatutils.client.render;

import net.minecraft.client.multiplayer.PlayerInfo;
import org.jetbrains.annotations.Nullable;

/**
 * Attaches a sender to a chat message and to each of its display lines.
 *
 * <p>Vanilla drops all knowledge of who sent a message once it has been turned into text, so the
 * chat head has nothing to look up at render time. Mixing this onto the message types carries the
 * sender through to where the line is finally drawn.
 */
public interface HeadOwner {

    @Nullable
    PlayerInfo chatutils$getOwner();

    void chatutils$setOwner(@Nullable PlayerInfo owner);
}
