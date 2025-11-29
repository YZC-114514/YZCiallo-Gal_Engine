import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.*;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/**
 * Main game UI class for the Visual Novel engine.
 * Features: Typing effect, audio system, screen effects, variable system,
 * multiple character positions, skip function, and settings.
 */
public class YZCiallo extends JFrame {

    // Constants - using static final for compile-time optimization
    private static final int IDX_LINE = 0;
    private static final int IDX_BG = 1;
    private static final int IDX_CHAR = 2;
    private static final int IDX_MOOD = 3;
    private static final int IDX_SCRIPT = 4;
    private static final int IDX_TEXT = 5;
    private static final String SAVE_SEPARATOR = "###";
    private static final String SAVE_FILE = "save.dat";
    private static final String DEFAULT_SCRIPT = "Chapter1_1.json";
    private static final String EMPTY_SLOT = "EMPTY";
    private static final int TRANSITION_SPEED = 10;
    private static final int TRANSITION_DELAY_MS = 10;
    private static final int AUTO_BASE_DELAY_MS = 1500;
    private static final int AUTO_MS_PER_CHAR = 50;
    private static final int AUTO_MAX_DELAY_MS = 10000;
    private static final int PREVIEW_MAX_LENGTH = 15;
    private static final int DEFAULT_TYPE_SPEED = 30;

    // Screen dimensions (final for immutability)
    private final int screenWidth;
    private final int screenHeight;
    private final int characterHeight;
    private final int characterWidth;

    // Game state
    private ArrayList<ScriptData> scriptLines = new ArrayList<>();
    private final Map<String, Function<ScriptData, Boolean>> commandMap = new HashMap<>();
    private int curtainAlpha = 0;
    private boolean isFadingOut = true;
    private Timer transitionTimer;
    private boolean isGameOver;
    private final ArrayList<String> currentState = new ArrayList<>(6);
    private final ArrayList<String> backlog = new ArrayList<>();
    private Timer autoTimer;
    private boolean isAutoMode;
    private int storyIndex = 0;
    private boolean isSkipMode = false;
    private int typeSpeed = DEFAULT_TYPE_SPEED;
    private boolean isTypingComplete = true;

    // Multiple character positions
    private final Map<String, JLabel> characterLabels = new HashMap<>();
    private final Map<String, Point> characterPositions = new HashMap<>();

    // Image cache for better performance
    private final Map<String, Image> scaledImageCache = new HashMap<>();
    private Map<String,Integer> gameVariable = new HashMap<>();

    // UI Components
    private final JLayeredPane layers;
    private JLabel backgroundLabel;
    private JPanel curtainPanel;
    private JLabel characterLabel;          // Main character (center)
    private JLabel characterLabelLeft;      // Left position
    private JLabel characterLabelRight;     // Right position
    private DialoguePanel dialogueBox;
    private TitleScreen titlePanel;
    private ChoicePanel choicePanel;
    private SaveLoadPanel saveLoadPanel;
    private BacklogPanel backlogPanel;
    private JLabel autoModeIndicator;
    private JLabel skipModeIndicator;
    private SettingsPanel settingsPanel;
    private ScreenEffects screenEffects;

    // Audio and game state managers
    private AudioManager audioManager;
    private GameState gameState;

    // Reusable Gson instance
    private static final Gson GSON = new Gson();

    public YZCiallo() {
        // Get screen dimensions once
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.screenWidth = screenSize.width;
        this.screenHeight = screenSize.height;
        this.characterHeight = screenHeight * 3 / 4;
        this.characterWidth = characterHeight / 2;

        // Initialize managers
        audioManager = AudioManager.getInstance();
        gameState = GameState.getInstance();

        // Initialize state
        initializeState();
        backlog.add(null);

        // Window setup
        setTitle("My Java Galgame Engine");
        setSize(screenWidth, screenHeight);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);
        setLocationRelativeTo(null);
        setResizable(false);

        // Create main layer container
        layers = new JLayeredPane();
        layers.setBounds(0, 0, screenWidth, screenHeight);
        add(layers);

        // Initialize all components
        initBackgroundLayer();
        initCharacterLayer();
        initUILayer();
        initTitleLayer();
        initCommands();
        initAutoTimer();
        initScreenEffects();
        initSettings();
        
