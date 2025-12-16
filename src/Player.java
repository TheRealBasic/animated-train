import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.List;

public class Player {
    private static final double GRAVITY = 0.6;
    private static final double MAX_FALL_SPEED = 12.0;
    private static final double JUMP_VELOCITY = 10.5;

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

    public void applyFriction(double factor, GravityDir gravityDir) {
        if (gravityDir.isVertical()) {
            velX *= factor;
            if (Math.abs(velX) < 0.1) {
                velX = 0;
            }
        } else {
            velY *= factor;
            if (Math.abs(velY) < 0.1) {
                velY = 0;
            }
        }
    }

    public void applyPhysics(List<Platform> platforms, GravityDir gravityDir) {
        if (gravityDir.isVertical()) {
            velY += GRAVITY * gravityDir.gravitySign();
            if (Math.abs(velY) > MAX_FALL_SPEED) {
                velY = MAX_FALL_SPEED * Math.signum(velY);
            }
        } else {
            velX += GRAVITY * gravityDir.gravitySign();
            if (Math.abs(velX) > MAX_FALL_SPEED) {
                velX = MAX_FALL_SPEED * Math.signum(velX);
            }
        }

        if (gravityDir.isVertical()) {
            double nextX = x + velX;
            Platform horizontalHit = collide(nextX, y, platforms);
            if (horizontalHit != null) {
                if (velX > 0) {
                    nextX = horizontalHit.getX() - width;
                } else if (velX < 0) {
                    nextX = horizontalHit.getX() + horizontalHit.getWidth();
                }
                velX = 0;
            }

            double nextY = y + velY;
            Platform verticalHit = collide(nextX, nextY, platforms);
            grounded = false;
            if (verticalHit != null) {
                if (velY > 0) {
                    nextY = verticalHit.getY() - height;
                    grounded = gravityDir == GravityDir.DOWN;
                } else if (velY < 0) {
                    nextY = verticalHit.getY() + verticalHit.getHeight();
                    grounded = gravityDir == GravityDir.UP;
                }
                velY = 0;
            }

            x = nextX;
            y = nextY;
        } else {
            double nextY = y + velY;
            Platform verticalTangentHit = collide(x, nextY, platforms);
            if (verticalTangentHit != null) {
                if (velY > 0) {
                    nextY = verticalTangentHit.getY() - height;
                } else if (velY < 0) {
                    nextY = verticalTangentHit.getY() + verticalTangentHit.getHeight();
                }
                velY = 0;
            }

            double nextX = x + velX;
            Platform horizontalHit = collide(nextX, nextY, platforms);
            grounded = false;
            if (horizontalHit != null) {
                if (velX > 0) {
                    nextX = horizontalHit.getX() - width;
                    grounded = gravityDir == GravityDir.RIGHT;
                } else if (velX < 0) {
                    nextX = horizontalHit.getX() + horizontalHit.getWidth();
                    grounded = gravityDir == GravityDir.LEFT;
                }
                velX = 0;
            }

            x = nextX;
            y = nextY;
        }
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

    public void jump(GravityDir gravityDir) {
        switch (gravityDir) {
            case DOWN:
                velY = -JUMP_VELOCITY;
                break;
            case UP:
                velY = JUMP_VELOCITY;
                break;
            case LEFT:
                velX = JUMP_VELOCITY;
                break;
            case RIGHT:
                velX = -JUMP_VELOCITY;
                break;
        }
        grounded = false;
    }

    public void setPosition(double newX, double newY) {
        this.x = newX;
        this.y = newY;
    }

    public void setVelocity(double newVelX, double newVelY) {
        this.velX = newVelX;
        this.velY = newVelY;
    }

    public void resetVelocity() {
        setVelocity(0, 0);
    }

    public void draw(Graphics2D g2d, GravityDir gravityDir) {
        AffineTransform old = g2d.getTransform();
        double centerX = x + width / 2.0;
        double centerY = y + height / 2.0;
        double angle = 0;
        switch (gravityDir) {
            case DOWN:
                angle = 0;
                break;
            case UP:
                angle = Math.PI;
                break;
            case LEFT:
                angle = -Math.PI / 2.0;
                break;
            case RIGHT:
                angle = Math.PI / 2.0;
                break;
        }
        g2d.rotate(angle, centerX, centerY);
        g2d.setColor(new Color(255, 196, 84));
        g2d.fillRect((int) x, (int) y, width, height);
        g2d.setTransform(old);
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
