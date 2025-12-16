import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Settings {
    private static final String SETTINGS_PATH = "save/settings.properties";
    private int masterVolume = 80;
    private double screenScale = 1.0;
    private boolean showDebugHud = false;
    private boolean showFps = false;
    private boolean reducedEffects = false;
    private int keyLeft = java.awt.event.KeyEvent.VK_A;
    private int keyRight = java.awt.event.KeyEvent.VK_D;
    private int keyJump = java.awt.event.KeyEvent.VK_SPACE;
    private String lastDirectIp = "127.0.0.1";

    public static Settings load() {
        Settings settings = new Settings();
        File file = new File(SETTINGS_PATH);
        if (!file.exists()) {
            return settings;
        }
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
            settings.masterVolume = Integer.parseInt(props.getProperty("masterVolume", "80"));
            settings.screenScale = Double.parseDouble(props.getProperty("screenScale", "1.0"));
            settings.showDebugHud = Boolean.parseBoolean(props.getProperty("showDebugHud", "false"));
            settings.showFps = Boolean.parseBoolean(props.getProperty("showFps", "false"));
            settings.reducedEffects = Boolean.parseBoolean(props.getProperty("reducedEffects", "false"));
            settings.keyLeft = Integer.parseInt(props.getProperty("keyLeft", Integer.toString(settings.keyLeft)));
            settings.keyRight = Integer.parseInt(props.getProperty("keyRight", Integer.toString(settings.keyRight)));
            settings.keyJump = Integer.parseInt(props.getProperty("keyJump", Integer.toString(settings.keyJump)));
            settings.lastDirectIp = props.getProperty("lastDirectIp", settings.lastDirectIp);
        } catch (IOException | NumberFormatException ex) {
            // keep defaults
        }
        return settings;
    }

    public void save() {
        File dir = new File("save");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        Properties props = new Properties();
        props.setProperty("masterVolume", Integer.toString(masterVolume));
        props.setProperty("screenScale", Double.toString(screenScale));
        props.setProperty("showDebugHud", Boolean.toString(showDebugHud));
        props.setProperty("showFps", Boolean.toString(showFps));
        props.setProperty("reducedEffects", Boolean.toString(reducedEffects));
        props.setProperty("keyLeft", Integer.toString(keyLeft));
        props.setProperty("keyRight", Integer.toString(keyRight));
        props.setProperty("keyJump", Integer.toString(keyJump));
        props.setProperty("lastDirectIp", lastDirectIp);
        try (FileOutputStream out = new FileOutputStream(SETTINGS_PATH)) {
            props.store(out, "Platformer settings");
        } catch (IOException ignored) {
        }
    }

    public int getMasterVolume() {
        return masterVolume;
    }

    public void setMasterVolume(int masterVolume) {
        this.masterVolume = Math.max(0, Math.min(100, masterVolume));
    }

    public double getScreenScale() {
        return screenScale;
    }

    public void setScreenScale(double screenScale) {
        this.screenScale = Math.max(1.0, Math.min(2.0, screenScale));
    }

    public boolean isShowDebugHud() {
        return showDebugHud;
    }

    public void setShowDebugHud(boolean showDebugHud) {
        this.showDebugHud = showDebugHud;
    }

    public boolean isReducedEffects() {
        return reducedEffects;
    }

    public void setReducedEffects(boolean reducedEffects) {
        this.reducedEffects = reducedEffects;
    }

    public int getKeyLeft() {
        return keyLeft;
    }

    public void setKeyLeft(int keyLeft) {
        this.keyLeft = keyLeft;
    }

    public int getKeyRight() {
        return keyRight;
    }

    public void setKeyRight(int keyRight) {
        this.keyRight = keyRight;
    }

    public int getKeyJump() {
        return keyJump;
    }

    public void setKeyJump(int keyJump) {
        this.keyJump = keyJump;
    }

    public boolean isShowFps() {
        return showFps;
    }

    public void setShowFps(boolean showFps) {
        this.showFps = showFps;
    }

    public String getLastDirectIp() {
        return lastDirectIp;
    }

    public void setLastDirectIp(String lastDirectIp) {
        if (lastDirectIp != null && !lastDirectIp.isBlank()) {
            this.lastDirectIp = lastDirectIp;
        }
    }
}
