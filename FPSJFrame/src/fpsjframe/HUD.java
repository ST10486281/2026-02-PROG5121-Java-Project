package fpsjframe;

import java.awt.*;

/**
 * HUD
 *
 * Renders all heads-up display elements onto the game screen:
 *   - Crosshair
 *   - Player coordinates / angle readout
 *   - Controls hint
 *   - Minimap (chunk grid + player dot + direction arrow + legend)
 */
public class HUD {

    private final WorldBuilder world;

    // Minimap — each chunk draws as CHUNK_PX × CHUNK_PX pixels
    private static final int CHUNK_PX = 28;
    private static final int MAP_PAD  = 12;

    public HUD(WorldBuilder world) {
        this.world = world;
    }

    /** Draw everything — call once per frame after the scene is rendered. */
    public void draw(Graphics g, double px, double pz, double angle, int sw, int sh) {
        drawProximityDarkness((Graphics2D) g, px, pz, sw, sh);
        drawCrosshair(g, sw, sh);
        drawCoords(g, px, pz, angle);
        drawControls(g, sh);
        drawMinimap((Graphics2D) g, px, pz, angle, sw);
    }

    // ── proximity darkness overlay ───────────────────────────────────────────

    /**
     * Full-screen darkness overlay — separate from per-cell fog in the renderer.
     * Finds nearest solid wall via 8 rays. Darkness increases monotonically
     * as player approaches; never gets brighter again right up against a wall.
     */
    private void drawProximityDarkness(Graphics2D g2, double px, double pz, int sw, int sh) {
        int    numRays  = 8;
        double maxCheck = 2.0;
        double nearest  = maxCheck;
        int    ey       = WorldBuilder.OBJ_SIZE / 2;

        for (int i = 0; i < numRays; i++) {
            double a  = (Math.PI * 2.0 * i) / numRays;
            double dx = Math.sin(a);
            double dz = Math.cos(a);
            for (double t = 0.1; t < maxCheck; t += 0.2) {
                int cx = (int)(px + dx * t);
                int cz = (int)(pz + dz * t);
                if (world.isSolid(cx, ey, cz)) {
                    if (t < nearest) nearest = t;
                    break;
                }
            }
        }

        if (nearest >= maxCheck) return;

        // Linear: closer = darker, strictly monotonic, never reverses
        float darkness = (float)(1.0 - nearest / maxCheck) * 0.65f;
        int   alpha    = (int)(darkness * 255);
        if (alpha <= 0) return;

        g2.setColor(new Color(0, 0, 0, alpha));
        g2.fillRect(0, 0, sw, sh);
    }

    // ── crosshair ────────────────────────────────────────────────────────────

    private void drawCrosshair(Graphics g, int sw, int sh) {
        g.setColor(new Color(255, 255, 255, 180));
        int cx = sw / 2, cy = sh / 2;
        g.drawLine(cx - 12, cy, cx - 4, cy);
        g.drawLine(cx +  4, cy, cx + 12, cy);
        g.drawLine(cx, cy - 12, cx, cy -  4);
        g.drawLine(cx, cy +  4, cx, cy + 12);
    }

    private void drawCoords(Graphics g, double px, double pz, double angle) {
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

    private static Color chunkColour(String name) {
        if (name == null) return new Color(60, 60, 60);
        switch (name) {
            case "chunkFlat": return new Color(180, 140,  80);  // sandy brown
            case "chunkBush": return new Color(100, 180,  60);  // bright green
            case "chunkTree": return new Color( 30,  90,  30);  // dark forest
            default:          return new Color( 80,  80,  80);  // grey unknown
        }
    }

    private void drawMinimap(Graphics2D g2, double px, double pz, double angle, int sw) {
        if (world.chunkNames == null) return;

        int numRows = world.chunkNames.length;
        int numCols = world.chunkNames[0].length;
        int mapW     = numCols * CHUNK_PX;
        int mapH     = numRows * CHUNK_PX;
        int legendW  = 3 * 38 + 30;  // 3 legend entries × 38px + label space
        int totalW   = Math.max(mapW, legendW);
        int ox       = sw - totalW - MAP_PAD;   // top-right anchor, wide enough for legend
        int oy       = MAP_PAD;

        // Background
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(ox - 2, oy - 2, mapW + 4, mapH + 4);

        // Chunk tiles
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                String name = world.chunkNames[row][col];
                g2.setColor(chunkColour(name));
                g2.fillRect(ox + col * CHUNK_PX, oy + row * CHUNK_PX, CHUNK_PX - 1, CHUNK_PX - 1);
                g2.setColor(new Color(0, 0, 0, 120));
                g2.drawRect(ox + col * CHUNK_PX, oy + row * CHUNK_PX, CHUNK_PX - 1, CHUNK_PX - 1);
            }
        }

        // Player position — px/pz are in cells, one chunk = OBJ_SIZE*10 cells
        int chunkCells = WorldBuilder.OBJ_SIZE * 10;
        int dotX = ox + (int)(px / chunkCells * CHUNK_PX);
        int dotZ = oy + (int)(pz / chunkCells * CHUNK_PX);

        // Direction arrow
        g2.setColor(Color.YELLOW);
        g2.drawLine(dotX, dotZ,
                dotX + (int)(Math.sin(angle) * 8),
                dotZ + (int)(Math.cos(angle) * 8));

        // Player dot
        g2.setColor(Color.WHITE);
        g2.fillOval(dotX - 3, dotZ - 3, 6, 6);
        g2.setColor(Color.BLACK);
        g2.drawOval(dotX - 3, dotZ - 3, 6, 6);

        // Legend
        drawLegend(g2, ox, oy + mapH + 6);
    }

    private void drawLegend(Graphics2D g2, int ox, int oy) {
        g2.setFont(new Font("Monospaced", Font.PLAIN, 9));
        String[][] entries = {
            { "chunkFlat", "Flat"  },
            { "chunkBush", "Bush"  },
            { "chunkTree", "Trees" },
        };
        for (int i = 0; i < entries.length; i++) {
            int lx = ox + i * 38;
            g2.setColor(chunkColour(entries[i][0]));
            g2.fillRect(lx, oy, 8, 8);
            g2.setColor(new Color(0, 0, 0, 120));
            g2.drawRect(lx, oy, 8, 8);
            g2.setColor(Color.WHITE);
            g2.drawString(entries[i][1], lx + 10, oy + 8);
        }
    }
}