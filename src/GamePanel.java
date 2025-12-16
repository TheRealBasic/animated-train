import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

@SuppressWarnings({"serial", "this-escape"})
public class GamePanel extends JPanel implements ActionListener, KeyListener {
    private static final long serialVersionUID = 1L;
    private static final int BASE_WIDTH = 960;
    private static final int BASE_HEIGHT = 540;
    private static final int PLAYER_W = 24;
    private static final int PLAYER_H = 38;
    private static final int KILL_PADDING = 500;
    private static final double WARP_COOLDOWN = 0.20;
    private static final double FRICTION = 0.85;

    private final Timer timer;
    private final Player player;
    private final EnumMap<GravityDir, Point2D.Double> lastSafeGroundedPos;
    private final LevelManager levelManager;
    private List<Platform> platforms;
    private List<MovingPlatform> movers;
    private List<Spike> spikes;
    private List<Checkpoint> checkpoints;
    private List<FluxOrb> orbs;
    private ExitGate exitGate;
    private ObjectiveManager objectiveManager;
    private GravityDir gravityDir;
    private double warpCooldownTimer;
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean jumpPressed;
    private boolean jumpHeld;

    private Settings settings;
    private SaveGame.SaveData saveData;

    private GameState gameState = GameState.MAIN_MENU;
    private int mainMenuIndex = 0;
    private int pauseMenuIndex = 0;
    private int settingsMenuIndex = 0;
    private int levelCompleteIndex = 0;
    private int levelSelectIndex = 0;
    private int lastCompletedIndex = 0;
    private boolean waitingForBinding = false;
    private String bindingTarget = "";
    private double scale;
    private GameState previousStateBeforeSettings = GameState.MAIN_MENU;
    private Point2D.Double respawnPosition;
    private GravityDir respawnGravity;
    private int deathCount;
    private long lastTickNanos = System.nanoTime();

    private enum Edge { LEFT, RIGHT, TOP, BOTTOM }

    private enum GameState {
        MAIN_MENU,
        SETTINGS,
        LEVEL_SELECT,
        IN_GAME,
        PAUSE,
        LEVEL_COMPLETE,
        CREDITS
    }

    public GamePanel() {
        settings = Settings.load();
        scale = settings.getScreenScale();
        setPreferredSize(new Dimension((int) (BASE_WIDTH * scale), (int) (BASE_HEIGHT * scale)));
        setBackground(new Color(20, 26, 34));
        setFocusable(true);
        addKeyListener(this);

        lastSafeGroundedPos = new EnumMap<>(GravityDir.class);
        gravityDir = GravityDir.DOWN;
        player = new Player(0, 0, PLAYER_W, PLAYER_H);

        levelManager = new LevelManager();
        saveData = SaveGame.load(levelManager.getLevelCount());
        loadLevel(saveData.currentLevelIndex);

        timer = new Timer(16, this);
    }

    public void start() {
        requestFocusInWindow();
        timer.start();
    }

