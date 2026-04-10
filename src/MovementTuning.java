import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class MovementTuning {
    private static final String PATH = "save/movement-tuning.properties";

    private double gravityPerSecond = 36.0;
    private double maxFallSpeed = 720.0;
    private double maxRunSpeed = 300.0;
    private double groundAcceleration = 36.0;
    private double airAcceleration = 24.0;
    private double groundFriction = 0.85;
    private double airFriction = 0.97;
    private double jumpVelocity = 630.0;
    private double jumpBufferSeconds = 0.18;
    private double coyoteSeconds = 0.16;
    private double gravitySwapCooldown = 0.40;
    private double sprintMultiplier = 1.50;
    private double sprintNudge = 42.0;

    private final String[] liveKeys = {
            "gravityPerSecond", "maxFallSpeed", "maxRunSpeed", "groundAcceleration", "airAcceleration",
            "groundFriction", "airFriction", "jumpVelocity", "jumpBufferSeconds", "coyoteSeconds",
            "gravitySwapCooldown", "sprintMultiplier", "sprintNudge"
    };

    public static MovementTuning load() {
        MovementTuning tuning = new MovementTuning();
        File file = new File(PATH);
        if (!file.exists()) {
            tuning.save();
            return tuning;
        }
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
            tuning.gravityPerSecond = read(props, "gravityPerSecond", tuning.gravityPerSecond, 1, 2500);
            tuning.maxFallSpeed = read(props, "maxFallSpeed", tuning.maxFallSpeed, 10, 4000);
            tuning.maxRunSpeed = read(props, "maxRunSpeed", tuning.maxRunSpeed, 10, 2000);
            tuning.groundAcceleration = read(props, "groundAcceleration", tuning.groundAcceleration, 1, 1500);
            tuning.airAcceleration = read(props, "airAcceleration", tuning.airAcceleration, 1, 1500);
            tuning.groundFriction = read(props, "groundFriction", tuning.groundFriction, 0.1, 0.9999);
            tuning.airFriction = read(props, "airFriction", tuning.airFriction, 0.1, 1.0);
            tuning.jumpVelocity = read(props, "jumpVelocity", tuning.jumpVelocity, 20, 2500);
            tuning.jumpBufferSeconds = read(props, "jumpBufferSeconds", tuning.jumpBufferSeconds, 0.0, 0.8);
            tuning.coyoteSeconds = read(props, "coyoteSeconds", tuning.coyoteSeconds, 0.0, 0.8);
            tuning.gravitySwapCooldown = read(props, "gravitySwapCooldown", tuning.gravitySwapCooldown, 0.0, 3.0);
            tuning.sprintMultiplier = read(props, "sprintMultiplier", tuning.sprintMultiplier, 1.0, 3.5);
            tuning.sprintNudge = read(props, "sprintNudge", tuning.sprintNudge, 0.0, 500.0);
        } catch (IOException ignored) {
        }
        return tuning;
    }

    public void save() {
        File dir = new File("save");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        Properties props = new Properties();
        for (Map.Entry<String, Double> entry : values().entrySet()) {
            props.setProperty(entry.getKey(), Double.toString(entry.getValue()));
        }
        try (FileOutputStream out = new FileOutputStream(PATH)) {
            props.store(out, "Movement tuning values");
        } catch (IOException ignored) {
        }
    }

    public Map<String, Double> values() {
        Map<String, Double> map = new LinkedHashMap<>();
        map.put("gravityPerSecond", gravityPerSecond);
        map.put("maxFallSpeed", maxFallSpeed);
        map.put("maxRunSpeed", maxRunSpeed);
        map.put("groundAcceleration", groundAcceleration);
        map.put("airAcceleration", airAcceleration);
        map.put("groundFriction", groundFriction);
        map.put("airFriction", airFriction);
        map.put("jumpVelocity", jumpVelocity);
        map.put("jumpBufferSeconds", jumpBufferSeconds);
        map.put("coyoteSeconds", coyoteSeconds);
        map.put("gravitySwapCooldown", gravitySwapCooldown);
        map.put("sprintMultiplier", sprintMultiplier);
        map.put("sprintNudge", sprintNudge);
        return map;
    }

    public String[] liveKeys() {
        return liveKeys;
    }

    public double adjust(String key, int direction) {
        double value = get(key);
        double step = getStep(key);
        double adjusted = value + step * Math.signum(direction);
        set(key, adjusted);
        return get(key);
    }

    public double get(String key) {
        return values().getOrDefault(key, 0.0);
    }

    public String getDisplayValue(String key) {
        double value = get(key);
        if (Math.abs(value) < 1.0) {
            return String.format("%.3f", value);
        }
        return String.format("%.2f", value);
    }

    private void set(String key, double value) {
        switch (key) {
            case "gravityPerSecond": gravityPerSecond = clamp(value, 1, 2500); break;
            case "maxFallSpeed": maxFallSpeed = clamp(value, 10, 4000); break;
            case "maxRunSpeed": maxRunSpeed = clamp(value, 10, 2000); break;
            case "groundAcceleration": groundAcceleration = clamp(value, 1, 1500); break;
            case "airAcceleration": airAcceleration = clamp(value, 1, 1500); break;
            case "groundFriction": groundFriction = clamp(value, 0.1, 0.9999); break;
            case "airFriction": airFriction = clamp(value, 0.1, 1.0); break;
            case "jumpVelocity": jumpVelocity = clamp(value, 20, 2500); break;
            case "jumpBufferSeconds": jumpBufferSeconds = clamp(value, 0.0, 0.8); break;
            case "coyoteSeconds": coyoteSeconds = clamp(value, 0.0, 0.8); break;
            case "gravitySwapCooldown": gravitySwapCooldown = clamp(value, 0.0, 3.0); break;
            case "sprintMultiplier": sprintMultiplier = clamp(value, 1.0, 3.5); break;
            case "sprintNudge": sprintNudge = clamp(value, 0.0, 500); break;
            default:
                break;
        }
    }

    private static double getStep(String key) {
        if (key.endsWith("Seconds") || key.endsWith("Friction") || key.endsWith("Multiplier")) {
            return 0.01;
        }
        return 5.0;
    }

    private static double read(Properties props, String key, double fallback, double min, double max) {
        try {
            return clamp(Double.parseDouble(props.getProperty(key, Double.toString(fallback))), min, max);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public double getGravityPerSecond() { return gravityPerSecond; }
    public double getMaxFallSpeed() { return maxFallSpeed; }
    public double getMaxRunSpeed() { return maxRunSpeed; }
    public double getGroundAcceleration() { return groundAcceleration; }
    public double getAirAcceleration() { return airAcceleration; }
    public double getGroundFriction() { return groundFriction; }
    public double getAirFriction() { return airFriction; }
    public double getJumpVelocity() { return jumpVelocity; }
    public double getJumpBufferSeconds() { return jumpBufferSeconds; }
    public double getCoyoteSeconds() { return coyoteSeconds; }
    public double getGravitySwapCooldown() { return gravitySwapCooldown; }
    public double getSprintMultiplier() { return sprintMultiplier; }
    public double getSprintNudge() { return sprintNudge; }
}
