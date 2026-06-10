import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class DisplayPanel extends JPanel implements MouseListener, MouseMotionListener, KeyListener {

    private int score;
    private boolean blueColor;

    private int marioX;
    private int marioY;

    private BufferedImage background;
    private BufferedImage mario;
    private BufferedImage winScreen;

    private boolean up, down, left, right;

    private double MarioXVelocity = 5;
    private double MarioYVelocity = -10;

    private int MarioXAcceleration = 3;
    private int MarioYAcceleration = 5;

    private Graphics GEE;

    private Rectangle marioHitbox = new Rectangle(0, 0, 48, 80);
    private Rectangle extraHitbox = new Rectangle(0, 0, 48, 80);

    ArrayList<Rectangle> Platforms = new ArrayList<>();
    private final SpeedPlatform platform1 = new SpeedPlatform(526, 440, 112, 11);

    // Create floor with two rectangles and a hole in the middle
    private Rectangle floorLeft = new Rectangle(0, 515, 400, 85);     // Left part of floor
    private Rectangle floorRight = new Rectangle(560, 515, 400, 85);   // Right part of floor
    private Rectangle hole = new Rectangle(400, 515, 160, 85);        // Hole in the middle

    private boolean onplatform;
    private boolean draw;
    private Point check = new Point();
    private boolean gameWon = false; // Flag to track win state

    private double grav = 0.5;

    public DisplayPanel() {

        score = 0;
        blueColor = true;

        marioX = 50;
        marioY = 335;

        Platforms.add(platform1);

        try {
            background = ImageIO.read(new File("src/background.png"));
            mario = ImageIO.read(new File("src/marioright.png"));
            // Load win screen image (you'll need to add this file)
            // winScreen = ImageIO.read(new File("src/winscreen.png"));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);

        setFocusable(true);
        requestFocusInWindow();

        Timer timer = new Timer(16, e -> {
            if (gameWon) return; // Stop game logic if won

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

            marioY += (int) MarioYVelocity;

            marioHitbox.setBounds(marioX, marioY, 48, 80);

            onplatform = false;

            // ---------------- Floor Collision ----------------
            // Check collision with both floor parts
            if (marioHitbox.intersects(floorLeft) || marioHitbox.intersects(floorRight)) {
                marioY = (marioHitbox.intersects(floorLeft) ? floorLeft : floorRight).y - 80;
                MarioYVelocity = 0;
                onplatform = true;
                marioHitbox.setBounds(marioX, marioY, 48, 80);
            }

            // ---------------- Platform Collision ----------------
            for (Rectangle thingy : Platforms) {
                if (thingy.intersects(new Rectangle(marioX, marioY + 1, 48, 80))) {
                    // Check if Mario was moving downward
                    if (MarioYVelocity > 0) {
                        marioY = thingy.y - 80;
                        MarioYVelocity = 0;
                        onplatform = true;
                        if (thingy instanceof SpeedPlatform) {
                            MarioXVelocity = 10;
                        }
                    } else if (MarioYVelocity < 0) {
                        // Head collision - bounce up
                        marioY = thingy.y + thingy.height;
                        MarioYVelocity *= -1;
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

            // Check for collisions after movement
            marioHitbox.setBounds(marioX, marioY, 48, 80);

            // Prevent sticking by checking if we're still colliding
            boolean stillColliding = false;
            for (Rectangle dih : Platforms) {
                if (marioHitbox.intersects(dih)) {
                    stillColliding = true;
                    break;
                }
            }
            // Check floor collision
            if (marioHitbox.intersects(floorLeft) || marioHitbox.intersects(floorRight)) {
                stillColliding = true;
            }

            // If still colliding, revert position
            if (stillColliding) {
                marioX = prevX;
                marioHitbox.setBounds(marioX, marioY, 48, 80);
            }

            // Check win condition - if Mario hits platform1
            if (marioHitbox.intersects(platform1)) {
                gameWon = true;
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

        // Draw win screen if game is won
        if (gameWon) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            g.drawString("YOU WIN!", 350, 250);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Congratulations! You've completed the level.", 250, 320);
            return;
        }

        // Draw Mario and platforms normally
        g.drawImage(mario, marioX, marioY, null);

        g.setColor(Color.BLUE);
        for (Rectangle r : Platforms) {
            if (r != Platforms.get(0)) {
                g.fillRect(r.x, r.y, 10, 10);
            }
        }

        // Draw floor as two rectangles with a hole
        g.setColor(Color.GREEN);
        g.fillRect(floorLeft.x, floorLeft.y, floorLeft.width, floorLeft.height);
        g.fillRect(floorRight.x, floorRight.y, floorRight.width, floorRight.height);

        // Draw hole (as a gap)
        g.setColor(Color.BLACK);
        g.fillRect(hole.x, hole.y, hole.width, hole.height);

        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(blueColor ? Color.BLUE : Color.BLACK);
        g.drawString("Score: " + score, 50, 30);
    }

    //ts loops
    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            check.setLocation(e.getX(), e.getY());
            if (!marioHitbox.contains(check)) {
                // Check for overlap before adding
                boolean overlaps = false;
                Rectangle newRect = new Rectangle(e.getX(), e.getY(), 10, 10);
                for (Rectangle existing : Platforms) {
                    if (existing.intersects(newRect)) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps && !floorLeft.intersects(newRect) && !floorRight.intersects(newRect)) {
                    Platforms.add(new Rectangle(e.getX(), e.getY(), 10, 10));
                    repaint();
                }
            }
        }
    }

    //ts loops
    @Override
    public void mouseDragged(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            check.setLocation(e.getX(), e.getY());
            if (!marioHitbox.contains(check)) {
                boolean overlaps = false;
                Rectangle newRect = new Rectangle(e.getX(), e.getY(), 10, 10);
                for (Rectangle existing : Platforms) {
                    if (existing.intersects(newRect)) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps && !floorLeft.intersects(newRect) && !floorRight.intersects(newRect)) {
                    Platforms.add(new Rectangle(e.getX(), e.getY(), 10, 10));
                    repaint();
                }
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            blueColor = !blueColor;
            repaint();
        }
    }

    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}

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
