import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class SaveGame {
    private static final String SAVE_PATH = "save/save.properties";

    public static class SaveData {
        public int currentLevelIndex;
        public int unlockedLevels;
        public double[] bestTimes;
        public String[] bestMedals;
        public int[] bestDeaths;
    }

    public static SaveData load(int levelCount) {
        SaveData data = new SaveData();
        data.currentLevelIndex = 0;
        data.unlockedLevels = 1;
        data.bestTimes = new double[levelCount];
        data.bestMedals = new String[levelCount];
        data.bestDeaths = new int[levelCount];
        File file = new File(SAVE_PATH);
        if (!file.exists()) {
            data.currentLevelIndex = clampIndex(data.currentLevelIndex, levelCount);
            data.unlockedLevels = clampUnlocked(data.unlockedLevels, levelCount);
            return data;
        }
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
            data.currentLevelIndex = Integer.parseInt(props.getProperty("currentLevelIndex", "0"));
            data.unlockedLevels = Integer.parseInt(props.getProperty("unlockedLevels", "1"));
            for (int i = 0; i < levelCount; i++) {
                double time = Double.parseDouble(props.getProperty("bestTime" + i, "0"));
                data.bestTimes[i] = sanitizeTime(time);
                data.bestMedals[i] = props.getProperty("bestMedal" + i, "");
                int deaths = Integer.parseInt(props.getProperty("bestDeaths" + i, "0"));
                data.bestDeaths[i] = Math.max(0, deaths);
            }
        } catch (IOException | NumberFormatException ex) {
            // keep defaults
        }
        data.currentLevelIndex = clampIndex(data.currentLevelIndex, levelCount);
        data.unlockedLevels = clampUnlocked(data.unlockedLevels, levelCount);
        sanitizeArrays(data);
        return data;
    }

    private static void sanitizeArrays(SaveData data) {
        int length = Math.max(data.bestTimes == null ? 0 : data.bestTimes.length,
                Math.max(data.bestMedals == null ? 0 : data.bestMedals.length,
                        data.bestDeaths == null ? 0 : data.bestDeaths.length));
        if (data.bestTimes == null) {
            data.bestTimes = new double[length];
        }
        if (data.bestMedals == null) {
            data.bestMedals = new String[length];
        }
        if (data.bestDeaths == null) {
            data.bestDeaths = new int[length];
        }
        for (int i = 0; i < length; i++) {
            data.bestTimes[i] = sanitizeTime(data.bestTimes[i]);
            if (data.bestMedals[i] == null) {
                data.bestMedals[i] = "";
            }
            data.bestDeaths[i] = Math.max(0, data.bestDeaths[i]);
        }
    }

    private static int clampIndex(int index, int levelCount) {
        if (levelCount <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(index, levelCount - 1));
    }

    private static int clampUnlocked(int unlocked, int levelCount) {
        if (levelCount <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(unlocked, levelCount));
    }

    private static double sanitizeTime(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            return 0;
        }
        return value;
    }

    public static void save(SaveData data) {
        if (data == null) {
            return;
        }
        sanitizeArrays(data);
        int levelCount = data.bestTimes.length;
        data.currentLevelIndex = clampIndex(data.currentLevelIndex, levelCount);
        data.unlockedLevels = clampUnlocked(data.unlockedLevels, levelCount);
        File dir = new File("save");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        Properties props = new Properties();
        props.setProperty("currentLevelIndex", Integer.toString(data.currentLevelIndex));
        props.setProperty("unlockedLevels", Integer.toString(data.unlockedLevels));
        for (int i = 0; i < data.bestTimes.length; i++) {
            props.setProperty("bestTime" + i, Double.toString(data.bestTimes[i]));
            props.setProperty("bestMedal" + i, data.bestMedals[i] == null ? "" : data.bestMedals[i]);
            props.setProperty("bestDeaths" + i, Integer.toString(data.bestDeaths[i]));
        }
        try (FileOutputStream out = new FileOutputStream(SAVE_PATH)) {
            props.store(out, "Platformer save");
        } catch (IOException ignored) {
        }
    }

    public static boolean exists() {
        return new File(SAVE_PATH).exists();
    }

    public static void wipe() {
        File file = new File(SAVE_PATH);
        if (file.exists()) {
            file.delete();
        }
    }
}
