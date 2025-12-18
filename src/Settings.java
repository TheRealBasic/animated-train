import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Settings {
    private static final String SETTINGS_PATH = "save/settings.properties";
    private int masterVolume = 80;
    private int uiVolume = 70;
    private int sfxVolume = 90;
    private double screenScale = 1.0;
    private boolean showDebugHud = false;
    private boolean showFps = false;
    private boolean highContrastHud = false;
    private boolean movementEffects = true;
    private boolean jumpEffects = true;
    private boolean deathEffects = true;
    private boolean screenDistortion = true;
    private boolean screenOverlay = true;
    private boolean screenBezel = true;
    private double crtSharpness = 1.0;
    private boolean sharedRespawns = true;
    private int suitPalette = 0;
    private int visorColor = 0;
    private boolean fullscreen = false;
    private boolean ironmanMode = false;
    private int activeSaveSlot = 1;
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
            settings.uiVolume = Integer.parseInt(props.getProperty("uiVolume", "70"));
            settings.sfxVolume = Integer.parseInt(props.getProperty("sfxVolume", "90"));
            settings.screenScale = Double.parseDouble(props.getProperty("screenScale", "1.0"));
            settings.showDebugHud = Boolean.parseBoolean(props.getProperty("showDebugHud", "false"));
            settings.showFps = Boolean.parseBoolean(props.getProperty("showFps", "false"));
            settings.highContrastHud = Boolean.parseBoolean(props.getProperty("highContrastHud", "false"));
            boolean legacyScreen = Boolean.parseBoolean(props.getProperty("screenEffects", "true"));
            settings.movementEffects = Boolean.parseBoolean(props.getProperty("movementEffects", "true"));
            settings.jumpEffects = Boolean.parseBoolean(props.getProperty("jumpEffects", "true"));
            settings.deathEffects = Boolean.parseBoolean(props.getProperty("deathEffects", "true"));
            settings.screenDistortion = Boolean.parseBoolean(props.getProperty("screenDistortion", Boolean.toString(legacyScreen)));
            settings.screenOverlay = Boolean.parseBoolean(props.getProperty("screenOverlay", Boolean.toString(legacyScreen)));
            settings.screenBezel = Boolean.parseBoolean(props.getProperty("screenBezel", Boolean.toString(legacyScreen)));
            settings.crtSharpness = Double.parseDouble(props.getProperty("crtSharpness", "1.0"));
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
            settings.visorColor = Integer.parseInt(props.getProperty("visorColor", "0"));
            settings.fullscreen = Boolean.parseBoolean(props.getProperty("fullscreen", "false"));
            settings.ironmanMode = Boolean.parseBoolean(props.getProperty("ironmanMode", "false"));
            settings.activeSaveSlot = Integer.parseInt(props.getProperty("activeSaveSlot", "1"));
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
        props.setProperty("uiVolume", Integer.toString(uiVolume));
        props.setProperty("sfxVolume", Integer.toString(sfxVolume));
        props.setProperty("screenScale", Double.toString(screenScale));
        props.setProperty("showDebugHud", Boolean.toString(showDebugHud));
        props.setProperty("showFps", Boolean.toString(showFps));
        props.setProperty("highContrastHud", Boolean.toString(highContrastHud));
        props.setProperty("movementEffects", Boolean.toString(movementEffects));
        props.setProperty("jumpEffects", Boolean.toString(jumpEffects));
        props.setProperty("deathEffects", Boolean.toString(deathEffects));
        props.setProperty("screenEffects", Boolean.toString(isScreenEffectsEnabled()));
        props.setProperty("screenDistortion", Boolean.toString(screenDistortion));
        props.setProperty("screenOverlay", Boolean.toString(screenOverlay));
        props.setProperty("screenBezel", Boolean.toString(screenBezel));
        props.setProperty("crtSharpness", Double.toString(crtSharpness));
        props.setProperty("sharedRespawns", Boolean.toString(sharedRespawns));
        props.setProperty("suitPalette", Integer.toString(suitPalette));
        props.setProperty("visorColor", Integer.toString(visorColor));
        props.setProperty("fullscreen", Boolean.toString(fullscreen));
        props.setProperty("ironmanMode", Boolean.toString(ironmanMode));
        props.setProperty("activeSaveSlot", Integer.toString(activeSaveSlot));
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

    public int getUiVolume() {
        return uiVolume;
    }

    public void setUiVolume(int uiVolume) {
        this.uiVolume = Math.max(0, Math.min(100, uiVolume));
    }

    public int getSfxVolume() {
        return sfxVolume;
    }

    public void setSfxVolume(int sfxVolume) {
        this.sfxVolume = Math.max(0, Math.min(100, sfxVolume));
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

    public double getCrtSharpness() {
        return crtSharpness;
    }

    public void setCrtSharpness(double crtSharpness) {
        this.crtSharpness = Math.max(0.5, Math.min(2.0, crtSharpness));
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

    public int getVisorColor() {
        return visorColor;
    }

    public void setVisorColor(int visorColor) {
        this.visorColor = Math.max(0, visorColor);
    }

    public boolean isFullscreenEnabled() {
        return fullscreen;
    }

    public void setFullscreenEnabled(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    public boolean isIronmanMode() {
        return ironmanMode;
    }

    public void setIronmanMode(boolean ironmanMode) {
        this.ironmanMode = ironmanMode;
    }

    public int getActiveSaveSlot() {
        return activeSaveSlot;
    }

    public void setActiveSaveSlot(int activeSaveSlot) {
        this.activeSaveSlot = Math.max(1, Math.min(3, activeSaveSlot));
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

    public boolean isHighContrastHud() {
        return highContrastHud;
    }

    public void setHighContrastHud(boolean highContrastHud) {
        this.highContrastHud = highContrastHud;
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
