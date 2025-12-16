import java.awt.Color;
import java.awt.Graphics2D;

public class ExitGate {
    private double x;
    private double y;
    private int width;
    private int height;
    private boolean unlocked;

    public ExitGate(double x, double y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void draw(Graphics2D g2d) {
        int ix = (int) x;
        int iy = (int) y;
        g2d.setColor(new Color(6, 10, 18, 150));
        g2d.fillRect(ix + 2, iy + 2, width, height);

        if (!unlocked) {
            g2d.setColor(new Color(42, 74, 110));
            g2d.fillRect(ix, iy, width, height);
            g2d.setColor(new Color(18, 28, 46));
            g2d.drawRect(ix, iy, width, height);
        } else {
            g2d.setColor(new Color(38, 94, 90, 90));
            g2d.fillRect(ix - 4, iy - 4, width + 8, height + 8);
            g2d.setColor(new Color(86, 172, 158));
            g2d.fillRect(ix, iy, width, height);
            g2d.setColor(new Color(30, 86, 80));
            g2d.drawRect(ix, iy, width, height);

            g2d.setColor(new Color(214, 236, 230, 190));
            for (int px = ix + 4; px < ix + width; px += 6) {
                g2d.fillRect(px, iy + 4, 3, 3);
                g2d.fillRect(px, iy + height - 8, 3, 3);
            }
        }
    }

    public boolean checkCollision(Player player) {
        if (!unlocked) {
            return false;
        }
        double px = player.getX();
        double py = player.getY();
        double pw = player.getWidth();
        double ph = player.getHeight();
        boolean overlapX = px + pw > x && px < x + width;
        boolean overlapY = py + ph > y && py < y + height;
        return overlapX && overlapY;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
