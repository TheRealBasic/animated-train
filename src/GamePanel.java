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
    private static final double FRICTION = 0.85;
    private static final int FLOOR_HEIGHT = 60;

    private final Timer timer;
    private final Player player;
    private final List<Platform> platforms;
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean jumpPressed;
    private boolean jumpHeld;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(20, 26, 34));
        setFocusable(true);
        addKeyListener(this);

        player = new Player(80, HEIGHT - FLOOR_HEIGHT - 60, 36, 48);
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
        handleInput();
        player.applyPhysics(platforms);
        repaint();
    }

    private void handleInput() {
        double moveSpeed = 0.9;
        if (leftPressed && !rightPressed) {
            player.addVelocity(-moveSpeed, 0);
        } else if (rightPressed && !leftPressed) {
            player.addVelocity(moveSpeed, 0);
        } else {
            player.applyFriction(FRICTION);
        }

        if (jumpPressed && !jumpHeld && player.isGrounded()) {
            player.jump();
        }
        jumpHeld = jumpPressed;
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
