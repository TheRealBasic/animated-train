import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class LevelData {
    private final String name;
    private final List<Platform> platforms;
    private final List<Point2D.Double> orbPositions;
    private final double exitGateX;
    private final double exitGateY;
    private final int exitGateWidth;
    private final int exitGateHeight;
    private final Point2D.Double spawnPosition;
    private final GravityDir spawnGravity;
    private final double parTimeSeconds;

    public LevelData(String name,
                     List<Platform> platforms,
                     List<Point2D.Double> orbPositions,
                     double exitGateX,
                     double exitGateY,
                     int exitGateWidth,
                     int exitGateHeight,
                     Point2D.Double spawnPosition,
                     GravityDir spawnGravity,
                     double parTimeSeconds) {
        this.name = name;
        this.platforms = new ArrayList<>(platforms);
        this.orbPositions = new ArrayList<>(orbPositions);
        this.exitGateX = exitGateX;
        this.exitGateY = exitGateY;
        this.exitGateWidth = exitGateWidth;
        this.exitGateHeight = exitGateHeight;
        this.spawnPosition = spawnPosition;
        this.spawnGravity = spawnGravity;
        this.parTimeSeconds = parTimeSeconds;
    }

    public String getName() {
        return name;
    }

    public List<Platform> getPlatforms() {
        return new ArrayList<>(platforms);
    }

    public List<Point2D.Double> getOrbPositions() {
        return new ArrayList<>(orbPositions);
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

    public GravityDir getSpawnGravity() {
        return spawnGravity;
    }

    public double getParTimeSeconds() {
        return parTimeSeconds;
    }
}
