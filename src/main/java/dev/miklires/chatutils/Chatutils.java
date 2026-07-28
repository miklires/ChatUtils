package dev.miklires.chatutils;

import net.fabricmc.api.ModInitializer;

/**
 * Common entrypoint. Chat Utils is a client-only mod, so everything of substance lives in
 * {@code dev.miklires.chatutils.client}; this class only exists to satisfy the "main" entrypoint.
 */
public class Chatutils implements ModInitializer {

    public static final String MOD_ID = "chatutils";

    @Override
    public void onInitialize() {
    }
}
