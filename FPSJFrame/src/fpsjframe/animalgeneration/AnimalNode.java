package fpsjframe.animalgeneration;

import java.util.ArrayList;
import java.util.List;

/**
 * A node in the procedural skeleton tree.
 * Direct port of AnimalNode in AnimalNode.hs from AnimalClub.
 *
 * Each node represents a "limb" between itself and its parent.
 * The tree is recursively walked to build a skeleton.
 *
 * Example - building a simple 4-legged animal:
 *
 *   AnimalNode body = new AnimalNode(new BoneId("body"), false,
 *       AbsOrRel.abs(new Vec3(0, 0, 0)), AbsOrRel.abs(0.5f));
 *
 *   AnimalNode frontLeg = new AnimalNode(new BoneId("leg", BoneId.Flag.FRONT, BoneId.Flag.LEFT),
 *       false, AbsOrRel.rel(new Vec3(1, -1, 0.5f)), AbsOrRel.rel(0.4f));
 *   body.addChild(frontLeg);
 */
public class AnimalNode {

    /** Whether position/thickness is absolute (world) or relative (to parent) */
    public static class AbsOrRel<T> {
        public final T value;
        public final boolean isAbsolute;

        private AbsOrRel(T value, boolean isAbsolute) {
            this.value      = value;
            this.isAbsolute = isAbsolute;
        }

        public static <T> AbsOrRel<T> abs(T v) { return new AbsOrRel<>(v, true); }
        public static <T> AbsOrRel<T> rel(T v) { return new AbsOrRel<>(v, false); }

        @Override public String toString() {
            return (isAbsolute ? "Abs(" : "Rel(") + value + ")";
        }
    }

    /** Reflection transform for creating symmetric limbs - equivalent to BoneTrans in AnimalNode.hs */
    public enum BoneTrans {
        SAME,   // no transform
        REFL_X, // mirror front/back  (negate X)
        REFL_Y, // mirror top/bottom  (negate Y)
        REFL_Z  // mirror left/right  (negate Z)
    }

    // --- Fields (mirrors AnimalNode record in Haskell) ---
    public BoneId             id;
    public BoneTrans          boneTrans;
    public AbsOrRel<Vec3>     position;   // _pos in Haskell
    public AbsOrRel<Float>    thickness;  // _thickness in Haskell
    public boolean            isPhantom;  // invisible node (no mesh), _isPhantom
    public List<AnimalNode>   children;   // _children

    public AnimalNode(BoneId id, boolean isPhantom,
                      AbsOrRel<Vec3> position, AbsOrRel<Float> thickness) {
        this.id        = id;
        this.boneTrans = BoneTrans.SAME;
        this.position  = position;
        this.thickness = thickness;
        this.isPhantom = isPhantom;
        this.children  = new ArrayList<>();
    }

    public void addChild(AnimalNode child) { children.add(child); }

    /**
     * Create a mirrored copy of this node and all its children.
     * Equivalent to flipAnimalNode in AnimalNode.hs
     *
     * Used to auto-generate the right leg from the left leg definition, etc.
     */
    public AnimalNode flip(BoneTrans transform) {
        AnimalNode copy = new AnimalNode(
            applyFlagTrans(id, transform),
            isPhantom,
            applyPosTrans(position, transform),
            thickness
        );
        copy.boneTrans = composeBoneTrans(transform, this.boneTrans);

        // Children inherit the transform but keep their own relative positions
        for (AnimalNode child : children) {
            copy.addChild(child.flip(BoneTrans.SAME));
        }
        return copy;
    }

    /** Compose two BoneTrans operations - equivalent to composeBoneTrans in AnimalNode.hs */
    public static BoneTrans composeBoneTrans(BoneTrans a, BoneTrans b) {
        if (a == BoneTrans.SAME) return b;
        if (b == BoneTrans.SAME) return a;
        if (a == b)              return BoneTrans.SAME; // double reflection = identity
        return BoneTrans.SAME; // simplified - ArbTrans not needed for basic animals
    }

    /** Apply position transform for mirroring */
    private AbsOrRel<Vec3> applyPosTrans(AbsOrRel<Vec3> pos, BoneTrans bt) {
        Vec3 v = pos.value;
        Vec3 transformed = switch (bt) {
            case SAME   -> v;
            case REFL_X -> new Vec3(-v.x,  v.y,  v.z);
            case REFL_Y -> new Vec3( v.x, -v.y,  v.z);
            case REFL_Z -> new Vec3( v.x,  v.y, -v.z);
        };
        return pos.isAbsolute
            ? AbsOrRel.abs(transformed)
            : AbsOrRel.rel(transformed);
    }

    /** Update bone flags for mirroring */
    private BoneId applyFlagTrans(BoneId bid, BoneTrans bt) {
        return switch (bt) {
            case REFL_Z -> bid.mirrorZ();
            case REFL_X -> bid.mirrorX();
            default     -> bid;
        };
    }

    /** Collect all BoneIds in this subtree */
    public List<BoneId> allBoneIds() {
        List<BoneId> result = new ArrayList<>();
        collectIds(result);
        return result;
    }

    private void collectIds(List<BoneId> acc) {
        acc.add(id);
        for (AnimalNode child : children) child.collectIds(acc);
    }

    @Override
    public String toString() {
        return "AnimalNode{" + id + ", phantom=" + isPhantom
             + ", pos=" + position + ", thick=" + thickness
             + ", children=" + children.size() + "}";
    }
}