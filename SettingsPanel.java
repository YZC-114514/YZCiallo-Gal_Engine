import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Settings panel for game configuration.
 */
public class SettingsPanel extends JPanel {
    
    private static final Color BG_COLOR = new Color(0, 0, 0, 220);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color ACCENT_COLOR = new Color(255, 200, 0);
    private static final String FONT_NAME = "Microsoft YaHei";
    
    private int textSpeed = 30;
    private int autoSpeed = 50;
    private float bgmVolume = 0.8f;
    private float seVolume = 1.0f;
    
    private JSlider textSpeedSlider;
    private JSlider autoSpeedSlider;
    private JSlider bgmVolumeSlider;
    private JSlider seVolumeSlider;
    private JCheckBox bgmMuteCheck;
    private JCheckBox seMuteCheck;
    private JCheckBox fullscreenCheck;
    
    private SettingsChangeListener changeListener;
    
    public interface SettingsChangeListener {
        void onTextSpeedChanged(int msPerChar);
        void onAutoSpeedChanged(int msPerChar);
        void onBgmVolumeChanged(float volume);
        void onSeVolumeChanged(float volume);
        void onBgmMuteChanged(boolean muted);
        void onSeMuteChanged(boolean muted);
        void onFullscreenChanged(boolean fullscreen);
    }
    
