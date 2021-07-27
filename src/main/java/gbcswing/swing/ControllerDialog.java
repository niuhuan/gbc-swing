package gbcswing.swing;

import gbcswing.config.SwingConfig;
import gbcswing.language.Language;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class ControllerDialog extends JDialog {

    private BufferedImage img;
    private TextField[] tf;

    public ControllerDialog(JFrame jFrame) {
        super(jFrame, false);
        try {
            InputStream is = this.getClass().getResourceAsStream("/gbcswing/swing/controller.png");
            img = ImageIO.read(is);
            is.close();
            this.setResizable(false);
            this.setSize(img.getWidth(), img.getHeight());
            JPanel p = new JPanel() {
                public void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.drawImage(img, 0, 0, this);
                    g.drawString(Language.KEY_CODES, 20, 20);
                }
            };
            this.setContentPane(p);
            p.setLayout(null);
            tf = new TextField[8];
            for (int i = 0; i < 8; i++) {
                final TextField tx = new TextField();
                tf[i] = tx;
                p.add(tf[i]);
                tx.addKeyListener(
                        new KeyAdapter() {
                            @Override
                            public void keyReleased(KeyEvent e) {
                                tx.setText(String.valueOf(e.getKeyCode()));
                            }
                        }
                );
            }
            tf[0].setBounds(140, 131, 50, 40);
            tf[1].setBounds(27, 130, 50, 40);
            tf[2].setBounds(87, 76, 50, 40);
            tf[3].setBounds(84, 190, 50, 40);
            tf[4].setBounds(688, 152, 50, 40);
            tf[5].setBounds(619, 226, 50, 40);
            tf[6].setBounds(306, 410, 50, 40);
            tf[7].setBounds(426, 407, 50, 40);

            JButton okButton = new JButton(Language.OK);
            p.add(okButton);
            okButton.setVisible(true);
            okButton.setBounds(this.getWidth() - 100, this.getHeight() - 100, 50, 35);
            okButton.setSize(50, 35);
            okButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (int i = 0; i < 8; i++) {
                        SwingConfig.keyCodes[i] = Integer.parseInt(tf[i].getText());
                    }
                    ControllerDialog.this.setVisible(false);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            this.setLocationRelativeTo(null);
            for (int i = 0; i < 8; i++)
                tf[i].setText(String.valueOf(SwingConfig.keyCodes[i]));
        }
        super.setVisible(b);
    }

}
