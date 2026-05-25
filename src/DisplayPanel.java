import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class DisplayPanel extends JPanel implements MouseListener, KeyListener {
    private int score;
    private boolean yellowColor;
    private int marioX;
    private int marioY;
    private BufferedImage background;
    private BufferedImage mario;
    private boolean up, down, left, right;
    private double MarioXVelocity;
    private double MarioYVelocity;
    private int MarioXAcceleration = 3;
    private int MarioYAcceleration = 5;
    private Rectangle marioHitbox = new Rectangle(marioX, marioY, 48, 80);
    private Rectangle platform1 = new Rectangle(524, 439, 117, 17);
    private boolean onplatform;

    public DisplayPanel() {
        score = 0;
        yellowColor = true;
        marioX = 50;
        marioY = 435;
        try {
            background = ImageIO.read(new File("src/background.png"));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        try {
            mario = ImageIO.read(new File("src/marioright.png"));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        addMouseListener(this);
        addKeyListener(this);
        setFocusable(true); // this line of code + one below makes this panel active for keylistener events
        requestFocusInWindow(); // see comment above

        Timer timerX = new Timer(16, e -> {
            // about 60 FPS
            int prevX = marioX;
            if (left) {
                    marioX -= 5;
                    marioHitbox.setBounds(marioX, marioY, 48, 80);
            }
            if (right) {
                    marioX += 5;
                    marioHitbox.setBounds(marioX, marioY, 48, 80);
            }

            if (marioHitbox.intersects(platform1)) {
                marioX = prevX;
                if (left) {
                    marioX = prevX;
                    marioHitbox.setBounds(marioX, marioY, 48, 80);
                }
                if (right) {
                    marioX = prevX;
                    marioHitbox.setBounds(marioX, marioY, 48, 80);
                }

            }

            repaint();
        });

        timerX.start();
        Timer timerY = new Timer(16, e -> {
            int prevY = marioY;
            if (up) {
                marioY -= 5;
                marioHitbox.setBounds(marioX, marioY, 48, 80);
                onplatform = false;
                System.out.println("false!!!!!");
            }
            if (down) {
                marioY += 5;
                marioHitbox.setBounds(marioX, marioY, 48, 80);
            }


            if (marioHitbox.intersects(platform1)) {
                marioY = prevY;
                if (up) {
                    marioY = prevY;
                    marioHitbox.setBounds(marioX, marioY, 48, 80);
                }
                if (down) {
                    marioY = prevY;
                    marioHitbox.setBounds(marioX, marioY, 48, 80);
                    onplatform = true;
                    System.out.println("true!!!!!");
                }
            }
            repaint();
        });
        timerY.start();
    }

@Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(background, 0, 0, null);
        g.drawImage(mario, marioX, marioY, null);

        // set font and color of text
        g.setFont(new Font("Arial", Font.BOLD, 16));
        if (yellowColor) {
            g.setColor(Color.YELLOW);
        } else {
            g.setColor(Color.BLACK);
        }
        g.drawString("Score: " + score, 50, 30);
    }

    @Override
    public void mouseClicked(MouseEvent e) { } // unimplemented
    // unimplemented because if you move your mouse while clicking, this method isn't
    // called, so mouseReleased is best

    @Override
    public void mousePressed(MouseEvent e) { } // unimplemented

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            yellowColor = !yellowColor;
            repaint();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) { } // unimplemented

    @Override
    public void mouseExited(MouseEvent e) { } // unimplemented

    @Override
    public void keyTyped(KeyEvent e) { } // unimplemented

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (keyCode == KeyEvent.VK_A) {
            left = true;
            try {
                mario = ImageIO.read(new File("src/marioleft.png"));
            } catch (IOException error) { }
        }
        if (keyCode == KeyEvent.VK_D) {
            right = true;
            try {
                mario = ImageIO.read(new File("src/marioright.png"));
            } catch (IOException error) { }
        }
        if (keyCode == KeyEvent.VK_W) {
            up = true;
        }
        if (keyCode == KeyEvent.VK_S) {
            down = true;
        }
        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (keyCode == KeyEvent.VK_A) {
            left = false;
        }
        if (keyCode == KeyEvent.VK_D) {
            right = false;
        }
        if (keyCode == KeyEvent.VK_W) {
            up = false;
        }
        if (keyCode == KeyEvent.VK_S) {
            down = false;
        }
    }  // unimplemented

    public void Kinematics() {

    }
}

