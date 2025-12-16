import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.GradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
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
        draw(g2d, gravityDir, new Color(255, 209, 120), new Color(255, 170, 64));
    }

    public void draw(Graphics2D g2d, GravityDir gravityDir, Color top, Color bottom) {
        AffineTransform old = g2d.getTransform();
        java.awt.Stroke oldStroke = g2d.getStroke();
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
        g2d.setColor(new Color(12, 12, 12, 160));
        g2d.fillRect((int) x + 2, (int) y + 2, width, height);

        g2d.setColor(top);
        g2d.fillRect((int) x, (int) y, width, height);
        g2d.setColor(bottom);
        g2d.fillRect((int) x, (int) y + height / 2, width, height / 2);

        g2d.setColor(new Color(20, 28, 40));
        g2d.fillRect((int) x + 4, (int) y + height / 3, width - 8, height / 3);
        g2d.setColor(new Color(160, 210, 240));
        g2d.fillRect((int) x + 6, (int) y + height / 3 + 2, width - 12, height / 4);

        g2d.setColor(new Color(250, 250, 250, 160));
        for (int px = (int) x + 2; px < x + width - 2; px += 4) {
            g2d.fillRect(px, (int) y + 3, 2, 2);
        }

        g2d.setColor(new Color(30, 30, 30));
        g2d.drawRect((int) x, (int) y, width, height);

        g2d.setStroke(oldStroke);
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

    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(x, y, width, height);
    }
}
