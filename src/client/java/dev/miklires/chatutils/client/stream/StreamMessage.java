package dev.miklires.chatutils.client.stream;

public record StreamMessage(Platform platform, String author, String text, int color) {

    public enum Platform {
        TWITCH("TW"),
        YOUTUBE("YT");

        private final String tag;

        Platform(String tag) {
            this.tag = tag;
        }

        public String tag() {
            return tag;
        }
    }
}
