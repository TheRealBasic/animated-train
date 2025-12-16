import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

public class CoopButton {
    private final Rectangle2D.Double bounds;
    private boolean pressed;

    public CoopButton(double x, double y, double w, double h) {
        this.bounds = new Rectangle2D.Double(x, y, w, h);
    }

    public boolean check(Player player) {
        if (bounds.intersects(player.getBounds())) {
            pressed = true;
            return true;
        }
        pressed = false;
        return false;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(pressed ? new Color(120, 255, 160) : new Color(100, 160, 255));
        g2d.fillRoundRect((int) bounds.x, (int) bounds.y, (int) bounds.width, (int) bounds.height, 8, 8);
        g2d.setColor(new Color(10, 20, 30));
        g2d.drawRoundRect((int) bounds.x, (int) bounds.y, (int) bounds.width, (int) bounds.height, 8, 8);
    }

    public Rectangle2D.Double getBounds() {
        return bounds;
    }

    public boolean isPressed() {
        return pressed;
    }
}
