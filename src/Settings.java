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
    private boolean movementEffects = true;
    private boolean jumpEffects = true;
    private boolean deathEffects = true;
    private boolean screenDistortion = true;
    private boolean screenOverlay = true;
    private boolean screenBezel = true;
    private boolean sharedRespawns = true;
    private int suitPalette = 0;
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
            boolean legacyScreen = Boolean.parseBoolean(props.getProperty("screenEffects", "true"));
            settings.movementEffects = Boolean.parseBoolean(props.getProperty("movementEffects", "true"));
            settings.jumpEffects = Boolean.parseBoolean(props.getProperty("jumpEffects", "true"));
            settings.deathEffects = Boolean.parseBoolean(props.getProperty("deathEffects", "true"));
            settings.screenDistortion = Boolean.parseBoolean(props.getProperty("screenDistortion", Boolean.toString(legacyScreen)));
            settings.screenOverlay = Boolean.parseBoolean(props.getProperty("screenOverlay", Boolean.toString(legacyScreen)));
            settings.screenBezel = Boolean.parseBoolean(props.getProperty("screenBezel", Boolean.toString(legacyScreen)));
            boolean legacyReduced = Boolean.parseBoolean(props.getProperty("reducedEffects", "false"));
            if (legacyReduced) {
                settings.movementEffects = false;
                settings.jumpEffects = false;
                settings.deathEffects = false;
                settings.screenDistortion = false;
                settings.screenOverlay = false;
                settings.screenBezel = false;
            }
            settings.sharedRespawns = Boolean.parseBoolean(props.getProperty("sharedRespawns", "true"));
            settings.suitPalette = Integer.parseInt(props.getProperty("suitPalette", "0"));
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
        props.setProperty("movementEffects", Boolean.toString(movementEffects));
        props.setProperty("jumpEffects", Boolean.toString(jumpEffects));
        props.setProperty("deathEffects", Boolean.toString(deathEffects));
        props.setProperty("screenEffects", Boolean.toString(isScreenEffectsEnabled()));
        props.setProperty("screenDistortion", Boolean.toString(screenDistortion));
        props.setProperty("screenOverlay", Boolean.toString(screenOverlay));
        props.setProperty("screenBezel", Boolean.toString(screenBezel));
        props.setProperty("sharedRespawns", Boolean.toString(sharedRespawns));
        props.setProperty("suitPalette", Integer.toString(suitPalette));
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

    public boolean isMovementEffectsEnabled() {
        return movementEffects;
    }

    public void setMovementEffectsEnabled(boolean movementEffects) {
        this.movementEffects = movementEffects;
    }

    public boolean isJumpEffectsEnabled() {
        return jumpEffects;
    }

    public void setJumpEffectsEnabled(boolean jumpEffects) {
        this.jumpEffects = jumpEffects;
    }

    public boolean isDeathEffectsEnabled() {
        return deathEffects;
    }

    public void setDeathEffectsEnabled(boolean deathEffects) {
        this.deathEffects = deathEffects;
    }

    public boolean isScreenEffectsEnabled() {
        return screenDistortion || screenOverlay || screenBezel;
    }

    public boolean isScreenProcessingEnabled() {
        return screenDistortion || screenOverlay;
    }

    public boolean isScreenDistortionEnabled() {
        return screenDistortion;
    }

    public void setScreenDistortionEnabled(boolean screenDistortion) {
        this.screenDistortion = screenDistortion;
    }

    public boolean isScreenOverlayEnabled() {
        return screenOverlay;
    }

    public void setScreenOverlayEnabled(boolean screenOverlay) {
        this.screenOverlay = screenOverlay;
    }

    public boolean isScreenBezelEnabled() {
        return screenBezel;
    }

    public void setScreenBezelEnabled(boolean screenBezel) {
        this.screenBezel = screenBezel;
    }

    public void setScreenEffectsEnabled(boolean enabled) {
        this.screenDistortion = enabled;
        this.screenOverlay = enabled;
        this.screenBezel = enabled;
    }

    public boolean isSharedRespawns() {
        return sharedRespawns;
    }

    public void setSharedRespawns(boolean sharedRespawns) {
        this.sharedRespawns = sharedRespawns;
    }

    public int getSuitPalette() {
        return suitPalette;
    }

    public void setSuitPalette(int suitPalette) {
        this.suitPalette = Math.max(0, suitPalette);
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