    private void loadLevel(int index) {
        LevelData data = levelManager.getLevel(index);
        if (data == null) {
            return;
        }
        platforms = data.getPlatforms();
        movers = data.getMovers();
        spikes = data.getSpikes();
        checkpoints = data.getCheckpoints();
        orbs = new ArrayList<>();
        for (Point2D.Double pos : data.getOrbPositions()) {
            orbs.add(new FluxOrb(pos, 12));
        }
        exitGate = new ExitGate(data.getExitGateX(), data.getExitGateY(), data.getExitGateWidth(), data.getExitGateHeight());
        exitGate.setUnlocked(false);
        objectiveManager = new ObjectiveManager(orbs, exitGate, data.getParTimeSeconds());
        objectiveManager.resetTimer();
        Point2D.Double spawn = data.getSpawnPosition();
        player.setPosition(spawn.x, spawn.y);
        player.resetVelocity();
        gravityDir = data.getSpawnGravity();
        respawnPosition = new Point2D.Double(spawn.x, spawn.y);
        respawnGravity = gravityDir;
        deathCount = 0;
        lastSafeGroundedPos.clear();
        for (GravityDir dir : GravityDir.values()) {
            lastSafeGroundedPos.put(dir, new Point2D.Double(spawn.x, spawn.y));
        }
        warpCooldownTimer = 0;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double dt = (now - lastTickNanos) / 1_000_000_000.0;
        dt = Math.min(dt, 0.05);
        lastTickNanos = now;
        if (warpCooldownTimer > 0) {
            warpCooldownTimer = Math.max(0, warpCooldownTimer - dt);
        }

        if (gameState == GameState.IN_GAME) {
            handleInput();
            updateMovingPlatforms(dt);
            player.applyPhysics(getAllPlatforms(), gravityDir);
            if (player.isGrounded()) {
                lastSafeGroundedPos.put(gravityDir, new Point2D.Double(player.getX(), player.getY()));
            }
            handleWarp();
            handleKillPlane();
            handleCheckpoints();
            handleHazards();
            objectiveManager.update(dt, player);
            if (exitGate.checkCollision(player)) {
                onLevelComplete();
            }
        }

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
        double px = player.getX();
        double py = player.getY();
        boolean outOfBounds = px < -KILL_PADDING || px > BASE_WIDTH + KILL_PADDING || py < -KILL_PADDING || py > BASE_HEIGHT + KILL_PADDING;
        if (outOfBounds) {
            respawn();
        }
    }

    private void handleHazards() {
        for (Spike spike : spikes) {
            if (spike.intersects(player)) {
                respawn();
                return;
            }
        }
    }

