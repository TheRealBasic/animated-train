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
        if (!unlocked) {
            g2d.setColor(new Color(120, 120, 150, 120));
            g2d.fillRect((int) x, (int) y, width, height);
            g2d.setColor(new Color(80, 80, 120));
            g2d.drawRect((int) x, (int) y, width, height);
        } else {
            g2d.setColor(new Color(166, 255, 200));
            g2d.fillRect((int) x, (int) y, width, height);
            g2d.setColor(new Color(66, 180, 120));
            g2d.drawRect((int) x, (int) y, width, height);
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
