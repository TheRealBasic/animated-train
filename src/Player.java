import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.GradientPaint;
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
        GradientPaint suit = new GradientPaint((float) x, (float) y, new Color(255, 209, 120), (float) (x + width), (float) (y + height), new Color(255, 170, 64));
        g2d.setPaint(suit);
        g2d.fillRoundRect((int) x, (int) y, width, height, 10, 10);
        g2d.setColor(new Color(255, 255, 255, 70));
        g2d.fillRoundRect((int) x + 3, (int) y + 4, width - 6, Math.max(6, (int) (height / 3.5)), 8, 8);

        g2d.setStroke(new BasicStroke(2f));
        g2d.setColor(new Color(60, 40, 20, 180));
        g2d.drawRoundRect((int) x, (int) y, width, height, 10, 10);

        g2d.setColor(new Color(40, 80, 120, 200));
        g2d.fillRoundRect((int) x + 4, (int) y + height / 3, width - 8, height / 3, 8, 8);
        g2d.setColor(new Color(140, 210, 255, 160));
        g2d.fillRoundRect((int) x + 6, (int) y + height / 3 + 4, width - 14, height / 5, 6, 6);

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
}