    private void handleCheckpoints() {
        for (Checkpoint checkpoint : checkpoints) {
            if (!checkpoint.isActivated() && checkpoint.check(player)) {
                respawnPosition = new Point2D.Double(checkpoint.getPosition().x, checkpoint.getPosition().y);
                respawnGravity = gravityDir;
            }
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
        double margin = 12;
        Edge exitEdge = null;

        if (px + pw < 0) {
            exitEdge = Edge.LEFT;
        } else if (px > BASE_WIDTH) {
            exitEdge = Edge.RIGHT;
        } else if (py + ph < 0) {
            exitEdge = Edge.TOP;
        } else if (py > BASE_HEIGHT) {
            exitEdge = Edge.BOTTOM;
        }

        if (exitEdge == null) {
            return;
        }

        double newX = px;
        double newY = py;
        switch (exitEdge) {
            case LEFT:
                newX = margin;
                gravityDir = GravityDir.LEFT;
                break;
            case RIGHT:
                newX = BASE_WIDTH - pw - margin;
                gravityDir = GravityDir.RIGHT;
                break;
            case TOP:
                newY = margin;
                gravityDir = GravityDir.UP;
                break;
            case BOTTOM:
                newY = BASE_HEIGHT - ph - margin;
                gravityDir = GravityDir.DOWN;
                break;
        }

        player.setPosition(newX, newY);
        player.resetVelocity();
        if (collidesWithPlatform(player.getX(), player.getY())) {
            Point2D.Double fallback = lastSafeGroundedPos.getOrDefault(gravityDir, new Point2D.Double(newX, newY));
            player.setPosition(fallback.x, fallback.y);
            player.resetVelocity();
        }
        warpCooldownTimer = WARP_COOLDOWN;
    }

    private void respawn() {
        deathCount++;
        player.setPosition(respawnPosition.x, respawnPosition.y);
        player.resetVelocity();
        gravityDir = respawnGravity;
        warpCooldownTimer = 0;
    }

    private void updateMovingPlatforms(double dt) {
        for (MovingPlatform mover : movers) {
            mover.update(dt);
        }
    }

    private List<Platform> getAllPlatforms() {
        List<Platform> all = new ArrayList<>(platforms);
        all.addAll(movers);
        return all;
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

    private void onLevelComplete() {
        double elapsed = objectiveManager.getElapsedTime();
        lastCompletedIndex = saveData.currentLevelIndex;
        double best = saveData.bestTimes[lastCompletedIndex];
        if (best == 0 || elapsed < best) {
            saveData.bestTimes[lastCompletedIndex] = elapsed;
        }
        String medal = determineMedal(elapsed, objectiveManager.getParTimeSeconds());
        saveData.bestMedals[lastCompletedIndex] = medal;
        saveData.bestDeaths[lastCompletedIndex] = (saveData.bestDeaths[lastCompletedIndex] == 0) ? deathCount : Math.min(saveData.bestDeaths[lastCompletedIndex], deathCount);
        if (lastCompletedIndex + 1 < levelManager.getLevelCount()) {
            saveData.unlockedLevels = Math.max(saveData.unlockedLevels, lastCompletedIndex + 2);
            saveData.currentLevelIndex = lastCompletedIndex + 1;
        }
        SaveGame.save(saveData);
        gameState = GameState.LEVEL_COMPLETE;
        levelCompleteIndex = 0;
    }

    private void nextLevel() {
        int idx = saveData.currentLevelIndex;
        if (idx >= levelManager.getLevelCount()) {
            saveData.currentLevelIndex = levelManager.getLevelCount() - 1;
            idx = saveData.currentLevelIndex;
        }
        loadLevel(idx);
        gameState = GameState.IN_GAME;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.scale(scale, scale);

        drawBackground(g2d);
        switch (gameState) {
            case MAIN_MENU:
                drawTitle(g2d, "Gravity Warp Trials");
                drawMainMenu(g2d);
                break;
            case SETTINGS:
                drawTitle(g2d, "Settings");
                drawSettingsMenu(g2d);
                break;
            case LEVEL_SELECT:
                drawTitle(g2d, "Level Select");
                drawLevelSelect(g2d);
                break;
            case IN_GAME:
                drawWorld(g2d);
                drawHud(g2d);
                break;
            case PAUSE:
                drawWorld(g2d);
                drawHud(g2d);
                drawPauseMenu(g2d);
                break;
            case LEVEL_COMPLETE:
                drawWorld(g2d);
                drawLevelComplete(g2d);
                break;
            case CREDITS:
                drawCredits(g2d);
                break;
        }
    }

    private void drawBackground(Graphics2D g2d) {
        g2d.setColor(new Color(33, 42, 56));
        g2d.fillRect(0, 0, BASE_WIDTH, BASE_HEIGHT);
        g2d.setColor(new Color(52, 63, 82));
        for (int x = 0; x < BASE_WIDTH; x += 40) {
            g2d.drawLine(x, 0, x, BASE_HEIGHT);
        }
        for (int y = 0; y < BASE_HEIGHT; y += 40) {
            g2d.drawLine(0, y, BASE_WIDTH, y);
        }
    }

    private void drawTitle(Graphics2D g2d, String text) {
        g2d.setColor(new Color(230, 235, 243));
        g2d.setFont(new Font("Consolas", Font.BOLD, 32));
        int width = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, (BASE_WIDTH - width) / 2, 120);
    }

    private void drawMainMenu(Graphics2D g2d) {
        g2d.setFont(new Font("Consolas", Font.PLAIN, 20));
        String[] options = new String[]{
                "Continue",
                "New Game",
                "Level Select",
                "Settings",
                "Credits",
                "Quit"
        };
        int startY = 200;
        for (int i = 0; i < options.length; i++) {
            boolean disabled = options[i].equals("Continue") && !SaveGame.exists();
            g2d.setColor(mainMenuIndex == i ? new Color(190, 255, 180) : new Color(230, 235, 243));
            if (disabled) {
                g2d.setColor(new Color(140, 150, 160));
            }
            String text = options[i];
            int width = g2d.getFontMetrics().stringWidth(text);
            g2d.drawString(text, (BASE_WIDTH - width) / 2, startY + i * 36);
        }
        drawControlHint(g2d, "Use W/S or Up/Down to navigate, Enter to select");
    }

    private void drawSettingsMenu(Graphics2D g2d) {
        g2d.setFont(new Font("Consolas", Font.PLAIN, 18));
        List<String> lines = new ArrayList<>();
        lines.add("Master Volume: " + settings.getMasterVolume());
        lines.add("Screen Scale: " + String.format("%.1fx", settings.getScreenScale()));
        lines.add("Show Debug HUD: " + (settings.isShowDebugHud() ? "On" : "Off"));
        lines.add("Rebind Left: " + KeyEvent.getKeyText(settings.getKeyLeft()));
        lines.add("Rebind Right: " + KeyEvent.getKeyText(settings.getKeyRight()));
        lines.add("Rebind Jump: " + KeyEvent.getKeyText(settings.getKeyJump()));
        lines.add("Back");

        int startY = 200;
        for (int i = 0; i < lines.size(); i++) {
            g2d.setColor(settingsMenuIndex == i ? new Color(190, 255, 180) : new Color(230, 235, 243));
            String text = lines.get(i);
            int width = g2d.getFontMetrics().stringWidth(text);
            g2d.drawString(text, (BASE_WIDTH - width) / 2, startY + i * 32);
        }
        if (waitingForBinding) {
            drawControlHint(g2d, "Press a key to bind for " + bindingTarget);
        } else {
            drawControlHint(g2d, "Left/Right adjust, Enter to edit");
        }
    }

    private void drawLevelSelect(Graphics2D g2d) {
        g2d.setFont(new Font("Consolas", Font.PLAIN, 18));
        int startY = 200;
        for (int i = 0; i < levelManager.getLevelCount(); i++) {
            boolean locked = i >= saveData.unlockedLevels;
            String label = (i + 1) + ". " + levelManager.getLevel(i).getName();
            if (locked) {
                label += " (Locked)";
            }
            g2d.setColor(levelSelectIndex == i ? new Color(190, 255, 180) : new Color(230, 235, 243));
            if (locked) {
                g2d.setColor(new Color(120, 130, 140));
            }
            int width = g2d.getFontMetrics().stringWidth(label);
            g2d.drawString(label, (BASE_WIDTH - width) / 2, startY + i * 26);
        }
        drawControlHint(g2d, "Enter to play, Esc to back");
    }

    private void drawWorld(Graphics2D g2d) {
        g2d.setColor(new Color(99, 209, 255));
        for (Platform platform : platforms) {
            g2d.fillRect((int) platform.getX(), (int) platform.getY(), platform.getWidth(), platform.getHeight());
        }
        g2d.setColor(new Color(120, 200, 255));
        for (MovingPlatform mover : movers) {
            g2d.fillRect((int) mover.getX(), (int) mover.getY(), mover.getWidth(), mover.getHeight());
        }
        exitGate.draw(g2d);
        for (Checkpoint checkpoint : checkpoints) {
            checkpoint.draw(g2d);
        }
        for (Spike spike : spikes) {
            spike.draw(g2d);
        }
        for (FluxOrb orb : orbs) {
            orb.draw(g2d);
        }
        player.draw(g2d, gravityDir);
    }

    private void drawHud(Graphics2D g2d) {
        g2d.setColor(new Color(230, 235, 243));
        g2d.setFont(new Font("Consolas", Font.PLAIN, 16));
        long collected = orbs.stream().filter(FluxOrb::isCollected).count();
        g2d.drawString("Orbs: " + collected + "/" + orbs.size(), 14, 22);
        g2d.drawString("Gate: " + (exitGate.isUnlocked() ? "Unlocked" : "Locked"), 14, 42);
        g2d.drawString("Gravity: " + gravityDir.name(), 14, 62);
        g2d.drawString("Time: " + String.format("%.1fs", objectiveManager.getElapsedTime()), 14, 82);
        g2d.drawString("Par: " + String.format("%.1fs", objectiveManager.getParTimeSeconds()), 14, 102);
        g2d.drawString("Deaths: " + deathCount, 14, 122);
        g2d.drawString("Controls: A/D move • Space jump • Warp edges flip gravity", 14, 142);
        if (settings.isShowDebugHud()) {
            g2d.drawString("Position: (" + (int) player.getX() + ", " + (int) player.getY() + ")", 14, 162);
            g2d.drawString("Velocity: (" + String.format("%.2f", player.getVelX()) + ", " + String.format("%.2f", player.getVelY()) + ")", 14, 182);
        }
    }

    private void drawPauseMenu(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 140));
        g2d.fillRect(0, 0, BASE_WIDTH, BASE_HEIGHT);
        g2d.setFont(new Font("Consolas", Font.BOLD, 24));
        g2d.setColor(new Color(230, 235, 243));
        g2d.drawString("Paused", BASE_WIDTH / 2 - 40, 160);

