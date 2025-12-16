import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"serial", "this-escape"})
public class GamePanel extends JPanel implements ActionListener, KeyListener {
    private static final long serialVersionUID = 1L;
    private static final int BASE_WIDTH = 960;
    private static final int BASE_HEIGHT = 540;
    private static final int PLAYER_W = 24;
    private static final int PLAYER_H = 38;
    private static final int KILL_PADDING = 500;
    private static final double FRICTION = 0.85;
    private static final double GRAVITY_COOLDOWN = 0.4;

    private final Timer timer;
    private final Player player;
    private Player partner;
    private final EnumMap<GravityDir, Point2D.Double> lastSafeGroundedPos;
    private final LevelManager levelManager;
    private List<Platform> platforms;
    private List<MovingPlatform> movers;
    private List<Spike> spikes;
    private List<Checkpoint> checkpoints;
    private List<FluxOrb> orbs;
    private List<CoopButton> buttons;
    private List<CoopDoor> doors;
    private ExitGate exitGate;
    private ObjectiveManager objectiveManager;
    private GravityDir gravityDir;
    private GravityDir partnerGravity;
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean jumpPressed;
    private boolean jumpHeld;

    private Settings settings;
    private SaveGame.SaveData saveData;
    private double gravityCooldownRemaining;

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
    private boolean multiplayerActive;
    private boolean multiplayerHost;
    private MultiplayerSession session;
    private long localOrbMask;
    private long remoteOrbMask;
    private int multiplayerMenuIndex = 0;
    private int multiplayerLevelIndex = 0;
    private boolean waitingForLevelSync;
    private String directIpInput = "127.0.0.1";

    private enum GameState {
        MAIN_MENU,
        MULTIPLAYER_MENU,
        MULTIPLAYER_WAIT,
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
        gravityCooldownRemaining = 0;
        player = new Player(0, 0, PLAYER_W, PLAYER_H);
        partner = new Player(0, 0, PLAYER_W, PLAYER_H);

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
        buttons = data.getButtons();
        doors = data.getDoors();
        for (Point2D.Double pos : data.getOrbPositions()) {
            orbs.add(new FluxOrb(pos, 12));
        }
        exitGate = new ExitGate(data.getExitGateX(), data.getExitGateY(), data.getExitGateWidth(), data.getExitGateHeight());
        exitGate.setUnlocked(false);
        objectiveManager = new ObjectiveManager(orbs, exitGate, data.getParTimeSeconds());
        objectiveManager.resetTimer();
        Point2D.Double spawn = data.getSpawnPosition();
        Point2D.Double partnerSpawn = data.getPartnerSpawnPosition();
        player.setPosition(spawn.x, spawn.y);
        player.resetVelocity();
        partner.setPosition(partnerSpawn.x, partnerSpawn.y);
        partner.resetVelocity();
        gravityDir = data.getSpawnGravity();
        partnerGravity = data.getSpawnGravity();
        respawnPosition = new Point2D.Double(spawn.x, spawn.y);
        respawnGravity = gravityDir;
        deathCount = 0;
        localOrbMask = 0;
        remoteOrbMask = 0;
        lastSafeGroundedPos.clear();
        for (GravityDir dir : GravityDir.values()) {
            lastSafeGroundedPos.put(dir, new Point2D.Double(spawn.x, spawn.y));
        }
        gravityCooldownRemaining = 0;
        waitingForLevelSync = false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double dt = (now - lastTickNanos) / 1_000_000_000.0;
        dt = Math.min(dt, 0.05);
        lastTickNanos = now;
        if (multiplayerActive && gameState == GameState.MULTIPLAYER_WAIT) {
            pollWaitingLevelSync();
        }

