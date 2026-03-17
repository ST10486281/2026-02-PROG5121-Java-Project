package fpsjframe.animalgeneration;

/**
 * Transform: Translate + Rotate + Scale
 * Direct port of TRS.hs from AnimalClub.
 *
 * Represents an affine transformation as:
 *   Matrix = T * R * S
 *
 * Usage:
 *   TRS t = new TRS(position, rotation, scale);
 *   float[] worldPos = t.multiplyPoint(localPos);
 */
public class TRS {
    public Vec3       translation;
    public Quaternion rotation;
    public Vec3       scale;

    public TRS(Vec3 translation, Quaternion rotation, Vec3 scale) {
        this.translation = translation;
        this.rotation    = rotation;
        this.scale       = scale;
    }

    /** Identity TRS - no transform applied. Equivalent to identityTRS in TRS.hs */
    public static TRS identity() {
        return new TRS(Vec3.zero(), Quaternion.identity(), Vec3.one());
    }

    /**
     * Convert this TRS to a 4x4 homogeneous matrix (row-major float[4][4])
     * Equivalent to conv_TRS_M44 in TRS.hs
     *
     * Matrix = T * R * S
     */
    public float[][] toMatrix4x4() {
        // Scale matrix (3x3 diagonal)
        float sx = scale.x, sy = scale.y, sz = scale.z;

        // Rotation matrix (3x3)
        float[][] R = rotation.toMatrix3x3();

        // RS = R * S  (just scale each column of R)
        float[][] RS = new float[3][3];
        for (int i = 0; i < 3; i++) {
            RS[i][0] = R[i][0] * sx;
            RS[i][1] = R[i][1] * sy;
            RS[i][2] = R[i][2] * sz;
        }

        // Build 4x4 homogeneous matrix with translation
        float tx = translation.x, ty = translation.y, tz = translation.z;
        return new float[][] {
            { RS[0][0], RS[0][1], RS[0][2], tx },
            { RS[1][0], RS[1][1], RS[1][2], ty },
            { RS[2][0], RS[2][1], RS[2][2], tz },
            {        0,        0,        0,  1 }
        };
    }

    /**
     * Apply this transform to a 3D point (w=1).
     * Equivalent to mul_TRS_V3 in TRS.hs
     */
    public Vec3 multiplyPoint(Vec3 v) {
        // Scale first, then rotate, then translate
        Vec3 scaled   = new Vec3(v.x * scale.x, v.y * scale.y, v.z * scale.z);
        Vec3 rotated  = rotation.rotate(scaled);
        return rotated.add(translation);
    }

    /**
     * Apply this transform to a direction vector (w=0, no translation).
     */
    public Vec3 multiplyDirection(Vec3 v) {
        Vec3 scaled  = new Vec3(v.x * scale.x, v.y * scale.y, v.z * scale.z);
        return rotation.rotate(scaled);
    }

    /**
     * Combine parent TRS with child TRS to get world-space TRS.
     * child is expressed in parent's local space.
     */
    public TRS multiply(TRS child) {
        Vec3 worldTrans = this.multiplyPoint(child.translation);
        Quaternion worldRot = this.rotation.mul(child.rotation);
        Vec3 worldScale = this.scale.mul(child.scale);
        return new TRS(worldTrans, worldRot, worldScale);
    }

    /**
     * Scale this TRS from parent space.
     * Equivalent to lossyScaleTRS in TRS.hs
     * "Lossy" because shear components are discarded.
     */
    public TRS lossyScale(Vec3 parentScale) {
        Vec3 newTrans = translation.mul(parentScale);
        // Keep rotation unchanged
        // Recompute scale by transforming through rotation
        Quaternion invRot = rotation.conjugate();
        Vec3 scaledAxis = new Vec3(
            parentScale.x * scale.x,
            parentScale.y * scale.y,
            parentScale.z * scale.z
        );
        // Approximate: rotate parentScale into local space, multiply
        Vec3 newScale = invRot.rotate(parentScale.mul(invRot.rotate(scale)));
        // Simplified version (diagonal only, same as AnimalClub's lossy approach):
        newScale = new Vec3(
            Math.abs(newScale.x),
            Math.abs(newScale.y),
            Math.abs(newScale.z)
        );
        return new TRS(newTrans, rotation, newScale);
    }

    @Override
    public String toString() {
        return "TRS{T=" + translation + ", R=" + rotation + ", S=" + scale + "}";
    }
}