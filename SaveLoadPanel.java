import java.awt.*;
import javax.swing.*;


public class SaveLoadPanel extends JPanel {
    
    private GalUI game;
    private JLabel titleLabel;
    private JButton[] slotButtons;
    private JButton closeButton;
    private boolean isSaveMode; // true=存檔, false=讀檔
    private final int SLOT_COUNT = 3; // 存檔位數量

    public SaveLoadPanel(int width, int height, GalUI game) {
        this.game = game;
        this.setLayout(null);
        this.setBounds(0, 0, width, height);
        this.setBackground(new Color(0, 0, 0, 200)); // 深色半透明背景
        this.setVisible(false); // 默認隱藏

        // 1. 標題文字
        titleLabel = new JLabel("保存進度", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 40));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBounds(0, 50, width, 60);
        this.add(titleLabel);

        // 2. 創建存檔位按鈕
        slotButtons = new JButton[SLOT_COUNT];
        int btnWidth = 600;
        int btnHeight = 80;
        int startY = 150;

        for (int i = 0; i < SLOT_COUNT; i++) {
            int slotIndex = i + 1; // Slot 1, 2, 3...
            
            slotButtons[i] = new JButton();
            slotButtons[i].setFont(new Font("Microsoft YaHei", Font.PLAIN, 20));
            slotButtons[i].setBounds((width - btnWidth)/2, startY + (i * (btnHeight + 20)), btnWidth, btnHeight);
            
            // 點擊事件
            slotButtons[i].addActionListener(e -> onSlotClicked(slotIndex));
            
            this.add(slotButtons[i]);
        }

        // 3. 關閉按鈕
        closeButton = new JButton("返回 (Close)");
        closeButton.setBounds((width - 200)/2, height - 100, 200, 50);
        closeButton.addActionListener(e -> this.setVisible(false));
        this.add(closeButton);
    }

    /**
     * 打開面板的方法
     * @param isSave true為存檔模式，false為讀檔模式
     */
    public void showPanel(boolean isSave) {
        this.isSaveMode = isSave;
        this.setVisible(true);
        
        // 根據模式改變標題
        if (isSaveMode) {
            titleLabel.setText("保存進度 (Save)");
            titleLabel.setForeground(Color.GREEN);
        } else {
            titleLabel.setText("讀取進度 (Load)");
            titleLabel.setForeground(Color.CYAN);
        }

        // 刷新按鈕上的預覽文字
        refreshSlots();
    }

    // 刷新所有按鈕的顯示文字
    private void refreshSlots() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            int slot = i + 1;
            // 調用 GalUI 的方法獲取預覽文本
            String preview = game.getSavePreview(slot);
            
            if (preview.equals("---- 空 ----")) {
                slotButtons[i].setText("存檔 " + slot + " : [空]");
            } else {
                // 顯示劇情摘要
                slotButtons[i].setText("<html>存檔 " + slot + "<br/><font size=4 color='gray'>" + preview + "</font></html>");
            }
        }
    }

    // 按鈕點擊邏輯
    private void onSlotClicked(int slot) {
        System.out.println("🔴 [DEBUG] 點擊了存檔位 Slot: " + slot);
        if (isSaveMode) {
            // 執行存檔
            game.saveGame(slot);
            // 存完後立刻刷新按鈕文字，顯示剛存好的內容
            refreshSlots(); 
        } else {
            // 執行讀檔
            game.loadGame(slot);
            // 讀檔成功後關閉面板
            this.setVisible(false); 
        }
    }
}