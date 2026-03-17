package fpsjframe.animalgeneration;

import java.util.ArrayList;
import java.util.List;

/**
 * Walks an AnimalNode tree and computes world-space TRS for every bone.
 *
 * This is the core of Skellygen — it takes the node tree definition and
 * resolves all relative positions into absolute world-space transforms,
 * ready to drive mesh generation or export to a game engine.
 *
 * Usage:
 *   AnimalNode root = buildMyAnimal();
 *   List<BoneTransform> bones = SkeletonResolver.evaluate(root);
 *   for (BoneTransform bt : bones) {
 *       // bt.id        = which bone
 *       // bt.worldTRS  = world-space transform
 *       // bt.thickness = final thickness in world space
 *   }
 */
public class SkeletonResolver {

    /** A fully resolved bone with its world-space transform */
    public static class BoneTransform {
        public final BoneId id;
        public final TRS    worldTRS;
        public final float  thickness;
        public final boolean isPhantom;

        public BoneTransform(BoneId id, TRS worldTRS, float thickness, boolean isPhantom) {
            this.id        = id;
            this.worldTRS  = worldTRS;
            this.thickness = thickness;
            this.isPhantom = isPhantom;
        }

        @Override
        public String toString() {
            return "Bone{" + id + ", " + worldTRS + ", thick=" + thickness + "}";
        }
    }

    /**
     * Evaluate the full skeleton tree starting from root.
     * Returns a flat list of all bones with world-space transforms.
     */
    public static List<BoneTransform> evaluate(AnimalNode root) {
        List<BoneTransform> results = new ArrayList<>();
        walk(root, Vec3.zero(), 1.0f, results);
        return results;
    }

    private static void walk(AnimalNode node, Vec3 parentWorldPos, float parentThickness,
                              List<BoneTransform> out) {

        // --- Resolve world position ---
        Vec3 localOffset = node.position.isAbsolute
            ? node.position.value
            : node.position.value; // rel offset from parent

        // Apply bone symmetry transform
        localOffset = applyBoneTrans(localOffset, node.boneTrans);

        // World position = parent world pos + local offset
        Vec3 worldPos = parentWorldPos.add(localOffset);

        // --- Resolve thickness (radius of this bone's capsule) ---
        float thickness = node.thickness.isAbsolute
            ? node.thickness.value
            : parentThickness * node.thickness.value;

        // --- Compute orientation: point from parent toward this bone ---
        Quaternion orientation;
        if (localOffset.length() < 1e-5f) {
            orientation = Quaternion.identity();
        } else {
            orientation = Quaternion.lookAtDefaultUp(localOffset.normalize());
        }

        // --- Bone length = length of the offset vector ---
        float boneLength = localOffset.length();

        // --- Build world TRS: position=worldPos, scale.x=boneLength for capsule test ---
        TRS worldTRS = new TRS(worldPos, orientation, new Vec3(boneLength, thickness, thickness));

        // Store this bone
        out.add(new BoneTransform(node.id, worldTRS, thickness, node.isPhantom));

        // --- Recurse into children ---
        for (AnimalNode child : node.children) {
            walk(child, worldPos, thickness, out);
        }
    }

    /** Apply reflection transform to a position vector */
    private static Vec3 applyBoneTrans(Vec3 v, AnimalNode.BoneTrans bt) {
        return switch (bt) {
            case SAME   -> v;
            case REFL_X -> new Vec3(-v.x,  v.y,  v.z);
            case REFL_Y -> new Vec3( v.x, -v.y,  v.z);
            case REFL_Z -> new Vec3( v.x,  v.y, -v.z);
        };
    }
}