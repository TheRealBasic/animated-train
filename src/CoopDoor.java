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
        int x = (int) bounds.x;
        int y = (int) bounds.y;
        int w = (int) bounds.width;
        int h = (int) bounds.height;

        g2d.setColor(new Color(12, 14, 22, 150));
        g2d.fillRect(x + 2, y + 2, w, h);

        g2d.setColor(open ? new Color(140, 240, 190) : new Color(48, 66, 98));
        g2d.fillRect(x, y, w, h);
        g2d.setColor(open ? new Color(20, 70, 60) : new Color(18, 26, 40));
        g2d.drawRect(x, y, w, h);
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
