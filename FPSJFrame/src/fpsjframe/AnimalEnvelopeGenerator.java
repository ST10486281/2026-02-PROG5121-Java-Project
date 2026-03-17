package fpsjframe;

import fpsjframe.animalgeneration.*;
import java.util.List;

/**
 * Generates a single object envelope (same format as SphereEnvelopeGenerator)
 * for a procedural animal defined by an AnimalNode skeleton tree.
 *
 * Each bone in the skeleton contributes a capsule-shaped region of solid voxels.
 * All bones are OR'd together into one voxel grid, then written out as slices.
 *
 * Envelope axis mapping (matches SphereEnvelopeGenerator / parseObject):
 *   slices (separated by ===) = X axis
 *   rows within a slice       = Y axis, row 0 = TOP = y(sizeY-1)
 *   chars within a row        = Z axis
 *
 * Usage:
 *   AnimalNode root = ExampleAnimal.buildGoat();
 *   String envelope = AnimalEnvelopeGenerator.generate(root, 20, 20, 20, 20);
 *   System.out.print(envelope);
 */
public class AnimalEnvelopeGenerator {

    /**
     * Generate an object envelope for the given animal skeleton.
     *
     * @param root      Root AnimalNode of the skeleton tree
     * @param sizeX     Voxel grid width  (X)
     * @param sizeY     Voxel grid height (Y)
     * @param sizeZ     Voxel grid depth  (Z)
     * @param slices    Number of X slices to output (usually == sizeX)
     * @param name      Name written into the envelope header
     * @param airChar   Character for empty voxels
     * @param solidChar Character for solid voxels
     */
    public static String generate(AnimalNode root,
                                   int sizeX, int sizeY, int sizeZ, int slices,
                                   String name, char airChar, char solidChar) {

        // --- Step 1: Resolve skeleton into world-space bones ---
        List<SkeletonResolver.BoneTransform> bones = SkeletonResolver.evaluate(root);

        // --- Step 2: Find bounding box of all bone positions so we can normalise ---
        // We'll map bone world positions into the voxel grid
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE, maxZ = Float.MIN_VALUE;

        for (SkeletonResolver.BoneTransform bt : bones) {
            Vec3 p = bt.worldTRS.translation;
            minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
            minZ = Math.min(minZ, p.z); maxZ = Math.max(maxZ, p.z);
        }

        // Add padding equal to the max thickness so bones at the edge aren't clipped
        float maxThick = 0;
        for (SkeletonResolver.BoneTransform bt : bones) maxThick = Math.max(maxThick, bt.thickness);
        float pad = maxThick * 1.5f;
        minX -= pad; minY -= pad; minZ -= pad;
        maxX += pad; maxY += pad; maxZ += pad;

        // World range
        float rangeX = maxX - minX;
        float rangeY = maxY - minY;
        float rangeZ = maxZ - minZ;

        // --- Step 3: Fill voxel grid ---
        // grid[x][y][z] = true means solid
        boolean[][][] grid = new boolean[sizeX][sizeY][sizeZ];

        for (int gx = 0; gx < sizeX; gx++) {
            for (int gy = 0; gy < sizeY; gy++) {
                for (int gz = 0; gz < sizeZ; gz++) {
                    // Map voxel index to world position
                    float wx = minX + (gx / (float)(sizeX - 1)) * rangeX;
                    float wy = minY + (gy / (float)(sizeY - 1)) * rangeY;
                    float wz = minZ + (gz / (float)(sizeZ - 1)) * rangeZ;
                    Vec3 voxelWorld = new Vec3(wx, wy, wz);

                    // Check if this voxel is inside any bone's capsule
                    for (SkeletonResolver.BoneTransform bt : bones) {
                        if (bt.isPhantom) continue;
                        if (isInsideCapsule(voxelWorld, bt)) {
                            grid[gx][gy][gz] = true;
                            break; // no need to check more bones
                        }
                    }
                }
            }
        }

        // --- Step 4: Write envelope format (matches SphereEnvelopeGenerator) ---
        StringBuilder sb = new StringBuilder();
        sb.append("name: ").append(name).append("\n");
        sb.append("type: object\n");
        sb.append("legend: ").append(airChar).append("=air ")
          .append(solidChar).append("=solid\n");
        sb.append("---\n");

        for (int s = 0; s < slices; s++) {
            if (s > 0) sb.append("===\n");

            // Map slice index to X voxel (same formula as SphereEnvelopeGenerator)
            int x = (slices == 1) ? (sizeX / 2)
                                  : (int) Math.round(s * (sizeX - 1.0) / (slices - 1));

            // Rows top-to-bottom = y(sizeY-1) down to y=0
            for (int row = 0; row < sizeY; row++) {
                int y = (sizeY - 1) - row;
                for (int z = 0; z < sizeZ; z++) {
                    sb.append(grid[x][y][z] ? solidChar : airChar);
                }
                sb.append('\n');
            }
        }

        return sb.toString();
    }

