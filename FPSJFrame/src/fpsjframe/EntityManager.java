package fpsjframe;

import fpsjframe.animalgeneration.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * EntityManager — manages animated entities at fixed world positions.
 *
 * Performance strategy: rebuild a flat HashMap of occupied world cells
 * once per tick. Raycaster queries are then O(1) — no per-ray loops.
 */
public class EntityManager {

    private final List<Entity>     entities = new ArrayList<>();
    private final HashMap<Long, Color> cellMap  = new HashMap<>();

    // ── player position for proximity culling ─────────────────────────────────
    public double playerX = 0;
    public double playerZ = 0;
    private static final double ACTIVE_RADIUS = 60.0;

    private boolean isNearPlayer(Entity e) {
        double dx = e.x - playerX;
        double dz = e.z - playerZ;
        return (dx * dx + dz * dz) <= (ACTIVE_RADIUS * ACTIVE_RADIUS);
    }

    private static long key(int x, int y, int z) {
        return x * 10_000_000L + y * 10_000L + z;
    }

    // ── raycaster API — O(1) ─────────────────────────────────────────────────

    public boolean isSolid(int wx, int wy, int wz) {
        return cellMap.containsKey(key(wx, wy, wz));
    }

    public Color getColor(int wx, int wy, int wz) {
        return cellMap.get(key(wx, wy, wz));
    }

    // ── tick: advance animations + rebuild cell map ───────────────────────────

    public void setPlayerPosition(double px, double pz) {
        this.playerX = px;
        this.playerZ = pz;
    }

    public void tick() {
        boolean anyChanged = false;
        for (Entity e : entities) {
            if (!isNearPlayer(e)) continue;
            boolean changed = e.tick(); // returns true if pose flipped
            if (changed) anyChanged = true;
        }

        // Only rebuild the cell map when a pose actually changed
        if (anyChanged || cellMap.isEmpty()) {
            cellMap.clear();
            for (Entity e : entities) {
                if (!isNearPlayer(e)) continue;
                int minX = e.worldMinX();
                int minZ = e.worldMinZ();
                for (int lx = 0; lx < Entity.SIZE; lx++)
                    for (int ly = 0; ly < Entity.SIZE; ly++)
                        for (int lz = 0; lz < Entity.SIZE; lz++)
                            if (e.isSolid(lx, ly, lz))
                                cellMap.put(key(minX + lx, ly, minZ + lz),
                                            e.getColor(lx, ly, lz));
            }
        }
    }

    public void spawn(Entity e) { entities.add(e); }

    // ── builders ─────────────────────────────────────────────────────────────

    public Entity buildSkeletonHorse(int wx, int wz) throws Exception {
        AnimalNode base = AnimalLoader.load("fpsjframe/animals/skeletonhorse.txt");
        Color col = new Color(220, 220, 220);

        FreeWill rightGait = new FreeWill()
            .override("fr_upper", new Vec3( 0.25f,  0.0f,   0.3f))
            .override("fr_lower", new Vec3( 0.0f,  -0.3f,  -0.3f))
            .override("fr_hoof",  new Vec3( 0.0f,  -0.3f,   0.3f))
            .override("bl_upper", new Vec3(-0.25f,  0.0f,  -0.3f))
            .override("bl_lower", new Vec3( 0.0f,  -0.3f,   0.3f))
            .override("bl_hoof",  new Vec3( 0.0f,  -0.3f,  -0.3f));

        FreeWill leftGait = new FreeWill()
            .override("fl_upper", new Vec3(-0.25f,  0.0f,   0.3f))
            .override("fl_lower", new Vec3( 0.0f,  -0.3f,  -0.3f))
            .override("fl_hoof",  new Vec3( 0.0f,  -0.3f,   0.3f))
            .override("br_upper", new Vec3( 0.25f,  0.0f,  -0.3f))
            .override("br_lower", new Vec3( 0.0f,  -0.3f,   0.3f))
            .override("br_hoof",  new Vec3( 0.0f,  -0.3f,  -0.3f));

        boolean[][][][] poses  = new boolean[2][Entity.SIZE][Entity.SIZE][Entity.SIZE];
        Color[][][][]   colors = new Color  [2][Entity.SIZE][Entity.SIZE][Entity.SIZE];
        bakePose(rightGait.apply(base), poses[0], colors[0], col);
        bakePose(leftGait.apply(base),  poses[1], colors[1], col);

        Entity e = new Entity("skeletonHorse", wx, wz, poses, colors);
        e.ticksPerPose = 15;
        return e;
    }

    public Entity buildGoat(int wx, int wz) throws Exception {
        AnimalNode base = AnimalLoader.load("fpsjframe/animals/goat.txt");
        Color col = new Color(180, 160, 130);

        FreeWill rightGait = new FreeWill()
            .override("leg_hip_fr",  new Vec3( 0.22f,  0.0f,   0.35f))
            .override("leg_knee_fr", new Vec3( 0.0f,  -0.35f,  0.15f))
            .override("leg_foot_fr", new Vec3( 0.0f,  -0.28f,  0.05f))
            .override("leg_hip_bl",  new Vec3(-0.22f,  0.0f,  -0.35f))
            .override("leg_knee_bl", new Vec3( 0.0f,  -0.35f,  0.1f))
            .override("leg_foot_bl", new Vec3( 0.0f,  -0.28f, -0.05f));

        FreeWill leftGait = new FreeWill()
            .override("leg_hip_fl",  new Vec3(-0.22f,  0.0f,   0.35f))
            .override("leg_knee_fl", new Vec3( 0.0f,  -0.35f,  0.15f))
            .override("leg_foot_fl", new Vec3( 0.0f,  -0.28f,  0.05f))
            .override("leg_hip_br",  new Vec3( 0.22f,  0.0f,  -0.35f))
            .override("leg_knee_br", new Vec3( 0.0f,  -0.35f,  0.1f))
            .override("leg_foot_br", new Vec3( 0.0f,  -0.28f, -0.05f));

        boolean[][][][] poses  = new boolean[2][Entity.SIZE][Entity.SIZE][Entity.SIZE];
        Color[][][][]   colors = new Color  [2][Entity.SIZE][Entity.SIZE][Entity.SIZE];
        bakePose(rightGait.apply(base), poses[0], colors[0], col);
        bakePose(leftGait.apply(base),  poses[1], colors[1], col);

        Entity e = new Entity("goat", wx, wz, poses, colors);
        e.ticksPerPose = 18;
        return e;
    }

    // ── bake AnimalNode → voxel arrays ───────────────────────────────────────

    private void bakePose(AnimalNode root, boolean[][][] voxels,
                          Color[][][] colors, Color bodyColor) {
        int S = Entity.SIZE;
        String env = AnimalEnvelopeGenerator.generate(root, S, S, S, S, "entity");
        String[] lines = env.split("\n");
        int x = -1, y = 0;
        for (String line : lines) {
            if (line.startsWith("name:") || line.startsWith("type:")
                    || line.startsWith("legend:") || line.startsWith("---")) continue;
            if (line.equals("===")) { x++; y = 0; continue; }
            if (x < 0) { x = 0; continue; }
            if (x >= S) break;
            int vy = (S - 1) - y;
            if (vy >= 0 && vy < S) {
                for (int z = 0; z < line.length() && z < S; z++) {
                    if (line.charAt(z) == '#') {
                        voxels[x][vy][z] = true;
                        colors[x][vy][z] = bodyColor;
                    }
                }
            }
            y++;
        }
    }
}