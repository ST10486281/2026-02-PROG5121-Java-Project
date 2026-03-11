package fpsjframe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;

public class FPSJFrame extends JPanel implements KeyListener, Runnable {
    private double posX = 2, posY = 2; // player pos
    private double dirX = -1, dirY = 0; // direction vector
    private double planeX = 0, planeY = 0.66; // camera plane
    private final int w = 640, h = 480;
    private boolean[] keys = new boolean[256];
    private int[][] worldMap = {
            { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }
    };
    private int score = 0;
    private boolean gameOver = false;
    private long startTime, lastTime = System.nanoTime();
    private int frames = 0, fps = 0;
    private Random rand = new Random();

    public FPSJFrame() {
        setPreferredSize(new Dimension(w, h));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);
        startTime = System.currentTimeMillis();
        new Thread(this).start();
        JFrame frame = new JFrame("Doom-Vibe FPSJFrame");
        frame.add(this);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    @Override
    public void run() {
        while (true) {
            long now = System.nanoTime();
            frames++;
            if (now - lastTime >= 1_000_000_000) {
                fps = frames;
                frames = 0;
                lastTime = now;
            }

            if (!gameOver) {
                // movement
                double moveSpeed = 0.08, rotSpeed = 0.05;
                if (keys[KeyEvent.VK_W]) {
                    if (worldMap[(int) (posX + dirX * moveSpeed)][(int) posY] == 0)
                        posX += dirX * moveSpeed;
                    if (worldMap[(int) posX][(int) (posY + dirY * moveSpeed)] == 0)
                        posY += dirY * moveSpeed;
                }
                if (keys[KeyEvent.VK_S]) {
                    if (worldMap[(int) (posX - dirX * moveSpeed)][(int) posY] == 0)
                        posX -= dirX * moveSpeed;
                    if (worldMap[(int) posX][(int) (posY - dirY * moveSpeed)] == 0)
                        posY -= dirY * moveSpeed;
                }
                if (keys[KeyEvent.VK_A] || keys[KeyEvent.VK_LEFT]) {
                    double oldDirX = dirX;
                    dirX = dirX * Math.cos(rotSpeed) - dirY * Math.sin(rotSpeed);
                    dirY = oldDirX * Math.sin(rotSpeed) + dirY * Math.cos(rotSpeed);
                    double oldPlaneX = planeX;
                    planeX = planeX * Math.cos(rotSpeed) - planeY * Math.sin(rotSpeed);
                    planeY = oldPlaneX * Math.sin(rotSpeed) + planeY * Math.cos(rotSpeed);
                }
                if (keys[KeyEvent.VK_D] || keys[KeyEvent.VK_RIGHT]) {
                    double oldDirX = dirX;
                    dirX = dirX * Math.cos(-rotSpeed) - dirY * Math.sin(-rotSpeed);
                    dirY = oldDirX * Math.sin(-rotSpeed) + dirY * Math.cos(-rotSpeed);
                    double oldPlaneX = planeX;
                    planeX = planeX * Math.cos(-rotSpeed) - planeY * Math.sin(-rotSpeed);
                    planeY = oldPlaneX * Math.sin(-rotSpeed) + planeY * Math.cos(-rotSpeed);
                }

                score = (int) ((System.currentTimeMillis() - startTime) / 1000);
                if (rand.nextInt(300) == 0)
                    gameOver = true; // random "demon attack"
            }

            repaint();
            try {
                Thread.sleep(10);
            } catch (Exception e) {
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // floor & ceiling
        g2.setColor(new Color(40, 40, 40));
        g2.fillRect(0, h / 2, w, h / 2);
        g2.setColor(new Color(80, 60, 40));
        g2.fillRect(0, 0, w, h / 2);

        // raycasting
        for (int x = 0; x < w; x++) {
            double cameraX = 2 * x / (double) w - 1;
            double rayDirX = dirX + planeX * cameraX;
            double rayDirY = dirY + planeY * cameraX;

            int mapX = (int) posX, mapY = (int) posY;
            double sideDistX, sideDistY;
            double deltaDistX = Math.abs(1 / rayDirX), deltaDistY = Math.abs(1 / rayDirY);
            double perpWallDist;

            int stepX = rayDirX < 0 ? -1 : 1;
            int stepY = rayDirY < 0 ? -1 : 1;

            if (rayDirX < 0)
                sideDistX = (posX - mapX) * deltaDistX;
            else
                sideDistX = (mapX + 1.0 - posX) * deltaDistX;
            if (rayDirY < 0)
                sideDistY = (posY - mapY) * deltaDistY;
            else
                sideDistY = (mapY + 1.0 - posY) * deltaDistY;

            boolean hit = false;
            int side = 0;
            while (!hit) {
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX;
                    mapX += stepX;
                    side = 0;
                } else {
                    sideDistY += deltaDistY;
                    mapY += stepY;
                    side = 1;
                }
                if (worldMap[mapX][mapY] > 0)
                    hit = true;
            }

            perpWallDist = side == 0 ? (mapX - posX + (1 - stepX) / 2) / rayDirX
                    : (mapY - posY + (1 - stepY) / 2) / rayDirY;

            int lineHeight = (int) (h / perpWallDist);
            int drawStart = Math.max(0, -lineHeight / 2 + h / 2);
            int drawEnd = Math.min(h, lineHeight / 2 + h / 2);

            Color wallColor = worldMap[mapX][mapY] == 1 ? new Color(180, 0, 0)
                    : worldMap[mapX][mapY] == 2 ? new Color(0, 180, 0)
                            : new Color(100, 100, 180);
            if (side == 1)
                wallColor = wallColor.darker();
            g2.setColor(wallColor);
            g2.fillRect(x, drawStart, 1, drawEnd - drawStart);
        }

        // HUD
        g2.setColor(Color.GREEN);
        g2.setFont(new Font("Arial", Font.BOLD, 24));
        g2.drawString("FPS: " + fps, 10, 30);
        g2.drawString("SCORE: " + score, 10, 60);
        if (gameOver) {
            g2.setColor(Color.RED);
            g2.setFont(new Font("Arial", Font.BOLD, 48));
            g2.drawString("YOU DIED", 180, 240);
            g2.setFont(new Font("Arial", Font.PLAIN, 20));
            g2.drawString("Press SPACE to respawn", 200, 300);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;
        if (gameOver && e.getKeyCode() == KeyEvent.VK_SPACE) {
            posX = 2;
            posY = 2;
            dirX = -1;
            dirY = 0;
            planeX = 0;
            planeY = 0.66;
            gameOver = false;
            startTime = System.currentTimeMillis();
            score = 0;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FPSJFrame::new);
    }
}