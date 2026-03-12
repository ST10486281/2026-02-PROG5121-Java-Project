package fpsjframe;

import java.awt.*;

/**
 * HUD
 *
 * Renders all heads-up display elements onto the game screen:
 *   - Crosshair
 *   - Player coordinates / angle readout
 *   - Controls hint
 *   - Minimap (chunk grid + player position + direction arrow)
 */
public class HUD {

    private final WorldBuilder world;

    // Minimap config
    private static final int MAP_SIZE   = 120;  // total minimap pixel size
    private static final int MAP_PAD    = 8;    // padding from screen edge

    public HUD(WorldBuilder world) {
        this.world = world;
    }

    /** Draw everything — call once per frame after the scene is rendered. */
    public void draw(Graphics g, double px, double pz, double angle, int sw, int sh) {
        drawCrosshair(g, sw, sh);
        drawCoords(g, px, pz, angle, sh);
        drawControls(g, sh);
        drawMinimap((Graphics2D) g, px, pz, angle, sw);
    }

    // ── crosshair ────────────────────────────────────────────────────────────

    private void drawCrosshair(Graphics g, int sw, int sh) {
        g.setColor(new Color(255, 255, 255, 180));
        int cx = sw / 2, cy = sh / 2;
        g.drawLine(cx - 8, cy, cx + 8, cy);
        g.drawLine(cx, cy - 8, cx, cy + 8);
    }

    // ── text overlays ─────────────────────────────────────────────────────────

    private void drawCoords(Graphics g, double px, double pz, double angle, int sh) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.drawString(String.format("X:%.1f  Z:%.1f  Angle:%.1f°",
                px, pz, Math.toDegrees(angle)), 8, 16);
    }

    private void drawControls(Graphics g, int sh) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.drawString("WASD/Arrows=move  Q/E=strafe  R=respawn", 8, sh - 8);
    }

    // ── minimap ───────────────────────────────────────────────────────────────

    private void drawMinimap(Graphics2D g2, double px, double pz, double angle, int sw) {
        int chunkCells = WorldBuilder.OBJ_SIZE * 10;   // cells per chunk (10 objects wide)
        int numChunksX = world.worldCellsX / chunkCells;
        int numChunksZ = world.worldCellsZ / chunkCells;
        int chunkPx    = MAP_SIZE / Math.max(numChunksX, numChunksZ);

        int mx = sw - MAP_SIZE - MAP_PAD;   // top-right anchor
        int mz = MAP_PAD;

        // Background
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRect(mx - 2, mz - 2, MAP_SIZE + 4, MAP_SIZE + 4);

        // Chunk tiles
        for (int cz = 0; cz < numChunksZ; cz++) {
            for (int cx = 0; cx < numChunksX; cx++) {
                int tx = mx + cx * chunkPx;
                int tz = mz + cz * chunkPx;

                // Sample the topmost solid cell near the chunk centre for colour
                int sampleX = cx * chunkCells + chunkCells / 2;
                int sampleZ = cz * chunkCells + chunkCells / 2;
                Color fill  = new Color(40, 60, 40);
                for (int y = WorldBuilder.WORLD_HEIGHT - 1; y >= 0; y--) {
                    if (world.isSolid(sampleX, y, sampleZ)) {
                        fill = world.getColor(sampleX, y, sampleZ).darker();
                        break;
                    }
                }

                g2.setColor(fill);
                g2.fillRect(tx, tz, chunkPx - 1, chunkPx - 1);
                g2.setColor(new Color(80, 80, 80));
                g2.drawRect(tx, tz, chunkPx - 1, chunkPx - 1);
            }
        }

        // Player position
        int dotX = mx + (int)((px / world.worldCellsX) * MAP_SIZE);
        int dotZ = mz + (int)((pz / world.worldCellsZ) * MAP_SIZE);

        // Direction arrow
        g2.setColor(Color.YELLOW);
        int dirLen = 6;
        g2.drawLine(dotX, dotZ,
                dotX + (int)(Math.sin(angle) * dirLen),
                dotZ + (int)(Math.cos(angle) * dirLen));

        // Player dot
        g2.setColor(Color.WHITE);
        g2.fillOval(dotX - 3, dotZ - 3, 6, 6);
        g2.setColor(Color.BLACK);
        g2.drawOval(dotX - 3, dotZ - 3, 6, 6);
    }
}