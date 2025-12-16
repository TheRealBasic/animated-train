import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
                    if (!(rootObj instanceof Map<?, ?> rootMap)) {
                        continue;
                    }
                    LevelData data = parseLevel(asStringObjectMap(rootMap));
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
        if (json == null) {
            return null;
        }
        String name = (String) json.getOrDefault("name", "Untitled");
        Map<String, Object> spawn = asStringObjectMap(json.get("spawn"));
        if (spawn == null) {
            return null;
        }
        double spawnX = toDouble(spawn.get("x"));
        double spawnY = toDouble(spawn.get("y"));
        GravityDir spawnGravity = GravityDir.valueOf(((String) spawn.getOrDefault("gravity", "DOWN")).toUpperCase());

        Map<String, Object> partnerSpawn = asStringObjectMap(json.getOrDefault("partnerSpawn", spawn));
        double partnerX = toDouble(partnerSpawn.getOrDefault("x", spawnX + 30));
        double partnerY = toDouble(partnerSpawn.getOrDefault("y", spawnY));

        Map<String, Object> gate = asStringObjectMap(json.get("exitGate"));
        if (gate == null) {
            return null;
        }
        double gateX = toDouble(gate.get("x"));
        double gateY = toDouble(gate.get("y"));
        int gateW = (int) toDouble(gate.get("w"));
        int gateH = (int) toDouble(gate.get("h"));

        double par = toDouble(json.getOrDefault("par", 60));

        List<Platform> platforms = parsePlatforms(asMapList(json.get("platforms")));
        List<MovingPlatform> movers = parseMovers(asMapList(json.get("movingPlatforms")));
        List<Spike> spikes = parseSpikes(asMapList(json.get("spikes")));
        List<Checkpoint> checkpoints = parseCheckpoints(asMapList(json.get("checkpoints")));
        List<Point2D.Double> orbs = parsePoints(asMapList(json.get("orbs")));
        List<CoopButton> buttons = parseButtons(asMapList(json.get("buttons")));
        List<CoopDoor> doors = parseDoors(asMapList(json.get("doors")), buttons.size());
        boolean multiplayerOnly = Boolean.TRUE.equals(json.get("multiplayerOnly"));

        return new LevelData(name, platforms, orbs, movers, spikes, checkpoints, buttons, doors,
                gateX, gateY, gateW, gateH,
                new Point2D.Double(spawnX, spawnY), new Point2D.Double(partnerX, partnerY), spawnGravity, par, multiplayerOnly);
    }

    private List<Platform> parsePlatforms(List<Map<String, Object>> list) {
        List<Platform> platforms = new ArrayList<>();
        if (list == null) return platforms;
        for (Map<String, Object> map : list) {
            platforms.add(new Platform(toDouble(map.get("x")), toDouble(map.get("y")),
                    (int) toDouble(map.get("w")), (int) toDouble(map.get("h"))));
        }
        return platforms;
    }

    private List<MovingPlatform> parseMovers(List<Map<String, Object>> list) {
        List<MovingPlatform> movers = new ArrayList<>();
        if (list == null) return movers;
        for (Map<String, Object> map : list) {
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

    private List<Spike> parseSpikes(List<Map<String, Object>> list) {
        List<Spike> spikes = new ArrayList<>();
        if (list == null) return spikes;
        for (Map<String, Object> map : list) {
            spikes.add(new Spike(toDouble(map.get("x")), toDouble(map.get("y")),
                    (int) toDouble(map.get("w")), (int) toDouble(map.get("h"))));
        }
        return spikes;
    }

    private List<Checkpoint> parseCheckpoints(List<Map<String, Object>> list) {
        List<Checkpoint> checkpoints = new ArrayList<>();
        if (list == null) return checkpoints;
        for (Map<String, Object> map : list) {
            checkpoints.add(new Checkpoint(new Point2D.Double(toDouble(map.get("x")), toDouble(map.get("y"))), 16));
        }
        return checkpoints;
    }

    private List<Point2D.Double> parsePoints(List<Map<String, Object>> list) {
        List<Point2D.Double> points = new ArrayList<>();
        if (list == null) return points;
        for (Map<String, Object> map : list) {
            points.add(new Point2D.Double(toDouble(map.get("x")), toDouble(map.get("y"))));
        }
        return points;
    }

    private List<CoopButton> parseButtons(List<Map<String, Object>> list) {
        List<CoopButton> buttons = new ArrayList<>();
        if (list == null) {
            return buttons;
        }
        for (Map<String, Object> map : list) {
            buttons.add(new CoopButton(toDouble(map.get("x")), toDouble(map.get("y")),
                    toDouble(map.getOrDefault("w", 24)), toDouble(map.getOrDefault("h", 12))));
        }
        return buttons;
    }

    private List<CoopDoor> parseDoors(List<Map<String, Object>> list, int buttonCount) {
        List<CoopDoor> doors = new ArrayList<>();
        if (list == null) {
            return doors;
        }
        for (Map<String, Object> map : list) {
            List<Integer> links = new ArrayList<>();
            Object rawLinks = map.get("buttons");
            if (rawLinks instanceof List<?> rawList) {
                for (Object obj : rawList) {
                    try {
                        int idx = Integer.parseInt(String.valueOf(obj));
                        if (idx >= 0 && idx < buttonCount) {
                            links.add(idx);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            doors.add(new CoopDoor(toDouble(map.get("x")), toDouble(map.get("y")),
                    toDouble(map.getOrDefault("w", 24)), toDouble(map.getOrDefault("h", 80)), links));
        }
        return doors;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private Map<String, Object> asStringObjectMap(Object value) {
        if (!(value instanceof Map<?, ?> rawMap)) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            Object key = entry.getKey();
            if (key instanceof String) {
                result.put((String) key, entry.getValue());
            }
        }
        return result;
    }

    private List<Map<String, Object>> asMapList(Object value) {
        if (!(value instanceof List<?> rawList)) {
            return null;
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object obj : rawList) {
            Map<String, Object> map = asStringObjectMap(obj);
            if (map != null) {
                result.add(map);
            }
        }
        return result;
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

    public int indexOf(LevelData data) {
        return levels.indexOf(data);
    }

    public List<LevelData> getSoloLevels() {
        return levels.stream().filter(l -> !l.isMultiplayerOnly()).collect(Collectors.toList());
    }

    public List<LevelData> getMultiplayerLevels() {
        return levels.stream().filter(LevelData::isMultiplayerOnly).collect(Collectors.toList());
    }
}
