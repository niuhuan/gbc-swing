package gbcswing;

import gbcswing.swing.MainFrame;

public class Main {

    public static void main(String[] args) {
        // This property takes effect in java11, does not take effect in java1.6
        // The effect is move JMenuBar to mac osx top bar
        // Side effect is can't pause when add this property if on osx, because JPanel focus not lost and gained
        System.setProperty("apple.laf.useScreenMenuBar","true");
        // look and feel
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Frame
        MainFrame mf = new MainFrame();
        mf.setVisible(true);
    }
}
