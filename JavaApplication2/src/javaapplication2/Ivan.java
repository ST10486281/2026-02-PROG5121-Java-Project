package fpsjframe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

public class FPSJFrame extends JPanel implements KeyListener, Runnable {
    private int playerX = 300, playerY = 400, playerSize = 30;
    private ArrayList<int[]> enemies = new ArrayList<>(); // [x, y]
    private boolean[] keys = new boolean[256];
    private int score = 0;
    private boolean gameOver = false;
    private long lastTime = System.nanoTime();
    private int frames = 0, fps = 0;
    private long gameStartTime;

    public FPSJFrame() {
        setPreferredSize(new Dimension(640, 480));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        // Spawn enemies periodically
        new Thread(() -> {
            while (true) {
                if (!gameOver) {
                    enemies.add(new int[] { (int) (Math.random() * 610), 0 });
                }
                try {
                    Thread.sleep(800);
                } catch (Exception e) {
                }
            }
        }).start();

        new Thread(this).start();
        JFrame frame = new JFrame("FPS Dodge - FPSJFrame");
        frame.add(this);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    @Override
    public void run() {
        gameStartTime = System.currentTimeMillis();
        while (true) {
            long now = System.nanoTime();
            frames++;
            if (now - lastTime >= 1_000_000_000) {
                fps = frames;
                frames = 0;
                lastTime = now;
            }

            if (!gameOver) {
                // Movement
                if (keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A])
                    playerX -= 5;
                if (keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D])
                    playerX += 5;
                if (keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W])
                    playerY -= 5;
                if (keys[KeyEvent.VK_DOWN] || keys[KeyEvent.VK_S])
                    playerY += 5;

                // Boundaries
                playerX = Math.max(0, Math.min(610, playerX));
                playerY = Math.max(0, Math.min(450, playerY));

                // Move enemies
                for (int i = 0; i < enemies.size(); i++) {
                    int[] e = enemies.get(i);
                    e[1] += 4;
                    if (e[1] > 480) {
                        enemies.remove(i--);
                        continue;
                    }

                    // Collision
                    if (playerX < e[0] + 20 && playerX + playerSize > e[0] &&
                            playerY < e[1] + 20 && playerY + playerSize > e[1]) {
                        gameOver = true;
                    }
                }

                score = (int) ((System.currentTimeMillis() - gameStartTime) / 1000);
            }

            repaint();
            try {
                Thread.sleep(16);
            } catch (Exception e) {
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Player
        g2.setColor(Color.CYAN);
        g2.fillRect(playerX, playerY, playerSize, playerSize);

        // Enemies
        g2.setColor(Color.RED);
        for (int[] e : enemies) {
            g2.fillRect(e[0], e[1], 20, 20);
        }

        // HUD
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.drawString("FPS: " + fps, 10, 25);
        g2.drawString("SCORE: " + score, 10, 50);

        if (gameOver) {
            g2.setFont(new Font("Arial", Font.BOLD, 40));
            g2.drawString("GAME OVER", 180, 220);
            g2.setFont(new Font("Arial", Font.PLAIN, 20));
            g2.drawString("Press SPACE to restart", 200, 270);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;
        if (gameOver && e.getKeyCode() == KeyEvent.VK_SPACE) {
            resetGame();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    private void resetGame() {
        playerX = 300;
        playerY = 400;
        enemies.clear();
        score = 0;
        gameOver = false;
        gameStartTime = System.currentTimeMillis();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FPSJFrame::new);
    }
}