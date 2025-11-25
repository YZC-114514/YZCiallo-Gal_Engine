import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.charset.*;
import java.util.ArrayList;
import javax.swing.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonSyntaxException;


public class GalUI extends JFrame {


    // Screen dimensions
    private int WIDTH;
    private int HEIGHT;
    private int cha_height;
    private int cha_width;
    private ArrayList<ScriptData> scriptLines = new ArrayList<>();
    private int curtainAlpha=0;
    private Timer transitionTimer;
    private boolean GameOver;
    private ArrayList<String> Current = new ArrayList<>();
    private final int IDX_Line = 0;
    private final int IDX_BG = 1;
    private final int IDX_CHAR = 2;
    private final int IDX_MOOD =3 ;
    private final int IDX_SCRIPT =4;
    private final int IDX_TEXT = 5;
    private final String load_sep ="###";
    private ArrayList<String> BkLog = new ArrayList<>();

    // Components
    private JLayeredPane layers;       // The container that holds stacked layers
    private JLabel backgroundLabel;    // Represents the background image
    private JPanel curtainPanel;
    private JLabel characterLabel;     // Represents the character sprite
    private DialoguePanel dialogueBox;        // The translucent box for text
    private TitleScreen titlePanel;
    private JPanel choicePanel;
    private SaveLoadPanel saveLoadPanel;


    public GalUI() {

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension ScreenSize = toolkit.getScreenSize();
        this.WIDTH = ScreenSize.width;
        this.HEIGHT = ScreenSize.height;

        Current.add("0");
        Current.add("Background");
        Current.add(null);
        Current.add(null);
        Current.add("Chapter1_1.json");
        Current.add("");

        BkLog.add(null);

        // 1. Basic Window Setup
        setTitle("My Java Galgame Engine");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null); // We use null layout to control exact pixel positions
        setLocationRelativeTo(null); // Center on screen
        setResizable(false); // Games usually have fixed sizes

        // 2. Initialize the Layered Pane
        // JLayeredPane lets us put the character ON TOP of the background.
        layers = new JLayeredPane();
        layers.setBounds(0, 0, WIDTH, HEIGHT);
        add(layers);

        // 3. Create The Layers (Function calls to keep code clean)
        initBackgroundLayer();
        initCharacterLayer();
        initUILayer();
        initTitleLayer();
        loopScript(Current.get(IDX_SCRIPT));
        advanceStory();

        this.setFocusable(true);
        this.requestFocusInWindow();

