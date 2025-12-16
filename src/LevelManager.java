import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LevelManager {
    private final List<LevelData> levels;

    public LevelManager() {
        levels = loadLevels();
    }

    private List<LevelData> loadLevels() {
        List<LevelData> result = new ArrayList<>();
        try {
            List<Path> files = Files.list(Path.of("assets/levels"))
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());
            for (Path path : files) {
                try {
                    String text = Files.readString(path);
                    Object rootObj = new MiniJson(text).parse();
                    if (!(rootObj instanceof Map)) {
                        continue;
                    }
                    LevelData data = parseLevel((Map<String, Object>) rootObj);
                    if (data != null) {
                        result.add(data);
                    }
                } catch (Exception ex) {
                    System.err.println("Failed to load level " + path + ": " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("No level directory found: " + e.getMessage());
        }
        return result;
    }

    private LevelData parseLevel(Map<String, Object> json) {
        String name = (String) json.getOrDefault("name", "Untitled");
        Map<String, Object> spawn = (Map<String, Object>) json.get("spawn");
        if (spawn == null) {
            return null;
        }
        double spawnX = toDouble(spawn.get("x"));
        double spawnY = toDouble(spawn.get("y"));
        GravityDir spawnGravity = GravityDir.valueOf(((String) spawn.getOrDefault("gravity", "DOWN")).toUpperCase());

        Map<String, Object> gate = (Map<String, Object>) json.get("exitGate");
        if (gate == null) {
            return null;
        }
        double gateX = toDouble(gate.get("x"));
        double gateY = toDouble(gate.get("y"));
        int gateW = (int) toDouble(gate.get("w"));
        int gateH = (int) toDouble(gate.get("h"));

        double par = toDouble(json.getOrDefault("par", 60));

        List<Platform> platforms = parsePlatforms((List<Object>) json.get("platforms"));
        List<MovingPlatform> movers = parseMovers((List<Object>) json.get("movingPlatforms"));
        List<Spike> spikes = parseSpikes((List<Object>) json.get("spikes"));
        List<Checkpoint> checkpoints = parseCheckpoints((List<Object>) json.get("checkpoints"));
        List<Point2D.Double> orbs = parsePoints((List<Object>) json.get("orbs"));

        return new LevelData(name, platforms, orbs, movers, spikes, checkpoints,
                gateX, gateY, gateW, gateH,
                new Point2D.Double(spawnX, spawnY), spawnGravity, par);
    }

    private List<Platform> parsePlatforms(List<Object> list) {
        List<Platform> platforms = new ArrayList<>();
        if (list == null) return platforms;
        for (Object obj : list) {
            Map<String, Object> map = (Map<String, Object>) obj;
            platforms.add(new Platform(toDouble(map.get("x")), toDouble(map.get("y")),
                    (int) toDouble(map.get("w")), (int) toDouble(map.get("h"))));
        }
        return platforms;
    }

    private List<MovingPlatform> parseMovers(List<Object> list) {
        List<MovingPlatform> movers = new ArrayList<>();
        if (list == null) return movers;
        for (Object obj : list) {
            Map<String, Object> map = (Map<String, Object>) obj;
            double x = toDouble(map.get("x"));
            double y = toDouble(map.get("y"));
            int w = (int) toDouble(map.get("w"));
            int h = (int) toDouble(map.get("h"));
            double ex = toDouble(map.get("ex"));
            double ey = toDouble(map.get("ey"));
            double speed = toDouble(map.getOrDefault("speed", 80));
            movers.add(new MovingPlatform(x, y, w, h, ex, ey, speed));
        }
        return movers;
    }

    private List<Spike> parseSpikes(List<Object> list) {
        List<Spike> spikes = new ArrayList<>();
        if (list == null) return spikes;
        for (Object obj : list) {
            Map<String, Object> map = (Map<String, Object>) obj;
            spikes.add(new Spike(toDouble(map.get("x")), toDouble(map.get("y")),
                    (int) toDouble(map.get("w")), (int) toDouble(map.get("h"))));
        }
        return spikes;
    }

    private List<Checkpoint> parseCheckpoints(List<Object> list) {
        List<Checkpoint> checkpoints = new ArrayList<>();
        if (list == null) return checkpoints;
        for (Object obj : list) {
            Map<String, Object> map = (Map<String, Object>) obj;
            checkpoints.add(new Checkpoint(new Point2D.Double(toDouble(map.get("x")), toDouble(map.get("y"))), 16));
        }
        return checkpoints;
    }

    private List<Point2D.Double> parsePoints(List<Object> list) {
        List<Point2D.Double> points = new ArrayList<>();
        if (list == null) return points;
        for (Object obj : list) {
            Map<String, Object> map = (Map<String, Object>) obj;
            points.add(new Point2D.Double(toDouble(map.get("x")), toDouble(map.get("y"))));
        }
        return points;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    public LevelData getLevel(int index) {
        if (index < 0 || index >= levels.size()) {
            return null;
        }
        return levels.get(index);
    }

    public int getLevelCount() {
        return levels.size();
    }
}
