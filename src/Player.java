import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

public class Player {
    private static final double GRAVITY = 0.6;
    private static final double MAX_FALL_SPEED = 12.0;
    private static final double JUMP_VELOCITY = -10.5;

    private double x;
    private double y;
    private final int width;
    private final int height;
    private double velX;
    private double velY;
    private boolean grounded;

    public Player(double x, double y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void addVelocity(double dx, double dy) {
        velX += dx;
        velY += dy;
    }

    public void applyFriction(double factor) {
        velX *= factor;
        if (Math.abs(velX) < 0.1) {
            velX = 0;
        }
    }

    public void applyPhysics(List<Platform> platforms) {
        velY += GRAVITY;
        if (velY > MAX_FALL_SPEED) {
            velY = MAX_FALL_SPEED;
        }

        double nextX = x + velX;
        double nextY = y + velY;

        // Horizontal collisions
        Platform horizontalHit = collide(nextX, y, platforms);
        if (horizontalHit != null) {
            if (velX > 0) {
                nextX = horizontalHit.getX() - width;
            } else if (velX < 0) {
                nextX = horizontalHit.getX() + horizontalHit.getWidth();
            }
            velX = 0;
        }

        // Vertical collisions
        Platform verticalHit = collide(nextX, nextY, platforms);
        if (verticalHit != null) {
            if (velY > 0) {
                nextY = verticalHit.getY() - height;
                grounded = true;
            } else if (velY < 0) {
                nextY = verticalHit.getY() + verticalHit.getHeight();
            }
            velY = 0;
        } else {
            grounded = false;
        }

        x = nextX;
        y = nextY;
    }

    private Platform collide(double nextX, double nextY, List<Platform> platforms) {
        for (Platform p : platforms) {
            boolean overlapX = nextX + width > p.getX() && nextX < p.getX() + p.getWidth();
            boolean overlapY = nextY + height > p.getY() && nextY < p.getY() + p.getHeight();
            if (overlapX && overlapY) {
                return p;
            }
        }
        return null;
    }

    public void jump() {
        velY = JUMP_VELOCITY;
        grounded = false;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(new Color(255, 196, 84));
        g2d.fillRect((int) x, (int) y, width, height);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getVelX() {
        return velX;
    }

    public double getVelY() {
        return velY;
    }

    public boolean isGrounded() {
        return grounded;
    }
}