        this.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e){
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_S && !titlePanel.isVisible()){
                    saveLoadPanel.showPanel(true);
                }
            }
        });

    }
    
    public void openLoadMenu() {
        saveLoadPanel.showPanel(false); // false = 讀檔模式
    }


    public void loopScript(String filename) {
        scriptLines.clear();

       
            try {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filename),StandardCharsets.UTF_8)
                );

                Gson gson = new Gson();
                java.lang.reflect.Type listType = new TypeToken<ArrayList<ScriptData>>(){}.getType();
                scriptLines = gson.fromJson(reader, listType);   
                reader.close();
                System.out.println("劇本讀取成功，共 " + scriptLines.size() + " 行");
                
            } catch (FileNotFoundException e) {
                System.out.println("錯誤：找不到劇本文件 [" + filename + "]");
            } catch (JsonSyntaxException e) {
                System.out.println("錯誤：JSON 語法錯誤！請檢查逗號、括號是否正確。");
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    /**
     * Layer 0: The Background
     * In a real game, this would be a JLabel containing an ImageIcon.
     */
    private void initBackgroundLayer() {
        ImageIcon bk_img = new ImageIcon("Background.jpg");
        Image bk_converted = bk_img.getImage().getScaledInstance(WIDTH, HEIGHT, Image.SCALE_SMOOTH);

        backgroundLabel = new JLabel(new ImageIcon(bk_converted));
        backgroundLabel.setBounds(0,0,WIDTH,HEIGHT);
        layers.add(backgroundLabel,JLayeredPane.DEFAULT_LAYER);

        curtainPanel = new JPanel(){
            @Override
            protected void paintComponent(Graphics g){
                g.setColor(new Color(0,0,0,curtainAlpha));
                g.fillRect(0,0,getWidth(),getHeight());
            }
        };

        curtainPanel.setOpaque(false);
        curtainPanel.setBounds(0,0,WIDTH,HEIGHT);
        layers.add(curtainPanel,Integer.valueOf(1000));

    }

    /**
     * Layer 1: The Character
     * This sits on top of the background.
     */
    private void initCharacterLayer() 
    { 
        cha_height = (int) HEIGHT*3/4;
        cha_width = cha_height/2;       
        
        characterLabel = new JLabel();
        // Position: Centered horizontally, standing at bottom
        characterLabel.setBounds(WIDTH/4,HEIGHT-cha_height,cha_width,cha_height); 
        // Add to the middle layer (PALETTE_LAYER)
        layers.add(characterLabel, JLayeredPane.PALETTE_LAYER);
    }

    /**
     * Layer 2: The UI (Text Box)
     * This sits on top of everything.
     */
    public void initUILayer() {

        dialogueBox = new DialoguePanel(WIDTH,HEIGHT);

        layers.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e){
                if (titlePanel.isVisible()) return;
                switch (e.getButton()) {
                    case MouseEvent.BUTTON1 :
                        if(GameOver){
                            returntitle();
                            GameOver=false;
                        } else {
                            if (!dialogueBox.isVisible()){
                                dialogueBox.setVisible(true);
                            } else{
                                advanceStory();
                                repaint();
                            }
                        }                       
                        break;

                    case MouseEvent.BUTTON3:
                        boolean isVisible = dialogueBox.isVisible();
                        dialogueBox.setVisible(!isVisible);
                        layers.repaint();
                        break;

                }
            }
        });

        //choice buttons
        choicePanel = new JPanel();
        int choice_width = (int) WIDTH/4;
        int choice_height = (int) choice_width/2;
        choicePanel.setBounds((WIDTH-choice_width)/2, (HEIGHT-choice_height)/2, choice_width, choice_height);

        choicePanel.setLayout(new GridLayout(0,1,0,20));
        choicePanel.setOpaque(false);
        choicePanel.setVisible(false);
        layers.add(choicePanel,Integer.valueOf(2000));

        // Add UI to the top layer (MODAL_LAYER)
        layers.add(dialogueBox, JLayeredPane.MODAL_LAYER);
        initSaveLoadLayers();
    }

    private void initSaveLoadLayers(){
        saveLoadPanel =new SaveLoadPanel(WIDTH,HEIGHT,this);
        layers.add(saveLoadPanel,Integer.valueOf(3000));
    }
    

    public void initTitleLayer(){
        titlePanel = new TitleScreen(WIDTH, HEIGHT, this);
        layers.add(titlePanel,Integer.valueOf(2000));

    }

    public void startGame(){
        titlePanel.setVisible(false);
        dialogueBox.setVisible(true);
        String startScript = "Chapter1_1.json"; 
        loopScript(startScript);
        Current.set(IDX_SCRIPT, startScript); 
        Current.set(IDX_Line, "0");
        GameOver =false;
        storyIndex=0;
        advanceStory();
    }

    private void returntitle(){
        titlePanel.setVisible(true);
        dialogueBox.setVisible(false);
    }


     public void setCharacterEmotion(String name,String mood) {
         String filename = name+"_"+mood+".jpg";

         ImageIcon icon= new ImageIcon(filename);

         if(icon.getImageLoadStatus()==MediaTracker.COMPLETE){
            Image scaledImage = icon.getImage().getScaledInstance(cha_width, cha_height, Image.SCALE_SMOOTH);
            characterLabel.setIcon(new ImageIcon(scaledImage));
            
            Current.set(IDX_CHAR,name);
            Current.set(IDX_MOOD,mood);

         } else {
            filename = name+".jpg";
            icon =new ImageIcon(filename);
            Image scaledImage = icon.getImage().getScaledInstance(cha_width, cha_height, Image.SCALE_SMOOTH);
            characterLabel.setIcon(new ImageIcon(scaledImage));

            Current.set(IDX_CHAR,name);
            Current.set(IDX_MOOD,null);
         }
         characterLabel.repaint();
     }


    private int storyIndex = 0; // Helper variable for you

    public void advanceStory() {
        if (storyIndex < scriptLines.size()){
            ScriptData action = scriptLines.get(storyIndex);

            if("Label".equalsIgnoreCase(action.type)){
                storyIndex++;
                Current.set(IDX_Line,Integer.toString(storyIndex));
                advanceStory();
                return;
            }
            
        
                String Type = action.type;
                String param = action.param;
                String text = action.text;
                String name = action.name;
                String mood = action.mood;

                switch (Type) {
                    case "BG":
                    case "bg":
                        playTransition(param);
                        dialogueBox.clearstage();
                        Current.set(IDX_BG,param);
                        break;

                    case "GOTO":
                    case "goto":
                        int targetIndex = findLineIndex(param);
                        if (targetIndex!=-1){
                            storyIndex = targetIndex;
                            Current.set(IDX_Line,Integer.toString(storyIndex));
                            advanceStory();
                        }

                        return;

                    case "CHOICE":
                    case "choice":
                        setupChoices(param);
                        return;

                    case "NEXT_CHAPTER":
                    case "next_chapter":
                            loopScript(param);
                            storyIndex = 0;
                            Current.set(IDX_SCRIPT,param);
                            Current.set(IDX_Line,"0");
                            advanceStory();
                            return;
                    case "DIALOGUE":
                    case "dialogue":
                        dialogueBox.uploadcontent(name, text);
                        setCharacterEmotion(name,mood);
                        if (name == null || name.equals("null") || name.isEmpty()) {
                            BkLog.add(text);
                        } else {
                            BkLog.add("【" + name + "】: " + text);
                        };
                        Current.set(IDX_CHAR,name);
                        Current.set(IDX_MOOD,mood);
                        Current.set(IDX_TEXT,text);
                        break;
                }
            
            storyIndex++;
            Current.set(IDX_Line,Integer.toString(storyIndex));
            repaint();
            printCurrentState();
        } else {
            dialogueBox.uploadcontent(null, "THE END(點擊鼠標返回主頁)");
            GameOver = true;
            loopScript("Chapter1_1.json");
            storyIndex = 0;
            Current.set(IDX_SCRIPT, "Chapter1_1.json");
            Current.set(IDX_Line, "0");
        }
        // -----------------------------
    }

    private int findLineIndex(String target){

        for (int i =0; i<scriptLines.size();i++){
            ScriptData action = scriptLines.get(i);
            if ("LABEL".equalsIgnoreCase(action.type) && target.equals(action.param)){
                return i;
            } 
        }
        System.out.println("錯誤：找不到標籤 [\" + searchTarget + \"]\"");
        return -1;
    }

    public void playTransition(String newBgfile) {
        if (transitionTimer != null && transitionTimer.isRunning()) return;

        transitionTimer = new Timer(10,null);

        transitionTimer.addActionListener(new java.awt.event.ActionListener(){
            boolean isFadingOut = true;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e){
                if (isFadingOut){
                    curtainAlpha +=10;
                    if (curtainAlpha>=255){
                        curtainAlpha=255;
                        isFadingOut=false;
                        changeBackgroundImage(newBgfile);
                        characterLabel.setIcon(null);
                    }
                } else{
                    curtainAlpha-=10;
                    if(curtainAlpha<=0){
                        curtainAlpha=0;
                        transitionTimer.stop();
                    }
                }

                curtainPanel.repaint();
            }
        });
        transitionTimer.start();
    }
    private void changeBackgroundImage(String filename) {
    ImageIcon icon = new ImageIcon(filename);
    if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) {
        // 使用全局變量 WIDTH, HEIGHT
        Image scaled = icon.getImage().getScaledInstance(WIDTH, HEIGHT, Image.SCALE_SMOOTH);
        backgroundLabel.setIcon(new ImageIcon(scaled));
        backgroundLabel.repaint(); // 確保刷新
        Current.set(IDX_BG,filename);

    } else {
        System.out.println("轉場失敗，找不到背景圖: " + filename);
    }
}
private void setupChoices(String optData){
    choicePanel.removeAll();

    String[] options = optData.split(",");
    for (String option :options){
        String[] parts = option.split(":");
        String btnTxt = parts[0];
        String targetLable = parts[1];

        JButton btn = new JButton(btnTxt);

        btn.addActionListener(e ->{
            int targetIndex = findLineIndex(targetLable);
            if (targetIndex !=-1) storyIndex=targetIndex;
            choicePanel.setVisible(false);
            advanceStory();
        });
        choicePanel.add(btn);
    }
    choicePanel.revalidate();
    choicePanel.repaint();

    choicePanel.setVisible(true);

}

