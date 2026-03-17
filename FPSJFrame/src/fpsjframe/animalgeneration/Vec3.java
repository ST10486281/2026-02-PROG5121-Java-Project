package fpsjframe.animalgeneration;

/**
 * 3D vector - equivalent to Linear.V3 in AnimalClub
 */
public class Vec3 {
    public float x, y, z;

    public Vec3(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
    }

    public static Vec3 zero()  { return new Vec3(0, 0, 0); }
    public static Vec3 one()   { return new Vec3(1, 1, 1); }
    public static Vec3 unitX() { return new Vec3(1, 0, 0); }
    public static Vec3 unitY() { return new Vec3(0, 1, 0); }
    public static Vec3 unitZ() { return new Vec3(0, 0, 1); }

    public Vec3 add(Vec3 o)    { return new Vec3(x+o.x, y+o.y, z+o.z); }
    public Vec3 sub(Vec3 o)    { return new Vec3(x-o.x, y-o.y, z-o.z); }
    public Vec3 scale(float s) { return new Vec3(x*s, y*s, z*s); }
    public Vec3 mul(Vec3 o)    { return new Vec3(x*o.x, y*o.y, z*o.z); } // component-wise

    public float dot(Vec3 o)   { return x*o.x + y*o.y + z*o.z; }
    public float lengthSq()    { return dot(this); }
    public float length()      { return (float) Math.sqrt(lengthSq()); }

    public Vec3 normalize() {
        float len = length();
        if (len < 1e-7f) return zero();
        return scale(1f / len);
    }

    public Vec3 cross(Vec3 o) {
        return new Vec3(
            y*o.z - z*o.y,
            z*o.x - x*o.z,
            x*o.y - y*o.x
        );
    }

    public boolean nearZero() { return lengthSq() < 1e-10f; }

    @Override
    public String toString() { return "Vec3(" + x + ", " + y + ", " + z + ")"; }
}