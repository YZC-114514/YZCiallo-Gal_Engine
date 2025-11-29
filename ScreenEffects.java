import java.awt.*;
import javax.swing.*;

/**
 * Screen effects manager for visual novel effects.
 */
public class ScreenEffects {
    
    private final JPanel effectLayer;
    private Timer effectTimer;
    private Runnable onEffectComplete;
    
    private float fadeAlpha = 0f;
    private float fadeTarget = 0f;
    private float fadeSpeed = 0.05f;
    private Color fadeColor = Color.BLACK;
    
    private Point originalLocation;
    private JComponent shakeTarget;
    private int shakeIntensity;
    private int shakeDuration;
    private int shakeElapsed;
    
    public ScreenEffects(int width, int height) {
        effectLayer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                if (fadeAlpha > 0) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setColor(new Color(
                        fadeColor.getRed(),
                        fadeColor.getGreen(),
                        fadeColor.getBlue(),
                        (int)(fadeAlpha * 255)
                    ));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    g2d.dispose();
                }
            }
        };
        effectLayer.setBounds(0, 0, width, height);
        effectLayer.setOpaque(false);
        effectLayer.setVisible(false);
    }
    
    public JPanel getEffectLayer() {
        return effectLayer;
    }
    
    public void fadeOut(int durationMs, Runnable onComplete) {
        fadeOut(Color.BLACK, durationMs, onComplete);
    }
    
    public void fadeOut(Color color, int durationMs, Runnable onComplete) {
        this.fadeColor = color != null ? color : Color.BLACK;
        this.fadeAlpha = 0f;
        this.fadeTarget = 1f;
        this.fadeSpeed = 1f / (durationMs / 16f);
        this.onEffectComplete = onComplete;
        
        effectLayer.setVisible(true);
        startFadeTimer();
    }
    
    public void fadeIn(int durationMs, Runnable onComplete) {
        fadeIn(Color.BLACK, durationMs, onComplete);
    }
    
    public void fadeIn(Color color, int durationMs, Runnable onComplete) {
        this.fadeColor = color != null ? color : Color.BLACK;
        this.fadeAlpha = 1f;
        this.fadeTarget = 0f;
        this.fadeSpeed = 1f / (durationMs / 16f);
        this.onEffectComplete = onComplete;
        
        effectLayer.setVisible(true);
        startFadeTimer();
    }
    
    private void startFadeTimer() {
        stopEffectTimer();
        
        effectTimer = new Timer(16, e -> {
            if (fadeTarget > fadeAlpha) {
                fadeAlpha = Math.min(fadeAlpha + fadeSpeed, fadeTarget);
            } else {
                fadeAlpha = Math.max(fadeAlpha - fadeSpeed, fadeTarget);
            }
            
            effectLayer.repaint();
            
            if (Math.abs(fadeAlpha - fadeTarget) < 0.01f) {
                fadeAlpha = fadeTarget;
                effectLayer.repaint();
                
                if (fadeAlpha == 0) {
                    effectLayer.setVisible(false);
                }
                
                stopEffectTimer();
                if (onEffectComplete != null) {
                    Runnable callback = onEffectComplete;
                    onEffectComplete = null;
                    callback.run();
                }
            }
        });
        effectTimer.start();
    }
    
    public void flash(int durationMs, Runnable onComplete) {
        flash(Color.WHITE, durationMs, onComplete);
    }
    
    public void flash(Color color, int durationMs, Runnable onComplete) {
        this.fadeColor = color != null ? color : Color.WHITE;
        this.fadeAlpha = 1f;
        this.onEffectComplete = onComplete;
        
        effectLayer.setVisible(true);
        effectLayer.repaint();
        
        stopEffectTimer();
        effectTimer = new Timer(durationMs / 2, e -> {
            fadeAlpha = 0f;
            effectLayer.repaint();
            effectLayer.setVisible(false);
            ((Timer)e.getSource()).stop();
            
            if (onEffectComplete != null) {
                Runnable callback = onEffectComplete;
                onEffectComplete = null;
                callback.run();
            }
        });
        effectTimer.setRepeats(false);
        effectTimer.start();
    }
    
    public void shake(JComponent target, int intensity, int durationMs, Runnable onComplete) {
        this.shakeTarget = target;
        this.shakeIntensity = intensity;
        this.shakeDuration = durationMs;
        this.shakeElapsed = 0;
        this.originalLocation = target.getLocation();
        this.onEffectComplete = onComplete;
        
        stopEffectTimer();
        effectTimer = new Timer(16, e -> {
            shakeElapsed += 16;
            
            if (shakeElapsed < shakeDuration) {
                int offsetX = (int)(Math.random() * shakeIntensity * 2) - shakeIntensity;
                int offsetY = (int)(Math.random() * shakeIntensity * 2) - shakeIntensity;
                shakeTarget.setLocation(
                    originalLocation.x + offsetX,
                    originalLocation.y + offsetY
                );
            } else {
                shakeTarget.setLocation(originalLocation);
                stopEffectTimer();
                
                if (onEffectComplete != null) {
                    Runnable callback = onEffectComplete;
                    onEffectComplete = null;
                    callback.run();
                }
            }
        });
        effectTimer.start();
    }
    
    public void stopEffectTimer() {
        if (effectTimer != null && effectTimer.isRunning()) {
            effectTimer.stop();
            effectTimer = null;
        }
    }
    
    public boolean isEffectRunning() {
        return effectTimer != null && effectTimer.isRunning();
    }
}