private void printCurrentState() {
    System.out.println("\n========== [DEBUG: 當前存檔數據] ==========");
    
    // 防止 Current 還沒初始化就調用導致報錯
    if (Current == null || Current.size() < 4) {
        System.out.println("錯誤：Current 尚未初始化或長度不足！");
        return;
    }

    System.out.println("IDX_Line (行號) : " + Current.get(IDX_Line));
    System.out.println("IDX_BG   (背景) : " + Current.get(IDX_BG));
    System.out.println("IDX_CHAR (名字) : " + Current.get(IDX_CHAR));
    System.out.println("IDX_MOOD (表情) : " + Current.get(IDX_MOOD));
    System.out.println("IDX_SCRIPT（Chapter）：" +Current.get(IDX_SCRIPT));
    System.out.println("===========================================");
}
    public String getSavePreview(int slot) {
        // 1. 把 save.dat 整個讀進來，變成一個列表
        ArrayList<String> allSaves = Load_saved();
        
        // 2. 把存檔位 (1, 2, 3) 轉換成列表索引 (0, 1, 2)
        int index = slot - 1;
        
        // 3. 安全檢查：如果存檔位超過列表長度，或者該行標記為 "EMPTY"
        if (index >= allSaves.size() || allSaves.get(index).equals("EMPTY")) {
            return "---- 空 ----";
        }
        
        // 4. 取出那一行的數據，例如： "5###bg.jpg###Girl###happy###Chap1.json###你好啊"
        String line = allSaves.get(index);
        
        // 5. 切割字串：用 "###" 把數據切開
        // 參數 -1 很重要，這保證即使最後一個是空字串也不會被丟棄
        String[] parts = line.split(load_sep, -1);
        
        // 6. 抓取文字：我們約定好第 6 格 (索引 5) 是對話文本
        // (IDX_TEXT = 5)
        if (parts.length > IDX_TEXT) {
            String text = parts[IDX_TEXT];
            
            // (可選) 美化：如果文字太長，就切斷並加 "..."
            if (text.length() > 15) {
                return text.substring(0, 15) + "...";
            }
            return text;
        }
        
        return "未知數據"; // 如果格式壞了
    }

    public ArrayList<String> Load_saved() {
    ArrayList<String> lines = new ArrayList<>();
    File file = new File("save.dat");

    if (!file.exists()) {
        System.out.println("[System] 無存檔文件，跳過讀取。");
        return lines;
    }

    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
            // 過濾空行，避免讀取錯誤
            if (!line.trim().isEmpty()) {
                lines.add(line);
            }
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
    
    // Debug 輸出：告訴我們讀到了幾行
    System.out.println("[System] 讀取 save.dat 成功，共發現 " + lines.size() + " 個存檔。");
    for(int i=0; i<lines.size(); i++) {
        System.out.println("   -> Index " + i + ": " + lines.get(i));
    }
    
    return lines;
}
 
