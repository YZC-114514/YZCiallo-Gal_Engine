import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.charset.*;
import java.util.ArrayList;
import javax.swing.*;

/**
 * A skeleton UI for a Visual Novel / Galgame.
 * * KEY CONCEPTS:
 * 1. JLayeredPane: This is the most important part. It allows us to stack images.
 * Background goes at the back, Character in the middle, Text Box at the front.
 * 2. Absolute Positioning (setBounds): Unlike standard apps, games usually use fixed
 * pixel coordinates so elements stay exactly where artists designed them.
 */
public class GalUI extends JFrame {


    // Screen dimensions
    private int WIDTH;
    private int HEIGHT;
    private int cha_height;
    private int cha_width;
    private ArrayList<String> scriptLines = new ArrayList<>();
    private int curtainAlpha=0;
    private Timer transitionTimer;
    private boolean GameOver;
    private ArrayList<String> Current = new ArrayList<>();
    private final int IDX_Line = 0;
    private final int IDX_BG = 1;
    private final int IDX_CHAR = 2;
    private final int IDX_MOOD =3 ;
    private final int IDX_SCRIPT =4;


    // Components
    private JLayeredPane layers;       // The container that holds stacked layers
    private JLabel backgroundLabel;    // Represents the background image
    private JPanel curtainPanel;
    private JLabel characterLabel;     // Represents the character sprite
    private DialoguePanel dialogueBox;        // The translucent box for text
    private TitleScreen titlePanel;
    private JPanel choicePanel;


    public GalUI() {

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension ScreenSize = toolkit.getScreenSize();
        this.WIDTH = ScreenSize.width;
        this.HEIGHT = ScreenSize.height;

        Current.add("0");
        Current.add("Background");
        Current.add(null);
        Current.add(null);
        Current.add("Chapter1_1.txt");

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

    }

    public void loopScript(String filename) {
        scriptLines.clear();
            try {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filename),StandardCharsets.UTF_8)
                );
                String line;
                while ((line = reader.readLine()) != null){

                    if (!line.trim().isEmpty()){
                        scriptLines.add(line);
                    }
                }
                reader.close();
                System.out.println("劇本讀取成功，共 " + scriptLines.size() + " 行");
                
            } catch (IOException e) {
                System.out.println("讀取劇本失敗！請檢查 Script.txt 是否存在");
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
    }

    

    public void initTitleLayer(){
        titlePanel = new TitleScreen(WIDTH, HEIGHT, this);
        layers.add(titlePanel,Integer.valueOf(2000));

    }

    public void startGame(){
        titlePanel.setVisible(false);
        dialogueBox.setVisible(true);
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
            String curt_line = scriptLines.get(storyIndex);

            if(curt_line.startsWith("#")){
                storyIndex++;
                Current.set(IDX_Line,Integer.toString(storyIndex));
                advanceStory();
                return;
            }
            
            String[] parts = curt_line.split("\\|");

            if(parts.length>=2){
                String Type =parts[0];
                String param =parts[1];
                String text = (parts.length>=3) ? parts[2] : ""; // if >=3 then text =parts[2]
                
                switch (Type) {
                    case "BG":
                    case "bg":
                        playTransition(param);
                        dialogueBox.clearstage();
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
                    default:
                        dialogueBox.uploadcontent(Type, text);
                        setCharacterEmotion(Type,param);
                        break;
                }
            }
            storyIndex++;
            Current.set(IDX_Line,Integer.toString(storyIndex));
            repaint();
            printCurrentState();
        } else {
            dialogueBox.uploadcontent(null, "THE END(點擊鼠標返回主頁)");
            GameOver = true;
            
        }
        // -----------------------------
    }

    private int findLineIndex(String target){
        String ScrnGet = "#"+ target;

        for (int i =0; i<scriptLines.size();i++){
            String line = scriptLines.get(i).trim();
            if (line.equals(ScrnGet)){
                System.out.println("找到了，在第 "+i+" 行");
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
        System.out.println("❌ 錯誤：Current 尚未初始化或長度不足！");
        return;
    }

    System.out.println("IDX_Line (行號) : " + Current.get(IDX_Line));
    System.out.println("IDX_BG   (背景) : " + Current.get(IDX_BG));
    System.out.println("IDX_CHAR (名字) : " + Current.get(IDX_CHAR));
    System.out.println("IDX_MOOD (表情) : " + Current.get(IDX_MOOD));
    System.out.println("IDX_SCRIPT（Chapter）：" +Current.get(IDX_SCRIPT));
    System.out.println("===========================================");
}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new GalUI().setVisible(true);
        });
    }
}