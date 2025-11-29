import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;

/**
 * Panel for displaying character dialogue with name and text.
 * Features typing effect, control buttons, and skip function.
 */
public class DialoguePanel extends JPanel {
    
    private static final Color NAME_COLOR = Color.YELLOW;
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color BG_COLOR = new Color(0, 0, 0, 150);
    private static final Color BTN_ACTIVE_COLOR = new Color(255, 200, 0);  // Golden
    private static final Color BTN_INACTIVE_COLOR = Color.WHITE;
    private static final int DEFAULT_TYPE_SPEED = 30;  // ms per character
    
    private final JLabel nameLabel;
    private final JTextArea dialogueText;
    private final JButton autoButton;
    private final JButton saveButton;
    private final JButton logButton;
    private final JButton skipButton;
    private final JButton settingsButton;
    
    // Typing effect state
    private Timer typingTimer;
    private String fullText;
    private int currentCharIndex;
    private int typeSpeed = DEFAULT_TYPE_SPEED;
    private boolean isTyping = false;
    private Runnable onTypingComplete;

    public DialoguePanel(int width, int height) {
        int dialogHeight = height / 4;
        int nameFontSize = height / 30;
        int textFontSize = height / 35;
        int btnFontSize = height / 50;

        setOpaque(false);
        setLayout(null);
        setBackground(BG_COLOR);
        setBounds(0, height - dialogHeight, width, dialogHeight);

        // Name label
        nameLabel = new JLabel("");
        nameLabel.setForeground(NAME_COLOR);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, nameFontSize));
        nameLabel.setBounds(20, 10, 300, 50);
        add(nameLabel);

        // Dialogue text area
        dialogueText = new JTextArea();
        dialogueText.setBounds(20, 65, width - 200, dialogHeight - 80);
        dialogueText.setOpaque(false);
        dialogueText.setForeground(TEXT_COLOR);
        dialogueText.setFont(new Font("Serif", Font.PLAIN, textFontSize));
        dialogueText.setLineWrap(true);
        dialogueText.setWrapStyleWord(true);
        dialogueText.setEditable(false);
        dialogueText.setFocusable(false);
        add(dialogueText);

        // Control buttons panel (right side)
        int btnWidth = 110;
        int btnHeight = 35;
        int btnX = width - btnWidth - 30;
        int btnSpacing = 5;

        // Auto button
        autoButton = createControlButton("Auto", btnFontSize);
        autoButton.setBounds(btnX, 5, btnWidth, btnHeight);
        add(autoButton);

        // Skip button
        skipButton = createControlButton("Skip", btnFontSize);
        skipButton.setBounds(btnX, 5 + btnHeight + btnSpacing, btnWidth, btnHeight);
        add(skipButton);

        // Save button
        saveButton = createControlButton("Save", btnFontSize);
        saveButton.setBounds(btnX, 5 + (btnHeight + btnSpacing) * 2, btnWidth, btnHeight);
        add(saveButton);

        // Log button (backlog)
        logButton = createControlButton("Log", btnFontSize);
        logButton.setBounds(btnX, 5 + (btnHeight + btnSpacing) * 3, btnWidth, btnHeight);
        add(logButton);

        // Settings button
        settingsButton = createControlButton("Settings", btnFontSize);
        settingsButton.setBounds(btnX, 5 + (btnHeight + btnSpacing) * 4, btnWidth, btnHeight);
        add(settingsButton);
    }

    private JButton createControlButton(String text, int fontSize) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        btn.setForeground(BTN_INACTIVE_COLOR);
        btn.setBackground(new Color(50, 50, 50, 200));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!btn.getForeground().equals(BTN_ACTIVE_COLOR)) {
                    btn.setForeground(new Color(200, 200, 200));
                }
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!btn.getForeground().equals(BTN_ACTIVE_COLOR)) {
                    btn.setForeground(BTN_INACTIVE_COLOR);
                }
            }
        });
        return btn;
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }

    /**
     * Updates the dialogue content with null safety.
     */
    public void uploadcontent(String name, String text) {
        nameLabel.setText(name != null ? name : "");
        dialogueText.setText(text != null ? text : "");
    }

    /**
     * Clears all dialogue content.
     */
    public void clearstage() {
        nameLabel.setText("");
        dialogueText.setText("");
    }

    // Button action setters
    public void setAutoButtonListener(ActionListener listener) {
        autoButton.addActionListener(listener);
    }

    public void setSaveButtonListener(ActionListener listener) {
        saveButton.addActionListener(listener);
    }

    public void setLogButtonListener(ActionListener listener) {
        logButton.addActionListener(listener);
    }

    /**
     * Updates the auto button appearance based on auto mode state.
     */
    public void setAutoModeActive(boolean active) {
        autoButton.setForeground(active ? BTN_ACTIVE_COLOR : BTN_INACTIVE_COLOR);
    }

    // Skip button listener
    public void setSkipButtonListener(ActionListener listener) {
        skipButton.addActionListener(listener);
    }

    // Settings button listener
    public void setSettingsButtonListener(ActionListener listener) {
        settingsButton.addActionListener(listener);
    }

    // Typing effect methods
    
    /**
     * Sets the typing speed (ms per character).
     */
    public void setTypeSpeed(int msPerChar) {
        this.typeSpeed = msPerChar;
    }

    /**
     * Displays text with typing effect.
     */
    public void typeText(String name, String text, Runnable onComplete) {
        nameLabel.setText(name != null ? name : "");
        this.fullText = text != null ? text : "";
        this.currentCharIndex = 0;
        this.isTyping = true;
        this.onTypingComplete = onComplete;
        
        dialogueText.setText("");
        
        if (fullText.isEmpty()) {
            finishTyping();
            return;
        }
        
        if (typingTimer != null && typingTimer.isRunning()) {
            typingTimer.stop();
        }
        
        typingTimer = new Timer(typeSpeed, e -> {
            if (currentCharIndex < fullText.length()) {
                currentCharIndex++;
                dialogueText.setText(fullText.substring(0, currentCharIndex));
            } else {
                finishTyping();
            }
        });
        typingTimer.start();
    }

    /**
     * Instantly completes the current typing effect.
     */
    public void skipTyping() {
        if (isTyping && fullText != null) {
            if (typingTimer != null) {
                typingTimer.stop();
            }
            dialogueText.setText(fullText);
            finishTyping();
        }
    }

    private void finishTyping() {
        isTyping = false;
        if (typingTimer != null) {
            typingTimer.stop();
        }
        if (onTypingComplete != null) {
            Runnable callback = onTypingComplete;
            onTypingComplete = null;
            callback.run();
        }
    }

    /**
     * Checks if text is currently being typed.
     */
    public boolean isTyping() {
        return isTyping;
    }

    /**
     * Gets the full text being displayed (or being typed).
     */
    public String getFullText() {
        return fullText;
    }
}