    public SettingsPanel(int width, int height) {
        setLayout(null);
        setBounds(0, 0, width, height);
        setBackground(BG_COLOR);
        setOpaque(false);
        setVisible(false);
        
        int panelWidth = 500;
        int panelHeight = 450;
        int startX = (width - panelWidth) / 2;
        int startY = (height - panelHeight) / 2;
        
        JLabel titleLabel = new JLabel("Settings", SwingConstants.CENTER);
        titleLabel.setFont(new Font(FONT_NAME, Font.BOLD, 32));
        titleLabel.setForeground(ACCENT_COLOR);
        titleLabel.setBounds(startX, startY, panelWidth, 50);
        add(titleLabel);
        
        int y = startY + 70;
        int labelWidth = 150;
        int sliderWidth = 300;
        int rowHeight = 50;
        
        // Text Speed
        JLabel textSpeedLabel = new JLabel("Text Speed:");
        textSpeedLabel.setFont(new Font(FONT_NAME, Font.PLAIN, 18));
        textSpeedLabel.setForeground(TEXT_COLOR);
        textSpeedLabel.setBounds(startX, y, labelWidth, 30);
        add(textSpeedLabel);
        
        textSpeedSlider = createSlider(10, 100, textSpeed);
        textSpeedSlider.setBounds(startX + labelWidth, y, sliderWidth, 30);
        textSpeedSlider.setInverted(true);
        textSpeedSlider.addChangeListener(e -> {
            textSpeed = textSpeedSlider.getValue();
            if (changeListener != null) changeListener.onTextSpeedChanged(textSpeed);
        });
        add(textSpeedSlider);
        
        y += rowHeight;
        
        // Auto Speed
        JLabel autoSpeedLabel = new JLabel("Auto Speed:");
        autoSpeedLabel.setFont(new Font(FONT_NAME, Font.PLAIN, 18));
        autoSpeedLabel.setForeground(TEXT_COLOR);
        autoSpeedLabel.setBounds(startX, y, labelWidth, 30);
        add(autoSpeedLabel);
        
        autoSpeedSlider = createSlider(20, 100, autoSpeed);
        autoSpeedSlider.setBounds(startX + labelWidth, y, sliderWidth, 30);
        autoSpeedSlider.setInverted(true);
        autoSpeedSlider.addChangeListener(e -> {
            autoSpeed = autoSpeedSlider.getValue();
            if (changeListener != null) changeListener.onAutoSpeedChanged(autoSpeed);
        });
        add(autoSpeedSlider);
        
        y += rowHeight + 20;
        
        // BGM Volume
        JLabel bgmLabel = new JLabel("BGM Volume:");
        bgmLabel.setFont(new Font(FONT_NAME, Font.PLAIN, 18));
        bgmLabel.setForeground(TEXT_COLOR);
        bgmLabel.setBounds(startX, y, labelWidth, 30);
        add(bgmLabel);
        
        bgmVolumeSlider = createSlider(0, 100, (int)(bgmVolume * 100));
        bgmVolumeSlider.setBounds(startX + labelWidth, y, sliderWidth - 80, 30);
        bgmVolumeSlider.addChangeListener(e -> {
            bgmVolume = bgmVolumeSlider.getValue() / 100f;
            if (changeListener != null) changeListener.onBgmVolumeChanged(bgmVolume);
        });
        add(bgmVolumeSlider);
        
        bgmMuteCheck = new JCheckBox("Mute");
        bgmMuteCheck.setFont(new Font(FONT_NAME, Font.PLAIN, 14));
        bgmMuteCheck.setForeground(TEXT_COLOR);
        bgmMuteCheck.setOpaque(false);
        bgmMuteCheck.setBounds(startX + labelWidth + sliderWidth - 70, y, 70, 30);
        bgmMuteCheck.addActionListener(e -> {
            if (changeListener != null) changeListener.onBgmMuteChanged(bgmMuteCheck.isSelected());
        });
        add(bgmMuteCheck);
        
        y += rowHeight;
        
        // SE Volume
        JLabel seLabel = new JLabel("SE Volume:");
        seLabel.setFont(new Font(FONT_NAME, Font.PLAIN, 18));
        seLabel.setForeground(TEXT_COLOR);
        seLabel.setBounds(startX, y, labelWidth, 30);
        add(seLabel);
        
        seVolumeSlider = createSlider(0, 100, (int)(seVolume * 100));
        seVolumeSlider.setBounds(startX + labelWidth, y, sliderWidth - 80, 30);
        seVolumeSlider.addChangeListener(e -> {
            seVolume = seVolumeSlider.getValue() / 100f;
            if (changeListener != null) changeListener.onSeVolumeChanged(seVolume);
        });
        add(seVolumeSlider);
        
        seMuteCheck = new JCheckBox("Mute");
        seMuteCheck.setFont(new Font(FONT_NAME, Font.PLAIN, 14));
        seMuteCheck.setForeground(TEXT_COLOR);
        seMuteCheck.setOpaque(false);
        seMuteCheck.setBounds(startX + labelWidth + sliderWidth - 70, y, 70, 30);
        seMuteCheck.addActionListener(e -> {
            if (changeListener != null) changeListener.onSeMuteChanged(seMuteCheck.isSelected());
        });
        add(seMuteCheck);
        
        y += rowHeight + 20;
        
        // Fullscreen
        fullscreenCheck = new JCheckBox("Fullscreen Mode");
        fullscreenCheck.setFont(new Font(FONT_NAME, Font.PLAIN, 18));
        fullscreenCheck.setForeground(TEXT_COLOR);
        fullscreenCheck.setOpaque(false);
        fullscreenCheck.setSelected(true);
        fullscreenCheck.setBounds(startX, y, 200, 30);
        fullscreenCheck.addActionListener(e -> {
            if (changeListener != null) changeListener.onFullscreenChanged(fullscreenCheck.isSelected());
        });
        add(fullscreenCheck);
        
        y += rowHeight + 30;
        
        // Close button
        JButton closeButton = new JButton("Close");
        closeButton.setFont(new Font(FONT_NAME, Font.BOLD, 18));
        closeButton.setBounds(startX + (panelWidth - 120) / 2, y, 120, 40);
        closeButton.addActionListener(e -> setVisible(false));
        add(closeButton);
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getX() < startX || e.getX() > startX + panelWidth ||
                    e.getY() < startY || e.getY() > startY + panelHeight) {
                    setVisible(false);
                }
            }
        });
    }
    
    private JSlider createSlider(int min, int max, int value) {
        JSlider slider = new JSlider(min, max, value);
        slider.setOpaque(false);
        slider.setForeground(ACCENT_COLOR);
        return slider;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }
    
    public void setChangeListener(SettingsChangeListener listener) {
        this.changeListener = listener;
    }
    
    public int getTextSpeed() { return textSpeed; }
    public int getAutoSpeed() { return autoSpeed; }
    public float getBgmVolume() { return bgmVolume; }
    public float getSeVolume() { return seVolume; }
    
    public void setTextSpeed(int speed) {
        this.textSpeed = speed;
        textSpeedSlider.setValue(speed);
    }
    
    public void setAutoSpeedValue(int speed) {
        this.autoSpeed = speed;
        autoSpeedSlider.setValue(speed);
    }
    
    public void setBgmVolumeValue(float volume) {
        this.bgmVolume = volume;
        bgmVolumeSlider.setValue((int)(volume * 100));
    }
    
    public void setSeVolumeValue(float volume) {
        this.seVolume = volume;
        seVolumeSlider.setValue((int)(volume * 100));
    }
}