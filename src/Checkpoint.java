import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

public class Checkpoint {
    private final Point2D.Double position;
    private final int radius;
    private boolean activated;

    public Checkpoint(Point2D.Double position, int radius) {
        this.position = position;
        this.radius = radius;
    }

    public boolean check(Player player) {
        double px = player.getX();
        double py = player.getY();
        double pw = player.getWidth();
        double ph = player.getHeight();
        double closestX = clamp(position.x, px, px + pw);
        double closestY = clamp(position.y, py, py + ph);
        double dx = position.x - closestX;
        double dy = position.y - closestY;
        if (dx * dx + dy * dy <= radius * radius) {
            activated = true;
            return true;
        }
        return false;
    }

    public void draw(Graphics2D g2d) {
        int x = (int) (position.x - radius);
        int y = (int) (position.y - radius);
        int size = radius * 2;

        g2d.setColor(new Color(8, 16, 22, 140));
        g2d.fillRect(x + 2, y + 2, size, size);

        g2d.setColor(activated ? new Color(140, 240, 200) : new Color(120, 180, 240));
        g2d.fillRect(x, y, size, size);
        g2d.setColor(new Color(30, 60, 90));
        g2d.drawRect(x, y, size, size);

        g2d.setColor(new Color(220, 255, 255, 180));
        g2d.fillRect(x + size / 2 - 2, y - 6, 4, 8);
        g2d.fillRect(x + size / 2 - 2, y + size - 2, 4, 6);
        g2d.fillRect(x - 6, y + size / 2 - 2, 8, 4);
        g2d.fillRect(x + size - 2, y + size / 2 - 2, 6, 4);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public Point2D.Double getPosition() {
        return position;
    }

    public boolean isActivated() {
        return activated;
    }
}
