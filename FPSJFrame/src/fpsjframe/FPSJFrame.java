package fpsjframe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * FPSJFrame
 *
 * A DDA-based raycaster that renders the voxel world built by WorldBuilder.
 * Each vertical screen column fires one ray; wall height is computed from
 * the perpendicular distance to the first solid cell hit.
 *
 * Controls
 *   W / Up     – move forward
 *   S / Down   – move backward
 *   A / Left   – turn left
 *   D / Right  – turn right
 *   Q          – strafe left
 *   E          – strafe right
 *   R          – restart (respawn at origin)
 */
public class FPSJFrame extends JPanel implements KeyListener, Runnable {

    // ── screen ───────────────────────────────────────────────────────────────

    static final int SW = 800;
    static final int SH = 500;

    // ── movement / look ──────────────────────────────────────────────────────

    static final double MOVE_SPEED   = 0.35;
    static final double TURN_SPEED   = 0.04;
    static final double STRAFE_SPEED = 0.28;
    static final double EYE_HEIGHT   = 10;     // cells above y = 0
    static final double CELL_SCALE_H = 30.0;   // height  – how tall cells appear on screen
    static final double CELL_SCALE_W = 1.0;    // width   – horizontal stretch (>1 = wider, <1 = narrower)
    static final double CELL_SCALE_B = 1.0;    // breadth – depth compression  (>1 = closer, <1 = farther)

    // ── player state ─────────────────────────────────────────────────────────

    double px, pz;          // position in cell-space (X and Z are the ground plane)
    double angle;           // yaw in radians (0 = +Z, π/2 = +X)
    static final double FOV = Math.PI / 3.0;   // 60°

    // ── world ────────────────────────────────────────────────────────────────

    WorldBuilder world;
    HUD hud;

    // ── rendering ────────────────────────────────────────────────────────────

    BufferedImage frameBuffer;
    int[]         pixels;

    // ── input ────────────────────────────────────────────────────────────────

    boolean[] keys = new boolean[65536];

    // ── colours ──────────────────────────────────────────────────────────────

    static final Color SKY_TOP    = new Color( 30,  90, 180);
    static final Color SKY_BTM    = new Color(120, 170, 220);
    static final Color FLOOR_COL  = new Color( 55,  75,  45);
    static final Color FLOOR_DARK = new Color( 35,  50,  30);

    // ─────────────────────────────────────────────────────────────────────────
    // Entry point
    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        JFrame frame = new JFrame("FPS Voxel World");
        FPSJFrame game = new FPSJFrame();
        frame.add(game);
        frame.setSize(SW, SH + 22);   // +22 for title bar
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        new Thread(game).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────

