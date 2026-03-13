package fpsjframe;

import java.awt.Color;

/**
 * Entity — a living thing in the world with a position, animated voxel body,
 * and simple AI walking behaviour.
 *
 * Poses are pre-baked voxel grids (boolean + Color) generated from AnimalNode
 * + FreeWill at startup. The entity cycles through them each tick.
 */
public class Entity {

    public static final int SIZE = 20; // must match envelope grid size

    // ── identity ──────────────────────────────────────────────────────────────
    public final String name;

    // ── world position (cell-space, same coord system as player px/pz) ────────
    public double x;
    public double z;
    public double angle; // facing direction in radians

    // ── animation ─────────────────────────────────────────────────────────────
    private final boolean[][][][] poses;  // [poseIndex][x][y][z]
    private final Color[][][][]   colors; // [poseIndex][x][y][z]
    private int poseIndex    = 0;
    private int tickCounter  = 0;
    public  int ticksPerPose = 18;

    // ── AI ────────────────────────────────────────────────────────────────────
    public  double  moveSpeed = 0.04;
    private double  turnTimer = 0;
    private double  turnEvery = 100 + Math.random() * 80;
    private boolean moving    = true;

    // ─────────────────────────────────────────────────────────────────────────

    public Entity(String name, double x, double z,
                  boolean[][][][] poses, Color[][][][] colors) {
        this.name   = name;
        this.x      = x;
        this.z      = z;
        this.poses  = poses;
        this.colors = colors;
        this.angle  = Math.random() * Math.PI * 2;
    }

    // ── voxel query in local space ────────────────────────────────────────────

    public boolean isSolid(int lx, int ly, int lz) {
        if (lx < 0 || lx >= SIZE || ly < 0 || ly >= SIZE || lz < 0 || lz >= SIZE)
            return false;
        return poses[poseIndex][lx][ly][lz];
    }

    public Color getColor(int lx, int ly, int lz) {
        if (lx < 0 || lx >= SIZE || ly < 0 || ly >= SIZE || lz < 0 || lz >= SIZE)
            return Color.GRAY;
        Color c = colors[poseIndex][lx][ly][lz];
        return c != null ? c : Color.GRAY;
    }

    // ── world footprint ───────────────────────────────────────────────────────

    public int worldMinX() { return (int) x - SIZE / 2; }
    public int worldMinZ() { return (int) z - SIZE / 2; }

    /** World X → local voxel X, or -1 if outside this entity's footprint. */
    public int toLocalX(int wx) {
        int lx = wx - worldMinX();
        return (lx >= 0 && lx < SIZE) ? lx : -1;
    }

    /** World Z → local voxel Z, or -1 if outside this entity's footprint. */
    public int toLocalZ(int wz) {
        int lz = wz - worldMinZ();
        return (lz >= 0 && lz < SIZE) ? lz : -1;
    }

    // ── tick: advance animation + AI movement ────────────────────────────────

    public void tick(WorldBuilder world) {

        // Animation cycle
        tickCounter++;
        if (tickCounter >= ticksPerPose) {
            tickCounter = 0;
            poseIndex   = (poseIndex + 1) % poses.length;
        }

        // Periodically change direction
        turnTimer++;
        if (turnTimer >= turnEvery) {
            turnTimer  = 0;
            turnEvery  = 100 + Math.random() * 80;
            angle     += (Math.random() - 0.5) * Math.PI * 1.2;
            moving     = Math.random() > 0.25;
        }

        // Move
        if (moving) {
            double nx = x + Math.sin(angle) * moveSpeed;
            double nz = z + Math.cos(angle) * moveSpeed;

            // Clamp to world bounds
            nx = Math.max(SIZE, Math.min(world.worldCellsX - SIZE, nx));
            nz = Math.max(SIZE, Math.min(world.worldCellsZ - SIZE, nz));

            int ey = WorldBuilder.WORLD_HEIGHT / 2;

            if (!world.isSolid((int) nx, ey, (int) z)) x = nx;
            else angle += Math.PI * 0.5; // bounce off wall

            if (!world.isSolid((int) x, ey, (int) nz)) z = nz;
            else angle -= Math.PI * 0.5;
        }
    }
}