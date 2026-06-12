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
    private BufferedImage winImage;
    private BufferedImage MillerImage;

    private BufferedImage activeWinImage;

    private boolean up, down, left, right;

    // Base walk speed and current frame speed (may be boosted by SpeedPlatform)
    private static final double BASE_SPEED = 5;
    private double MarioXVelocity = BASE_SPEED;
    private double MarioYVelocity = 0;

    private Rectangle marioHitbox = new Rectangle(0, 0, 48, 80);

    // -------------------- Ink --------------------
    private static int MAX_INK = 500;
    private int ink = MAX_INK;

    // -------------------- Levels --------------------
    private int currentLevel = 1;
    private static final int TOTAL_LEVELS = 6;

    ArrayList<Rectangle> Platforms = new ArrayList<>();

    private ArrayList<Rectangle> staticPlatforms = new ArrayList<>();
    private ArrayList<Rectangle> floorSegments = new ArrayList<>();
    private Rectangle Winbox;
    private ArrayList<Rectangle> killZones = new ArrayList<>();

    private boolean onplatform;
    private boolean gameWon = false;
    private boolean gameLost = false;
    private boolean levelComplete = false;

    // SpeedPlatform boost lasts a few frames so it isn't clobbered immediately
    private boolean speedBoostActive = false;

    private double grav = 0.5;

    private Point check = new Point();

    // -------------------- Middle-click drag drawing --------------------
    private Point dragStart = null;
    private Point dragCurrent = null;

    // -------------------- Konami code --------------------
    private static final int[] KONAMI = {
            KeyEvent.VK_UP, KeyEvent.VK_UP,
            KeyEvent.VK_DOWN, KeyEvent.VK_DOWN,
            KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
            KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT
    };
    private int konamiProgress = 0;

    public DisplayPanel() {

        score = 0;
        blueColor = true;

        try {
            background = ImageIO.read(new File("src/background.png"));
            mario = ImageIO.read(new File("src/marioright.png"));
            winImage = ImageIO.read(new File("src/Windoor.png"));
            MillerImage = ImageIO.read(new File("src/MILLER.png"));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        activeWinImage = winImage;

        loadLevel(currentLevel);

        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);

        setFocusable(true);
        requestFocusInWindow();

        Timer timer = new Timer(16, e -> {
            if (gameWon) return;
            if (levelComplete) return;

            // Only reset speed if no boost is active this frame
            if (!speedBoostActive) {
                MarioXVelocity = BASE_SPEED;
            }
            speedBoostActive = false;

            // Win condition
            marioHitbox.setBounds(marioX, marioY, 48, 80);
            if (Winbox != null && marioHitbox.intersects(Winbox)) {
                if (currentLevel < TOTAL_LEVELS) {
                    levelComplete = true;
                    repaint();
                    Timer nextLevelTimer = new Timer(1800, ev -> {
                        currentLevel++;
                        loadLevel(currentLevel);
                        levelComplete = false;
                        repaint();
                    });
                    nextLevelTimer.setRepeats(false);
                    nextLevelTimer.start();
                } else {
                    gameWon = true;
                }
                return;
            }

            // Kill zones
            marioHitbox.setBounds(marioX, marioY, 48, 80);
            for (Rectangle kill : killZones) {
                if (marioHitbox.intersects(kill)) {
                    gameLost = true;
                }
            }

            // Build combined solid list once per frame
            ArrayList<Rectangle> allPlatforms = new ArrayList<>();
            allPlatforms.addAll(staticPlatforms);
            allPlatforms.addAll(Platforms);
            allPlatforms.addAll(floorSegments);

            // ---------------- GRAVITY + JUMP ----------------
            MarioYVelocity += grav;
            if (down) MarioYVelocity += 1;

            if (up && onplatform) {
                boolean onSpeed = false;
                for (Rectangle thingy : allPlatforms) {
                    if (thingy instanceof SpeedPlatform) {
                        Rectangle probe = new Rectangle(marioX, marioY + 1, 48, 2);
                        if (thingy.intersects(probe)) {
                            onSpeed = true;
                            break;
                        }
                    }
                }
                MarioYVelocity = onSpeed ? -20 : -10;
                onplatform = false;
            }

            // ================== HORIZONTAL PASS ==================
            int prevX = marioX;

            if (left)  marioX -= (int) MarioXVelocity;
            if (right) marioX += (int) MarioXVelocity;

            // Wall clamp
            marioX = Math.max(0, Math.min(960 - 48, marioX));

            marioHitbox.setBounds(marioX, marioY, 48, 80);

            for (Rectangle solid : allPlatforms) {
                if (!marioHitbox.intersects(solid)) continue;

                // How deep is Mario into the platform from each horizontal side
                int overlapLeft  = (marioHitbox.x + marioHitbox.width) - solid.x;
                int overlapRight = (solid.x + solid.width) - marioHitbox.x;

                // Step-up: only thin horizontal platforms (height <= 12) can be
                // stepped over.  Tall walls and pillars always block — no teleport.
                if (solid.height <= 12) {
                    int feetBelowTop = (marioHitbox.y + marioHitbox.height) - solid.y;
                    if (feetBelowTop > 0 && feetBelowTop <= 10) {
                        marioY -= feetBelowTop;
                        marioHitbox.setBounds(marioX, marioY, 48, 80);
                        continue;
                    }
                }

                // overlapLeft  = how far Mario's right edge crossed the solid's left edge (right-move penetration)
                // overlapRight = how far the solid's right edge crossed Mario's left edge (left-move penetration)
                if (marioX > prevX) {
                    // moved right — right edge penetrated solid's left edge
                    marioX -= overlapLeft;
                } else if (marioX < prevX) {
                    // moved left — left edge penetrated solid's right edge
                    marioX += overlapRight;
                } else {
                    // spawned inside something — use smallest overlap
                    if (overlapLeft < overlapRight) marioX -= overlapLeft;
                    else                            marioX += overlapRight;
                }
                marioHitbox.setBounds(marioX, marioY, 48, 80);
            }

            // ================== VERTICAL PASS ==================
            // Record where Mario's feet were BEFORE the move so we know
            // which side of a platform he came from.
            int prevFeetY = marioY + 80;

            marioY += (int) MarioYVelocity;
            marioY = Math.max(0, marioY);          // ceiling clamp
            marioHitbox.setBounds(marioX, marioY, 48, 80);
            onplatform = false;

            for (Rectangle solid : allPlatforms) {
                if (!marioHitbox.intersects(solid)) continue;

                int solidTop    = solid.y;
                int solidBottom = solid.y + solid.height;
                int marioFeet   = marioHitbox.y + marioHitbox.height;
                int marioHead   = marioHitbox.y;

                if (MarioYVelocity >= 0 && prevFeetY <= solidTop) {
                    // Falling — land on top
                    marioY = solidTop - 80;
                    MarioYVelocity = 0;
                    onplatform = true;

                    if (solid instanceof SpeedPlatform) {
                        MarioXVelocity = 30;
                        speedBoostActive = true;
                    }

                } else if (MarioYVelocity < 0 && marioHead > solidBottom - (int) Math.abs(MarioYVelocity) - 2) {
                    // Rising — hit the underside
                    marioY = solidBottom;
                    MarioYVelocity = 0;
                }

                marioHitbox.setBounds(marioX, marioY, 48, 80);
            }

            repaint();
        });

        timer.start();
    }

    // -------------------- Level Loading --------------------

    private void loadLevel(int level) {
        marioX = 50;
        marioY = 335;
        MarioYVelocity = 0;
        MarioXVelocity = BASE_SPEED;
        speedBoostActive = false;
        onplatform = false;
        gameLost = false;
        Platforms.clear();
        staticPlatforms.clear();
        floorSegments.clear();
        killZones.clear();

        if (level == 1) {
            //speed bridge launch
            floorSegments.add(new Rectangle(0,   515, 320, 999));
            floorSegments.add(new Rectangle(620,  515, 340, 999));

            killZones.add(new Rectangle(320, 815, 300, 50));

            staticPlatforms.add(new SpeedPlatform(280, 440, 100, 11));

            Winbox = new Rectangle(870, 430, 60, 80);

        } else if (level == 2) {
            //roblox ahh parkour
            floorSegments.add(new Rectangle(0, 515, 960, 999));

            killZones.add(new Rectangle(160,  490, 80, 30));
            killZones.add(new Rectangle(360,  490, 80, 30));
            killZones.add(new Rectangle(560,  490, 80, 30));
            killZones.add(new Rectangle(740,  490, 80, 30));

            staticPlatforms.add(new Rectangle(840, 420, 80, 11));

            Winbox = new Rectangle(870, 335, 60, 80);

        } else if (level == 3) {

            killZones.add(new Rectangle(0, 505, 960, 60));

            staticPlatforms.add(new Rectangle(0,   420, 90,  11));
            staticPlatforms.add(new Rectangle(870, 420, 90,  11));
            staticPlatforms.add(new Rectangle(280, 360, 60,  11));
            staticPlatforms.add(new Rectangle(590, 300, 60,  11));
            staticPlatforms.add(new Rectangle(380, 220, 60,  11));

            Winbox = new Rectangle(730, 135, 60, 80);

        } else if (level == 4) {
            floorSegments.add(new Rectangle(0, 515, 960, 999));

            // left-wall ledges
            staticPlatforms.add(new Rectangle(0,   450, 140, 11));
            staticPlatforms.add(new Rectangle(0,   340, 120, 11));
            staticPlatforms.add(new Rectangle(0,   230, 120, 11));
            staticPlatforms.add(new Rectangle(0,   120, 120, 11));

            // right-wall ledges
            staticPlatforms.add(new Rectangle(840, 395, 120, 11));
            staticPlatforms.add(new Rectangle(840, 285, 120, 11));
            staticPlatforms.add(new Rectangle(840, 175, 120, 11));
            staticPlatforms.add(new Rectangle(840,  85, 120, 11));

            // punish strips at the base of each side
            killZones.add(new Rectangle(130, 490, 200, 30));
            killZones.add(new Rectangle(630, 490, 200, 30));
            killZones.add(new Rectangle(430, 100, 60, 400));

            Winbox = new Rectangle(880, 0, 60, 80);   // just above top-right ledge

        } else if (level == 5) {
            floorSegments.add(new Rectangle(0,   515, 150, 999));
            floorSegments.add(new Rectangle(830, 515, 130, 999));

            killZones.add(new Rectangle(150, 505, 680, 50));

            staticPlatforms.add(new SpeedPlatform(130, 430, 110, 11));
            staticPlatforms.add(new SpeedPlatform(320, 370, 110, 11));
            staticPlatforms.add(new SpeedPlatform(510, 310, 110, 11));
            staticPlatforms.add(new SpeedPlatform(680, 250, 110, 11));

            Winbox = new Rectangle(870, 430, 60, 80);

        } else if (level == 6) {
            floorSegments.add(new Rectangle(0,   515, 130, 999));
            floorSegments.add(new Rectangle(880, 515, 80,  999));

            killZones.add(new Rectangle(130, 505, 200, 50));
            killZones.add(new Rectangle(480, 490,  80, 30));
            killZones.add(new Rectangle(680, 490,  80, 30));
            killZones.add(new Rectangle(0, 999,  999, 30));

            staticPlatforms.add(new Rectangle(310, 300, 11, 220));
            staticPlatforms.add(new Rectangle(470, 0, 11, 400));
            staticPlatforms.add(new Rectangle(630, 280, 11, 240));

            staticPlatforms.add(new SpeedPlatform(800, 430, 100, 11));

            Winbox = new Rectangle(890, 200, 60, 80);
        }

        marioHitbox.setBounds(marioX, marioY, 48, 80);
    }

    // -------------------- Ink helpers --------------------

    private int inkCost(int w, int h) {
        return Math.max(10, w * h / 8);
    }

    private int inkRefund(Rectangle r) {
        return inkCost(r.width, r.height);
    }

    // -------------------- Paint --------------------

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.drawImage(background, 0, 0, null);

        // Floor
        g.setColor(new Color(34, 139, 34));
        for (Rectangle floor : floorSegments) {
            g.fillRect(floor.x, floor.y, floor.width, floor.height);
        }

        // Kill zones
        g.setColor(new Color(200, 40, 0));
        for (Rectangle kill : killZones) {
            g.fillRect(kill.x, kill.y, kill.width, kill.height);
        }

        // Static platforms — speed platforms get a distinct amber tint
        for (Rectangle r : staticPlatforms) {
            g.setColor(r instanceof SpeedPlatform ? new Color(200, 140, 30) : new Color(60, 63, 64));
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        // Drawn platforms (right-click dots/drags)
        g.setColor(new Color(70, 130, 200));
        for (Rectangle r : Platforms) {
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        // Win zone
        if (Winbox != null) {
            if (activeWinImage != null) {
                g.drawImage(activeWinImage, Winbox.x, Winbox.y, Winbox.width, Winbox.height, null);
            } else {
                g.setColor(new Color(255, 220, 0, 180));
                g.fillRect(Winbox.x, Winbox.y, Winbox.width, Winbox.height);
                g.setColor(new Color(255, 180, 0));
                g.drawRect(Winbox.x, Winbox.y, Winbox.width, Winbox.height);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 10));
                g.drawString("GOAL", Winbox.x + 8, Winbox.y + 14);
            }
        }

        g.drawImage(mario, marioX, marioY, null);

        // Middle-click drag preview
        if (dragStart != null && dragCurrent != null) {
            int rx = Math.min(dragStart.x, dragCurrent.x);
            int ry = Math.min(dragStart.y, dragCurrent.y);
            int rw = Math.abs(dragCurrent.x - dragStart.x);
            int rh = Math.abs(dragCurrent.y - dragStart.y);
            int previewCost = inkCost(rw, rh);
            boolean canAfford = previewCost <= ink;
            g.setColor(canAfford ? new Color(70, 130, 200, 120) : new Color(200, 60, 60, 120));
            g.fillRect(rx, ry, rw, rh);
            g.setColor(canAfford ? new Color(70, 130, 200) : new Color(200, 60, 60));
            g.drawRect(rx, ry, rw, rh);
        }

        // ---- HUD ----
        drawInkBar(g);
        drawLevelLabel(g);

        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(blueColor ? Color.BLUE : Color.BLACK);
        g.drawString("Score: " + score, 50, 30);

        g.setColor(Color.RED);
        g.drawString("Winning?: " + gameWon, 120, 30);

        // Overlays
        if (levelComplete) {
            drawOverlay(g, new Color(69, 255, 180, 180),
                    "LEVEL " + currentLevel + " CLEAR!",
                    "Fake ahh loading screen..");
            return;
        }

        if (gameWon) {
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRect(0, 0, 960, 540);
            g.setColor(new Color(69, 255, 247));
            g.setFont(new Font("Minecraft", Font.BOLD, 48));
            g.drawString("YOU WIN!", 350, 250);
            g.setFont(new Font("Minecraft", Font.BOLD, 24));
            g.drawString("Congratulations! You're goated twin.", 250, 320);
            return;
        }

        if (gameLost) {
            g.setColor(new Color(0, 0, 0, 125));
            g.fillRect(0, 135, 960, 240);
            g.setColor(new Color(0, 0, 0, 125));
            g.fillRect(0, 145, 960, 220);
            g.setColor(new Color(0, 0, 0, 125));
            g.fillRect(0, 155, 960, 200);
            g.setColor(Color.RED);
            g.setFont(new Font("Minecraft", Font.BOLD, 48));
            g.drawString("YOU DIED", 350, 250);
            g.setFont(new Font("Minecraft", Font.BOLD, 24));
            g.drawString("nah twin u dead lost   [R to retry]", 270, 320);
            return;
        }
    }

    private void drawInkBar(Graphics g) {
        int barX = 720, barY = 14;
        int barW = 160, barH = 18;

        g.setColor(new Color(0, 0, 0, 140));
        g.fillRoundRect(barX - 2, barY - 2, barW + 4, barH + 4, 6, 6);

        float ratio = (float) ink / MAX_INK;
        int fillW = (int) (barW * ratio);
        Color inkColor = ratio > 0.5f ? new Color(70, 130, 255)
                : ratio > 0.2f ? new Color(255, 180, 30)
                : new Color(220, 50, 50);
        g.setColor(inkColor);
        g.fillRoundRect(barX, barY, fillW, barH, 4, 4);

        g.setColor(new Color(200, 200, 200, 180));
        g.drawRoundRect(barX, barY, barW, barH, 4, 4);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 11));
        g.drawString("INK  " + ink + " / " + MAX_INK, barX + 4, barY + 13);
    }

    private void drawLevelLabel(Graphics g) {
        g.setColor(new Color(0, 0, 0, 130));
        g.fillRoundRect(14, 40, 110, 20, 6, 6);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.drawString("Level " + currentLevel + " / " + TOTAL_LEVELS, 20, 55);
    }

    private void drawOverlay(Graphics g, Color bg, String line1, String line2) {
        g.setColor(bg);
        g.fillRect(0, 170, 960, 200);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Minecraft", Font.BOLD, 44));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(line1, (960 - fm.stringWidth(line1)) / 2, 265);
        g.setFont(new Font("Minecraft", Font.BOLD, 20));
        fm = g.getFontMetrics();
        g.drawString(line2, (960 - fm.stringWidth(line2)) / 2, 310);
    }

    // -------------------- Mouse --------------------

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            check.setLocation(e.getX(), e.getY());
            if (!marioHitbox.contains(check)) {
                boolean overlaps = false;
                Rectangle newRect = new Rectangle(e.getX(), e.getY(), 10, 10);
                for (Rectangle existing : Platforms) {
                    if (existing.intersects(newRect)) { overlaps = true; break; }
                }
                for (Rectangle sp : staticPlatforms) {
                    if (sp.intersects(newRect)) { overlaps = true; break; }
                }
                for (Rectangle floor : floorSegments) {
                    if (floor.intersects(newRect)) { overlaps = true; break; }
                }
                if (!overlaps && ink >= inkCost(10, 10)) {
                    Platforms.add(newRect);
                    ink -= inkCost(10, 10);
                    repaint();
                }
            }
        }

        // Middle-click: start drag-to-draw a rectangle
        if (e.getButton() == MouseEvent.BUTTON2) {
            dragStart = e.getPoint();
            dragCurrent = e.getPoint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // Right-click drag: keep placing dot platforms
        if (SwingUtilities.isRightMouseButton(e)) {
            check.setLocation(e.getX(), e.getY());
            if (!marioHitbox.contains(check)) {
                boolean overlaps = false;
                Rectangle newRect = new Rectangle(e.getX(), e.getY(), 10, 10);
                for (Rectangle existing : Platforms) {
                    if (existing.intersects(newRect)) { overlaps = true; break; }
                }
                for (Rectangle sp : staticPlatforms) {
                    if (sp.intersects(newRect)) { overlaps = true; break; }
                }
                for (Rectangle floor : floorSegments) {
                    if (floor.intersects(newRect)) { overlaps = true; break; }
                }
                if (!overlaps && ink >= inkCost(10, 10)) {
                    Platforms.add(new Rectangle(e.getX(), e.getY(), 10, 10));
                    ink -= inkCost(10, 10);
                    repaint();
                }
            }
        }

        // Left-click drag: erase drawn platforms and refund ink
        if (SwingUtilities.isLeftMouseButton(e)) {
            for (Rectangle dih : Platforms) {
                if (dih.contains(e.getPoint())) {
                    ink = Math.min(MAX_INK, ink + inkRefund(dih));
                    Platforms.remove(dih);
                    repaint();
                    break;
                }
            }
        }

        // Middle-click drag: update preview rectangle
        if (e.getModifiersEx() == MouseEvent.BUTTON2_DOWN_MASK) {
            if (dragStart != null) {
                dragCurrent = e.getPoint();
                repaint();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Right-click release: toggle score color
        if (e.getButton() == MouseEvent.BUTTON3) {
            blueColor = !blueColor;
            repaint();
        }

        // Middle-click release: finalize the drawn rectangle
        if (e.getButton() == MouseEvent.BUTTON2) {
            if (dragStart != null && dragCurrent != null) {
                int rx = Math.min(dragStart.x, dragCurrent.x);
                int ry = Math.min(dragStart.y, dragCurrent.y);
                int rw = Math.abs(dragCurrent.x - dragStart.x);
                int rh = Math.abs(dragCurrent.y - dragStart.y);

                if (rw > 4 && rh > 4) {
                    int cost = inkCost(rw, rh);
                    if (cost <= ink) {
                        Rectangle newRect = new Rectangle(rx, ry, rw, rh);
                        boolean overlaps = false;
                        for (Rectangle existing : Platforms) {
                            if (existing.intersects(newRect)) { overlaps = true; break; }
                        }
                        for (Rectangle sp : staticPlatforms) {
                            if (sp.intersects(newRect)) { overlaps = true; break; }
                        }
                        if (!overlaps) {
                            Platforms.add(newRect);
                            ink -= cost;
                        }
                    }
                }
            }
            dragStart = null;
            dragCurrent = null;
            repaint();
        }
    }

    @Override public void mouseMoved(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}

    // -------------------- Keyboard --------------------

    @Override public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {

        int keyCode = e.getKeyCode();

        // Konami code check — runs on every key press before anything else
        if (keyCode == KONAMI[konamiProgress]) {
            konamiProgress++;
            if (konamiProgress == KONAMI.length) {
                activeWinImage = MillerImage;
                konamiProgress = 0;
                MAX_INK = 67676767;
                ink = MAX_INK;
                repaint();
            }
        } else {
            // Wrong key — restart, but re-check index 0 in case this key
            // happens to be the correct first key of a fresh sequence
            konamiProgress = (keyCode == KONAMI[0]) ? 1 : 0;
        }

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

        if (keyCode == KeyEvent.VK_R && gameLost) {
            loadLevel(currentLevel);
            repaint();
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