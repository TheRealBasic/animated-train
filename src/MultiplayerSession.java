import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class MultiplayerSession {
    public enum Role { HOST, CLIENT }

    private static final int PORT = 9484;
    private static final String DISCOVERY_TOKEN = "GWT_DISCOVER";
    private static final String DISCOVERY_RESPONSE = "GWT_HOST";

    private final Role role;
    private final Socket socket;
    private final AtomicReference<RemoteState> latestState = new AtomicReference<>(new RemoteState());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private BufferedWriter writer;

    private MultiplayerSession(Role role, Socket socket) {
        this.role = role;
        this.socket = socket;
        startReader();
    }

    public static MultiplayerSession host() throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        Socket client = serverSocket.accept();
        serverSocket.close();
        return new MultiplayerSession(Role.HOST, client);
    }

    public static MultiplayerSession join(String ip) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(ip, PORT), 5000);
        return new MultiplayerSession(Role.CLIENT, socket);
    }

    public static Optional<String> discoverLanHost() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            byte[] data = DISCOVERY_TOKEN.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), PORT);
            socket.send(packet);
            socket.setSoTimeout(1000);
            byte[] buffer = new byte[128];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);
            String payload = new String(response.getData(), 0, response.getLength(), StandardCharsets.UTF_8);
            if (DISCOVERY_RESPONSE.equals(payload)) {
                return Optional.of(response.getAddress().getHostAddress());
            }
        } catch (IOException ignored) {
        }
        return Optional.empty();
    }

    public static void advertiseLanHost() {
        Executors.newSingleThreadExecutor().submit(() -> {
            try (DatagramSocket socket = new DatagramSocket(PORT, InetAddress.getByName("0.0.0.0"))) {
                byte[] buffer = new byte[128];
                while (!Thread.currentThread().isInterrupted()) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String payload = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    if (DISCOVERY_TOKEN.equals(payload)) {
                        byte[] reply = DISCOVERY_RESPONSE.getBytes(StandardCharsets.UTF_8);
                        DatagramPacket response = new DatagramPacket(reply, reply.length, packet.getAddress(), packet.getPort());
                        socket.send(response);
                    }
                }
            } catch (IOException ignored) {
            }
        });
    }

    private void startReader() {
        executor.submit(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    RemoteState parsed = RemoteState.parse(line);
                    if (parsed != null) {
                        latestState.set(parsed);
                    }
                }
            } catch (IOException ignored) {
            }
        });
    }

    public void sendState(double x, double y, GravityDir gravity, long orbMask) {
        try {
            if (writer == null) {
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            }
            String payload = String.format("STATE %.2f %.2f %s %d\n", x, y, gravity.name(), orbMask);
            writer.write(payload);
            writer.flush();
        } catch (IOException ignored) {
        }
    }

    public void sendLevelIndex(int index) {
        try {
            if (writer == null) {
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            }
            writer.write("LEVEL " + index + "\n");
            writer.flush();
        } catch (IOException ignored) {
        }
    }

    public RemoteState pollRemoteState() {
        return latestState.getAndSet(new RemoteState());
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
        executor.shutdownNow();
    }

    public Role getRole() {
        return role;
    }

    public record RemoteState(Double x, Double y, GravityDir gravity, Long orbMask, Integer levelIndex) {
        public RemoteState() {
            this(null, null, null, null, null);
        }

        private static RemoteState parse(String line) {
            if (line == null || line.isBlank()) {
                return null;
            }
            String[] parts = line.split(" ");
            if (parts.length < 2) {
                return null;
            }
            if ("STATE".equals(parts[0]) && parts.length >= 5) {
                try {
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    GravityDir gravity = GravityDir.valueOf(parts[3]);
                    long mask = Long.parseLong(parts[4]);
                    return new RemoteState(x, y, gravity, mask, null);
                } catch (Exception ex) {
                    return null;
                }
            }
            if ("LEVEL".equals(parts[0]) && parts.length >= 2) {
                try {
                    int level = Integer.parseInt(parts[1]);
                    return new RemoteState(null, null, null, null, level);
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            return null;
        }
    }
}
