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
        Color aura = activated ? new Color(140, 255, 200, 120) : new Color(90, 150, 220, 110);
        g2d.setColor(aura);
        g2d.fillOval((int) (position.x - radius * 1.6), (int) (position.y - radius * 1.6), (int) (radius * 3.2), (int) (radius * 3.2));

        g2d.setColor(activated ? new Color(180, 255, 210) : new Color(140, 190, 230));
        g2d.fillOval((int) (position.x - radius), (int) (position.y - radius), radius * 2, radius * 2);
        g2d.setColor(new Color(30, 70, 100));
        g2d.drawOval((int) (position.x - radius), (int) (position.y - radius), radius * 2, radius * 2);
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
