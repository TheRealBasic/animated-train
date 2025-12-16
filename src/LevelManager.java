import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LevelManager {
    private final List<LevelData> levels;

    public LevelManager() {
        levels = buildLevels();
    }

    private List<LevelData> buildLevels() {
        List<LevelData> result = new ArrayList<>();

        // Tutorial Twist
        result.add(new LevelData(
                "Tutorial Twist",
                Arrays.asList(
                        new Platform(0, 500, 960, 40),
                        new Platform(140, 420, 160, 18),
                        new Platform(360, 340, 180, 18),
                        new Platform(600, 260, 180, 18),
                        new Platform(120, 200, 140, 18),
                        new Platform(0, 0, 40, 540),
                        new Platform(920, 0, 40, 540)
                ),
                Arrays.asList(
                        new Point2D.Double(190, 378),
                        new Point2D.Double(430, 298),
                        new Point2D.Double(650, 218)
                ),
                820, 220, 60, 120,
                new Point2D.Double(80, 440), GravityDir.DOWN, 25));

        // Ring Corridor
        result.add(new LevelData(
                "Ring Corridor",
                Arrays.asList(
                        new Platform(60, 60, 840, 24),
                        new Platform(60, 60, 24, 420),
                        new Platform(60, 456, 840, 24),
                        new Platform(876, 60, 24, 420),
                        new Platform(280, 220, 400, 18),
                        new Platform(280, 300, 400, 18)
                ),
                Arrays.asList(
                        new Point2D.Double(120, 100),
                        new Point2D.Double(840, 120),
                        new Point2D.Double(120, 420),
                        new Point2D.Double(840, 380)
                ),
                460, 250, 40, 120,
                new Point2D.Double(120, 420), GravityDir.DOWN, 32));

        // Two-Chamber Flip
        result.add(new LevelData(
                "Two-Chamber Flip",
                Arrays.asList(
                        new Platform(0, 500, 480, 40),
                        new Platform(480, 0, 480, 40),
                        new Platform(0, 260, 260, 18),
                        new Platform(700, 260, 260, 18),
                        new Platform(220, 150, 180, 18),
                        new Platform(560, 390, 220, 18),
                        new Platform(420, 200, 120, 18)
                ),
                Arrays.asList(
                        new Point2D.Double(200, 218),
                        new Point2D.Double(760, 218),
                        new Point2D.Double(620, 350),
                        new Point2D.Double(260, 120)
                ),
                450, 300, 60, 120,
                new Point2D.Double(120, 420), GravityDir.DOWN, 38));

        // Final Test
        result.add(new LevelData(
                "Final Test",
                Arrays.asList(
                        new Platform(0, 500, 960, 40),
                        new Platform(0, 0, 40, 540),
                        new Platform(920, 0, 40, 540),
                        new Platform(160, 420, 200, 18),
                        new Platform(380, 340, 200, 18),
                        new Platform(600, 260, 200, 18),
                        new Platform(380, 180, 200, 18),
                        new Platform(160, 120, 200, 18),
                        new Platform(600, 120, 200, 18),
                        new Platform(420, 60, 120, 18)
                ),
                Arrays.asList(
                        new Point2D.Double(200, 378),
                        new Point2D.Double(460, 298),
                        new Point2D.Double(680, 218),
                        new Point2D.Double(440, 138),
                        new Point2D.Double(200, 88),
                        new Point2D.Double(700, 88),
                        new Point2D.Double(440, 28)
                ),
                40, 180, 60, 120,
                new Point2D.Double(80, 440), GravityDir.DOWN, 52));

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
}
