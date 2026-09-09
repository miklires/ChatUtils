package dev.miklires.chatutils.client.notify;

import dev.miklires.chatutils.Chatutils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class SoundPlayer {

    private static final Logger LOGGER = LoggerFactory.getLogger("chatutils/sound");

    private static final Map<String, byte[]> FILE_CACHE = new HashMap<>();

    private static final int MAX_CONCURRENT_CLIPS = 4;
    private static final long MAX_WAV_BYTES = 8L * 1024L * 1024L;
    private static int activeClips;

    private SoundPlayer() {
    }

    public static Path soundsDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve(Chatutils.MOD_ID).resolve("sounds");
    }

    public static void play(String id, float volume, float pitch) {
        if (id == null || id.isBlank() || volume <= 0.0f) {
            return;
        }

        if (id.toLowerCase(java.util.Locale.ROOT).endsWith(".wav")) {
            playFile(id, volume);
        } else {
            playVanilla(id, volume, pitch);
        }
    }

    private static void playVanilla(String id, float volume, float pitch) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null) {
            return;
        }
        BuiltInRegistries.SOUND_EVENT.getOptional(identifier).ifPresent(event ->
                Minecraft.getInstance().getSoundManager()
                        .play(SimpleSoundInstance.forUI(event, pitch, volume)));
    }

    private static void playFile(String fileName, float volume) {
        byte[] data = load(fileName);
        if (data == null) {
            return;
        }
        synchronized (SoundPlayer.class) {
            if (activeClips >= MAX_CONCURRENT_CLIPS) {
                return;
            }
            activeClips++;
        }

        Thread thread = new Thread(() -> {
            try (AudioInputStream stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data))) {
                Clip clip = AudioSystem.getClip();
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                        synchronized (SoundPlayer.class) {
                            activeClips--;
                        }
                    }
                });
                clip.open(stream);
                applyVolume(clip, volume);
                clip.start();
            } catch (Exception e) {
                synchronized (SoundPlayer.class) {
                    activeClips--;
                }
                LOGGER.warn("Could not play custom sound {}: {}", fileName, e.toString());
            }
        }, "chatutils-sound");
        thread.setDaemon(true);
        thread.start();
    }

    private static void applyVolume(Clip clip, float volume) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float clamped = Math.max(0.0001f, Math.min(1.0f, volume));
        float gain = (float) (20.0 * Math.log10(clamped));
        control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), gain)));
    }

    private static byte[] load(String fileName) {
        if (FILE_CACHE.containsKey(fileName)) {
            return FILE_CACHE.get(fileName);
        }

        Path supplied;
        try {
            supplied = Path.of(fileName);
        } catch (InvalidPathException e) {
            FILE_CACHE.put(fileName, null);
            return null;
        }
        if (supplied.isAbsolute() || supplied.getNameCount() != 1) {
            FILE_CACHE.put(fileName, null);
            return null;
        }
        Path path = soundsDirectory().resolve(supplied).normalize();
        if (!path.startsWith(soundsDirectory())) {
            FILE_CACHE.put(fileName, null);
            return null;
        }

        byte[] data = null;
        try {
            if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isSymbolicLink(path)) {
                try (InputStream input = Files.newInputStream(path)) {
                    byte[] candidate = input.readNBytes((int) MAX_WAV_BYTES + 1);
                    if (candidate.length <= MAX_WAV_BYTES) {
                        data = candidate;
                    }
                }
            } else {
                LOGGER.warn("Custom sound {} is missing, linked, or larger than {} MiB",
                        fileName, MAX_WAV_BYTES / 1024 / 1024);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not read custom sound {}: {}", fileName, e.toString());
        }
        FILE_CACHE.put(fileName, data);
        return data;
    }

    public static void clearCache() {
        FILE_CACHE.clear();
    }
}
