public class MovingPlatform extends Platform {
    private final double startX;
    private final double startY;
    private final double endX;
    private final double endY;
    private final double speed;
    private boolean forward = true;

    public MovingPlatform(double x, double y, int width, int height, double endX, double endY, double speed) {
        super(x, y, width, height);
        this.startX = x;
        this.startY = y;
        this.endX = endX;
        this.endY = endY;
        this.speed = speed;
    }

    public void update(double dt) {
        double targetX = forward ? endX : startX;
        double targetY = forward ? endY : startY;
        double dx = targetX - getX();
        double dy = targetY - getY();
        double distance = Math.hypot(dx, dy);
        if (distance < 0.5) {
            forward = !forward;
            return;
        }
        double move = speed * dt;
        if (move > distance) {
            move = distance;
        }
        double nx = getX() + (dx / distance) * move;
        double ny = getY() + (dy / distance) * move;
        setPosition(nx, ny);
    }
}
