package fpsjframe;

import fpsjframe.animalgeneration.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * EntityManager — owns all living entities in the world.
 *
 * Responsibilities:
 *   - Build entities from animal txt files + FreeWill gait poses
 *   - Spawn entities at world positions
 *   - Tick all entities each frame (animation + AI)
 *   - Answer isSolid / getColor queries from the raycaster
 *
 * The raycaster checks the static world first, then calls the EntityManager.
 * Entities are rendered by the same DDA raycaster — they get fog, shade,
 * and depth for free.
 */
public class EntityManager {

    private final List<Entity> entities = new ArrayList<>();
    private final WorldBuilder world;

    public EntityManager(WorldBuilder world) {
        this.world = world;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Raycaster query API — called from FPSJFrame renderFrame()
    // ─────────────────────────────────────────────────────────────────────────

    public boolean isSolid(int wx, int wy, int wz) {
        for (Entity e : entities) {
            int lx = e.toLocalX(wx);
            int lz = e.toLocalZ(wz);
            if (lx < 0 || lz < 0) continue;
            if (e.isSolid(lx, wy, lz)) return true;
        }
        return false;
    }

    public Color getColor(int wx, int wy, int wz) {
        for (Entity e : entities) {
            int lx = e.toLocalX(wx);
            int lz = e.toLocalZ(wz);
            if (lx < 0 || lz < 0) continue;
            if (e.isSolid(lx, wy, lz)) return e.getColor(lx, wy, lz);
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tick — call once per frame from game loop
    // ─────────────────────────────────────────────────────────────────────────

    public void tick() {
        for (Entity e : entities) e.tick(world);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Spawn
    // ─────────────────────────────────────────────────────────────────────────

    public void spawn(Entity e) {
        entities.add(e);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Animal builders — create entities with 2-pose walking gait
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Skeleton horse — right/left diagonal gait, grey.
     */
    public Entity buildSkeletonHorse(double wx, double wz) throws Exception {
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

    /**
     * Goat — right/left diagonal gait, sandy brown.
     */
    public Entity buildGoat(double wx, double wz) throws Exception {
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

    // ─────────────────────────────────────────────────────────────────────────
    // Bake an AnimalNode pose into raw voxel arrays
    // ─────────────────────────────────────────────────────────────────────────

    private void bakePose(AnimalNode root,
                          boolean[][][] voxels, Color[][][] colors,
                          Color bodyColor) {
        int S = Entity.SIZE;
        String env = AnimalEnvelopeGenerator.generate(root, S, S, S, S, "entity");

        // Parse the envelope format: header, then slices separated by ===
        // Slices = X axis, rows = Y (row 0 = top = high Y), chars = Z
        String[] lines = env.split("\n");
        int x = -1;
        int y = 0;

        for (String line : lines) {
            if (line.startsWith("name:") || line.startsWith("type:")
                    || line.startsWith("legend:") || line.startsWith("---")) continue;

            if (line.equals("===")) {
                x++;
                y = 0;
                continue;
            }

            if (x < 0) { x = 0; continue; } // first slice before any ===

            if (x >= S) break;

            // Each char in the line is a Z position
            // row y=0 is the TOP of the model → maps to voxel Y = (S-1-y)
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