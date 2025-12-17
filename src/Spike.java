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

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
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
        g2d.setColor(new Color(8, 6, 16, 170));
        g2d.fillRect((int) x + 2, (int) y + 2, width, height);

        g2d.setColor(new Color(156, 58, 92));
        for (int i = 0; i < teeth; i++) {
            int startX = (int) x + i * toothWidth;
            g2d.fillRect(startX, (int) y + height / 2, toothWidth - 2, height / 2);
            g2d.fillRect(startX + (toothWidth - 4) / 2, (int) y, 4, height / 2);
        }
        java.awt.Stroke old = g2d.getStroke();
        g2d.setStroke(new BasicStroke(2f));
        g2d.setColor(new Color(82, 22, 52, 220));
        g2d.drawRect((int) x, (int) y, width, height);
        g2d.setStroke(old);
    }
}
