import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.*;

/**
 * Audio manager for BGM and sound effects.
 * Singleton pattern for global access.
 */
public class AudioManager {
    
    private static AudioManager instance;
    
    private Clip bgmClip;
    private float bgmVolume = 0.8f;
    private float seVolume = 1.0f;
    private boolean bgmMuted = false;
    private boolean seMuted = false;
    
    // Cache for sound effects
    private final Map<String, Clip> seCache = new HashMap<>();
    
    private AudioManager() {}
    
    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }
    
    public void playBGM(String filename) {
        stopBGM();
        try {
            File audioFile = new File(filename);
            if (!audioFile.exists()) {
                System.err.println("[Audio] BGM file not found: " + filename);
                return;
            }
            
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            bgmClip = AudioSystem.getClip();
            bgmClip.open(audioStream);
            
            setBgmVolume(bgmVolume);
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            
            if (!bgmMuted) {
                bgmClip.start();
            }
            System.out.println("[Audio] Playing BGM: " + filename);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("[Audio] Error playing BGM: " + e.getMessage());
        }
    }
    
    public void stopBGM() {
        if (bgmClip != null) {
            bgmClip.stop();
            bgmClip.close();
            bgmClip = null;
        }
    }
    
    public void pauseBGM() {
        if (bgmClip != null && bgmClip.isRunning()) {
            bgmClip.stop();
        }
    }
    
    public void resumeBGM() {
        if (bgmClip != null && !bgmMuted) {
            bgmClip.start();
        }
    }
    
    public void playSE(String filename) {
        if (seMuted) return;
        
        try {
            Clip seClip = seCache.get(filename);
            
            if (seClip == null) {
                File audioFile = new File(filename);
                if (!audioFile.exists()) {
                    System.err.println("[Audio] SE file not found: " + filename);
                    return;
                }
                
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                seClip = AudioSystem.getClip();
                seClip.open(audioStream);
                seCache.put(filename, seClip);
            }
            
            seClip.setFramePosition(0);
            setClipVolume(seClip, seVolume);
            seClip.start();
            System.out.println("[Audio] Playing SE: " + filename);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("[Audio] Error playing SE: " + e.getMessage());
        }
    }
    
    public void setBgmVolume(float volume) {
        this.bgmVolume = Math.max(0f, Math.min(1f, volume));
        if (bgmClip != null) {
            setClipVolume(bgmClip, bgmMuted ? 0f : bgmVolume);
        }
    }
    
    public void setSeVolume(float volume) {
        this.seVolume = Math.max(0f, Math.min(1f, volume));
    }
    
    public void setBgmMuted(boolean muted) {
        this.bgmMuted = muted;
        if (bgmClip != null) {
            if (muted) {
                bgmClip.stop();
            } else {
                setBgmVolume(bgmVolume);
                bgmClip.start();
            }
        }
    }
    
    public void setSeMuted(boolean muted) {
        this.seMuted = muted;
    }
    
    private void setClipVolume(Clip clip, float volume) {
        if (clip == null) return;
        try {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float) (Math.log(Math.max(0.0001, volume)) / Math.log(10.0) * 20.0);
            dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
            gainControl.setValue(dB);
        } catch (IllegalArgumentException e) {
            // Volume control not supported
        }
    }
    
    public void cleanup() {
        stopBGM();
        for (Clip clip : seCache.values()) {
            clip.close();
        }
        seCache.clear();
    }
    
    public float getBgmVolume() { return bgmVolume; }
    public float getSeVolume() { return seVolume; }
    public boolean isBgmMuted() { return bgmMuted; }
    public boolean isSeMuted() { return seMuted; }
}