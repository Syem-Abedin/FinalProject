import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Super Mario Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(960, 579);
        frame.setLocationRelativeTo(null);

        // create a DisplayPanel object
        DisplayPanel panel = new DisplayPanel();

        // add it to the frame
        frame.add(panel);

        // call setVisible after everything else
        frame.setVisible(true);
    }
}
