package fpsjframe;

import java.awt.Color;

/**
 * Entity — a voxel object at a fixed world position that cycles through
 * pre-baked poses to create animation.
 *
 * No movement, no AI. Just animation.
 */
public class Entity {

    public static final int SIZE = 20;

    public final String name;
    public final int x; // fixed world X
    public final int z; // fixed world Z

    private final boolean[][][][] poses;  // [poseIndex][x][y][z]
    private final Color[][][][]   colors; // [poseIndex][x][y][z]
    private int poseIndex   = 0;
    private int tickCounter = 0;
    public  int ticksPerPose = 18;

    public Entity(String name, int x, int z,
                  boolean[][][][] poses, Color[][][][] colors) {
        this.name   = name;
        this.x      = x;
        this.z      = z;
        this.poses  = poses;
        this.colors = colors;
    }

    public int worldMinX() { return x - SIZE / 2; }
    public int worldMinZ() { return z - SIZE / 2; }

    public int toLocalX(int wx) {
        int lx = wx - worldMinX();
        return (lx >= 0 && lx < SIZE) ? lx : -1;
    }

    public int toLocalZ(int wz) {
        int lz = wz - worldMinZ();
        return (lz >= 0 && lz < SIZE) ? lz : -1;
    }

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

    /** Advance animation by one frame. Returns true if the pose changed. */
    public boolean tick() {
        tickCounter++;
        if (tickCounter >= ticksPerPose) {
            tickCounter = 0;
            poseIndex   = (poseIndex + 1) % poses.length;
            return true;  // pose flipped — cell map needs rebuild
        }
        return false;
    }
}