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
        int x = (int) bounds.x;
        int y = (int) bounds.y;
        int w = (int) bounds.width;
        int h = (int) bounds.height;

        g2d.setColor(new Color(8, 6, 16, 170));
        g2d.fillRect(x + 2, y + 2, w, h);

        g2d.setColor(pressed ? new Color(120, 214, 172) : new Color(132, 82, 154));
        g2d.fillRect(x, y, w, h);
        g2d.setColor(new Color(24, 12, 34));
        g2d.drawRect(x, y, w, h);
    }

    public Rectangle2D.Double getBounds() {
        return bounds;
    }

    public boolean isPressed() {
        return pressed;
    }
}
