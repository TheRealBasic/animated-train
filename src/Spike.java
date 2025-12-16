import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;

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
        int teeth = Math.max(3, width / 14);
        int toothWidth = Math.max(8, width / teeth);
        g2d.setColor(new Color(255, 120, 120, 220));
        for (int i = 0; i < teeth; i++) {
            int startX = (int) x + i * toothWidth;
            int[] xs = {startX, startX + toothWidth / 2, startX + toothWidth};
            int[] ys = {(int) (y + height), (int) y, (int) (y + height)};
            g2d.fillPolygon(xs, ys, 3);
        }
        java.awt.Stroke old = g2d.getStroke();
        g2d.setStroke(new BasicStroke(2f));
        g2d.setColor(new Color(140, 40, 40, 220));
        g2d.drawLine((int) x, (int) (y + height), (int) (x + width), (int) (y + height));
        g2d.setStroke(old);
    }
}
