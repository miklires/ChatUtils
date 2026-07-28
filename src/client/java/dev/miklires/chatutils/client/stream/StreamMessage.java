package dev.miklires.chatutils.client.stream;

/**
 * One message from a streaming platform, reduced to what the Minecraft chat can show.
 *
 * @param platform which service it came from, for the tag and its colour
 * @param author   display name as the platform gave it
 * @param text     the message body, already stripped of platform markup
 * @param color    the author's own colour as {@code 0xRRGGBB}, or 0 when the platform sent none
 */
public record StreamMessage(Platform platform, String author, String text, int color) {

    public enum Platform {
        TWITCH("TW"),
        YOUTUBE("YT");

        private final String tag;

        Platform(String tag) {
            this.tag = tag;
        }

        /** Short label shown in front of the message. The colour comes from the config. */
        public String tag() {
            return tag;
        }
    }
}
