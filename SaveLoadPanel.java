import java.awt.*;
import javax.swing.*;

/**
 * Panel for save/load game functionality.
 * Optimized with constants and improved null safety.
 */
public class SaveLoadPanel extends JPanel {

    private static final Color BG_COLOR = new Color(0, 0, 0, 200);
    private static final Color SAVE_COLOR = Color.GREEN;
    private static final Color LOAD_COLOR = Color.CYAN;
    private static final String FONT_NAME = "Microsoft YaHei";
    private static final int SLOT_COUNT = 3;
    private static final int BTN_WIDTH = 600;
    private static final int BTN_HEIGHT = 80;

    private final YZCiallo game;
    private final JLabel titleLabel;
    private final JButton[] slotButtons;
    private boolean isSaveMode;

    public SaveLoadPanel(int width, int height, YZCiallo game) {
        this.game = game;
        
        setLayout(null);
        setBounds(0, 0, width, height);
        setBackground(BG_COLOR);
        setVisible(false);

        // Title label
        titleLabel = new JLabel("Save Progress", SwingConstants.CENTER);
        titleLabel.setFont(new Font(FONT_NAME, Font.BOLD, 40));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBounds(0, 50, width, 60);
        add(titleLabel);

        // Create slot buttons
        slotButtons = new JButton[SLOT_COUNT];
        int startY = 150;

        for (int i = 0; i < SLOT_COUNT; i++) {
            final int slotIndex = i + 1;
            
            slotButtons[i] = new JButton();
            slotButtons[i].setFont(new Font(FONT_NAME, Font.PLAIN, 20));
            slotButtons[i].setBounds(
                (width - BTN_WIDTH) / 2, 
                startY + (i * (BTN_HEIGHT + 20)), 
                BTN_WIDTH, 
                BTN_HEIGHT
            );
            slotButtons[i].addActionListener(e -> onSlotClicked(slotIndex));
            add(slotButtons[i]);
        }

        // Close button
        JButton closeButton = new JButton("Close");
        closeButton.setBounds((width - 200) / 2, height - 100, 200, 50);
        closeButton.addActionListener(e -> setVisible(false));
        add(closeButton);
    }

    /**
     * Shows the panel in save or load mode.
     */
    public void showPanel(boolean isSave) {
        this.isSaveMode = isSave;
        setVisible(true);

        if (isSaveMode) {
            titleLabel.setText("Save Progress");
            titleLabel.setForeground(SAVE_COLOR);
        } else {
            titleLabel.setText("Load Progress");
            titleLabel.setForeground(LOAD_COLOR);
        }

        refreshSlots();
    }

    private void refreshSlots() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            int slot = i + 1;
            String preview = game.getSavePreview(slot);

            if (preview.contains("Empty") || preview.contains("空")) {
                slotButtons[i].setText("Slot " + slot + " : [Empty]");
            } else {
                slotButtons[i].setText(
                    "<html>Slot " + slot + 
                    "<br/><font size=4 color='gray'>" + preview + "</font></html>"
                );
            }
        }
    }

    private void onSlotClicked(int slot) {
        if (isSaveMode) {
            game.saveGame(slot);
            refreshSlots();
        } else {
            game.loadGame(slot);
            setVisible(false);
        }
    }
}