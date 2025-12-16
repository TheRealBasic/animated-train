import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class SaveGame {
    private static final String SAVE_PATH = "save/save.properties";

    public static class SaveData {
        public int currentLevelIndex;
        public double[] bestTimes;
    }

    public static SaveData load(int levelCount) {
        SaveData data = new SaveData();
        data.currentLevelIndex = 0;
        data.bestTimes = new double[levelCount];
        for (int i = 0; i < data.bestTimes.length; i++) {
            data.bestTimes[i] = 0;
        }
        File file = new File(SAVE_PATH);
        if (!file.exists()) {
            return data;
        }
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
            data.currentLevelIndex = Integer.parseInt(props.getProperty("currentLevelIndex", "0"));
            for (int i = 0; i < data.bestTimes.length; i++) {
                String key = "bestTime" + i;
                if (props.containsKey(key)) {
                    data.bestTimes[i] = Double.parseDouble(props.getProperty(key, "0"));
                }
            }
        } catch (IOException | NumberFormatException ex) {
            // default stays
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
        for (int i = 0; i < data.bestTimes.length; i++) {
            props.setProperty("bestTime" + i, Double.toString(data.bestTimes[i]));
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
