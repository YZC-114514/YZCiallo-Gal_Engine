import java.awt.*;
import javax.swing.*;

public class TitleScreen extends JPanel {

    public TitleScreen(int width, int height, GalUI game){
        this.setLayout(null);
        this.setBounds(0,0,width,height);
        this.setBackground(Color.BLACK);

        JButton startButton = new JButton("開始遊戲（Start）");
        startButton.setBounds(width/2 -100, height/2, 200, 50);

        startButton.addActionListener(e ->{
            game.startGame();
        });
         this.add(startButton);

        JButton loadButton = new JButton("讀取存檔");
        loadButton.setBounds(width/2 - 100, height/2+50 +130, 200, 50);
        loadButton.addActionListener(e->{
            game.openLoadMenu();
        });
        this.add(loadButton);
        

    }
    
}
