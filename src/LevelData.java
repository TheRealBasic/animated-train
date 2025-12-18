import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class LevelData {
    private final String id;
    private final String name;
    private final List<Platform> platforms;
    private final List<MovingPlatform> movers;
    private final List<Spike> spikes;
    private final List<Checkpoint> checkpoints;
    private final List<Point2D.Double> orbPositions;
    private final List<CoopButton> buttons;
    private final List<CoopDoor> doors;
    private final double exitGateX;
    private final double exitGateY;
    private final int exitGateWidth;
    private final int exitGateHeight;
    private final Point2D.Double spawnPosition;
    private final Point2D.Double partnerSpawnPosition;
    private final GravityDir spawnGravity;
    private final double parTimeSeconds;
    private final boolean multiplayerOnly;
    private final boolean custom;

    public LevelData(String id,
                     String name,
                     List<Platform> platforms,
                     List<Point2D.Double> orbPositions,
                     List<MovingPlatform> movers,
                     List<Spike> spikes,
                     List<Checkpoint> checkpoints,
                     List<CoopButton> buttons,
                     List<CoopDoor> doors,
                     double exitGateX,
                     double exitGateY,
                     int exitGateWidth,
                     int exitGateHeight,
                     Point2D.Double spawnPosition,
                     Point2D.Double partnerSpawnPosition,
                     GravityDir spawnGravity,
                     double parTimeSeconds,
                     boolean multiplayerOnly,
                     boolean custom) {
        this.id = id == null || id.isBlank() ? name : id;
        this.name = name;
        this.platforms = new ArrayList<>(platforms);
        this.movers = new ArrayList<>(movers);
        this.spikes = new ArrayList<>(spikes);
        this.checkpoints = new ArrayList<>(checkpoints);
        this.orbPositions = new ArrayList<>(orbPositions);
        this.buttons = new ArrayList<>(buttons);
        this.doors = new ArrayList<>(doors);
        this.exitGateX = exitGateX;
        this.exitGateY = exitGateY;
        this.exitGateWidth = exitGateWidth;
        this.exitGateHeight = exitGateHeight;
        this.spawnPosition = spawnPosition;
        this.partnerSpawnPosition = partnerSpawnPosition;
        this.spawnGravity = spawnGravity;
        this.parTimeSeconds = parTimeSeconds;
        this.multiplayerOnly = multiplayerOnly;
        this.custom = custom;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Platform> getPlatforms() {
        return new ArrayList<>(platforms);
    }

    public List<MovingPlatform> getMovers() {
        return new ArrayList<>(movers);
    }

    public List<Spike> getSpikes() {
        return new ArrayList<>(spikes);
    }

    public List<Checkpoint> getCheckpoints() {
        return new ArrayList<>(checkpoints);
    }

    public List<Point2D.Double> getOrbPositions() {
        return new ArrayList<>(orbPositions);
    }

    public List<CoopButton> getButtons() {
        return new ArrayList<>(buttons);
    }

    public List<CoopDoor> getDoors() {
        return new ArrayList<>(doors);
    }

    public double getExitGateX() {
        return exitGateX;
    }

    public double getExitGateY() {
        return exitGateY;
    }

    public int getExitGateWidth() {
        return exitGateWidth;
    }

    public int getExitGateHeight() {
        return exitGateHeight;
    }

    public Point2D.Double getSpawnPosition() {
        return spawnPosition;
    }

    public Point2D.Double getPartnerSpawnPosition() {
        return partnerSpawnPosition;
    }

    public GravityDir getSpawnGravity() {
        return spawnGravity;
    }

    public double getParTimeSeconds() {
        return parTimeSeconds;
    }

    public boolean isMultiplayerOnly() {
        return multiplayerOnly;
    }

    public boolean isCustom() {
        return custom;
    }
}