        if (gameState == GameState.IN_GAME) {
            updateGravityCooldown(dt);
            handleInput();
            updateMovingPlatforms(dt);
            player.applyPhysics(getAllPlatforms(), gravityDir);
            if (multiplayerActive && session != null) {
                syncMultiplayer();
            }
            if (player.isGrounded()) {
                lastSafeGroundedPos.put(gravityDir, new Point2D.Double(player.getX(), player.getY()));
            }
            updateCoopButtons();
            collectOrbs(player, true);
            if (multiplayerActive) {
                collectOrbs(partner, false);
            }
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

    private void pollWaitingLevelSync() {
        if (session == null) {
            return;
        }
        MultiplayerSession.RemoteState remote = session.pollRemoteState();
        if (remote.levelIndex() != null) {
            saveData.currentLevelIndex = remote.levelIndex();
            SaveGame.save(saveData);
            loadLevel(remote.levelIndex());
            waitingForLevelSync = false;
            gameState = GameState.IN_GAME;
        }
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

    private void collectOrbs(Player target, boolean localPlayer) {
        if (target == null) {
            return;
        }
        for (int i = 0; i < orbs.size(); i++) {
            FluxOrb orb = orbs.get(i);
            if (orb.checkCollected(target)) {
                if (localPlayer) {
                    localOrbMask |= (1L << i);
                } else {
                    remoteOrbMask |= (1L << i);
                }
            }
            boolean collected = ((localOrbMask | remoteOrbMask) & (1L << i)) != 0;
            orb.setCollected(collected);
        }
    }

    private void updateCoopButtons() {
        if (buttons == null || doors == null) {
            return;
        }
        for (CoopButton button : buttons) {
            boolean pressed = button.check(player);
            if (multiplayerActive) {
                pressed = button.check(player) || button.check(partner);
            }
            if (!pressed) {
                button.check(player);
            }
        }
        for (CoopDoor door : doors) {
            door.update(buttons);
        }
    }

    private void syncMultiplayer() {
        MultiplayerSession.RemoteState remote = session.pollRemoteState();
        if (remote.levelIndex() != null && !waitingForLevelSync) {
            saveData.currentLevelIndex = remote.levelIndex();
            SaveGame.save(saveData);
            loadLevel(remote.levelIndex());
            multiplayerActive = true;
            waitingForLevelSync = false;
            gameState = GameState.IN_GAME;
        }
        if (remote.x() != null && remote.y() != null && remote.gravity() != null) {
            partner.setPosition(remote.x(), remote.y());
            partnerGravity = remote.gravity();
            if (remote.orbMask() != null) {
                remoteOrbMask = remote.orbMask();
            }
        }
        if (session != null) {
            session.sendState(player.getX(), player.getY(), gravityDir, localOrbMask);
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

    private void respawn() {
        deathCount++;
        player.setPosition(respawnPosition.x, respawnPosition.y);
        player.resetVelocity();
        gravityDir = respawnGravity;
    }

    private void updateMovingPlatforms(double dt) {
        for (MovingPlatform mover : movers) {
            mover.update(dt);
        }
    }

    private List<Platform> getAllPlatforms() {
        List<Platform> all = new ArrayList<>(platforms);
        all.addAll(movers);
        for (CoopDoor door : doors) {
            if (!multiplayerActive || !door.blocks(partner)) {
                // closed doors behave as solid walls for everyone
                all.add(new Platform(door.getBounds().x, door.getBounds().y, (int) door.getBounds().width, (int) door.getBounds().height));
            }
        }
        return all;
    }

    private void updateGravityCooldown(double dt) {
        if (gravityCooldownRemaining > 0) {
            gravityCooldownRemaining = Math.max(0, gravityCooldownRemaining - dt);
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

    private void changeGravity(GravityDir newDir) {
        if (gravityDir == newDir || gravityCooldownRemaining > 0) {
            return;
        }

        GravityDir previousDir = gravityDir;
        Point2D.Double previousPos = new Point2D.Double(player.getX(), player.getY());
        gravityDir = newDir;
        player.resetVelocity();

        boolean reverted = false;
        if (collidesWithPlatform(player.getX(), player.getY())) {
            Point2D.Double safePos = lastSafeGroundedPos.getOrDefault(newDir, respawnPosition);
            player.setPosition(safePos.x, safePos.y);
            if (collidesWithPlatform(player.getX(), player.getY())) {
                gravityDir = previousDir;
                player.setPosition(previousPos.x, previousPos.y);
                reverted = true;
            }
        }

        if (!reverted && gravityDir == newDir) {
            gravityCooldownRemaining = GRAVITY_COOLDOWN;
        }
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
                drawMainMenu(g2d);
                break;
            case MULTIPLAYER_MENU:
                drawTitle(g2d, "Multiplayer Co-op");
                drawMultiplayerMenu(g2d);
                break;
            case MULTIPLAYER_WAIT:
                drawTitle(g2d, "Waiting for partner...");
                drawMultiplayerWait(g2d);
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

        drawCrtOverlay(g2d);
    }

    private void drawBackground(Graphics2D g2d) {
        GradientPaint dusk = new GradientPaint(0, 0, new Color(6, 8, 12), 0, BASE_HEIGHT, new Color(3, 4, 8));
        g2d.setPaint(dusk);
        g2d.fillRect(0, 0, BASE_WIDTH, BASE_HEIGHT);

        g2d.setColor(new Color(48, 60, 72, 55));
        for (int y = -40; y < BASE_HEIGHT + 80; y += 36) {
            g2d.fillRect(-20, y, BASE_WIDTH + 40, 8);
        }

        g2d.setColor(new Color(90, 104, 132, 38));
        for (int i = 0; i < BASE_WIDTH; i += 120) {
            g2d.drawLine(i, 0, i + 60, BASE_HEIGHT);
        }

        g2d.setColor(new Color(12, 180, 200, 18));
        for (int i = 0; i < 10; i++) {
            int size = 220 + i * 24;
            g2d.drawOval(BASE_WIDTH - size, 40 - (i * 10), size, size);
        }
    }

    private void drawCrtOverlay(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 10));
        for (int y = 0; y < BASE_HEIGHT; y += 3) {
            g2d.drawLine(0, y, BASE_WIDTH, y);
        }

        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.drawRect(0, 0, BASE_WIDTH - 1, BASE_HEIGHT - 1);
        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.fillRect(0, 0, BASE_WIDTH, 18);
        g2d.fillRect(0, BASE_HEIGHT - 18, BASE_WIDTH, 18);
    }

    private void drawTitle(Graphics2D g2d, String text) {
        g2d.setColor(new Color(232, 235, 222));
        g2d.setFont(new Font("Consolas", Font.BOLD, 30));
        int width = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, (BASE_WIDTH - width) / 2, 110);
        g2d.setColor(new Color(240, 206, 80, 160));
        g2d.drawLine((BASE_WIDTH - width) / 2, 118, (BASE_WIDTH + width) / 2, 118);
    }

    private void drawMainMenu(Graphics2D g2d) {
        Color accent = new Color(240, 206, 80);
        Color text = new Color(225, 225, 214);
        g2d.setFont(new Font("Consolas", Font.BOLD, 34));
        String top = "GRAVITY WARP";
        String bottom = "TRIALS";
        int topWidth = g2d.getFontMetrics().stringWidth(top);
        int bottomWidth = g2d.getFontMetrics().stringWidth(bottom);
        g2d.setColor(text);
        g2d.drawString(top, (BASE_WIDTH - topWidth) / 2, 120);
        g2d.drawString(bottom, (BASE_WIDTH - bottomWidth) / 2, 156);
        g2d.setColor(new Color(240, 206, 80, 160));
        g2d.drawLine((BASE_WIDTH - bottomWidth) / 2, 164, (BASE_WIDTH + bottomWidth) / 2, 164);

        g2d.setFont(new Font("Consolas", Font.PLAIN, 21));
        String[] options = new String[]{
                "Continue",
                "New Game",
                "Level Select",
                "Multiplayer",
                "Settings",
                "Credits",
                "Quit"
        };
        int startY = 210;
        for (int i = 0; i < options.length; i++) {
            boolean disabled = options[i].equals("Continue") && !SaveGame.exists();
            boolean selected = mainMenuIndex == i;
            g2d.setColor(disabled ? new Color(110, 110, 110) : (selected ? accent : text));
            String prefix = selected ? "> " : "  ";
            String suffix = selected ? " <" : "";
            String label = prefix + options[i] + suffix;
            int width = g2d.getFontMetrics().stringWidth(label);
            g2d.drawString(label, (BASE_WIDTH - width) / 2, startY + i * 34);
        }

        g2d.setFont(new Font("Consolas", Font.PLAIN, 14));
        drawControlHint(g2d, "[Arrows/Enter to Navigate]");
    }

    private void drawMultiplayerMenu(Graphics2D g2d) {
        g2d.setFont(new Font("Consolas", Font.PLAIN, 18));
        String[] options = new String[]{
                "Host (Direct IP 9484)",
                "Join (Direct IP)",
                "LAN Quick Connect",
                "Back"
        };
        int startY = 210;
        for (int i = 0; i < options.length; i++) {
            g2d.setColor(multiplayerMenuIndex == i ? new Color(190, 255, 180) : new Color(230, 235, 243));
            String text = options[i];
            int width = g2d.getFontMetrics().stringWidth(text);
            g2d.drawString(text, (BASE_WIDTH - width) / 2, startY + i * 32);
        }
        g2d.setColor(new Color(180, 220, 255));
        g2d.drawString("Target IP: " + directIpInput, (BASE_WIDTH - 360) / 2, startY + options.length * 32 + 12);
        drawControlHint(g2d, "Use numbers and dot to edit IP, Enter to select");
    }

    private void drawMultiplayerWait(Graphics2D g2d) {
        g2d.setFont(new Font("Consolas", Font.PLAIN, 18));
        g2d.setColor(new Color(230, 235, 243));
        String status = multiplayerHost ? "Hosting on port 9484..." : "Connecting to " + directIpInput + "...";
        int width = g2d.getFontMetrics().stringWidth(status);
        g2d.drawString(status, (BASE_WIDTH - width) / 2, 240);
        drawControlHint(g2d, "Esc to cancel");
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
        for (Platform platform : platforms) {
            drawPlatformBlock(g2d, platform, new Color(54, 102, 144), new Color(98, 190, 255));
        }
        for (MovingPlatform mover : movers) {
            drawPlatformBlock(g2d, mover, new Color(74, 126, 180), new Color(180, 240, 255));
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
        if (buttons != null) {
            for (CoopButton button : buttons) {
                button.draw(g2d);
            }
        }
        if (doors != null) {
            for (CoopDoor door : doors) {
                door.draw(g2d);
            }
        }
        player.draw(g2d, gravityDir);
        if (multiplayerActive) {
            partner.draw(g2d, partnerGravity, new Color(180, 220, 255), new Color(120, 180, 255));
        }
    }

    private void drawPlatformBlock(Graphics2D g2d, Platform platform, Color base, Color highlight) {
        int x = (int) platform.getX();
        int y = (int) platform.getY();
        int w = platform.getWidth();
        int h = platform.getHeight();

        Color shadow = new Color(0, 0, 0, 80);
        g2d.setColor(shadow);
        g2d.fillRoundRect(x + 4, y + 4, w, h, 10, 10);

        GradientPaint paint = new GradientPaint(x, y, highlight, x, y + h, base);
        g2d.setPaint(paint);
        g2d.fillRoundRect(x, y, w, h, 10, 10);

        g2d.setColor(new Color(255, 255, 255, 40));
        g2d.fillRoundRect(x + 3, y + 3, w - 6, Math.max(6, h / 4), 8, 8);

        java.awt.Stroke old = g2d.getStroke();
        g2d.setStroke(new BasicStroke(2f));
        g2d.setColor(new Color(14, 22, 32, 180));
        g2d.drawRoundRect(x, y, w, h, 10, 10);
        g2d.setStroke(old);
        g2d.setColor(new Color(255, 255, 255, 30));
        g2d.drawRoundRect(x + 1, y + 1, w - 2, h - 2, 10, 10);
    }

    private void drawHud(Graphics2D g2d) {
        long collected = orbs.stream().filter(FluxOrb::isCollected).count();

        Color panelBg = new Color(10, 16, 26, 180);
        Color panelBorder = new Color(120, 200, 255, 150);
        g2d.setColor(panelBg);
        g2d.fillRoundRect(10, 10, 270, 175, 14, 14);
        g2d.setColor(panelBorder);
        g2d.drawRoundRect(10, 10, 270, 175, 14, 14);

        g2d.setColor(new Color(226, 240, 255));
        g2d.setFont(new Font("Consolas", Font.BOLD, 18));
        g2d.drawString("Mission Data", 24, 36);

        g2d.setFont(new Font("Consolas", Font.PLAIN, 16));
        g2d.setColor(new Color(200, 220, 235));
        g2d.drawString("Orbs: " + collected + "/" + orbs.size(), 24, 62);
        g2d.drawString("Gate: " + (exitGate.isUnlocked() ? "Unlocked" : "Locked"), 24, 84);
        g2d.drawString("Gravity: " + gravityDir.name(), 24, 106);
        g2d.drawString("Time: " + String.format("%.1fs", objectiveManager.getElapsedTime()), 24, 128);
        g2d.drawString("Par: " + String.format("%.1fs", objectiveManager.getParTimeSeconds()), 24, 150);
        g2d.drawString("Deaths: " + deathCount, 24, 172);

        int cooldownX = BASE_WIDTH - 230;
        int cooldownY = 16;
        int cooldownWidth = 210;
        int cooldownHeight = 92;
        g2d.setColor(panelBg);
        g2d.fillRoundRect(cooldownX, cooldownY, cooldownWidth, cooldownHeight, 14, 14);
        g2d.setColor(panelBorder);
        g2d.drawRoundRect(cooldownX, cooldownY, cooldownWidth, cooldownHeight, 14, 14);

        double readiness = 1.0 - Math.min(1.0, gravityCooldownRemaining / GRAVITY_COOLDOWN);
        int barX = cooldownX + 16;
        int barY = cooldownY + 48;
        int barW = cooldownWidth - 32;
        int barH = 18;
        g2d.setColor(new Color(22, 34, 52, 200));
        g2d.fillRoundRect(barX, barY, barW, barH, 10, 10);

        int filled = (int) (barW * readiness);
        GradientPaint barPaint = new GradientPaint(barX, barY, new Color(120, 236, 255), barX + filled, barY + barH, new Color(0, 180, 200));
        g2d.setPaint(barPaint);
        g2d.fillRoundRect(barX, barY, filled, barH, 10, 10);
        g2d.setColor(new Color(170, 220, 255));
        g2d.drawRoundRect(barX, barY, barW, barH, 10, 10);

        g2d.setFont(new Font("Consolas", Font.BOLD, 15));
        String cooldownText = gravityCooldownRemaining <= 0 ? "Gravity Core Ready" : String.format("Recharging: %.1fs", gravityCooldownRemaining);
        g2d.drawString(cooldownText, barX, barY - 12);

        g2d.setColor(new Color(160, 190, 210));
        g2d.setFont(new Font("Consolas", Font.PLAIN, 14));
        g2d.drawString("Controls: A/D move • Space jump • I/J/K/L rotate", 18, BASE_HEIGHT - 28);
        if (settings.isShowDebugHud()) {
            g2d.drawString("Position: (" + (int) player.getX() + ", " + (int) player.getY() + ")", 18, BASE_HEIGHT - 50);
            g2d.drawString("Velocity: (" + String.format("%.2f", player.getVelX()) + ", " + String.format("%.2f", player.getVelY()) + ")", 18, BASE_HEIGHT - 70);
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
        g2d.setColor(new Color(190, 180, 150));
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
            handleMenuNavigation(e, 7, () -> handleMainMenuSelect(mainMenuIndex));
            return;
        }
        if (gameState == GameState.MULTIPLAYER_MENU) {
            handleMenuNavigation(e, 4, () -> handleMultiplayerSelect(multiplayerMenuIndex));
            return;
        }
        if (gameState == GameState.MULTIPLAYER_WAIT) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                closeSession();
                gameState = GameState.MULTIPLAYER_MENU;
            }
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

        if (e.getKeyCode() == KeyEvent.VK_I) {
            changeGravity(GravityDir.UP);
        }
        if (e.getKeyCode() == KeyEvent.VK_J) {
            changeGravity(GravityDir.LEFT);
        }
        if (e.getKeyCode() == KeyEvent.VK_K) {
            changeGravity(GravityDir.DOWN);
        }
        if (e.getKeyCode() == KeyEvent.VK_L) {
            changeGravity(GravityDir.RIGHT);
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
                case MULTIPLAYER_MENU:
                    multiplayerMenuIndex = (multiplayerMenuIndex - 1 + itemCount) % itemCount;
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
                case MULTIPLAYER_MENU:
                    multiplayerMenuIndex = (multiplayerMenuIndex + 1) % itemCount;
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
            case 3: // Multiplayer
                gameState = GameState.MULTIPLAYER_MENU;
                multiplayerMenuIndex = 0;
                break;
            case 4: // Settings
                previousStateBeforeSettings = GameState.MAIN_MENU;
                gameState = GameState.SETTINGS;
                settingsMenuIndex = 0;
                break;
            case 5: // Credits
                gameState = GameState.CREDITS;
                break;
            case 6: // Quit
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
                closeSession();
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

    private LevelData getCurrentMultiplayerLevel() {
        List<LevelData> coop = levelManager.getMultiplayerLevels();
        if (coop.isEmpty()) {
            return levelManager.getLevel(Math.max(0, levelManager.getLevelCount() - 1));
        }
        multiplayerLevelIndex = Math.max(0, Math.min(multiplayerLevelIndex, coop.size() - 1));
        return coop.get(multiplayerLevelIndex);
    }

    private void startHostingSession() {
        closeSession();
        multiplayerHost = true;
        multiplayerActive = true;
        gameState = GameState.MULTIPLAYER_WAIT;
        new Thread(() -> {
            try {
                MultiplayerSession.advertiseLanHost();
                session = MultiplayerSession.host();
                LevelData level = getCurrentMultiplayerLevel();
                int idx = levelManager.indexOf(level);
                saveData.currentLevelIndex = idx;
                SaveGame.save(saveData);
                session.sendLevelIndex(idx);
                loadLevel(idx);
                gameState = GameState.IN_GAME;
            } catch (IOException ex) {
                System.err.println("Multiplayer host failed: " + ex.getMessage());
                gameState = GameState.MULTIPLAYER_MENU;
            }
        }).start();
    }

    private void startClientSession(String ip) {
        closeSession();
        multiplayerHost = false;
        multiplayerActive = true;
        waitingForLevelSync = true;
        gameState = GameState.MULTIPLAYER_WAIT;
        new Thread(() -> {
            try {
                session = MultiplayerSession.join(ip);
            } catch (IOException ex) {
                System.err.println("Multiplayer join failed: " + ex.getMessage());
                gameState = GameState.MULTIPLAYER_MENU;
            }
        }).start();
    }

    private void closeSession() {
        if (session != null) {
            session.close();
            session = null;
        }
        multiplayerActive = false;
    }

    private void handleMultiplayerSelect(int index) {
        switch (index) {
            case 0:
                startHostingSession();
                break;
            case 1:
                startClientSession(directIpInput);
                break;
            case 2:
                Optional<String> lanIp = MultiplayerSession.discoverLanHost();
                lanIp.ifPresent(ip -> directIpInput = ip);
                startClientSession(directIpInput);
                break;
            case 3:
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
        if (gameState == GameState.MULTIPLAYER_MENU) {
            char c = e.getKeyChar();
            if ((c >= '0' && c <= '9') || c == '.') {
                directIpInput += c;
            } else if (c == '\b' && !directIpInput.isEmpty()) {
                directIpInput = directIpInput.substring(0, directIpInput.length() - 1);
            }
        }
    }
}