public void saveGame(int slot) {
    int index = slot - 1;
    System.out.println("[Save] 正在保存到 Slot " + slot + "...");

    try {
        // 1. 準備當前數據字符串
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Current.size(); i++) {
            String data = Current.get(i);
            sb.append(data == null ? "null" : data);
            if (i < Current.size() - 1) sb.append(load_sep);
        }
        String newSaveLine = sb.toString();

        // 2. 讀取舊的所有存檔
        ArrayList<String> allSaves = Load_saved();

        // 3. 如果存檔列表比當前 Slot 短，用 "EMPTY" 補齊
        // 例如：列表長度 0，想存 Index 2 (Slot 3)，需要補 Index 0, 1 為 EMPTY
        while (allSaves.size() <= index) {
            allSaves.add("EMPTY");
        }

        // 4. 替換指定位置的存檔
        allSaves.set(index, newSaveLine);

        // 5. 寫回文件 (覆蓋模式，一次寫入所有行)
        BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream("save.dat", false), StandardCharsets.UTF_8)
        );
        
        for (String line : allSaves) {
            writer.write(line);
            writer.newLine(); // 確保每一筆存檔都換行
        }
        writer.close();
        System.out.println("✅ [Save] 保存成功！");

    } catch (IOException e) {
        e.printStackTrace();
    }
}


