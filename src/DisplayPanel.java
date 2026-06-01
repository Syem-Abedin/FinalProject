import org.w3c.dom.css.Rect;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class DisplayPanel extends JPanel implements MouseListener, KeyListener {

    private int score;
    private boolean yellowColor;

    private int marioX;
    private int marioY;

    private BufferedImage background;
    private BufferedImage mario;

    private boolean up, down, left, right;

    private double MarioXVelocity = 5;
    private double MarioYVelocity = -10;

    private int MarioXAcceleration = 3;
    private int MarioYAcceleration = 5;

    private Rectangle marioHitbox = new Rectangle(0, 0, 48, 80);

    ArrayList<Rectangle> Platforms = new ArrayList<>();
    private final SpeedPlatform platform1 = new SpeedPlatform(526, 440, 112, 11);

    private Rectangle floor = new Rectangle(0, 515, 960, 1);

    private boolean onplatform;

    private double grav = 0.5;

    private Point check = new Point(0,0);
    private Point check2 = new Point(0,0);

    public DisplayPanel() {

        score = 0;
        yellowColor = true;

        marioX = 50;
        marioY = 335;

        Platforms.add(platform1);


        try {
            background = ImageIO.read(new File("src/background.png"));
            mario = ImageIO.read(new File("src/marioright.png"));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        addMouseListener(this);
        addKeyListener(this);

        setFocusable(true);
        requestFocusInWindow();

        Timer timer = new Timer(16, e -> {
            int prevX = marioX;
            int prevY = marioY;
            MarioXVelocity = 5;

            marioHitbox.setBounds(marioX, marioY, 48, 80);

// ---------------- GRAVITY ----------------
            MarioYVelocity += grav;

            if (down) {
                MarioYVelocity += 1;
            }
            if (up && onplatform) {
                MarioYVelocity = -10;
                onplatform = false;
                for (Rectangle thingy : Platforms) {
                    if(thingy.intersects(new Rectangle(marioX, marioY + 1, 48, 80))) {
                        if(thingy instanceof SpeedPlatform) {
                            MarioYVelocity = -20;
                        }
                    }
                }
            }

            // apply vertical movement
            marioY += (int) MarioYVelocity;

            marioHitbox.setBounds(marioX, marioY, 48, 80);

            onplatform = false;

// ---------------- Floor Collision ----------------
            if (marioHitbox.intersects(floor)) {

                marioY = floor.y - 80;
                MarioYVelocity = 0;
                onplatform = true;

                marioHitbox.setBounds(marioX, marioY, 48, 80);
            }

// ---------------- Platform Collision ----------------
            if (MarioYVelocity >= 0) {
                for (Rectangle thingy : Platforms) {
                    if (thingy.intersects(new Rectangle(marioX, marioY + 1, 48, 80))) {

                        marioY = thingy.y - 80;
                        MarioYVelocity = 0;
                        onplatform = true;
                        if (thingy instanceof SpeedPlatform) {
                            MarioXVelocity = 10;
                        }
                        marioHitbox.setBounds(marioX, marioY, 48, 80);
                    }
                }

                    if (left) {
                        marioX -= (int) MarioXVelocity;
                    }
                    if (right) {
                        marioX += (int) MarioXVelocity;
                    }

                marioHitbox.setBounds(marioX, marioY, 48, 80);
                for (Rectangle dih : Platforms) {
                    if (marioHitbox.intersects(dih) || marioHitbox.intersects(floor)) {
                        marioX = prevX;
                    }
                }
            }
            repaint();
        });

        timer.start();
    }

// -------------------- Score Display--------------------
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.drawImage(background, 0, 0, null);
        g.drawImage(mario, marioX, marioY, null);

        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(yellowColor ? Color.YELLOW : Color.BLACK);
        g.drawString("Score: " + score, 50, 30);
    }

// -------------------- Recoloring --------------------
    @Override public void mouseClicked(MouseEvent e) {
    }
    @Override public void mousePressed(MouseEvent e) {
        int i;
        if(e.getButton() == MouseEvent.BUTTON3) {
            Platforms.add(new Rectangle(e.getX(), e.getY(), 1, 1));
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            yellowColor = !yellowColor;
            repaint();
        }
    }

    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

// -------------------- Keyboard --------------------
    @Override public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {

        int keyCode = e.getKeyCode();

        if (keyCode == KeyEvent.VK_A) {
            left = true;
            try {
                mario = ImageIO.read(new File("src/marioleft.png"));
            } catch (IOException ignored) {}
        }

        if (keyCode == KeyEvent.VK_D) {
            right = true;
            try {
                mario = ImageIO.read(new File("src/marioright.png"));
            } catch (IOException ignored) {}
        }

        if (keyCode == KeyEvent.VK_W) {
            up = true;
        }

        if (keyCode == KeyEvent.VK_S) {
            down = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

        int keyCode = e.getKeyCode();

        if (keyCode == KeyEvent.VK_A) left = false;
        if (keyCode == KeyEvent.VK_D) right = false;
        if (keyCode == KeyEvent.VK_W) up = false;
        if (keyCode == KeyEvent.VK_S) down = false;
    }

}