    /**
     * Test whether a world-space point falls inside a bone's capsule.
     *
     * A capsule is a cylinder with hemispherical caps. It's defined by:
     *   - A start point (the bone's parent position, approximated as origin offset)
     *   - An end point  (the bone's world translation)
     *   - A radius      (the bone's thickness)
     *
     * For simplicity we treat each bone as a sphere at its centre with
     * radius = thickness. For better results the capsule extends along
     * the bone's forward (X) axis by the bone's own length.
     */
    private static boolean isInsideCapsule(Vec3 point, SkeletonResolver.BoneTransform bt) {
        TRS trs = bt.worldTRS;
        float radius  = bt.thickness;
        float halfLen = trs.scale.x * 0.5f; // bone length stored in scale.x

        // Midpoint of bone segment = worldPos - forward * halfLen
        Vec3 forward = trs.rotation.rotate(new Vec3(1, 0, 0));
        Vec3 midPoint = new Vec3(
            trs.translation.x - forward.x * halfLen,
            trs.translation.y - forward.y * halfLen,
            trs.translation.z - forward.z * halfLen
        );

        Vec3 toPoint = new Vec3(
            point.x - midPoint.x,
            point.y - midPoint.y,
            point.z - midPoint.z
        );

        float proj = toPoint.dot(forward);
        float clampedProj = Math.max(-halfLen, Math.min(halfLen, proj));

        Vec3 nearest = new Vec3(
            midPoint.x + forward.x * clampedProj,
            midPoint.y + forward.y * clampedProj,
            midPoint.z + forward.z * clampedProj
        );

        Vec3 diff = new Vec3(point.x - nearest.x, point.y - nearest.y, point.z - nearest.z);
        return diff.dot(diff) <= radius * radius;
    }

    // --- Convenience overloads ---

    /** Generate with default air='O', solid='#' */
    public static String generate(AnimalNode root, int sizeX, int sizeY, int sizeZ, int slices, String name) {
        return generate(root, sizeX, sizeY, sizeZ, slices, name, 'O', '#');
    }

    /** Generate at default 20x20x20 resolution */
    public static String generate(AnimalNode root, String name) {
        return generate(root, 20, 20, 20, 20, name, 'O', '#');
    }

    public static void main(String[] args) {
        int sizeX  = args.length > 0 ? Integer.parseInt(args[0]) : 20;
        int sizeY  = args.length > 1 ? Integer.parseInt(args[1]) : 20;
        int sizeZ  = args.length > 2 ? Integer.parseInt(args[2]) : 20;
        int slices = args.length > 3 ? Integer.parseInt(args[3]) : 20;
        String animalFile = args.length > 4 ? args[4] : "animals/goat.txt";

        try {
            AnimalNode root = AnimalLoader.load(animalFile);
            String name = animalFile.replaceAll(".*/", "").replace(".txt", "");
            System.out.print(generate(root, sizeX, sizeY, sizeZ, slices, "object_" + name));
        } catch (Exception e) {
            System.err.println("Error loading animal: " + e.getMessage());
        }
    }
}