public void loadGame(int slot) {
    int index = slot - 1; // Slot 1 對應 Index 0
    System.out.println("\n🚀 [Load] 正在嘗試讀取 Slot " + slot + " (對應 List Index: " + index + ")");

    ArrayList<String> allSaves = Load_saved();

    // 1. 檢查是否有這個存檔
    if (index >= allSaves.size()) {
        System.out.println(" [Error] 讀取失敗！該存檔位沒有數據 (Index Out of Bounds)。");
        return;
    }

    String saveLine = allSaves.get(index);
    System.out.println("🔍 [Debug] 抓取到的原始數據: " + saveLine);

    if (saveLine.equals("EMPTY")) {
        System.out.println("[Load] 該位置是空的 (EMPTY)。");
        return;
    }

    // 2. 切割數據
    String[] parts = saveLine.split(load_sep, -1);
    
    try {
        Current.clear();
        for (String part : parts) {
            Current.add(part.equals("null") ? null : part);
        }

        // --- 核心還原邏輯 ---
        
        // A. 劇本文件
        String savedScript = Current.get(IDX_SCRIPT);
        System.out.println("   -> 還原劇本: " + savedScript);
        loopScript(savedScript);

        // B. 行號 (這是最關鍵的地方)
        int savedIndex = Integer.parseInt(Current.get(IDX_Line));
        System.out.println("   -> 還原行號: " + savedIndex);
        
        // 修正：因為 advanceStory 會 +1，所以我們要設為 savedIndex - 1，
        // 這樣執行 advanceStory 後才會剛好停在 savedIndex
        storyIndex = savedIndex > 0 ? savedIndex - 1 : 0;

        // C. 背景與角色
        changeBackgroundImage(Current.get(IDX_BG));
        
        String charName = Current.get(IDX_CHAR);
        String charMood = Current.get(IDX_MOOD);
        if (charName == null || charName.equals("null") || charName.isEmpty()) {
            characterLabel.setIcon(null);
        } else {
            setCharacterEmotion(charName, charMood);
        }

        // D. 恢復 UI
        if (titlePanel != null) titlePanel.setVisible(false);
        dialogueBox.setVisible(true);
        GameOver = false;

        // E. 推進一步以顯示文字
        advanceStory();
        
        System.out.println("✅ [Load] 讀檔完成！");

    } catch (Exception e) {
        System.out.println("❌ [Error] 存檔數據損壞或解析失敗");
        e.printStackTrace();
    }
}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new GalUI().setVisible(true);
        });
    }
}