        g2d.setFont(new Font("Consolas", Font.PLAIN, 18));
        String[] options = new String[]{"Resume", "Restart Level", "Settings", "Save & Quit"};
        int startY = 210;
        for (int i = 0; i < options.length; i++) {
            g2d.setColor(pauseMenuIndex == i ? new Color(190, 255, 180) : new Color(230, 235, 243));
            int width = g2d.getFontMetrics().stringWidth(options[i]);
            g2d.drawString(options[i], (BASE_WIDTH - width) / 2, startY + i * 32);
        }
    }

    private void drawLevelComplete(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 160));
        g2d.fillRect(0, 0, BASE_WIDTH, BASE_HEIGHT);
        g2d.setFont(new Font("Consolas", Font.BOLD, 24));
        g2d.setColor(new Color(230, 235, 243));
        g2d.drawString("Level Complete!", BASE_WIDTH / 2 - 80, 160);

        LevelData data = levelManager.getLevel(lastCompletedIndex);
        double elapsed = objectiveManager.getElapsedTime();
        double par = data.getParTimeSeconds();
        double best = saveData.bestTimes[Math.min(lastCompletedIndex, saveData.bestTimes.length - 1)];
        g2d.setFont(new Font("Consolas", Font.PLAIN, 18));
        g2d.drawString("Level: " + data.getName(), BASE_WIDTH / 2 - 120, 200);
        g2d.drawString("Time: " + String.format("%.1fs", elapsed) + (elapsed <= par ? " (PAR BEAT!)" : ""), BASE_WIDTH / 2 - 120, 230);
        String medal = determineMedal(elapsed, par);
        g2d.drawString("Medal: " + medal, BASE_WIDTH / 2 - 120, 260);
        g2d.drawString("Deaths: " + deathCount, BASE_WIDTH / 2 - 120, 290);
        if (best > 0) {
            g2d.drawString("Best: " + String.format("%.1fs", best), BASE_WIDTH / 2 - 120, 320);
        }

        String[] options = new String[]{"Next / Retry", "Main Menu"};
        int startY = 360;
        for (int i = 0; i < options.length; i++) {
            g2d.setColor(levelCompleteIndex == i ? new Color(190, 255, 180) : new Color(230, 235, 243));
            int width = g2d.getFontMetrics().stringWidth(options[i]);
            g2d.drawString(options[i], (BASE_WIDTH - width) / 2, startY + i * 32);
        }
    }

    private void drawControlHint(Graphics2D g2d, String hint) {
        g2d.setColor(new Color(140, 180, 200));
        int width = g2d.getFontMetrics().stringWidth(hint);
        g2d.drawString(hint, (BASE_WIDTH - width) / 2, BASE_HEIGHT - 40);
    }

    private void drawCredits(Graphics2D g2d) {
        drawTitle(g2d, "Gravity Warp Trials");
        g2d.setFont(new Font("Consolas", Font.PLAIN, 18));
        g2d.setColor(new Color(230, 235, 243));
        String[] lines = {
                "Programming & Design: Solo Dev",
                "Engine: Custom Java2D", 
                "Thanks for playing!"
        };
        int startY = 220;
        for (int i = 0; i < lines.length; i++) {
            int width = g2d.getFontMetrics().stringWidth(lines[i]);
            g2d.drawString(lines[i], (BASE_WIDTH - width) / 2, startY + i * 28);
        }
        drawControlHint(g2d, "Press Esc or Enter to return");
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (waitingForBinding) {
            applyBinding(e.getKeyCode());
            return;
        }

        if (gameState == GameState.MAIN_MENU) {
            handleMenuNavigation(e, 6, () -> handleMainMenuSelect(mainMenuIndex));
            return;
        }
        if (gameState == GameState.CREDITS) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                gameState = GameState.MAIN_MENU;
            }
            return;
        }
        if (gameState == GameState.LEVEL_SELECT) {
            handleMenuNavigation(e, levelManager.getLevelCount(), () -> handleLevelSelect(levelSelectIndex));
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                gameState = GameState.MAIN_MENU;
            }
            return;
        }
        if (gameState == GameState.SETTINGS) {
            handleMenuNavigation(e, 7, this::handleSettingsSelect);
            if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                adjustSetting(-1);
            } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                adjustSetting(1);
            } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                gameState = previousStateBeforeSettings;
            }
            return;
        }
        if (gameState == GameState.PAUSE) {
            handleMenuNavigation(e, 4, () -> handlePauseSelect(pauseMenuIndex));
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                gameState = GameState.IN_GAME;
            }
            return;
        }
        if (gameState == GameState.LEVEL_COMPLETE) {
            handleMenuNavigation(e, 2, () -> handleLevelCompleteSelect(levelCompleteIndex));
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            gameState = GameState.PAUSE;
            pauseMenuIndex = 0;
            return;
        }

        if (e.getKeyCode() == settings.getKeyLeft()) {
            leftPressed = true;
        }
        if (e.getKeyCode() == settings.getKeyRight()) {
            rightPressed = true;
        }
        if (e.getKeyCode() == settings.getKeyJump() || e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_UP) {
            jumpPressed = true;
        }
    }

    private void handleMenuNavigation(KeyEvent e, int itemCount, Runnable onEnter) {
        if (e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_UP) {
            switch (gameState) {
                case MAIN_MENU:
                    mainMenuIndex = (mainMenuIndex - 1 + itemCount) % itemCount;
                    break;
                case SETTINGS:
                    settingsMenuIndex = (settingsMenuIndex - 1 + itemCount) % itemCount;
                    break;
                case PAUSE:
                    pauseMenuIndex = (pauseMenuIndex - 1 + itemCount) % itemCount;
                    break;
                case LEVEL_COMPLETE:
                    levelCompleteIndex = (levelCompleteIndex - 1 + itemCount) % itemCount;
                    break;
                case LEVEL_SELECT:
                    levelSelectIndex = (levelSelectIndex - 1 + itemCount) % itemCount;
                    break;
            }
        }
        if (e.getKeyCode() == KeyEvent.VK_S || e.getKeyCode() == KeyEvent.VK_DOWN) {
            switch (gameState) {
                case MAIN_MENU:
                    mainMenuIndex = (mainMenuIndex + 1) % itemCount;
                    break;
                case SETTINGS:
                    settingsMenuIndex = (settingsMenuIndex + 1) % itemCount;
                    break;
                case PAUSE:
                    pauseMenuIndex = (pauseMenuIndex + 1) % itemCount;
                    break;
                case LEVEL_COMPLETE:
                    levelCompleteIndex = (levelCompleteIndex + 1) % itemCount;
                    break;
                case LEVEL_SELECT:
                    levelSelectIndex = (levelSelectIndex + 1) % itemCount;
                    break;
            }
        }
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            onEnter.run();
        }
    }

    private void handleMainMenuSelect(int index) {
        switch (index) {
            case 0: // Continue
                loadLevel(Math.min(saveData.currentLevelIndex, levelManager.getLevelCount() - 1));
                gameState = GameState.IN_GAME;
                break;
            case 1: // New Game
                SaveGame.wipe();
                saveData = SaveGame.load(levelManager.getLevelCount());
                loadLevel(0);
                gameState = GameState.IN_GAME;
                break;
            case 2: // Level Select
                gameState = GameState.LEVEL_SELECT;
                levelSelectIndex = 0;
                break;
            case 3: // Settings
                previousStateBeforeSettings = GameState.MAIN_MENU;
                gameState = GameState.SETTINGS;
                settingsMenuIndex = 0;
                break;
            case 4: // Credits
                gameState = GameState.CREDITS;
                break;
            case 5: // Quit
                System.exit(0);
                break;
        }
    }

    private void handlePauseSelect(int index) {
        switch (index) {
            case 0:
                gameState = GameState.IN_GAME;
                break;
            case 1:
                loadLevel(saveData.currentLevelIndex);
                gameState = GameState.IN_GAME;
                break;
            case 2:
                previousStateBeforeSettings = GameState.PAUSE;
                gameState = GameState.SETTINGS;
                settingsMenuIndex = 0;
                break;
            case 3:
                SaveGame.save(saveData);
                settings.save();
                gameState = GameState.MAIN_MENU;
                break;
        }
    }

    private void handleLevelSelect(int index) {
        if (index < saveData.unlockedLevels) {
            saveData.currentLevelIndex = index;
            SaveGame.save(saveData);
            loadLevel(index);
            gameState = GameState.IN_GAME;
        }
    }

    private void handleLevelCompleteSelect(int index) {
        switch (index) {
            case 0:
                nextLevel();
                break;
            case 1:
                gameState = GameState.MAIN_MENU;
                break;
        }
    }

    private String determineMedal(double elapsed, double par) {
        if (elapsed <= par) {
            return "Gold";
        }
        if (elapsed <= par * 1.25) {
            return "Silver";
        }
        if (elapsed <= par * 1.5) {
            return "Bronze";
        }
        return "None";
    }

    private void adjustSetting(int delta) {
        switch (settingsMenuIndex) {
            case 0:
                settings.setMasterVolume(settings.getMasterVolume() + delta * 5);
                settings.save();
                break;
            case 1:
                settings.setScreenScale(settings.getScreenScale() + delta * 0.5);
                scale = settings.getScreenScale();
                setPreferredSize(new Dimension((int) (BASE_WIDTH * scale), (int) (BASE_HEIGHT * scale)));
                revalidate();
                if (SwingUtilities.getWindowAncestor(this) != null) {
                    SwingUtilities.getWindowAncestor(this).pack();
                }
                settings.save();
                break;
            case 2:
                settings.setShowDebugHud(!settings.isShowDebugHud());
                settings.save();
                break;
            case 3:
                waitingForBinding = true;
                bindingTarget = "Left";
                break;
            case 4:
                waitingForBinding = true;
                bindingTarget = "Right";
                break;
            case 5:
                waitingForBinding = true;
                bindingTarget = "Jump";
                break;
            case 6:
                settings.save();
                gameState = previousStateBeforeSettings;
                break;
        }
    }

    private void applyBinding(int keyCode) {
        if (bindingTarget.equals("Left")) {
            settings.setKeyLeft(keyCode);
        } else if (bindingTarget.equals("Right")) {
            settings.setKeyRight(keyCode);
        } else if (bindingTarget.equals("Jump")) {
            settings.setKeyJump(keyCode);
        }
        waitingForBinding = false;
        bindingTarget = "";
        settings.save();
    }

    private void handleSettingsSelect() {
        if (settingsMenuIndex == 6) {
            gameState = previousStateBeforeSettings;
        } else {
            adjustSetting(0);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == settings.getKeyLeft()) {
            leftPressed = false;
        }
        if (e.getKeyCode() == settings.getKeyRight()) {
            rightPressed = false;
        }
        if (e.getKeyCode() == settings.getKeyJump() || e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_UP) {
            jumpPressed = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // not used
    }
}
