package fpsjframe.animalgeneration;

import java.util.HashMap;
import java.util.Map;

/**
 * FreeWill — a runtime transformer applied on top of an animal's genetics.
 *
 * Genetics define what an animal was born with (bone positions, thickness).
 * FreeWill defines what the animal chose to do with those bones — a pose.
 *
 * Usage:
 *   FreeWill pose = new FreeWill()
 *       .override("leg_hip_fl", new Vec3(0.0f, -0.1f, 0.25f))  // step forward
 *       .override("leg_knee_fl", new Vec3(0.0f, -0.3f, 0.1f)); // bend knee
 *
 *   AnimalNode posed = pose.apply(original);
 */
public class FreeWill {

    // Map of bone id name → new relative position offset to apply
    private final Map<String, Vec3> positionOverrides = new HashMap<>();

    /**
     * Override the relative position of a named bone.
     * The new position replaces the bone's original position entirely.
     */
    public FreeWill override(String boneName, Vec3 newRelativePosition) {
        positionOverrides.put(boneName, newRelativePosition);
        return this; // fluent API
    }

    /**
     * Apply this FreeWill pose to an AnimalNode tree.
     * Returns a new tree — the original is not modified.
     */
    public AnimalNode apply(AnimalNode root) {
        return applyToNode(root);
    }

    private AnimalNode applyToNode(AnimalNode node) {
        // Check if this bone has a free will override
        String name = node.id.name;
        AnimalNode.AbsOrRel<Vec3> newPos = node.position;

        if (positionOverrides.containsKey(name)) {
            Vec3 overridePos = positionOverrides.get(name);
            // Always apply as relative — free will is a choice relative to your genetics
            newPos = AnimalNode.AbsOrRel.rel(overridePos);
        }

        // Build a copy of this node with the (possibly overridden) position
        AnimalNode copy = new AnimalNode(node.id, node.isPhantom, newPos, node.thickness);
        copy.boneTrans = node.boneTrans;

        // Recurse into children
        for (AnimalNode child : node.children) {
            copy.addChild(applyToNode(child));
        }

        return copy;
    }
}