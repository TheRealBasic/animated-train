import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

public class FluxOrb {
    private final Point2D.Double position;
    private final int radius;
    private boolean collected;

    public FluxOrb(Point2D.Double position, int radius) {
        this.position = position;
        this.radius = radius;
    }

    public void draw(Graphics2D g2d) {
        if (collected) {
            return;
        }
        int diameter = radius * 2;
        g2d.setColor(new Color(120, 226, 255, 100));
        g2d.fillOval((int) (position.x - radius * 1.6), (int) (position.y - radius * 1.6), (int) (diameter * 1.6), (int) (diameter * 1.6));

        g2d.setColor(new Color(180, 246, 255));
        g2d.fillOval((int) (position.x - radius), (int) (position.y - radius), diameter, diameter);
        g2d.setColor(new Color(0, 140, 200));
        g2d.drawOval((int) (position.x - radius), (int) (position.y - radius), diameter, diameter);
        g2d.setColor(new Color(255, 255, 255, 120));
        g2d.fillOval((int) (position.x - radius / 1.8), (int) (position.y - radius / 1.8), (int) (diameter / 1.8), (int) (diameter / 1.8));
    }

    public boolean checkCollected(Player player) {
        if (collected) {
            return false;
        }
        double px = player.getX();
        double py = player.getY();
        double pw = player.getWidth();
        double ph = player.getHeight();
        double closestX = clamp(position.x, px, px + pw);
        double closestY = clamp(position.y, py, py + ph);
        double dx = position.x - closestX;
        double dy = position.y - closestY;
        if (dx * dx + dy * dy <= radius * radius) {
            collected = true;
            return true;
        }
        return false;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public boolean isCollected() {
        return collected;
    }
}