        loadScript(currentState.get(IDX_SCRIPT));
        advanceStory();

        setFocusable(true);
        requestFocusInWindow();
        setupInputListeners();
    }

    private void initScreenEffects() {
        screenEffects = new ScreenEffects(screenWidth, screenHeight);
        layers.add(screenEffects.getEffectLayer(), Integer.valueOf(3500));
    }

    private void initSettings() {
        settingsPanel = new SettingsPanel(screenWidth, screenHeight);
        settingsPanel.setChangeListener(new SettingsPanel.SettingsChangeListener() {
            @Override
            public void onTextSpeedChanged(int msPerChar) {
                typeSpeed = msPerChar;
                dialogueBox.setTypeSpeed(msPerChar);
            }
            @Override
            public void onAutoSpeedChanged(int msPerChar) {
                // Update auto timer calculation base
            }
            @Override
            public void onBgmVolumeChanged(float volume) {
                audioManager.setBgmVolume(volume);
            }
            @Override
            public void onSeVolumeChanged(float volume) {
                audioManager.setSeVolume(volume);
            }
            @Override
            public void onBgmMuteChanged(boolean muted) {
                audioManager.setBgmMuted(muted);
            }
            @Override
            public void onSeMuteChanged(boolean muted) {
                audioManager.setSeMuted(muted);
            }
            @Override
            public void onFullscreenChanged(boolean fullscreen) {
                // Handle fullscreen toggle if needed
            }
        });
        layers.add(settingsPanel, Integer.valueOf(4000));
    }

    private void initializeState() {
        currentState.add("0");              // IDX_LINE
        currentState.add("Background");     // IDX_BG
        currentState.add(null);             // IDX_CHAR
        currentState.add(null);             // IDX_MOOD
        currentState.add(DEFAULT_SCRIPT);   // IDX_SCRIPT
        currentState.add("");               // IDX_TEXT
    }

    private void setupInputListeners() {
        // Keyboard listener for shortcuts
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (titlePanel.isVisible()) return;
                if (choicePanel.isVisible()) return;
                
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_S -> saveLoadPanel.showPanel(true);
                    case KeyEvent.VK_A -> toggleAutoMode();
                    case KeyEvent.VK_CONTROL -> setSkipMode(true);
                    case KeyEvent.VK_ESCAPE -> {
                        if (isAutoMode) setAutoMode(false);
                        if (isSkipMode) setSkipMode(false);
                        if (settingsPanel.isVisible()) settingsPanel.setVisible(false);
                    }
                    case KeyEvent.VK_SPACE -> {
                        // Skip typing or advance story
                        if(isGameOver){
                            returnToTitle();
                            isGameOver=false;
                            return;
                        }
                        if (dialogueBox.isTyping()) {
                            dialogueBox.skipTyping();
                        } else {
                            advanceStory();
                        }
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
                    setSkipMode(false);
                }
            }
        });

        // Mouse wheel listener for backlog
        addMouseWheelListener(e -> {
            if (!titlePanel.isVisible() && !saveLoadPanel.isVisible() 
                && e.getWheelRotation() < 0 && !backlogPanel.isVisible()) {
                if (isAutoMode) setAutoMode(false);  // Pause auto mode when viewing backlog
                backlogPanel.updateLogs(backlog);
                backlogPanel.setVisible(true);
            }
        });
    }

    public void openLoadMenu() {
        saveLoadPanel.showPanel(false);
    }

    /**
     * Loads and parses a script file with improved resource management.
     */
    public void loadScript(String filename) {
        scriptLines.clear();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8))) {
            java.lang.reflect.Type listType = new TypeToken<ArrayList<ScriptData>>(){}.getType();
            scriptLines = GSON.fromJson(reader, listType);
            System.out.println("Script loaded: " + scriptLines.size() + " lines");
        } catch (FileNotFoundException e) {
            System.err.println("Error: Script file not found [" + filename + "]");
        } catch (JsonSyntaxException e) {
            System.err.println("Error: JSON syntax error in script");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error reading script file");
            e.printStackTrace();
        }
    }

    private void initBackgroundLayer() {
        Image bgImage = loadAndScaleImage("Background.jpg", screenWidth, screenHeight);
        backgroundLabel = new JLabel(new ImageIcon(bgImage));
        backgroundLabel.setBounds(0, 0, screenWidth, screenHeight);
        layers.add(backgroundLabel, JLayeredPane.DEFAULT_LAYER);

        // Curtain panel for transitions
        curtainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, curtainAlpha));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        curtainPanel.setOpaque(false);
        curtainPanel.setBounds(0, 0, screenWidth, screenHeight);
        layers.add(curtainPanel, Integer.valueOf(1000));
    }

    private void initCharacterLayer() {
        characterLabel = new JLabel();
        characterLabel.setBounds(screenWidth / 4, screenHeight - characterHeight, 
                                 characterWidth, characterHeight);
        layers.add(characterLabel, JLayeredPane.PALETTE_LAYER);
    }

    private void initUILayer() {
        dialogueBox = new DialoguePanel(screenWidth, screenHeight);

        // Connect dialogue panel buttons
        dialogueBox.setAutoButtonListener(e -> toggleAutoMode());
        dialogueBox.setSaveButtonListener(e -> saveLoadPanel.showPanel(true));
        dialogueBox.setLogButtonListener(e -> {
            if (!backlogPanel.isVisible()) {
                if (isAutoMode) setAutoMode(false);
                backlogPanel.updateLogs(backlog);
                backlogPanel.setVisible(true);
            }
        });
        dialogueBox.setSkipButtonListener(e -> {
            if (dialogueBox.isTyping()) {
                dialogueBox.skipTyping();
            }
        });
        dialogueBox.setSettingsButtonListener(e -> {
            settingsPanel.setVisible(!settingsPanel.isVisible());
        });

        layers.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (titlePanel.isVisible() || backlogPanel.isVisible()) return;

                if (e.getButton() == MouseEvent.BUTTON1) {
                    handleLeftClick();
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    dialogueBox.setVisible(!dialogueBox.isVisible());
                    layers.repaint();
                }
            }
        });

        choicePanel = new ChoicePanel(screenWidth,screenHeight);
        layers.add(choicePanel, Integer.valueOf(2000));

        backlogPanel = new BacklogPanel(screenWidth, screenHeight);
        layers.add(backlogPanel, Integer.valueOf(2500));

        layers.add(dialogueBox, JLayeredPane.MODAL_LAYER);
        
        // Auto mode indicator
        initAutoModeIndicator();
        
        initSaveLoadLayers();
    }

    private void initAutoModeIndicator() {
        autoModeIndicator = new JLabel("AUTO");
        autoModeIndicator.setFont(new Font("SansSerif", Font.BOLD, 24));
        autoModeIndicator.setForeground(new Color(255, 200, 0));
        autoModeIndicator.setBounds(screenWidth - 120, 20, 100, 30);
        autoModeIndicator.setVisible(false);
        layers.add(autoModeIndicator, Integer.valueOf(2600));

        // Skip mode indicator
        skipModeIndicator = new JLabel("SKIP");
        skipModeIndicator.setFont(new Font("SansSerif", Font.BOLD, 24));
        skipModeIndicator.setForeground(new Color(255, 100, 100));
        skipModeIndicator.setBounds(screenWidth - 120, 55, 100, 30);
        skipModeIndicator.setVisible(false);
        layers.add(skipModeIndicator, Integer.valueOf(2600));
    }

    private void handleLeftClick() {
        if (choicePanel.isVisible()) return;
        if (isGameOver) {
            returnToTitle();
            isGameOver = false;
        } else if (!dialogueBox.isVisible()) {
            dialogueBox.setVisible(true);
        } else if (dialogueBox.isTyping()) {
            // Skip typing effect on click
            dialogueBox.skipTyping();
        } else {
            advanceStory();
            repaint();
        }
    }

    private void initSaveLoadLayers() {
        saveLoadPanel = new SaveLoadPanel(screenWidth, screenHeight, this);
        layers.add(saveLoadPanel, Integer.valueOf(3000));
    }

    private void initTitleLayer() {
        titlePanel = new TitleScreen(screenWidth, screenHeight, this, "title.png");
        layers.add(titlePanel, Integer.valueOf(2000));
    }

    public void startGame() {
        titlePanel.setVisible(false);
        dialogueBox.setVisible(true);
        loadScript(DEFAULT_SCRIPT);
        currentState.set(IDX_SCRIPT, DEFAULT_SCRIPT);
        currentState.set(IDX_LINE, "0");
        isGameOver = false;
        storyIndex = 0;
        advanceStory();
    }

    private void returnToTitle() {
        titlePanel.setVisible(true);
        dialogueBox.setVisible(false);
    }

    /**
     * Sets character emotion with image caching for performance.
     */
    public void setCharacterEmotion(String name, String mood) {
        if (name == null || name.isEmpty() || "null".equals(name)) {
            characterLabel.setIcon(null);
            return;
        }

        String filename = mood != null && !mood.isEmpty() && !"null".equals(mood)
            ? name + "_" + mood + ".jpg"
            : name + ".jpg";

        Image cachedImage = getOrLoadCharacterImage(filename, name + ".jpg");
        if (cachedImage != null) {
            characterLabel.setIcon(new ImageIcon(cachedImage));
            currentState.set(IDX_CHAR, name);
            currentState.set(IDX_MOOD, mood);
        }
        characterLabel.repaint();
    }

    /**
     * Gets image from cache or loads and caches it.
     */
    private Image getOrLoadCharacterImage(String primaryFile, String fallbackFile) {
        // Check cache first
        if (scaledImageCache.containsKey(primaryFile)) {
            return scaledImageCache.get(primaryFile);
        }

        ImageIcon icon = new ImageIcon(primaryFile);
        String fileToUse = primaryFile;

        if (icon.getImageLoadStatus() != MediaTracker.COMPLETE) {
            icon = new ImageIcon(fallbackFile);
            fileToUse = fallbackFile;
            if (icon.getImageLoadStatus() != MediaTracker.COMPLETE) {
                return null;
            }
        }

        Image scaled = icon.getImage().getScaledInstance(characterWidth, characterHeight, Image.SCALE_SMOOTH);
        scaledImageCache.put(fileToUse, scaled);
        return scaled;
    }

    private void initCommands() {
        commandMap.put("bg", action -> {
            playTransition(action.param);
            dialogueBox.clearstage();
            currentState.set(IDX_BG, action.param);
            return false;
        });

        commandMap.put("dialogue", action -> {
            isTypingComplete = false;
            String name = action.name;
            String text = action.text;
            
            setCharacterEmotion(action.name, action.mood);
            currentState.set(IDX_CHAR, action.name);
            currentState.set(IDX_MOOD, action.mood);
            currentState.set(IDX_TEXT, action.text);
            
            // Use typing effect
            dialogueBox.typeText(name, text, () -> {
                isTypingComplete = true;
            });
            
            // Add to backlog
            String logEntry = isNullOrEmpty(action.name) 
                ? action.text 
                : "【" + action.name + "】: " + action.text;
            backlog.add(logEntry);
            return false;
        });

        commandMap.put("goto", action -> {
            int targetIndex = findLabelIndex(action.param);
            if (targetIndex != -1) {
                storyIndex = targetIndex;
                currentState.set(IDX_LINE, Integer.toString(storyIndex));
                advanceStory();
            }
            return true;
        });

        commandMap.put("choice", action -> {
            setupChoices(action.param);
            return true;
        });

        commandMap.put("next_chapter", action -> {
            loadScript(action.param);
            storyIndex = 0;
            currentState.set(IDX_SCRIPT, action.param);
            currentState.set(IDX_LINE, "0");
            advanceStory();
            return true;
        });

        // Audio commands
        commandMap.put("bgm", action -> {
            if ("stop".equalsIgnoreCase(action.text)) {
                audioManager.stopBGM();
            } else if (action.param != null) {
                audioManager.playBGM(action.param);
            }
            storyIndex++;
            currentState.set(IDX_LINE, Integer.toString(storyIndex));
            advanceStory();
            return true;
        });

        commandMap.put("se", action -> {
            if (action.param != null) {
                audioManager.playSE(action.param);
            }
            storyIndex++;
            currentState.set(IDX_LINE, Integer.toString(storyIndex));
            advanceStory();
            return true;
        });

        // Variable commands
        commandMap.put("set", action -> {
            gameState.executeSet(action.param);
            storyIndex++;
            currentState.set(IDX_LINE, Integer.toString(storyIndex));
            advanceStory();
            return true;
        });

        commandMap.put("var", action ->{
            String[] args =action.text.split(",");
            
            if(args.length>=3){
                String op = args[0].trim();
                String key = args[1].trim();
                int val =0;
                String jump =null;
    
                try {
                    val = Integer.parseInt(args[2].trim());
                } catch (Exception e) {
                    System.err.println("Variable error: "+ args[2]);
                }

                if (args.length >=4) jump = args[3].trim();
                
                Variable_op(op, key, val, jump);

            } else {
                storyIndex++;
                advanceStory();
            }
            return true;
        });

        commandMap.put("if", action -> {
            if (gameState.evaluateCondition(action.param)) {
                // Condition is true, jump to label
                int targetIndex = findLabelIndex(action.text);
                if (targetIndex != -1) {
                    storyIndex = targetIndex;
                    currentState.set(IDX_LINE, Integer.toString(storyIndex));
                }
            } else {
                storyIndex++;
                currentState.set(IDX_LINE, Integer.toString(storyIndex));
            }
            advanceStory();
            return true;
        });

        // Screen effects
        commandMap.put("effect", action -> {
            String effectType = action.param;
            String params = action.text;
            
            if ("fade_out".equalsIgnoreCase(effectType)) {
                int duration = parseIntOrDefault(params, 500);
                screenEffects.fadeOut(duration, () -> {
                    storyIndex++;
                    currentState.set(IDX_LINE, Integer.toString(storyIndex));
                    advanceStory();
                });
            } else if ("fade_in".equalsIgnoreCase(effectType)) {
                int duration = parseIntOrDefault(params, 500);
                screenEffects.fadeIn(duration, () -> {
                    storyIndex++;
                    currentState.set(IDX_LINE, Integer.toString(storyIndex));
                    advanceStory();
                });
            } else if ("flash".equalsIgnoreCase(effectType)) {
                int duration = parseIntOrDefault(params, 200);
                screenEffects.flash(duration, () -> {
                    storyIndex++;
                    currentState.set(IDX_LINE, Integer.toString(storyIndex));
                    advanceStory();
                });
            } else if ("shake".equalsIgnoreCase(effectType)) {
                int intensity = parseIntOrDefault(params, 10);
                screenEffects.shake(backgroundLabel, intensity, 300, () -> {
                    storyIndex++;
                    currentState.set(IDX_LINE, Integer.toString(storyIndex));
                    advanceStory();
                });
            } else {
                storyIndex++;
                currentState.set(IDX_LINE, Integer.toString(storyIndex));
                advanceStory();
            }
            return true;
        });

        // Wait command
        commandMap.put("wait", action -> {
            int delay = parseIntOrDefault(action.param, 1000);
            Timer waitTimer = new Timer(delay, e -> {
                ((Timer)e.getSource()).stop();
                storyIndex++;
                currentState.set(IDX_LINE, Integer.toString(storyIndex));
                advanceStory();
            });
            waitTimer.setRepeats(false);
            waitTimer.start();
            return true;
        });

        // Character positioning
        commandMap.put("char", action -> {
            String position = action.param != null ? action.param.toLowerCase() : "center";
            showCharacterAt(action.name, action.mood, position);
            storyIndex++;
            currentState.set(IDX_LINE, Integer.toString(storyIndex));
            advanceStory();
            return true;
        });

        commandMap.put("char_hide", action -> {
            String target = action.param != null ? action.param.toLowerCase() : "all";
            hideCharacter(target);
            storyIndex++;
            currentState.set(IDX_LINE, Integer.toString(storyIndex));
            advanceStory();
            return true;
        });
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void showCharacterAt(String name, String mood, String position) {
        JLabel targetLabel;
        int xPos;
        
        switch (position) {
            case "left":
                if (characterLabelLeft == null) {
                    characterLabelLeft = new JLabel();
                    characterLabelLeft.setBounds(screenWidth / 8, screenHeight - characterHeight, 
                                                 characterWidth, characterHeight);
                    layers.add(characterLabelLeft, JLayeredPane.PALETTE_LAYER);
                }
                targetLabel = characterLabelLeft;
                xPos = screenWidth / 8;
                break;
            case "right":
                if (characterLabelRight == null) {
                    characterLabelRight = new JLabel();
                    characterLabelRight.setBounds(screenWidth * 5 / 8, screenHeight - characterHeight, 
                                                  characterWidth, characterHeight);
                    layers.add(characterLabelRight, JLayeredPane.PALETTE_LAYER);
                }
                targetLabel = characterLabelRight;
                xPos = screenWidth * 5 / 8;
                break;
            default: // center
                targetLabel = characterLabel;
                xPos = screenWidth / 4;
                break;
        }
        
        String filename = mood != null && !mood.isEmpty() && !"null".equals(mood)
            ? name + "_" + mood + ".jpg"
            : name + ".jpg";
        
        Image cachedImage = getOrLoadCharacterImage(filename, name + ".jpg");
        if (cachedImage != null) {
            targetLabel.setIcon(new ImageIcon(cachedImage));
        }
        characterLabels.put(position, targetLabel);
        characterPositions.put(position, new Point(xPos, screenHeight - characterHeight));
    }

    private void hideCharacter(String target) {
        if ("all".equals(target)) {
            characterLabel.setIcon(null);
            if (characterLabelLeft != null) characterLabelLeft.setIcon(null);
            if (characterLabelRight != null) characterLabelRight.setIcon(null);
        } else if ("left".equals(target) && characterLabelLeft != null) {
            characterLabelLeft.setIcon(null);
        } else if ("right".equals(target) && characterLabelRight != null) {
            characterLabelRight.setIcon(null);
        } else if ("center".equals(target)) {
            characterLabel.setIcon(null);
        }
    }

    private void initAutoTimer() {
        // Initial delay - will be updated dynamically based on text length
        autoTimer = new Timer(AUTO_BASE_DELAY_MS, e -> {
            // Stop auto mode on choices, game over, or when panels are open
            if (choicePanel.isVisible() || isGameOver || 
                backlogPanel.isVisible() || saveLoadPanel.isVisible()) {
                setAutoMode(false);
                return;
            }
            // Don't advance during transitions
            if (transitionTimer != null && transitionTimer.isRunning()) {
                return;
            }
            advanceStory();
            
            // Update timer delay for next line based on current text length
            updateAutoTimerDelay();
        });
        autoTimer.setRepeats(true);
    }

    /**
     * Calculates and sets the auto timer delay based on current dialogue text length.
     * Reading speed: ~200-250 words per minute, approximately 50ms per character.
     */
    private void updateAutoTimerDelay() {
        String currentText = currentState.get(IDX_TEXT);
        int textLength = (currentText != null) ? currentText.length() : 0;
        
        // Calculate delay: base delay + time per character
        int delay = AUTO_BASE_DELAY_MS + (textLength * AUTO_MS_PER_CHAR);
        
        // Clamp to maximum delay
        delay = Math.min(delay, AUTO_MAX_DELAY_MS);
        
        autoTimer.setDelay(delay);
    }

    public void advanceStory() {
        if (storyIndex >= scriptLines.size()) {
            handleEndOfScript();
            return;
        }

        ScriptData action = scriptLines.get(storyIndex);

        // Skip labels
        if ("Label".equalsIgnoreCase(action.type)) {
            storyIndex++;
            currentState.set(IDX_LINE, Integer.toString(storyIndex));
            advanceStory();
            return;
        }

        Function<ScriptData, Boolean> command = commandMap.get(action.type.toLowerCase());
        if (command != null && command.apply(action)) {
            return;
        }

        storyIndex++;
        currentState.set(IDX_LINE, Integer.toString(storyIndex));
        repaint();
    }

    private void handleEndOfScript() {
        dialogueBox.uploadcontent(null, "THE END (Click to return to title)");
        isGameOver = true;
        loadScript(DEFAULT_SCRIPT);
        storyIndex = 0;
        currentState.set(IDX_SCRIPT, DEFAULT_SCRIPT);
        currentState.set(IDX_LINE, "0");
    }

    private int findLabelIndex(String target) {
        return IntStream.range(0, scriptLines.size())
            .filter(i -> {
                ScriptData action = scriptLines.get(i);
                return "LABEL".equalsIgnoreCase(action.type) && target.equals(action.param);
            })
            .findFirst()
            .orElse(-1);
    }

    public void playTransition(String newBgFile) {
        if (transitionTimer != null && transitionTimer.isRunning()) return;
        
        isFadingOut = true;
        curtainAlpha = 0;
        
        transitionTimer = new Timer(TRANSITION_DELAY_MS, e -> {
            if (isFadingOut) {
                curtainAlpha += TRANSITION_SPEED;
                if (curtainAlpha >= 255) {
                    curtainAlpha = 255;
                    isFadingOut = false;
                    changeBackgroundImage(newBgFile);
                    characterLabel.setIcon(null);
                }
            } else {
                curtainAlpha -= TRANSITION_SPEED;
                if (curtainAlpha <= 0) {
                    curtainAlpha = 0;
                    transitionTimer.stop();
                }
            }
            curtainPanel.repaint();
        });
        transitionTimer.start();
    }

    private void changeBackgroundImage(String filename) {
        Image scaled = loadAndScaleImage(filename, screenWidth, screenHeight);
        if (scaled != null) {
            backgroundLabel.setIcon(new ImageIcon(scaled));
            backgroundLabel.repaint();
            currentState.set(IDX_BG, filename);
        } else {
            System.err.println("Failed to load background: " + filename);
        }
    }

    /**
     * Loads and scales an image with caching.
     */
    private Image loadAndScaleImage(String filename, int width, int height) {
        String cacheKey = filename + "_" + width + "x" + height;
        if (scaledImageCache.containsKey(cacheKey)) {
            return scaledImageCache.get(cacheKey);
        }

        ImageIcon icon = new ImageIcon(filename);
        if (icon.getImageLoadStatus() != MediaTracker.COMPLETE) {
            return null;
        }

        Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        scaledImageCache.put(cacheKey, scaled);
        return scaled;
    }

    private void setupChoices(String optData) {
       choicePanel.showChoices(optData, (targetLabel) -> {
        
        // 這裡是回調邏輯 (Callback Logic)
        int targetIndex = findLabelIndex(targetLabel);
        
        if (targetIndex != -1) {
            storyIndex = targetIndex;
            currentState.set(IDX_LINE, Integer.toString(storyIndex));
            advanceStory(); // 繼續推進劇情
        } else {
            System.err.println("找不到跳轉標籤: " + targetLabel);
        }
    });
    }

    public String getSavePreview(int slot) {
        ArrayList<String> allSaves = loadSavedGames();
        int index = slot - 1;

        if (index >= allSaves.size() || EMPTY_SLOT.equals(allSaves.get(index))) {
            return "---- Empty ----";
        }

        String[] parts = allSaves.get(index).split(SAVE_SEPARATOR, -1);
        if (parts.length > IDX_TEXT) {
            String text = parts[IDX_TEXT];
            return text.length() > PREVIEW_MAX_LENGTH 
                ? text.substring(0, PREVIEW_MAX_LENGTH) + "..." 
                : text;
        }

        return "Unknown data";
    }

    /**
     * Loads saved games from file with improved error handling.
     */
    public ArrayList<String> loadSavedGames() {
        ArrayList<String> lines = new ArrayList<>();
        File file = new File(SAVE_FILE);

        if (!file.exists()) {
            System.out.println("[System] No save file found.");
            return lines;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("[System] Error reading save file");
            e.printStackTrace();
        }

        System.out.println("[System] Loaded " + lines.size() + " save(s).");
        return lines;
    }

    public void saveGame(int slot) {
        int index = slot - 1;
        System.out.println("[Save] Saving to slot " + slot + "...");

        try {
            String newSaveLine = currentState.stream()
                .map(data -> data == null ? "null" : data)
                .collect(Collectors.joining(SAVE_SEPARATOR));

            ArrayList<String> allSaves = loadSavedGames();

            // Pad with empty slots if needed
            while (allSaves.size() <= index) {
                allSaves.add(EMPTY_SLOT);
            }

            allSaves.set(index, newSaveLine);

            // Write all saves with try-with-resources
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(SAVE_FILE, false), StandardCharsets.UTF_8))) {
                for (String line : allSaves) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            System.out.println("[Save] Save successful!");

        } catch (IOException e) {
            System.err.println("[Save] Save failed!");
            e.printStackTrace();
        }
    }

    public void loadGame(int slot) {
        int index = slot - 1;
        System.out.println("\n[Load] Loading slot " + slot + "...");

        ArrayList<String> allSaves = loadSavedGames();

        if (index >= allSaves.size()) {
            System.err.println("[Error] Slot " + slot + " is empty.");
            return;
        }

        String saveLine = allSaves.get(index);
        if (EMPTY_SLOT.equals(saveLine)) {
            System.out.println("[Load] Slot is empty.");
            return;
        }

        try {
            String[] parts = saveLine.split(SAVE_SEPARATOR, -1);
            currentState.clear();
            for (String part : parts) {
                currentState.add("null".equals(part) ? null : part);
            }

            // Restore script
            String savedScript = currentState.get(IDX_SCRIPT);
            loadScript(savedScript);

            // Restore position (subtract 1 because advanceStory increments)
            int savedIndex = Integer.parseInt(currentState.get(IDX_LINE));
            storyIndex = Math.max(0, savedIndex - 1);

            // Restore visuals
            changeBackgroundImage(currentState.get(IDX_BG));

            String charName = currentState.get(IDX_CHAR);
            String charMood = currentState.get(IDX_MOOD);
            if (isNullOrEmpty(charName)) {
                characterLabel.setIcon(null);
            } else {
                setCharacterEmotion(charName, charMood);
            }

            // Restore UI state
            titlePanel.setVisible(false);
            dialogueBox.setVisible(true);
            isGameOver = false;

            advanceStory();
            System.out.println("[Load] Load complete!");

        } catch (Exception e) {
            System.err.println("[Error] Save data corrupted");
            e.printStackTrace();
        }
    }

    public void setAutoMode(boolean enabled) {
        isAutoMode = enabled;
        if (enabled) {
            autoTimer.start();
            if (autoModeIndicator != null) {
                autoModeIndicator.setVisible(true);
            }
            if (dialogueBox != null) {
                dialogueBox.setAutoModeActive(true);
            }
            System.out.println("[Auto] Auto mode ON");
        } else {
            autoTimer.stop();
            if (autoModeIndicator != null) {
                autoModeIndicator.setVisible(false);
            }
            if (dialogueBox != null) {
                dialogueBox.setAutoModeActive(false);
            }
            System.out.println("[Auto] Auto mode OFF");
        }
    }

    public void setSkipMode(boolean enabled) {
        isSkipMode = enabled;
        if (skipModeIndicator != null) {
            skipModeIndicator.setVisible(enabled);
        }
        if (enabled) {
            // Skip current typing
            if (dialogueBox.isTyping()) {
                dialogueBox.skipTyping();
            }
            System.out.println("[Skip] Skip mode ON");
        } else {
            System.out.println("[Skip] Skip mode OFF");
        }
    }

    public void toggleAutoMode() {
        // Don't allow toggle during choices or game over
        if (choicePanel.isVisible() || isGameOver || titlePanel.isVisible()) {
            return;
        }
        setAutoMode(!isAutoMode);
    }

    public boolean isAutoMode() {
        return isAutoMode;
    }

    public boolean isSkipMode() {
        return isSkipMode;
    }

    public void showSettings() {
        if (settingsPanel != null) {
            settingsPanel.setVisible(true);
        }
    }

    private static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty() || "null".equals(str);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new YZCiallo().setVisible(true));
    }

    public void Variable_op(String operation, String key, int v, String targetLabel){
        int current_var = gameVariable.getOrDefault(key,0);
        switch (operation){
            case "Add":
            case "ADD":
                gameVariable.put(key,current_var+v);
                storyIndex++;
                currentState.set(IDX_LINE, Integer.toString(storyIndex));
                break;
            case "View":
                if (current_var >= v){
                    int targetIndex = findLabelIndex(targetLabel);
                    if (targetIndex != -1){
                        storyIndex = targetIndex;
                        currentState.set(IDX_LINE, Integer.toString(storyIndex));
                        advanceStory(); 
                    } else{
                        storyIndex++;
                        currentState.set(IDX_LINE, Integer.toString(storyIndex));
                        advanceStory();
                    }
                } else {
                    storyIndex++;
                    currentState.set(IDX_LINE, Integer.toString(storyIndex));
                    advanceStory();
                }
                break;
        }
    }
}
