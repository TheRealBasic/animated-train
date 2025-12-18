import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.contacts.ContactEdge;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;

import java.nio.ShortBuffer;

import static org.lwjgl.system.MemoryUtil.NULL;

public class LwjglGame {
    private long window;
    private long audioDevice;
    private long audioContext;
    private int jumpBuffer;
    private int jumpSource;

    private final World physicsWorld = new World(new Vec2(0.0f, -12.0f));
    private Body playerBody;
    private boolean onGround;
    private float moveDirection;
    private final Vector2f playerPosition = new Vector2f();

    public static void main(String[] args) {
        new LwjglGame().run();
    }

    private void run() {
        initWindow();
        initAudio();
        initPhysics();
        loop();
        cleanup();
    }

    private void initWindow() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
        window = GLFW.glfwCreateWindow(960, 540, "LWJGL + JOML + OpenAL + jbox2d", NULL, NULL);
        if (window == NULL) {
            throw new IllegalStateException("Failed to create window");
        }

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GL.createCapabilities();
    }

    private void initAudio() {
        String defaultDevice = ALC10.alcGetString(0, ALC10.ALC_DEFAULT_DEVICE_SPECIFIER);
        audioDevice = ALC10.alcOpenDevice(defaultDevice);
        if (audioDevice == NULL) {
            throw new IllegalStateException("Failed to open default audio device");
        }
        audioContext = ALC10.alcCreateContext(audioDevice, (int[]) null);
        ALC10.alcMakeContextCurrent(audioContext);
        AL.createCapabilities(ALC.createCapabilities(audioDevice));

        jumpBuffer = AL10.alGenBuffers();
        jumpSource = AL10.alGenSources();
        AL10.alSourcei(jumpSource, AL10.AL_BUFFER, jumpBuffer);
        AL10.alSourcef(jumpSource, AL10.AL_GAIN, 0.5f);

        // Generate a short sine wave for feedback when jumping.
        int sampleRate = 44100;
        float frequency = 880f;
        short[] samples = new short[(int) (sampleRate * 0.1f)];
        for (int i = 0; i < samples.length; i++) {
            double t = i / (double) sampleRate;
            samples[i] = (short) (Math.sin(2 * Math.PI * frequency * t) * Short.MAX_VALUE);
        }
        ShortBuffer buffer = ShortBuffer.wrap(samples);
        AL10.alBufferData(jumpBuffer, AL10.AL_FORMAT_MONO16, buffer, sampleRate);
    }

    private void initPhysics() {
        // Ground plane
        BodyDef groundDef = new BodyDef();
        groundDef.position.set(0, -4);
        Body ground = physicsWorld.createBody(groundDef);
        PolygonShape groundShape = new PolygonShape();
        groundShape.setAsBox(30f, 1f);
        ground.createFixture(groundShape, 0f);

        // Player body
        BodyDef playerDef = new BodyDef();
        playerDef.type = BodyType.DYNAMIC;
        playerDef.position.set(0, 2);
        playerDef.fixedRotation = true;
        playerBody = physicsWorld.createBody(playerDef);
        PolygonShape playerShape = new PolygonShape();
        playerShape.setAsBox(0.5f, 1f);
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = playerShape;
        fixtureDef.density = 2.5f;
        fixtureDef.friction = 0.8f;
        playerBody.createFixture(fixtureDef);
    }

    private void loop() {
        long lastTime = System.nanoTime();
        while (!GLFW.glfwWindowShouldClose(window)) {
            long now = System.nanoTime();
            float delta = (now - lastTime) / 1_000_000_000f;
            lastTime = now;

            pollInput();
            stepPhysics(delta);
            render();

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    private void pollInput() {
        moveDirection = 0;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS) {
            moveDirection -= 1f;
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS) {
            moveDirection += 1f;
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS && onGround) {
            playerBody.applyLinearImpulse(new Vec2(0, 6f), playerBody.getWorldCenter());
            AL10.alSourceStop(jumpSource);
            AL10.alSourcePlay(jumpSource);
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
            GLFW.glfwSetWindowShouldClose(window, true);
        }
    }

    private void stepPhysics(float delta) {
        // Apply movement force
        Vec2 velocity = playerBody.getLinearVelocity();
        float desired = moveDirection * 5f;
        float change = desired - velocity.x;
        float impulse = playerBody.getMass() * change;
        playerBody.applyLinearImpulse(new Vec2(impulse, 0), playerBody.getWorldCenter());

        physicsWorld.step(delta, 8, 3);
        onGround = false;
        for (ContactEdge edge = playerBody.getContactList(); edge != null; edge = edge.next) {
            if (edge.contact.isTouching()) {
                onGround = true;
                break;
            }
        }

        Vec2 pos = playerBody.getPosition();
        playerPosition.set(pos.x, pos.y);
    }

    private void render() {
        GL11.glClearColor(0.05f, 0.07f, 0.12f, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        // Simple 2D orthographic projection
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(-12, 12, -7, 7, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        drawGround();
        drawPlayer();
    }

    private void drawGround() {
        GL11.glColor3f(0.2f, 0.9f, 0.35f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(-30f, -5f);
        GL11.glVertex2f(30f, -5f);
        GL11.glVertex2f(30f, -3f);
        GL11.glVertex2f(-30f, -3f);
        GL11.glEnd();
    }

    private void drawPlayer() {
        GL11.glColor3f(0.8f, 0.2f, 0.3f);
        GL11.glPushMatrix();
        GL11.glTranslatef(playerPosition.x, playerPosition.y, 0);
        GL11.glScalef(1f, 2f, 1f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(-0.5f, -0.5f);
        GL11.glVertex2f(0.5f, -0.5f);
        GL11.glVertex2f(0.5f, 0.5f);
        GL11.glVertex2f(-0.5f, 0.5f);
        GL11.glEnd();
        GL11.glPopMatrix();
    }

    private void cleanup() {
        AL10.alDeleteSources(jumpSource);
        AL10.alDeleteBuffers(jumpBuffer);
        ALC10.alcDestroyContext(audioContext);
        ALC10.alcCloseDevice(audioDevice);

        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }
}
