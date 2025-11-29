import java.awt.*;
import java.util.function.Consumer;
import javax.swing.*;

public class ChoicePanel extends  JPanel {
    
    public ChoicePanel (int width, int height){

        int panelWidth = width/3;
        int panelHeight = height/2;

        this.setLayout(new GridLayout(0, 1, 0, 15));
        this.setBounds((width-panelWidth)/2,(height-panelHeight)/2,panelWidth,panelHeight);
        this.setOpaque(false);
        this.setVisible(false);
    }

    public void showChoices(String optionsData, Consumer<String> onChoiceSelected){
        this.removeAll();

        System.out.println("ChoicePanel 被呼叫！數據: " + optionsData);
        
        String[] Options = optionsData.split(",");

       for (String option : Options) {
            String[] parts = option.split(":");
            if (parts.length < 2){
                System.out.println("格式錯誤 (跳過): " + option);
                continue; 
            }

            String btnText = parts[0].trim();
            String targetLabel = parts[1].trim();

            // 創建按鈕
            JButton btn = createStyledButton(btnText);

            btn.addActionListener(e -> {
                this.setEnabled(false);
                this.setVisible(false);
                onChoiceSelected.accept(targetLabel); 
                this.setEnabled(true);
            });

            this.add(btn);
        }

        this.revalidate();
        this.repaint();
        this.setVisible(true);
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Microsoft YaHei", Font.BOLD, 22));
        btn.setBackground(new Color(255, 255, 255, 220));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 100), 2),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        return btn;
    }
}
