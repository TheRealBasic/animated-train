import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        Settings settings = Settings.load();
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Java Platform Sandbox");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            GamePanel gamePanel = new GamePanel(settings);
            frame.add(gamePanel);
            frame.pack();
            gamePanel.applyFullscreenPreference();
            gamePanel.start();
        });
    }
}
