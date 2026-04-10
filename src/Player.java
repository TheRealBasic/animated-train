import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;

public class Player {
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

    public void applyTangentialFriction(double factor, GravityDir gravityDir) {
        double tangent = getTangentVelocity(gravityDir) * factor;
        if (Math.abs(tangent) < 2.0) {
            tangent = 0;
        }
        setTangentVelocity(gravityDir, tangent);
    }

    public void applyPhysics(List<Platform> platforms, GravityDir gravityDir, MovementTuning tuning, double dt) {
        double nx = gravityDir.getXSign();
        double ny = gravityDir.getYSign();
        double tx = -ny;
        double ty = nx;

        double normalVel = velX * nx + velY * ny;
        double tangentVel = velX * tx + velY * ty;
        normalVel = clampMagnitude(normalVel + tuning.getGravityPerSecond() * dt, tuning.getMaxFallSpeed());
        tangentVel = clampMagnitude(tangentVel, tuning.getMaxRunSpeed() * tuning.getSprintMultiplier());

        moveAlongAxis(tx, ty, tangentVel * dt, platforms, false, nx, ny);
        grounded = false;
        moveAlongAxis(nx, ny, normalVel * dt, platforms, true, nx, ny);

        velX = tangentVel * tx + normalVel * nx;
        velY = tangentVel * ty + normalVel * ny;
    }

    private void moveAlongAxis(double axisX, double axisY, double distance, List<Platform> platforms,
                               boolean normalAxis, double nx, double ny) {
        if (distance == 0) {
            return;
        }
        x += axisX * distance;
        y += axisY * distance;
        Platform overlap = collide(x, y, platforms);
        if (overlap == null) {
            return;
        }
        double correction = computeCorrection(overlap, axisX, axisY);
        x += axisX * correction;
        y += axisY * correction;
        if (normalAxis && (axisX * nx + axisY * ny) > 0) {
            grounded = true;
        }
        if (normalAxis) {
            if (Math.abs(nx) > 0) {
                velX = 0;
            } else {
                velY = 0;
            }
        } else {
            if (Math.abs(axisX) > 0) {
                velX = 0;
            } else {
                velY = 0;
            }
        }
    }

    private double computeCorrection(Platform p, double axisX, double axisY) {
        if (Math.abs(axisX) > 0.5) {
            if (axisX > 0) {
                return (p.getX() - width) - x;
            }
            return (p.getX() + p.getWidth()) - x;
        }
        if (axisY > 0) {
            return (p.getY() - height) - y;
        }
        return (p.getY() + p.getHeight()) - y;
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

    public void jump(GravityDir gravityDir, MovementTuning tuning) {
        double jumpSpeed = tuning.getJumpVelocity();
        velX = velX - gravityDir.getXSign() * jumpSpeed;
        velY = velY - gravityDir.getYSign() * jumpSpeed;
        grounded = false;
    }

    public double getTangentVelocity(GravityDir gravityDir) {
        double tx = -gravityDir.getYSign();
        double ty = gravityDir.getXSign();
        return velX * tx + velY * ty;
    }

    public void setTangentVelocity(GravityDir gravityDir, double velocity) {
        double nx = gravityDir.getXSign();
        double ny = gravityDir.getYSign();
        double tx = -ny;
        double ty = nx;
        double normal = velX * nx + velY * ny;
        velX = velocity * tx + normal * nx;
        velY = velocity * ty + normal * ny;
    }

    public void addTangentialVelocity(GravityDir gravityDir, double delta) {
        setTangentVelocity(gravityDir, getTangentVelocity(gravityDir) + delta);
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

        double facingScale = facingRight ? 1 : -1;
        g2d.translate(centerX, centerY);
        g2d.scale(facingScale, 1);
        g2d.translate(-centerX, -centerY);

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

        g2d.setColor(new Color(42, 30, 54));
        g2d.fillOval((int) headX, (int) headY, (int) headSize, (int) headSize);
        g2d.setColor(visorColor);
        g2d.fillRoundRect((int) headX + 2, (int) headY + 4, (int) headSize - 4, 10, 6, 6);
        g2d.setColor(new Color(255, 255, 255, 160));
        g2d.fillRoundRect((int) headX + 4, (int) headY + 5, 8, 4, 4, 4);

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

    public void setFacingRight(boolean value) {
        facingRight = value;
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
