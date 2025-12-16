import java.util.ArrayList;
import java.util.List;

public class ObjectiveManager {
    private final List<FluxOrb> orbs;
    private final ExitGate exitGate;
    private final double parTimeSeconds;
    private double elapsedTime;

    public ObjectiveManager(List<FluxOrb> orbs, ExitGate exitGate, double parTimeSeconds) {
        this.orbs = new ArrayList<>(orbs);
        this.exitGate = exitGate;
        this.parTimeSeconds = parTimeSeconds;
    }

    public void update(double dt, Player player) {
        elapsedTime += dt;
        int collectedCount = 0;
        for (FluxOrb orb : orbs) {
            if (orb.checkCollected(player)) {
                // collected handled inside
            }
            if (orb.isCollected()) {
                collectedCount++;
            }
        }
        if (collectedCount == orbs.size()) {
            exitGate.setUnlocked(true);
        }
    }

    public boolean allOrbsCollected() {
        int collectedCount = 0;
        for (FluxOrb orb : orbs) {
            if (orb.isCollected()) {
                collectedCount++;
            }
        }
        return collectedCount == orbs.size();
    }

    public double getElapsedTime() {
        return elapsedTime;
    }

    public void resetTimer() {
        elapsedTime = 0;
    }

    public double getParTimeSeconds() {
        return parTimeSeconds;
    }

    public List<FluxOrb> getOrbs() {
        return orbs;
    }

    public ExitGate getExitGate() {
        return exitGate;
    }
}
