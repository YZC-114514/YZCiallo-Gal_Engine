import java.awt.*;
import javax.swing.*;


public class DialoguePanel extends JPanel{
    private JLabel NameLabel;
    private JTextArea DialogueText;

    public DialoguePanel(int width,int height) {
        // The panel holding the text

        this.setOpaque(false);
        this.setLayout(null);
        this.setBackground(new Color(0, 0, 0, 150));
        int dialog_height = (int)height/4;
        this.setBounds(0, height-dialog_height, width, dialog_height);

        // Name Tag
        int nameFontSize = height/30;
        NameLabel = new JLabel("Name");
        NameLabel.setForeground(Color.YELLOW);
        NameLabel.setFont(new Font("SansSerif", Font.BOLD, nameFontSize));
        NameLabel.setBounds(20, 10, 300, 50);
        this.add(NameLabel);

        // Dialogue Text Area
        int textFontSize = height/35;
        DialogueText = new JTextArea();
        DialogueText.setBounds(20, 65, 600, 80);
        DialogueText.setBackground(new Color(0, 0, 0, 0)); // Transparent
        DialogueText.setOpaque(false);
        DialogueText.setForeground(Color.WHITE);
        DialogueText.setFont(new Font("Serif", Font.PLAIN, textFontSize));
        DialogueText.setLineWrap(true);
        DialogueText.setWrapStyleWord(true);
        DialogueText.setEditable(false); // User cannot type here
        this.add(DialogueText);
    }

    @Override
    protected void paintComponent(Graphics g){
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }

    public void uploadcontent(String name, String text){
        NameLabel.setText(name);
        DialogueText.setText(text);
    }
    
    public void clearstage(){
        NameLabel.setText(null);
        DialogueText.setText(null);
    }        

}
