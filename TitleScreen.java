import java.awt.*;
import javax.swing.*;

/**
 * Title screen panel with start and load buttons.
 * Optimized with better null safety and image handling.
 */
public class TitleScreen extends JPanel {

    private static final int BTN_WIDTH = 200;
    private static final int BTN_HEIGHT = 50;
    private static final int BTN_SPACING = 70;

    private final Image backgroundImg;

    public TitleScreen(int width, int height, YZCiallo game, String backgroundName) {
        setLayout(null);
        setBounds(0, 0, width, height);

        // Load and cache background image
        ImageIcon icon = new ImageIcon(backgroundName);
        if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) {
            backgroundImg = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        } else {
            backgroundImg = null;
        }

        // Start button
        JButton startButton = new JButton("Start Game");
        startButton.setBounds(width / 2 - BTN_WIDTH / 2, height / 2, BTN_WIDTH, BTN_HEIGHT);
        startButton.addActionListener(e -> game.startGame());
        add(startButton);

        JButton Setting = new JButton("Setting");
        Setting.setBounds(width / 2 - BTN_WIDTH / 2, height / 2 + BTN_SPACING, BTN_WIDTH, BTN_HEIGHT);
        Setting.addActionListener(e -> game.showSettings());
        add(Setting);

        // Load button
        JButton loadButton = new JButton("Load Save");
        loadButton.setBounds(width / 2 - BTN_WIDTH / 2, height / 2 + 2*BTN_SPACING, BTN_WIDTH, BTN_HEIGHT);
        loadButton.addActionListener(e -> game.openLoadMenu());
        add(loadButton);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (backgroundImg != null) {
            g.drawImage(backgroundImg, 0, 0, this);
        } else {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
