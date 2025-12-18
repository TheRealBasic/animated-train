import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;

public class Player {
    private static final double GRAVITY = 0.6;
    private static final double MAX_FALL_SPEED = 12.0;
    private static final double MAX_RUN_SPEED = 5.0;
    private static final double JUMP_VELOCITY = 10.5;

    private double x;
    private double y;
    private final int width;
    private final int height;
    private double velX;
    private double velY;
    private boolean grounded;
    private double animationTimer;
    private double walkCycle;
    private boolean facingRight = true;

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
            velX = clampMagnitude(velX, MAX_RUN_SPEED);
        } else {
            velX += GRAVITY * gravityDir.gravitySign();
            if (Math.abs(velX) > MAX_FALL_SPEED) {
                velX = MAX_FALL_SPEED * Math.signum(velX);
            }
            velY = clampMagnitude(velY, MAX_RUN_SPEED);
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

    private double clampMagnitude(double value, double max) {
        if (Math.abs(value) <= max) {
            return value;
        }
        return max * Math.signum(value);
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

    public void draw(Graphics2D g2d, GravityDir gravityDir, Color suitPrimary, Color suitSecondary, Color visorColor) {
        AffineTransform old = g2d.getTransform();
        java.awt.Stroke oldStroke = g2d.getStroke();
        double centerX = x + width / 2.0;
        double centerY = y + height / 2.0;
        double angle = switch (gravityDir) {
            case DOWN -> 0;
            case UP -> Math.PI;
            case LEFT -> -Math.PI / 2.0;
            case RIGHT -> Math.PI / 2.0;
        };
        g2d.rotate(angle, centerX, centerY);

        double bodyX = x;
        double bodyY = y;
        double limbThickness = Math.max(4, width / 5.0);
        double torsoWidth = width - limbThickness;
        double torsoHeight = height - 12;
        double headSize = 14;
        double headX = bodyX + (width - headSize) / 2.0;
        double headY = bodyY - 4;

        double walkSwing = Math.sin(walkCycle) * 16;
        double armSwing = Math.sin(walkCycle + Math.PI / 2) * 10;
        if (!grounded) {
            walkSwing = 8;
            armSwing = -12;
        }
        double idleBob = Math.sin(animationTimer * 2.4) * (grounded ? 1.2 : 0.2);

        double facing = facingRight ? 1 : -1;

        g2d.setColor(new Color(8, 6, 16, 130));
        g2d.fillRoundRect((int) bodyX + 2, (int) bodyY + 6, (int) torsoWidth, (int) torsoHeight, 6, 6);

        g2d.translate(0, idleBob);

        drawLimb(g2d, bodyX + 4, bodyY + torsoHeight - 2, limbThickness, 14, -walkSwing * 0.4, suitSecondary);
        drawLimb(g2d, bodyX + torsoWidth - limbThickness + 2, bodyY + torsoHeight - 2, limbThickness, 14, walkSwing * 0.4, suitSecondary);
        drawLimb(g2d, bodyX + 2, bodyY + 12, limbThickness - 1, 12, -armSwing * 0.5, suitPrimary.darker());
        drawLimb(g2d, bodyX + torsoWidth - limbThickness + 3, bodyY + 12, limbThickness - 1, 12, armSwing * 0.5, suitPrimary.darker());

        g2d.setColor(suitPrimary);
        g2d.fillRoundRect((int) bodyX + 4, (int) bodyY + 8, (int) torsoWidth - 4, (int) torsoHeight - 6, 8, 10);
        g2d.setColor(suitSecondary);
        g2d.fillRoundRect((int) bodyX + 4, (int) (bodyY + torsoHeight / 2.0 + 2), (int) torsoWidth - 4, (int) (torsoHeight / 2.0 - 4), 8, 8);

        g2d.setColor(new Color(30, 18, 42, 180));
        g2d.setStroke(new BasicStroke(1.8f));
        g2d.drawRoundRect((int) bodyX + 4, (int) bodyY + 8, (int) torsoWidth - 4, (int) torsoHeight - 6, 8, 10);

        g2d.setColor(new Color(234, 232, 232, 180));
        g2d.fillRoundRect((int) bodyX + (int) torsoWidth / 2 - 6, (int) bodyY + 14, 12, 6, 6, 6);
        g2d.setColor(new Color(184, 214, 214, 160));
        g2d.fillRoundRect((int) bodyX + (int) torsoWidth / 2 - 4, (int) bodyY + 13, 8, 4, 4, 4);

        AffineTransform faceTransform = g2d.getTransform();
        g2d.translate(facing > 0 ? 0 : torsoWidth, 0);
        g2d.scale(facing, 1);
        g2d.setColor(new Color(42, 30, 54));
        g2d.fillOval((int) headX, (int) headY, (int) headSize, (int) headSize);
        g2d.setColor(visorColor);
        g2d.fillRoundRect((int) headX + 2, (int) headY + 4, (int) headSize - 4, 10, 6, 6);
        g2d.setColor(new Color(255, 255, 255, 160));
        g2d.fillRoundRect((int) headX + 4, (int) headY + 5, 8, 4, 4, 4);
        g2d.setTransform(faceTransform);

        g2d.setStroke(oldStroke);
        g2d.setTransform(old);
    }

    private void drawLimb(Graphics2D g2d, double baseX, double baseY, double thickness, double length, double angleDeg, Color color) {
        AffineTransform old = g2d.getTransform();
        g2d.translate(baseX, baseY);
        g2d.rotate(Math.toRadians(angleDeg));
        g2d.setColor(color);
        g2d.fillRoundRect(0, 0, (int) thickness, (int) length, 4, 4);
        g2d.setTransform(old);
    }

    public void updateAnimation(double dt, GravityDir gravityDir, double tangentialVelocity, boolean groundedState) {
        grounded = groundedState;
        animationTimer += dt;
        double speed = Math.abs(tangentialVelocity);
        if (speed > 0.15) {
            walkCycle += dt * (4 + speed * 0.6);
            if (tangentialVelocity > 0) {
                facingRight = true;
            } else if (tangentialVelocity < 0) {
                facingRight = false;
            }
        } else {
            walkCycle += dt * 2.2;
        }
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
