package dev.miklires.chatutils.client.render;

/**
 * Marks the display line that begins a chat message.
 *
 * <p>A message too long for one line becomes several, and every one of them knows its sender — the
 * text of all of them has to clear the head. Only one should actually draw a head, though, so the
 * line that starts the message is flagged separately from the sender itself.
 */
public interface MessageStart {

    boolean chatutils$isMessageStart();

    void chatutils$setMessageStart(boolean messageStart);
}
