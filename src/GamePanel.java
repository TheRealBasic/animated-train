import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;

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
    private final Random vhsNoise = new Random();
    private double deathEffectTimer;
    private double screenShakeTimer;
    private double screenShakeStrength;
    private BufferedImage sceneBuffer;
    private BufferedImage distortionBuffer;
    private BufferedImage smearBuffer;
    private BufferedImage tintBufferCool;
    private BufferedImage tintBufferWarm;
    private final List<Platform> platformScratch = new ArrayList<>();
    private final List<Platform> doorPlatforms = new ArrayList<>();
    private boolean shiftPressed;
    private double fpsTimer;
    private int fpsFrames;
    private double fpsDisplay;
    private String toastMessage = "";
    private double toastTimer;
    private Color toastColor = new Color(214, 210, 196);

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
        SoundManager.setMasterVolume(settings.getMasterVolume() / 100.0);
        scale = settings.getScreenScale();
        directIpInput = settings.getLastDirectIp();
        setPreferredSize(new Dimension((int) (BASE_WIDTH * scale), (int) (BASE_HEIGHT * scale)));
        setBackground(new Color(8, 8, 14));
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
        doorPlatforms.clear();
        for (CoopDoor door : doors) {
            var bounds = door.getBounds();
            doorPlatforms.add(new Platform(bounds.x, bounds.y, (int) bounds.width, (int) bounds.height));
        }
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
        fpsFrames++;
        fpsTimer += dt;
        if (fpsTimer >= 1.0) {
            fpsDisplay = fpsFrames / fpsTimer;
            fpsFrames = 0;
            fpsTimer = 0;
        }
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

        updateEffects(dt);

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
        double moveSpeed = shiftPressed ? 1.35 : 0.9;
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
            SoundManager.playTone(880, 120, 0.4);
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
            boolean alreadyCollected = ((localOrbMask | remoteOrbMask) & (1L << i)) != 0;
            if (orb.checkCollected(target) && !alreadyCollected) {
                if (localPlayer) {
                    localOrbMask |= (1L << i);
                    SoundManager.playTone(1320, 160, 0.55);
                    setToast("Flux orb secured!", new Color(154, 248, 196));
                    screenShakeTimer = 0.3;
                    screenShakeStrength = 3.5;
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
        if (remote.levelIndex() != null) {
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
                setToast("Checkpoint reached", new Color(206, 166, 248));
            }
        }
    }

    private void respawn() {
        deathCount++;
        player.setPosition(respawnPosition.x, respawnPosition.y);
        player.resetVelocity();
        gravityDir = respawnGravity;
        deathEffectTimer = 1.0;
        screenShakeTimer = 0.6;
        screenShakeStrength = 8.0;
        SoundManager.playNoise(220, 0.8);
    }

    private void updateMovingPlatforms(double dt) {
        for (MovingPlatform mover : movers) {
            mover.update(dt);
        }
    }

    private List<Platform> getAllPlatforms() {
        platformScratch.clear();
        platformScratch.addAll(platforms);
        platformScratch.addAll(movers);
        for (int i = 0; i < doors.size(); i++) {
            CoopDoor door = doors.get(i);
            if (!door.isOpen() && i < doorPlatforms.size()) {
                platformScratch.add(doorPlatforms.get(i));
            }
        }
        return platformScratch;
    }

    private void updateGravityCooldown(double dt) {
        if (gravityCooldownRemaining > 0) {
            gravityCooldownRemaining = Math.max(0, gravityCooldownRemaining - dt);
        }
    }

    private void updateEffects(double dt) {
        if (deathEffectTimer > 0) {
            deathEffectTimer = Math.max(0, deathEffectTimer - dt);
        }
        if (screenShakeTimer > 0) {
            screenShakeTimer = Math.max(0, screenShakeTimer - dt);
        }
        if (toastTimer > 0) {
            toastTimer = Math.max(0, toastTimer - dt);
            if (toastTimer == 0) {
                toastMessage = "";
            }
        }
    }

    private void setToast(String message, Color color) {
        toastMessage = message;
        toastColor = color;
        toastTimer = 2.2;
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
        ensureBuffers(BASE_WIDTH, BASE_HEIGHT);
        Graphics2D sceneG = sceneBuffer.createGraphics();
        clearImage(sceneBuffer, sceneG);
        sceneG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        renderScene(sceneG);
        sceneG.dispose();

        BufferedImage processed = settings.isReducedEffects() ? sceneBuffer : applyScreenEffects(sceneBuffer);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        double renderScale = Math.min(getWidth() / (double) BASE_WIDTH, getHeight() / (double) BASE_HEIGHT);
        double offsetX = (getWidth() - BASE_WIDTH * renderScale) / 2.0;
        double offsetY = (getHeight() - BASE_HEIGHT * renderScale) / 2.0;
        AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(offsetX, offsetY);
        g2d.scale(renderScale, renderScale);
        g2d.drawImage(processed, 0, 0, null);
        if (!settings.isReducedEffects()) {
            drawCrtBezel(g2d);
        }
        g2d.setTransform(oldTransform);
    }

    private void ensureBuffers(int width, int height) {
        if (sceneBuffer == null || sceneBuffer.getWidth() != width || sceneBuffer.getHeight() != height) {
            sceneBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            distortionBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            smearBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            tintBufferCool = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            tintBufferWarm = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
    }

    private void clearImage(BufferedImage image, Graphics2D g2d) {
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2d.setComposite(AlphaComposite.SrcOver);
    }

    private void renderScene(Graphics2D g2d) {
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
    }

    private BufferedImage applyScreenEffects(BufferedImage source) {
        if (settings.isReducedEffects()) {
            return source;
        }
        int width = source.getWidth();
        int height = source.getHeight();
        ensureBuffers(width, height);
        BufferedImage distorted = distortionBuffer;
        int[] src = ((DataBufferInt) source.getRaster().getDataBuffer()).getData();
        int[] dst = ((DataBufferInt) distorted.getRaster().getDataBuffer()).getData();

        double cx = width / 2.0;
        double cy = height / 2.0;
        double fishEyeStrength = 0.18;
        double aberration = 0.6 + 3.2 * Math.min(1.0, deathEffectTimer);
        double wobble = 0.35 * Math.sin(System.nanoTime() / 1_000_000_000.0 * 4.0);
        aberration += wobble;

        double shakeDuration = 0.6;
        double shakeScale = screenShakeTimer > 0 ? screenShakeStrength * (screenShakeTimer / shakeDuration) : 0.0;
        double shakeX = (vhsNoise.nextDouble() * 2 - 1) * shakeScale;
        double shakeY = (vhsNoise.nextDouble() * 2 - 1) * shakeScale;

        for (int y = 0; y < height; y++) {
            double dy = (y - cy - shakeY) / cy;
            for (int x = 0; x < width; x++) {
                double dx = (x - cx - shakeX) / cx;
                double r = Math.sqrt(dx * dx + dy * dy);
                double distort = 1 + fishEyeStrength * r * r;
                double sampleX = cx + dx * distort * cx + shakeX;
                double sampleY = cy + dy * distort * cy + shakeY;

                int baseX = clampToInt(Math.round(sampleX), 0, width - 1);
                int baseY = clampToInt(Math.round(sampleY), 0, height - 1);
                int baseRgb = src[baseY * width + baseX];
                int alpha = (baseRgb >>> 24) & 0xFF;

                int rSample = sampleChannel(src, sampleX + aberration, sampleY - aberration, width, height, 16);
                int gSample = sampleChannel(src, sampleX, sampleY, width, height, 8);
                int bSample = sampleChannel(src, sampleX - aberration, sampleY + aberration, width, height, 0);

                int rgb = (alpha << 24) | (rSample << 16) | (gSample << 8) | bSample;
                dst[y * width + x] = rgb;
            }
        }

        BufferedImage smeared = addColorSmear(distorted);

        Graphics2D overlay = smeared.createGraphics();
        overlay.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawCrtOverlay(overlay);
        overlay.dispose();
        return smeared;
    }

    private BufferedImage addColorSmear(BufferedImage source) {
        BufferedImage smeared = smearBuffer;
        Graphics2D g2d = smeared.createGraphics();
        clearImage(smeared, g2d);
        g2d.drawImage(source, 0, 0, null);

        BufferedImage coolShift = tintImage(source, new Color(126, 86, 196, 120), tintBufferCool);
        BufferedImage warmShift = tintImage(source, new Color(196, 122, 74, 120), tintBufferWarm);

        g2d.setComposite(java.awt.AlphaComposite.SrcOver.derive(0.35f));
        g2d.drawImage(coolShift, 2, 0, null);
        g2d.setComposite(java.awt.AlphaComposite.SrcOver.derive(0.3f));
        g2d.drawImage(warmShift, -2, 1, null);
        g2d.dispose();
        return smeared;
    }

    private BufferedImage tintImage(BufferedImage source, Color tint, BufferedImage target) {
        Graphics2D g2d = target.createGraphics();
        clearImage(target, g2d);
        g2d.drawImage(source, 0, 0, null);
        g2d.setComposite(java.awt.AlphaComposite.SrcAtop);
        g2d.setColor(tint);
        g2d.fillRect(0, 0, source.getWidth(), source.getHeight());
        g2d.dispose();
        return target;
    }

    private int sampleChannel(int[] src, double sx, double sy, int width, int height, int shift) {
        int x = clampToInt(Math.round(sx), 0, width - 1);
        int y = clampToInt(Math.round(sy), 0, height - 1);
        return (src[y * width + x] >> shift) & 0xFF;
    }

    private int clampToInt(long value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return (int) value;
    }

    private void drawBackground(Graphics2D g2d) {
        GradientPaint topGlow = new GradientPaint(0, 0, new Color(14, 10, 24), 0, BASE_HEIGHT, new Color(6, 4, 14));
        g2d.setPaint(topGlow);
        g2d.fillRect(0, 0, BASE_WIDTH, BASE_HEIGHT);

        int cx = BASE_WIDTH / 2;
        int cy = BASE_HEIGHT / 2;
        for (int i = 0; i < 6; i++) {
            int radius = 320 + i * 40;
            Color halo = new Color(132, 60, 150, Math.max(0, 88 - i * 12));
            g2d.setColor(halo);
            g2d.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
        }

        g2d.setColor(new Color(48, 30, 78, 32));
        for (int y = 20; y < BASE_HEIGHT; y += 30) {
            g2d.fillRect(-6, y, BASE_WIDTH + 12, 4);
        }

        g2d.setColor(new Color(132, 92, 186, 26));
        for (int i = 0; i < BASE_WIDTH; i += 140) {
            g2d.drawLine(i, 0, i + 80, BASE_HEIGHT);
        }

        GradientPaint vignette = new GradientPaint(0, 0, new Color(0, 0, 0, 0), 0, BASE_HEIGHT, new Color(0, 0, 0, 190));
        g2d.setPaint(vignette);
        g2d.fillRect(0, 0, BASE_WIDTH, BASE_HEIGHT);
    }

    private void drawCrtOverlay(Graphics2D g2d) {
        AffineTransform oldTransform = g2d.getTransform();
        double t = System.nanoTime() / 1_000_000_000.0;
        int jitter = (int) (Math.sin(t * 7.3) * 2);
        g2d.translate(jitter, 0);

        int vignetteAlpha = (int) (48 + 18 * (1 + Math.sin(t * 0.7)) / 2);
        GradientPaint vignette = new GradientPaint(0, 0, new Color(10, 6, 16, vignetteAlpha), BASE_WIDTH, BASE_HEIGHT, new Color(4, 2, 10, vignetteAlpha + 28));
        g2d.setPaint(vignette);
        g2d.fillRect(0, 0, BASE_WIDTH, BASE_HEIGHT);

        int scanAlpha = (int) (12 + 5 * Math.sin(t * 12.0));
        g2d.setColor(new Color(220, 214, 196, scanAlpha));
        for (int y = 0; y < BASE_HEIGHT; y += 3) {
            int wobble = (int) (Math.sin((t * 0.8) + y * 0.03) * 2);
            g2d.drawLine(0, y + wobble, BASE_WIDTH, y + wobble);
        }

        g2d.setColor(new Color(146, 84, 186, 40));
        g2d.drawRoundRect(-4, -4, BASE_WIDTH + 8, BASE_HEIGHT + 8, 16, 16);
        g2d.setColor(new Color(184, 124, 74, 28));
        g2d.drawRoundRect(6, 6, BASE_WIDTH - 12, BASE_HEIGHT - 12, 20, 20);

        g2d.setColor(new Color(214, 202, 188, 12));
        for (int i = 0; i < 90; i++) {
            int x = vhsNoise.nextInt(BASE_WIDTH);
            int y = (int) ((vhsNoise.nextInt(BASE_HEIGHT) + t * 60) % BASE_HEIGHT);
            int w = 1 + vhsNoise.nextInt(2);
            int h = 1 + vhsNoise.nextInt(2);
            g2d.fillRect(x, y, w, h);
        }

        g2d.setColor(new Color(104, 214, 178, 28));
        int bandY = (int) ((t * 80) % BASE_HEIGHT);
        g2d.fillRect(0, bandY, BASE_WIDTH, 5);
        g2d.fillRect(0, (bandY + BASE_HEIGHT / 2) % BASE_HEIGHT, BASE_WIDTH, 5);

        g2d.setTransform(oldTransform);
    }

    private void drawCrtBezel(Graphics2D g2d) {
        int outerPadding = 18;
        int frameRadius = 30;
        Color frameOuter = new Color(18, 10, 22, 240);
        Color frameInner = new Color(52, 32, 64, 230);
        Color frameHighlight = new Color(200, 126, 216, 170);

        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(outerPadding * 1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(frameOuter);
        g2d.drawRoundRect(-outerPadding, -outerPadding, BASE_WIDTH + outerPadding * 2, BASE_HEIGHT + outerPadding * 2, frameRadius, frameRadius);

        g2d.setStroke(new BasicStroke(outerPadding * 0.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(frameInner);
        g2d.drawRoundRect(6 - outerPadding, 6 - outerPadding, BASE_WIDTH + (outerPadding - 6) * 2, BASE_HEIGHT + (outerPadding - 6) * 2, frameRadius - 4, frameRadius - 4);

        g2d.setStroke(new BasicStroke(6f));
        g2d.setColor(new Color(16, 12, 26, 190));
        g2d.drawRoundRect(12, 12, BASE_WIDTH - 24, BASE_HEIGHT - 24, 18, 18);
        g2d.setColor(new Color(110, 68, 142, 120));
        g2d.drawRoundRect(18, 18, BASE_WIDTH - 36, BASE_HEIGHT - 36, 14, 14);

        int controlHeight = 60;
        int controlY = BASE_HEIGHT - controlHeight - 10;
        g2d.setStroke(new BasicStroke(3.2f));
        g2d.setColor(new Color(18, 12, 26, 220));
        g2d.fillRoundRect(16, controlY, BASE_WIDTH - 32, controlHeight, 18, 18);
        g2d.setColor(new Color(78, 42, 88, 200));
        g2d.drawRoundRect(16, controlY, BASE_WIDTH - 32, controlHeight, 18, 18);
        g2d.setColor(new Color(116, 78, 182, 170));
        g2d.drawRoundRect(26, controlY + 10, BASE_WIDTH - 52, controlHeight - 20, 14, 14);

        g2d.setColor(new Color(154, 118, 176, 190));
        for (int i = 0; i < 6; i++) {
            int holeX = 34 + i * 22;
            for (int y = 0; y < 2; y++) {
                g2d.fillRoundRect(holeX, controlY + 16 + y * 12, 6, 6, 2, 2);
            }
        }

        g2d.setColor(new Color(180, 84, 102));
        g2d.fillOval(BASE_WIDTH - 130, controlY + 16, 18, 18);
        g2d.setColor(new Color(104, 214, 178));
        g2d.fillOval(BASE_WIDTH - 102, controlY + 16, 18, 18);
        g2d.setColor(new Color(198, 150, 72));
        g2d.fillOval(BASE_WIDTH - 74, controlY + 16, 18, 18);

        g2d.setColor(frameHighlight);
        g2d.setStroke(new BasicStroke(2.4f));
        g2d.drawRoundRect(6, 6, BASE_WIDTH - 12, BASE_HEIGHT - 12, frameRadius - 10, frameRadius - 10);
        g2d.setColor(new Color(182, 66, 86));
        g2d.fillRoundRect(BASE_WIDTH - 102, controlY + 10, 24, 24, 6, 6);
        g2d.setColor(new Color(206, 158, 82));
        g2d.fillRoundRect(BASE_WIDTH - 64, controlY + 10, 24, 24, 6, 6);

        g2d.setFont(new Font("Consolas", Font.BOLD, 14));
        g2d.setColor(new Color(214, 206, 192));
        g2d.drawString("CRT MODE", 32, controlY + controlHeight - 10);
        g2d.setStroke(oldStroke);
    }

    private void drawTitle(Graphics2D g2d, String text) {
        g2d.setColor(new Color(214, 206, 192));
        g2d.setFont(new Font("Consolas", Font.BOLD, 30));
        int width = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, (BASE_WIDTH - width) / 2, 110);
        g2d.setColor(new Color(186, 106, 188, 150));
        g2d.drawLine((BASE_WIDTH - width) / 2, 118, (BASE_WIDTH + width) / 2, 118);
    }

    private void drawMainMenu(Graphics2D g2d) {
        Color accent = new Color(198, 112, 230);
        Color text = new Color(218, 208, 196);
        g2d.setFont(new Font("Consolas", Font.BOLD, 34));
        String top = "GRAVITY WARP";
        String bottom = "TRIALS";
        int topWidth = g2d.getFontMetrics().stringWidth(top);
        int bottomWidth = g2d.getFontMetrics().stringWidth(bottom);
        g2d.setColor(text);
        g2d.drawString(top, (BASE_WIDTH - topWidth) / 2, 120);
        g2d.drawString(bottom, (BASE_WIDTH - bottomWidth) / 2, 156);
        g2d.setColor(new Color(198, 112, 230, 120));
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
            g2d.setColor(disabled ? new Color(90, 84, 92) : (selected ? accent : text));
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
            g2d.setColor(multiplayerMenuIndex == i ? new Color(198, 112, 230) : new Color(218, 208, 196));
            String text = options[i];
            int width = g2d.getFontMetrics().stringWidth(text);
            g2d.drawString(text, (BASE_WIDTH - width) / 2, startY + i * 32);
        }
        g2d.setColor(new Color(162, 202, 186));
        g2d.drawString("Target IP: " + directIpInput, (BASE_WIDTH - 360) / 2, startY + options.length * 32 + 12);
        drawControlHint(g2d, "Use numbers and dot to edit IP, Enter to select");
    }

    private void drawMultiplayerWait(Graphics2D g2d) {
        g2d.setFont(new Font("Consolas", Font.PLAIN, 18));
        g2d.setColor(new Color(218, 208, 196));
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
        lines.add("Show FPS Counter: " + (settings.isShowFps() ? "On" : "Off"));
        lines.add("Reduced Effects: " + (settings.isReducedEffects() ? "On" : "Off"));
        lines.add("Rebind Left: " + KeyEvent.getKeyText(settings.getKeyLeft()));
        lines.add("Rebind Right: " + KeyEvent.getKeyText(settings.getKeyRight()));
        lines.add("Rebind Jump: " + KeyEvent.getKeyText(settings.getKeyJump()));
        lines.add("Back");

        int startY = 200;
        for (int i = 0; i < lines.size(); i++) {
            g2d.setColor(settingsMenuIndex == i ? new Color(198, 112, 230) : new Color(218, 208, 196));
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
            } else {
                double best = saveData.bestTimes[i];
                String medal = saveData.bestMedals[i];
                if (best > 0 || (medal != null && !medal.isEmpty())) {
                    label += " • Best: " + (best > 0 ? String.format("%.1fs", best) : "--");
                    if (medal != null && !medal.isEmpty()) {
                        label += " (" + medal + ")";
                    }
                }
            }
            g2d.setColor(levelSelectIndex == i ? new Color(198, 112, 230) : new Color(218, 208, 196));
        if (locked) {
                g2d.setColor(new Color(98, 86, 108));
            }
            int width = g2d.getFontMetrics().stringWidth(label);
            g2d.drawString(label, (BASE_WIDTH - width) / 2, startY + i * 26);
        }
        drawControlHint(g2d, "Enter to play, Esc to back");
    }

    private void drawWorld(Graphics2D g2d) {
        for (Platform platform : platforms) {
            drawPlatformBlock(g2d, platform, new Color(42, 28, 60), new Color(98, 62, 124));
        }
        for (MovingPlatform mover : movers) {
            drawPlatformBlock(g2d, mover, new Color(64, 36, 78), new Color(132, 82, 154));
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
            partner.draw(g2d, partnerGravity, new Color(120, 214, 172), new Color(86, 160, 138));
        }
    }

    private void drawPlatformBlock(Graphics2D g2d, Platform platform, Color base, Color highlight) {
        int x = (int) platform.getX();
        int y = (int) platform.getY();
        int w = platform.getWidth();
        int h = platform.getHeight();

        g2d.setColor(new Color(8, 6, 16, 170));
        g2d.fillRect(x + 3, y + 3, w, h);

        g2d.setColor(base);
        g2d.fillRect(x, y, w, h);
        g2d.setColor(highlight);
        g2d.fillRect(x, y, w, 6);
        g2d.fillRect(x, y + h - 8, w, 6);

        g2d.setColor(new Color(180, 168, 200, 26));
        for (int px = x + 2; px < x + w - 2; px += 6) {
            for (int py = y + 2; py < y + h - 2; py += 6) {
                if (((px + py) / 6) % 2 == 0) {
                    g2d.fillRect(px, py, 3, 3);
                }
            }
        }

        g2d.setColor(new Color(18, 10, 24));
        g2d.drawRect(x, y, w, h);
    }

    private void drawHud(Graphics2D g2d) {
        long collected = orbs.stream().filter(FluxOrb::isCollected).count();

        Color panelBg = new Color(10, 8, 18, 210);
        Color panelAccent = new Color(126, 66, 156, 180);
        int panelHeight = 240;
        g2d.setColor(panelBg);
        g2d.fillRoundRect(12, 12, 270, panelHeight, 18, 18);
        g2d.setColor(new Color(18, 12, 30, 170));
        g2d.fillRoundRect(18, 24, 258, panelHeight - 24, 14, 14);
        g2d.setColor(panelAccent);
        g2d.drawRoundRect(12, 12, 270, panelHeight, 18, 18);
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRoundRect(18, 24, 258, panelHeight - 24, 14, 14);

        g2d.setColor(new Color(214, 206, 192));
        g2d.setFont(new Font("Consolas", Font.BOLD, 18));
        g2d.drawString("Mission Data", 28, 40);

        g2d.setFont(new Font("Consolas", Font.PLAIN, 16));
        g2d.setColor(new Color(192, 178, 166));
        g2d.drawString("Orbs: " + collected + "/" + orbs.size(), 28, 66);
        g2d.drawString("Gate: " + (exitGate.isUnlocked() ? "Unlocked" : "Locked"), 28, 88);
        g2d.drawString("Gravity: " + gravityDir.name(), 28, 110);
        g2d.drawString("Time: " + String.format("%.1fs", objectiveManager.getElapsedTime()), 28, 132);
        g2d.drawString("Par: " + String.format("%.1fs", objectiveManager.getParTimeSeconds()), 28, 154);
        g2d.drawString("Deaths: " + deathCount, 28, 176);

        double orbProgress = orbs.isEmpty() ? 1.0 : collected / (double) orbs.size();
        drawProgressBar(g2d, 28, 190, 210, 14, orbProgress, new Color(104, 214, 178), "Gate unlock");

        double par = objectiveManager.getParTimeSeconds();
        if (par > 0) {
            double elapsed = objectiveManager.getElapsedTime();
            double parProgress = Math.min(1.0, elapsed / par);
            Color parColor = elapsed <= par ? new Color(182, 210, 110) : new Color(196, 86, 102);
            drawProgressBar(g2d, 28, 214, 210, 14, parProgress, parColor, "Par pace");
        }

        int cooldownX = BASE_WIDTH - 230;
        int cooldownY = 16;
        int cooldownWidth = 210;
        int cooldownHeight = 92;
        g2d.setColor(panelBg);
        g2d.fillRoundRect(cooldownX, cooldownY, cooldownWidth, cooldownHeight, 16, 16);
        g2d.setColor(panelAccent);
        g2d.drawRoundRect(cooldownX, cooldownY, cooldownWidth, cooldownHeight, 16, 16);

        double readiness = 1.0 - Math.min(1.0, gravityCooldownRemaining / GRAVITY_COOLDOWN);
        int barX = cooldownX + 16;
        int barY = cooldownY + 48;
        int barW = cooldownWidth - 32;
        int barH = 18;
        g2d.setColor(new Color(10, 8, 18, 210));
        g2d.fillRoundRect(barX, barY, barW, barH, 10, 10);

        int filled = (int) (barW * readiness);
        GradientPaint barPaint = new GradientPaint(barX, barY, new Color(156, 102, 212), barX + filled, barY + barH, new Color(86, 182, 146));
        g2d.setPaint(barPaint);
        g2d.fillRoundRect(barX, barY, filled, barH, 10, 10);
        g2d.setColor(new Color(186, 170, 204));
        g2d.drawRoundRect(barX, barY, barW, barH, 10, 10);

        g2d.setFont(new Font("Consolas", Font.BOLD, 15));
        String cooldownText = gravityCooldownRemaining <= 0 ? "Gravity Core Ready" : String.format("Recharging: %.1fs", gravityCooldownRemaining);
        g2d.drawString(cooldownText, barX, barY - 12);

        g2d.setFont(new Font("Consolas", Font.PLAIN, 14));
        String controlsText = "Controls: A/D move • Space jump • Shift sprint • R restart • I/J/K/L rotate";
        int bezelOffset = settings.isReducedEffects() ? 12 : 70;
        int controlsY = BASE_HEIGHT - bezelOffset;
        int controlsX = 18;
        int controlsWidth = g2d.getFontMetrics().stringWidth(controlsText);
        int controlsHeight = g2d.getFontMetrics().getHeight();
        int controlsBgY = controlsY - g2d.getFontMetrics().getAscent() - 6;
        int controlsBgHeight = controlsHeight + 12;

        g2d.setColor(new Color(8, 6, 16, 200));
        g2d.fillRoundRect(controlsX - 10, controlsBgY, controlsWidth + 20, controlsBgHeight, 10, 10);
        g2d.setColor(new Color(192, 178, 166));
        g2d.drawString(controlsText, controlsX, controlsY);

        if (settings.isShowDebugHud()) {
            int debugY = controlsBgY - 8;
            g2d.drawString("Position: (" + (int) player.getX() + ", " + (int) player.getY() + ")", controlsX, debugY - 2);
            g2d.drawString("Velocity: (" + String.format("%.2f", player.getVelX()) + ", " + String.format("%.2f", player.getVelY()) + ")", controlsX, debugY - 22);
        }
        if (settings.isShowFps()) {
            String fpsText = String.format("FPS: %.0f", fpsDisplay);
            int fpsX = BASE_WIDTH - g2d.getFontMetrics().stringWidth(fpsText) - 18;
            g2d.drawString(fpsText, fpsX, controlsY);
        }

        if (!toastMessage.isEmpty()) {
            g2d.setColor(new Color(8, 6, 16, 190));
            int width = g2d.getFontMetrics().stringWidth(toastMessage);
            int x = (BASE_WIDTH - width) / 2 - 12;
            int y = 70;
            g2d.fillRoundRect(x, y - 24, width + 24, 40, 12, 12);
            g2d.setColor(toastColor);
            g2d.drawString(toastMessage, (BASE_WIDTH - width) / 2, y);
        }

        drawGravityCompass(g2d);
    }

    private void drawProgressBar(Graphics2D g2d, int x, int y, int width, int height, double progress, Color fill, String label) {
        progress = Math.max(0, Math.min(1.0, progress));
        g2d.setColor(new Color(12, 8, 18, 180));
        g2d.fillRoundRect(x, y, width, height, 8, 8);
        g2d.setColor(fill);
        g2d.fillRoundRect(x, y, (int) (width * progress), height, 8, 8);
        g2d.setColor(new Color(186, 170, 204, 180));
        g2d.drawRoundRect(x, y, width, height, 8, 8);
        g2d.setFont(new Font("Consolas", Font.PLAIN, 12));
        g2d.drawString(label, x, y - 4);
    }

    private void drawGravityCompass(Graphics2D g2d) {
        int size = 70;
        int x = BASE_WIDTH - size - 24;
        int y = 20;
        g2d.setColor(new Color(10, 8, 18, 200));
        g2d.fillOval(x, y, size, size);
        g2d.setColor(new Color(126, 66, 156));
        g2d.drawOval(x, y, size, size);

        int cx = x + size / 2;
        int cy = y + size / 2;
        int arrowLength = 26;
        int dx = 0;
        int dy = 0;
        switch (gravityDir) {
            case UP:
                dy = -arrowLength;
                break;
            case DOWN:
                dy = arrowLength;
                break;
            case LEFT:
                dx = -arrowLength;
                break;
            case RIGHT:
                dx = arrowLength;
                break;
        }
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(cx, cy, cx + dx, cy + dy);
        g2d.fillOval(cx - 4, cy - 4, 8, 8);
        g2d.setStroke(new BasicStroke(1));
        g2d.setFont(new Font("Consolas", Font.PLAIN, 12));
        g2d.drawString("Gravity", x + 8, y + size + 14);
    }

    private void drawPauseMenu(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 140));
        g2d.fillRect(0, 0, BASE_WIDTH, BASE_HEIGHT);
        g2d.setFont(new Font("Consolas", Font.BOLD, 24));
        g2d.setColor(new Color(218, 208, 196));
        g2d.drawString("Paused", BASE_WIDTH / 2 - 40, 160);

        g2d.setFont(new Font("Consolas", Font.PLAIN, 18));
        String[] options = new String[]{"Resume", "Restart Level", "Settings", "Save & Quit"};
        int startY = 210;
        for (int i = 0; i < options.length; i++) {
            g2d.setColor(pauseMenuIndex == i ? new Color(198, 112, 230) : new Color(218, 208, 196));
            int width = g2d.getFontMetrics().stringWidth(options[i]);
            g2d.drawString(options[i], (BASE_WIDTH - width) / 2, startY + i * 32);
        }
    }

    private void drawLevelComplete(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 160));
        g2d.fillRect(0, 0, BASE_WIDTH, BASE_HEIGHT);
        g2d.setFont(new Font("Consolas", Font.BOLD, 24));
        g2d.setColor(new Color(218, 208, 196));
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
            g2d.setColor(levelCompleteIndex == i ? new Color(198, 112, 230) : new Color(218, 208, 196));
            int width = g2d.getFontMetrics().stringWidth(options[i]);
            g2d.drawString(options[i], (BASE_WIDTH - width) / 2, startY + i * 32);
        }
    }

    private void drawControlHint(Graphics2D g2d, String hint) {
        g2d.setColor(new Color(186, 150, 118));
        int width = g2d.getFontMetrics().stringWidth(hint);
        g2d.drawString(hint, (BASE_WIDTH - width) / 2, BASE_HEIGHT - 40);
    }

    private void drawCredits(Graphics2D g2d) {
        drawTitle(g2d, "Gravity Warp Trials");
        g2d.setFont(new Font("Consolas", Font.PLAIN, 18));
        g2d.setColor(new Color(218, 208, 196));
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
            handleMenuNavigation(e, 9, this::handleSettingsSelect);
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

        if (e.getKeyCode() == KeyEvent.VK_R) {
            loadLevel(saveData.currentLevelIndex);
            gameState = GameState.IN_GAME;
            setToast("Level restarted", new Color(210, 186, 236));
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
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            shiftPressed = true;
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
                settings.setLastDirectIp(directIpInput);
                settings.save();
                startClientSession(directIpInput);
                break;
            case 2:
                Optional<String> lanIp = MultiplayerSession.discoverLanHost();
                lanIp.ifPresent(ip -> directIpInput = ip);
                settings.setLastDirectIp(directIpInput);
                settings.save();
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
                SoundManager.setMasterVolume(settings.getMasterVolume() / 100.0);
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
                settings.setShowFps(!settings.isShowFps());
                settings.save();
                break;
            case 4:
                settings.setReducedEffects(!settings.isReducedEffects());
                settings.save();
                break;
            case 5:
                waitingForBinding = true;
                bindingTarget = "Left";
                break;
            case 6:
                waitingForBinding = true;
                bindingTarget = "Right";
                break;
            case 7:
                waitingForBinding = true;
                bindingTarget = "Jump";
                break;
            case 8:
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
        if (settingsMenuIndex == 8) {
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
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            shiftPressed = false;
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
