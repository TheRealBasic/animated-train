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
            return data;
        }
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
            data.currentLevelIndex = Integer.parseInt(props.getProperty("currentLevelIndex", "0"));
            data.unlockedLevels = Integer.parseInt(props.getProperty("unlockedLevels", "1"));
            for (int i = 0; i < levelCount; i++) {
                data.bestTimes[i] = Double.parseDouble(props.getProperty("bestTime" + i, "0"));
                data.bestMedals[i] = props.getProperty("bestMedal" + i, "");
                data.bestDeaths[i] = Integer.parseInt(props.getProperty("bestDeaths" + i, "0"));
            }
        } catch (IOException | NumberFormatException ex) {
            // keep defaults
        }
        return data;
    }

    public static void save(SaveData data) {
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
