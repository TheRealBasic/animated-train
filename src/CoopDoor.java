import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

public class CoopDoor {
    private final Rectangle2D.Double bounds;
    private final List<Integer> linkedButtons;
    private boolean open;

    public CoopDoor(double x, double y, double w, double h, List<Integer> linkedButtons) {
        this.bounds = new Rectangle2D.Double(x, y, w, h);
        this.linkedButtons = linkedButtons;
    }

    public void update(List<CoopButton> buttons) {
        open = linkedButtons.stream().allMatch(idx -> idx >= 0 && idx < buttons.size() && buttons.get(idx).isPressed());
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(open ? new Color(160, 255, 200, 120) : new Color(40, 60, 90, 200));
        g2d.fill(bounds);
        g2d.setColor(new Color(200, 240, 255));
        g2d.draw(bounds);
    }

    public boolean isOpen() {
        return open;
    }

    public boolean blocks(Player player) {
        return !open && bounds.intersects(player.getBounds());
    }

    public Rectangle2D.Double getBounds() {
        return bounds;
    }
}
