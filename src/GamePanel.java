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
import java.util.EnumMap;
import java.util.List;

public class GamePanel extends JPanel implements ActionListener, KeyListener {
    private static final int WIDTH = 960;
    private static final int HEIGHT = 540;
    private static final int PLAYER_W = 24;
    private static final int PLAYER_H = 38;
    private static final int KILL_PLANE_PADDING = 400;
    private static final double WARP_COOLDOWN = 0.20;
    private static final double FRICTION = 0.85;
    private static final int FLOOR_HEIGHT = 60;

    private final Timer timer;
    private final Player player;
    private final List<Platform> platforms;
    private final java.awt.geom.Point2D.Double spawnPoint;
    private final EnumMap<GravityDir, java.awt.geom.Point2D.Double> lastSafeGroundedPos;
    private GravityDir gravityDir;
    private enum Edge { LEFT, RIGHT, TOP, BOTTOM }
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean jumpPressed;
    private boolean jumpHeld;
    private double warpCooldownTimer;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(20, 26, 34));
        setFocusable(true);
        addKeyListener(this);

        spawnPoint = new java.awt.geom.Point2D.Double(80, HEIGHT - FLOOR_HEIGHT - PLAYER_H - 12);
        lastSafeGroundedPos = new EnumMap<>(GravityDir.class);
        gravityDir = GravityDir.DOWN;
        lastSafeGroundedPos.put(gravityDir, new java.awt.geom.Point2D.Double(spawnPoint.x, spawnPoint.y));
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
        handleInput();
        player.applyPhysics(platforms, gravityDir);
        if (player.isGrounded()) {
            lastSafeGroundedPos.put(gravityDir, new java.awt.geom.Point2D.Double(player.getX(), player.getY()));
        }
        handleWarp();
        handleKillPlane();
        repaint();
    }

    private void handleInput() {
        double moveSpeed = 0.9;
        boolean moveLeft = leftPressed;
        boolean moveRight = rightPressed;

        if (gravityDir.isVertical()) {
            if (moveLeft && !moveRight) {
                player.addVelocity(-moveSpeed, 0);
            } else if (moveRight && !moveLeft) {
                player.addVelocity(moveSpeed, 0);
            } else {
                player.applyFriction(FRICTION, gravityDir);
            }
        } else {
            if (moveLeft && !moveRight) {
                player.addVelocity(0, -moveSpeed);
            } else if (moveRight && !moveLeft) {
                player.addVelocity(0, moveSpeed);
            } else {
                player.applyFriction(FRICTION, gravityDir);
            }
        }

        if (jumpPressed && !jumpHeld && player.isGrounded()) {
            player.jump(gravityDir);
        }
        jumpHeld = jumpPressed;
    }

    private void handleKillPlane() {
        boolean outOfBounds = false;
        switch (gravityDir) {
            case DOWN:
                outOfBounds = player.getY() > HEIGHT + KILL_PLANE_PADDING;
                break;
            case UP:
                outOfBounds = player.getY() < -KILL_PLANE_PADDING;
                break;
            case LEFT:
                outOfBounds = player.getX() < -KILL_PLANE_PADDING;
                break;
            case RIGHT:
                outOfBounds = player.getX() > WIDTH + KILL_PLANE_PADDING;
                break;
        }

        if (outOfBounds) {
            java.awt.geom.Point2D.Double fallback = lastSafeGroundedPos.getOrDefault(gravityDir, spawnPoint);
            player.setPosition(fallback.x, fallback.y);
            player.resetVelocity();
            warpCooldownTimer = 0;
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
        double margin = 8;
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
        switch (exitEdge) {
            case LEFT:
                newX = viewLeft + margin;
                newY = viewTop + margin;
                gravityDir = GravityDir.UP;
                break;
            case RIGHT:
                newX = viewRight - pw - margin;
                newY = viewTop + margin;
                gravityDir = GravityDir.UP;
                break;
            case TOP:
                newX = viewRight - pw - margin;
                newY = viewBottom - ph - margin;
                gravityDir = GravityDir.DOWN;
                break;
            case BOTTOM:
                newX = viewLeft + margin;
                newY = viewBottom - ph - margin;
                gravityDir = GravityDir.DOWN;
                break;
        }

        player.setPosition(newX, newY);
        player.resetVelocity();
        if (collidesWithPlatform(player.getX(), player.getY())) {
            java.awt.geom.Point2D.Double fallback = lastSafeGroundedPos.getOrDefault(gravityDir, spawnPoint);
            player.setPosition(fallback.x, fallback.y);
            player.resetVelocity();
        }
        warpCooldownTimer = WARP_COOLDOWN;
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g2d);
        drawPlatforms(g2d);
        player.draw(g2d, gravityDir);
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
        g2d.drawString("MODE: " + gravityDir.name(), 14, 82);
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
