import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

public class GamePanel extends JPanel implements ActionListener, KeyListener {
    private static final int WIDTH = 960;
    private static final int HEIGHT = 540;
    private static final int PLAYER_W = 24;
    private static final int PLAYER_H = 38;
    private static final int KILL_PLANE_PADDING = 400;
    private static final double WARP_COOLDOWN = 0.20;
    private static final double INVERT_DURATION = 1.2;
    private static final double FRICTION = 0.85;
    private static final int FLOOR_HEIGHT = 60;

    private final Timer timer;
    private final Player player;
    private final List<Platform> platforms;
    private final java.awt.geom.Point2D.Double spawnPoint;
    private java.awt.geom.Point2D.Double lastSafeGroundedPos;
    private enum Edge { LEFT, RIGHT, TOP, BOTTOM }
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean jumpPressed;
    private boolean jumpHeld;
    private double warpCooldownTimer;
    private double invertControlsTimer;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(20, 26, 34));
        setFocusable(true);
        addKeyListener(this);

        spawnPoint = new java.awt.geom.Point2D.Double(80, HEIGHT - FLOOR_HEIGHT - PLAYER_H - 12);
        lastSafeGroundedPos = new java.awt.geom.Point2D.Double(spawnPoint.x, spawnPoint.y);
        player = new Player(spawnPoint.x, spawnPoint.y, PLAYER_W, PLAYER_H);
        platforms = buildPlatforms();
        timer = new Timer(16, this);
    }

    public void start() {
        requestFocusInWindow();
        timer.start();
    }

    private List<Platform> buildPlatforms() {
        List<Platform> list = new ArrayList<>();
        list.add(new Platform(0, HEIGHT - FLOOR_HEIGHT, WIDTH, FLOOR_HEIGHT));
        list.add(new Platform(140, HEIGHT - 180, 160, 20));
        list.add(new Platform(420, HEIGHT - 260, 200, 20));
        list.add(new Platform(720, HEIGHT - 180, 140, 20));
        list.add(new Platform(320, HEIGHT - 120, 120, 18));
        return list;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        double dt = 0.016;
        if (warpCooldownTimer > 0) {
            warpCooldownTimer = Math.max(0, warpCooldownTimer - dt);
        }
        if (invertControlsTimer > 0) {
            invertControlsTimer = Math.max(0, invertControlsTimer - dt);
        }
        handleInput();
        player.applyPhysics(platforms);
        if (player.isGrounded()) {
            lastSafeGroundedPos = new java.awt.geom.Point2D.Double(player.getX(), player.getY());
        }
        handleWarp();
        handleKillPlane();
        repaint();
    }

    private void handleInput() {
        double moveSpeed = 0.9;
        boolean moveLeft = leftPressed;
        boolean moveRight = rightPressed;
        if (invertControlsTimer > 0) {
            boolean temp = moveLeft;
            moveLeft = moveRight;
            moveRight = temp;
        }
        if (moveLeft && !moveRight) {
            player.addVelocity(-moveSpeed, 0);
        } else if (moveRight && !moveLeft) {
            player.addVelocity(moveSpeed, 0);
        } else {
            player.applyFriction(FRICTION);
        }

        if (jumpPressed && !jumpHeld && player.isGrounded()) {
            player.jump();
        }
        jumpHeld = jumpPressed;
    }

    private void handleKillPlane() {
        if (player.getY() > HEIGHT + KILL_PLANE_PADDING) {
            java.awt.geom.Point2D.Double fallback = lastSafeGroundedPos != null ? lastSafeGroundedPos : spawnPoint;
            player.setPosition(fallback.x, fallback.y);
            player.resetVelocity();
            warpCooldownTimer = 0;
            invertControlsTimer = 0;
        }
    }

    private void handleWarp() {
        if (warpCooldownTimer > 0) {
            return;
        }

        double px = player.getX();
        double py = player.getY();
        double pw = player.getWidth();
        double ph = player.getHeight();
        double viewLeft = 0;
        double viewTop = 0;
        double viewRight = WIDTH;
        double viewBottom = HEIGHT;
        double margin = 4;
        Edge exitEdge = null;

        if (px + pw < viewLeft) {
            exitEdge = Edge.LEFT;
        } else if (px > viewRight) {
            exitEdge = Edge.RIGHT;
        } else if (py + ph < viewTop) {
            exitEdge = Edge.TOP;
        } else if (py > viewBottom) {
            exitEdge = Edge.BOTTOM;
        }

        if (exitEdge == null) {
            return;
        }

        double newX = px;
        double newY = py;
        double newVelX = player.getVelX();
        double newVelY = player.getVelY();
        double normalX = 0;
        double normalY = 0;

        switch (exitEdge) {
            case LEFT:
                double tLeft = clamp((py - viewTop) / (viewBottom - viewTop), 0, 1);
                newX = clamp(viewLeft + tLeft * (viewRight - viewLeft), viewLeft + margin, viewRight - pw - margin);
                newY = viewTop + margin;
                double tempVelLX = newVelX;
                newVelX = newVelY;
                newVelY = -tempVelLX;
                normalY = 1;
                break;
            case RIGHT:
                double tRight = clamp((py - viewTop) / (viewBottom - viewTop), 0, 1);
                newX = clamp(viewLeft + tRight * (viewRight - viewLeft), viewLeft + margin, viewRight - pw - margin);
                newY = viewBottom - ph - margin;
                double tempVelRX = newVelX;
                newVelX = -newVelY;
                newVelY = tempVelRX;
                normalY = -1;
                break;
            case TOP:
                double tTop = clamp((px - viewLeft) / (viewRight - viewLeft), 0, 1);
                newY = clamp(viewTop + tTop * (viewBottom - viewTop), viewTop + margin, viewBottom - ph - margin);
                newX = viewRight - pw - margin;
                double tempVelTY = newVelY;
                newVelY = newVelX;
                newVelX = -tempVelTY;
                normalX = -1;
                break;
            case BOTTOM:
                double tBottom = clamp((px - viewLeft) / (viewRight - viewLeft), 0, 1);
                newY = clamp(viewTop + tBottom * (viewBottom - viewTop), viewTop + margin, viewBottom - ph - margin);
                newX = viewLeft + margin;
                double tempVelBY = newVelY;
                newVelY = -newVelX;
                newVelX = tempVelBY;
                normalX = 1;
                break;
        }

        player.setPosition(newX, newY);
        player.setVelocity(newVelX, newVelY);
        resolvePostWarpOverlap(normalX, normalY);
        warpCooldownTimer = WARP_COOLDOWN;
        invertControlsTimer = INVERT_DURATION;
    }

    private void resolvePostWarpOverlap(double normalX, double normalY) {
        double step = 2.0;
        int attempts = 8;
        while (collidesWithPlatform(player.getX(), player.getY()) && attempts-- > 0) {
            player.setPosition(player.getX() + normalX * step, player.getY() + normalY * step);
        }

        if (collidesWithPlatform(player.getX(), player.getY())) {
            java.awt.geom.Point2D.Double fallback = lastSafeGroundedPos != null ? lastSafeGroundedPos : spawnPoint;
            player.setPosition(fallback.x, fallback.y);
            player.resetVelocity();
        }
    }

    private boolean collidesWithPlatform(double px, double py) {
        for (Platform p : platforms) {
            boolean overlapX = px + player.getWidth() > p.getX() && px < p.getX() + p.getWidth();
            boolean overlapY = py + player.getHeight() > p.getY() && py < p.getY() + p.getHeight();
            if (overlapX && overlapY) {
                return true;
            }
        }
        return false;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g2d);
        drawPlatforms(g2d);
        player.draw(g2d);
        drawHud(g2d);
    }

    private void drawBackground(Graphics2D g2d) {
        g2d.setColor(new Color(33, 42, 56));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(new Color(52, 63, 82));
        for (int x = 0; x < WIDTH; x += 40) {
            g2d.drawLine(x, 0, x, HEIGHT);
        }
        for (int y = 0; y < HEIGHT; y += 40) {
            g2d.drawLine(0, y, WIDTH, y);
        }
    }

    private void drawPlatforms(Graphics2D g2d) {
        g2d.setColor(new Color(99, 209, 255));
        for (Platform platform : platforms) {
            g2d.fillRect((int) platform.getX(), (int) platform.getY(), platform.getWidth(), platform.getHeight());
        }
    }

    private void drawHud(Graphics2D g2d) {
        g2d.setColor(new Color(230, 235, 243));
        g2d.drawString("Controls: A/D to move, W or Space to jump", 14, 22);
        g2d.drawString("Position: (" + (int) player.getX() + ", " + (int) player.getY() + ")", 14, 42);
        g2d.drawString("Velocity: (" + String.format("%.2f", player.getVelX()) + ", " + String.format("%.2f", player.getVelY()) + ")", 14, 62);
        if (invertControlsTimer > 0) {
            String confusion = String.format("WARP CONFUSION: %.1fs", invertControlsTimer);
            g2d.drawString(confusion, 14, 82);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_LEFT) {
            leftPressed = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_RIGHT) {
            rightPressed = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_UP) {
            jumpPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_LEFT) {
            leftPressed = false;
        }
        if (e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_RIGHT) {
            rightPressed = false;
        }
        if (e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_UP) {
            jumpPressed = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // not used
    }
}
