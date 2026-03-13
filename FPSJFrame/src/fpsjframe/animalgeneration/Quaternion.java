package fpsjframe.animalgeneration;

/**
 * Quaternion for rotations - equivalent to Linear.Quaternion in AnimalClub
 * Stored as (w, x, y, z)
 */
public class Quaternion {
    public float w, x, y, z;

    public Quaternion(float w, float x, float y, float z) {
        this.w = w; this.x = x; this.y = y; this.z = z;
    }

    /** Identity rotation - no rotation applied */
    public static Quaternion identity() {
        return new Quaternion(1, 0, 0, 0);
    }

    /** Conjugate = inverse for unit quaternions */
    public Quaternion conjugate() {
        return new Quaternion(w, -x, -y, -z);
    }

    public float lengthSq() { return w*w + x*x + y*y + z*z; }
    public float length()   { return (float) Math.sqrt(lengthSq()); }

    public Quaternion normalize() {
        float len = length();
        if (len < 1e-7f) return identity();
        return new Quaternion(w/len, x/len, y/len, z/len);
    }

    /** Quaternion multiplication - combines two rotations */
    public Quaternion mul(Quaternion o) {
        return new Quaternion(
            w*o.w - x*o.x - y*o.y - z*o.z,
            w*o.x + x*o.w + y*o.z - z*o.y,
            w*o.y - x*o.z + y*o.w + z*o.x,
            w*o.z + x*o.y - y*o.x + z*o.w
        );
    }

    /** Rotate a vector by this quaternion */
    public Vec3 rotate(Vec3 v) {
        // q * (0, v) * q^-1
        Quaternion qv  = new Quaternion(0, v.x, v.y, v.z);
        Quaternion res = this.mul(qv).mul(this.conjugate());
        return new Vec3(res.x, res.y, res.z);
    }

    /** Convert to 3x3 rotation matrix (row-major float[3][3]) */
    public float[][] toMatrix3x3() {
        float xx = x*x, yy = y*y, zz = z*z;
        float xy = x*y, xz = x*z, yz = y*z;
        float wx = w*x, wy = w*y, wz = w*z;
        return new float[][] {
            { 1-2*(yy+zz),   2*(xy-wz),   2*(xz+wy) },
            {   2*(xy+wz), 1-2*(xx+zz),   2*(yz-wx) },
            {   2*(xz-wy),   2*(yz+wx), 1-2*(xx+yy) }
        };
    }

    /**
     * Create rotation from axis and angle (radians)
     * Equivalent to Linear.axisAngle
     */
    public static Quaternion axisAngle(Vec3 axis, float angle) {
        Vec3 n = axis.normalize();
        float half = angle * 0.5f;
        float s = (float) Math.sin(half);
        return new Quaternion((float) Math.cos(half), n.x*s, n.y*s, n.z*s);
    }

    /**
     * Rotation from Euler angles XYZ order
     * Equivalent to fromEulerXYZ in TRS.hs
     */
    public static Quaternion fromEulerXYZ(float rx, float ry, float rz) {
        Quaternion qx = axisAngle(Vec3.unitX(), rx);
        Quaternion qy = axisAngle(Vec3.unitY(), ry);
        Quaternion qz = axisAngle(Vec3.unitZ(), rz);
        return qz.mul(qy).mul(qx);
    }

    /**
     * Rotation that takes fromVec direction to toVec direction
     * Equivalent to fromTo in TRS.hs
     */
    public static Quaternion fromTo(Vec3 from, Vec3 to) {
        Vec3 u = from.normalize();
        Vec3 v = to.normalize();
        Vec3 diff = new Vec3(u.x + v.x, u.y + v.y, u.z + v.z);

        if (diff.nearZero()) {
            // 180 degree rotation - find orthogonal axis
            return new Quaternion(0, orthogonal(u).x, orthogonal(u).y, orthogonal(u).z).normalize();
        }
        if (new Vec3(u.x - v.x, u.y - v.y, u.z - v.z).nearZero()) {
            return identity();
        }
        Vec3 half = diff.normalize();
        Vec3 cross = u.cross(half);
        return new Quaternion(u.dot(half), cross.x, cross.y, cross.z);
    }

    /** Rotation looking toward direction (neutral = +X axis, up = +Y) */
    public static Quaternion lookAtDefaultUp(Vec3 direction) {
        return fromTo(Vec3.unitX(), direction);
    }

    private static Vec3 orthogonal(Vec3 v) {
        float ax = Math.abs(v.x), ay = Math.abs(v.y), az = Math.abs(v.z);
        Vec3 other;
        if (ax < ay) other = (ax < az) ? Vec3.unitX() : Vec3.unitZ();
        else         other = (ay < az) ? Vec3.unitY() : Vec3.unitZ();
        return v.cross(other).normalize();
    }

    @Override
    public String toString() { return "Quat(" + w + ", " + x + ", " + y + ", " + z + ")"; }
}