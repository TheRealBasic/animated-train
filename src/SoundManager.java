import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public final class SoundManager {
    private static final AudioFormat FORMAT = new AudioFormat(44100, 8, 1, true, true);
    private static final Object LINE_LOCK = new Object();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "sfx-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    });

    private static volatile double masterVolume = 1.0;
    private static SourceDataLine line;

    private SoundManager() {
    }

    public static void setMasterVolume(double volume) {
        masterVolume = Math.max(0.0, Math.min(1.0, volume));
    }

    public static void playTone(double frequency, int durationMs, double volume) {
        enqueue(morphTone(frequency, frequency, durationMs, volume));
    }

    public static void playNoise(int durationMs, double volume) {
        enqueue(noiseBuffer(durationMs, volume));
    }

    public static void playJump() {
        byte[] sweep = morphTone(920, 620, 120, 0.55);
        byte[] hiss = noiseBuffer(80, 0.25);
        enqueue(mix(sweep, hiss));
    }

    public static void playStep() {
        enqueue(morphTone(120, 80, 60, 0.4));
    }

    public static void playLanding() {
        byte[] thump = morphTone(110, 160, 90, 0.45);
        byte[] puff = noiseBuffer(70, 0.2);
        enqueue(mix(thump, puff));
    }

    public static void playDeath() {
        byte[] bass = morphTone(240, 70, 260, 0.7);
        byte[] crackle = noiseBuffer(200, 0.55);
        enqueue(mix(bass, crackle));
    }

    public static void playOrb() {
        byte[] chime = morphTone(1320, 1860, 180, 0.6);
        enqueue(chime);
    }

    private static void enqueue(byte[] buffer) {
        if (buffer.length == 0 || masterVolume <= 0) {
            return;
        }
        EXECUTOR.execute(() -> playBuffer(applyVolume(buffer, masterVolume)));
    }

    private static void ensureLine() throws LineUnavailableException {
        synchronized (LINE_LOCK) {
            if (line != null && line.isOpen()) {
                return;
            }
            line = AudioSystem.getSourceDataLine(FORMAT);
            line.open(FORMAT, (int) FORMAT.getSampleRate());
            line.start();
        }
    }

    private static void playBuffer(byte[] buffer) {
        try {
            ensureLine();
        } catch (LineUnavailableException ex) {
            return;
        }
        SourceDataLine active;
        synchronized (LINE_LOCK) {
            active = line;
        }
        if (active == null) {
            return;
        }
        active.write(buffer, 0, buffer.length);
    }

    private static byte[] morphTone(double startFreq, double endFreq, int durationMs, double volume) {
        int samples = (int) (FORMAT.getSampleRate() * (durationMs / 1000.0));
        if (samples <= 0) {
            return new byte[0];
        }
        byte[] buffer = new byte[samples];
        double amplitude = 127 * volume;
        double sampleRate = FORMAT.getSampleRate();
        for (int i = 0; i < samples; i++) {
            double t = i / (double) samples;
            double freq = startFreq + (endFreq - startFreq) * t;
            double envelope = 1.0 - (t * t);
            double angle = 2.0 * Math.PI * freq * (i / sampleRate);
            buffer[i] = (byte) (Math.sin(angle) * amplitude * envelope);
        }
        return buffer;
    }

    private static byte[] noiseBuffer(int durationMs, double volume) {
        int samples = (int) (FORMAT.getSampleRate() * (durationMs / 1000.0));
        if (samples <= 0) {
            return new byte[0];
        }
        byte[] buffer = new byte[samples];
        double amplitude = 127 * volume;
        for (int i = 0; i < samples; i++) {
            double falloff = 1.0 - (i / (double) samples);
            buffer[i] = (byte) ((Math.random() * 2 - 1) * amplitude * falloff);
        }
        return buffer;
    }

    private static byte[] mix(byte[] a, byte[] b) {
        int max = Math.max(a.length, b.length);
        byte[] out = new byte[max];
        for (int i = 0; i < max; i++) {
            int sample = 0;
            if (i < a.length) {
                sample += a[i];
            }
            if (i < b.length) {
                sample += b[i];
            }
            sample = Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, sample));
            out[i] = (byte) sample;
        }
        return out;
    }

    private static byte[] applyVolume(byte[] buffer, double volume) {
        if (volume >= 0.999) {
            return buffer;
        }
        byte[] scaled = new byte[buffer.length];
        for (int i = 0; i < buffer.length; i++) {
            scaled[i] = (byte) (buffer[i] * volume);
        }
        return scaled;
    }
}