    public FPSJFrame() {
        setPreferredSize(new Dimension(SW, SH));
        setFocusable(true);
        addKeyListener(this);

        frameBuffer = new BufferedImage(SW, SH, BufferedImage.TYPE_INT_RGB);
        pixels      = ((java.awt.image.DataBufferInt) frameBuffer.getRaster().getDataBuffer()).getData();

        // Load world from envelope files (same directory as the .class files, or adjust path)
        // Envelopes live in src/envelopes/. Since the game is run from src/,
        // the folder is simply "envelopes" relative to the working directory.
        // Override with:  java -Denvelopes=<path> fpsjframe.FPSJFrame
        String envelopeDir = System.getProperty("envelopes", "envelopes");
        try {
            world = new WorldBuilder(envelopeDir);
            hud   = new HUD(world);
        } catch (Exception e) {
            System.err.println("Failed to load world: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        respawn();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Spawn player in an open area near the centre of the world
    // ─────────────────────────────────────────────────────────────────────────

    void respawn() {
        angle = 0;
        // Start near the centre; scan outward until we find an open ground cell
        double cx = world.worldCellsX / 2.0;
        double cz = world.worldCellsZ / 2.0;
        for (int r = 0; r < 40; r++) {
            int tx = (int)(cx) + r;
            int tz = (int)(cz) + r;
            if (!world.isSolid(tx, (int) EYE_HEIGHT, tz)) {
                px = tx + 0.5;
                pz = tz + 0.5;
                return;
            }
        }
        // Fallback
        px = 1.5;
        pz = 1.5;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Game loop
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        while (true) {
            long now = System.nanoTime();
            double dt = (now - lastTime) / 1_000_000_000.0;
            lastTime  = now;

            handleInput(dt);
            renderFrame();
            repaint();

            // ~60 fps cap
            try { Thread.sleep(16); } catch (InterruptedException ignored) {}
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Input
    // ─────────────────────────────────────────────────────────────────────────

    void handleInput(double dt) {
        double speed   = MOVE_SPEED;
        double strafe  = STRAFE_SPEED;

        if (keys[KeyEvent.VK_W] || keys[KeyEvent.VK_UP])    tryMove( Math.sin(angle) * speed,  Math.cos(angle) * speed);
        if (keys[KeyEvent.VK_S] || keys[KeyEvent.VK_DOWN])  tryMove(-Math.sin(angle) * speed, -Math.cos(angle) * speed);
        if (keys[KeyEvent.VK_Q])                             tryMove( Math.cos(angle) * strafe, -Math.sin(angle) * strafe);
        if (keys[KeyEvent.VK_E])                             tryMove(-Math.cos(angle) * strafe,  Math.sin(angle) * strafe);
        if (keys[KeyEvent.VK_A] || keys[KeyEvent.VK_LEFT])  angle -= TURN_SPEED;
        if (keys[KeyEvent.VK_D] || keys[KeyEvent.VK_RIGHT]) angle += TURN_SPEED;
        if (keys[KeyEvent.VK_R])                             respawn();
    }

    /** Move with simple axis-separated collision. */
    void tryMove(double dx, double dz) {
        double nx = px + dx;
        double nz = pz + dz;
        int ey    = (int) EYE_HEIGHT;

        // X axis
        if (!world.isSolid((int)(nx), ey, (int)(pz))) px = nx;
        // Z axis
        if (!world.isSolid((int)(px), ey, (int)(nz))) pz = nz;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Renderer  (DDA raycaster, column by column)
    // ─────────────────────────────────────────────────────────────────────────

    void renderFrame() {
        drawBackground();

        for (int col = 0; col < SW; col++) {

            // Ray direction for this screen column
            // CELL_SCALE_W stretches columns away from centre (>1 = wider view = world appears narrower)
            double normCol  = (col - SW / 2.0) / (SW / 2.0);   // -1 to +1
            double rayAngle = angle + Math.atan(Math.tan(FOV / 2.0) * normCol / CELL_SCALE_W);
            double rdx = Math.sin(rayAngle);
            double rdz = Math.cos(rayAngle);

            // DDA setup -- steps one CELL at a time on the X/Z ground plane
            double posX = px;
            double posZ = pz;

            int mapX = (int) posX;
            int mapZ = (int) posZ;

            double deltaDistX = Math.abs(rdx) < 1e-10 ? 1e30 : Math.abs(1.0 / rdx);
            double deltaDistZ = Math.abs(rdz) < 1e-10 ? 1e30 : Math.abs(1.0 / rdz);

            double sideDistX, sideDistZ;
            int stepX, stepZ;

            if (rdx < 0) { stepX = -1; sideDistX = (posX - mapX) * deltaDistX; }
            else          { stepX =  1; sideDistX = (mapX + 1.0 - posX) * deltaDistX; }
            if (rdz < 0) { stepZ = -1; sideDistZ = (posZ - mapZ) * deltaDistZ; }
            else          { stepZ =  1; sideDistZ = (mapZ + 1.0 - posZ) * deltaDistZ; }

            // Track painted rows so nearer cells always win over farther ones
            boolean[] rowPainted = new boolean[SH];

            int maxSteps = world.worldCellsX + world.worldCellsZ;

            for (int step = 0; step < maxSteps; step++) {

                boolean sideHit;
                if (sideDistX < sideDistZ) {
                    sideDistX += deltaDistX;
                    mapX      += stepX;
                    sideHit    = true;
                } else {
                    sideDistZ += deltaDistZ;
                    mapZ      += stepZ;
                    sideHit    = false;
                }

                // Perpendicular distance to this cell column (corrects fish-eye)
                double perpDist = sideHit
                    ? (mapX - posX + (1 - stepX) / 2.0) / rdx
                    : (mapZ - posZ + (1 - stepZ) / 2.0) / rdz;
                if (perpDist <= 0) perpDist = 0.001;

                // How many pixels tall is one cell at this distance?
                double cellScreenH = (double) SH / (WorldBuilder.WORLD_HEIGHT * (perpDist / CELL_SCALE_B)) * CELL_SCALE_H;

                // Screen Y of the bottom of y=0 (ground level).
                // Horizon is at SH/2; eye is EYE_HEIGHT cells above ground.
                double groundScreenY = SH / 2.0 + EYE_HEIGHT * cellScreenH;

                // Shade + fog factors shared across all y-layers in this column
                float shade = sideHit ? 0.65f : 1.0f;
                float fog   = (float) Math.max(0.1, 1.0 - perpDist / 80.0);

                // Paint every solid y-layer individually.
                // Air gaps (e.g. between trunk and canopy) stay transparent.
                boolean anyHit = false;
                for (int y = 0; y < WorldBuilder.WORLD_HEIGHT; y++) {
                    if (!world.isSolid(mapX, y, mapZ)) continue;
                    anyHit = true;

                    // Screen rows for this cell layer (y=0 on ground, y=7 at top)
                    int scrY0 = Math.max(0,       (int)(groundScreenY - (y + 1) * cellScreenH));
                    int scrY1 = Math.min(SH - 1,  (int)(groundScreenY - y       * cellScreenH));

                    Color c  = world.getColor(mapX, y, mapZ);
                    float r  = (c.getRed()   / 255f) * shade * fog;
                    float g  = (c.getGreen() / 255f) * shade * fog;
                    float b  = (c.getBlue()  / 255f) * shade * fog;
                    int   rgb = toRGB(r, g, b);

                    for (int sy = scrY0; sy <= scrY1; sy++) {
                        if (!rowPainted[sy]) {
                            pixels[sy * SW + col] = rgb;
                            rowPainted[sy] = true;
                        }
                    }
                }

                // Early-out: if this column was fully solid and filled every
                // visible screen row, nothing behind it can be seen.
                if (anyHit) {
                    int topRow = Math.max(0,      (int)(groundScreenY - WorldBuilder.WORLD_HEIGHT * cellScreenH));
                    int botRow = Math.min(SH - 1, (int) groundScreenY);
                    boolean allPainted = true;
                    for (int sy = topRow; sy <= botRow; sy++) {
                        if (!rowPainted[sy]) { allPainted = false; break; }
                    }
                    if (allPainted) break;
                }
            }
        }

    }


    /** Draw sky gradient and floor gradient into the pixel buffer. */
    void drawBackground() {
        int midY = SH / 2;
        for (int y = 0; y < SH; y++) {
            int rgb;
            if (y < midY) {
                // Sky: gradient from top to horizon
                float t   = (float) y / midY;
                float r   = lerp(SKY_TOP.getRed(),   SKY_BTM.getRed(),   t) / 255f;
                float g   = lerp(SKY_TOP.getGreen(), SKY_BTM.getGreen(), t) / 255f;
                float b   = lerp(SKY_TOP.getBlue(),  SKY_BTM.getBlue(),  t) / 255f;
                rgb = toRGB(r, g, b);
            } else {
                // Floor: gradient from horizon to bottom
                float t   = (float)(y - midY) / (SH - midY);
                float r   = lerp(FLOOR_COL.getRed(),   FLOOR_DARK.getRed(),   t) / 255f;
                float g   = lerp(FLOOR_COL.getGreen(), FLOOR_DARK.getGreen(), t) / 255f;
                float b   = lerp(FLOOR_COL.getBlue(),  FLOOR_DARK.getBlue(),  t) / 255f;
                rgb = toRGB(r, g, b);
            }
            for (int x = 0; x < SW; x++) pixels[y * SW + x] = rgb;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Painting
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(frameBuffer, 0, 0, null);
        hud.draw(g, px, pz, angle, SW, SH);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Key listeners
    // ─────────────────────────────────────────────────────────────────────────

    @Override public void keyPressed (KeyEvent e) { keys[e.getKeyCode()] = true;  }
    @Override public void keyReleased(KeyEvent e) { keys[e.getKeyCode()] = false; }
    @Override public void keyTyped   (KeyEvent e) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────────

    static int toRGB(float r, float g, float b) {
        int ri = (int)(Math.min(1, Math.max(0, r)) * 255);
        int gi = (int)(Math.min(1, Math.max(0, g)) * 255);
        int bi = (int)(Math.min(1, Math.max(0, b)) * 255);
        return (ri << 16) | (gi << 8) | bi;
    }

    static float lerp(int a, int b, float t) {
        return a + (b - a) * t;
    }
}