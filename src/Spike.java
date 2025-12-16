import java.awt.Color;
import java.awt.Graphics2D;

public class Spike {
    private final double x;
    private final double y;
    private final int width;
    private final int height;

    public Spike(double x, double y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean intersects(Player player) {
        double px = player.getX();
        double py = player.getY();
        double pw = player.getWidth();
        double ph = player.getHeight();
        boolean overlapX = px + pw > x && px < x + width;
        boolean overlapY = py + ph > y && py < y + height;
        return overlapX && overlapY;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(new Color(220, 120, 120));
        g2d.fillRect((int) x, (int) y, width, height);
        g2d.setColor(new Color(120, 40, 40));
        g2d.drawRect((int) x, (int) y, width, height);
    }
}
