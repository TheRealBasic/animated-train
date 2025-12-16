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
        int x = (int) (position.x - radius);
        int y = (int) (position.y - radius);

        g2d.setColor(new Color(12, 22, 30, 140));
        g2d.fillRect(x + 2, y + 2, diameter, diameter);

        g2d.setColor(new Color(90, 220, 255));
        g2d.fillRect(x, y, diameter, diameter);
        g2d.setColor(new Color(0, 80, 140));
        g2d.drawRect(x, y, diameter, diameter);

        g2d.setColor(new Color(255, 255, 255, 180));
        g2d.fillRect(x + radius - 2, y - 4, 4, 6);
        g2d.fillRect(x + radius - 2, y + diameter - 2, 4, 6);
        g2d.fillRect(x - 4, y + radius - 2, 6, 4);
        g2d.fillRect(x + diameter - 2, y + radius - 2, 6, 4);

        g2d.setColor(new Color(0, 150, 220, 140));
        for (int i = 0; i < diameter; i += 4) {
            g2d.fillRect(x + i, y + i / 2 % diameter, 2, 2);
        }
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

    public void setCollected(boolean value) {
        this.collected = value;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public boolean isCollected() {
        return collected;
    }
}
