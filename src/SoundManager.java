import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public final class SoundManager {
    private static final AudioFormat FORMAT = new AudioFormat(44100, 8, 1, true, true);
    private static double masterVolume = 1.0;

    private SoundManager() {
    }

    public static void setMasterVolume(double volume) {
        masterVolume = Math.max(0.0, Math.min(1.0, volume));
    }

    public static void playTone(double frequency, int durationMs, double volume) {
        double clampedVol = volume * masterVolume;
        if (clampedVol <= 0) {
            return;
        }
        new Thread(() -> generateTone(frequency, durationMs, clampedVol), "tone-" + frequency).start();
    }

    public static void playNoise(int durationMs, double volume) {
        double clampedVol = volume * masterVolume;
        if (clampedVol <= 0) {
            return;
        }
        new Thread(() -> generateNoise(durationMs, clampedVol), "noise").start();
    }

    private static void generateTone(double frequency, int durationMs, double volume) {
        int samples = (int) (FORMAT.getSampleRate() * (durationMs / 1000.0));
        byte[] buffer = new byte[samples];
        double amplitude = 127 * volume;
        for (int i = 0; i < samples; i++) {
            double angle = 2.0 * Math.PI * frequency * i / FORMAT.getSampleRate();
            buffer[i] = (byte) (Math.sin(angle) * amplitude);
        }
        playBuffer(buffer);
    }

    private static void generateNoise(int durationMs, double volume) {
        int samples = (int) (FORMAT.getSampleRate() * (durationMs / 1000.0));
        byte[] buffer = new byte[samples];
        double amplitude = 127 * volume;
        for (int i = 0; i < samples; i++) {
            buffer[i] = (byte) ((Math.random() * 2 - 1) * amplitude);
        }
        playBuffer(buffer);
    }

    private static void playBuffer(byte[] buffer) {
        try (SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT)) {
            line.open(FORMAT);
            line.start();
            line.write(buffer, 0, buffer.length);
            line.drain();
        } catch (LineUnavailableException ignored) {
        }
    }
}
