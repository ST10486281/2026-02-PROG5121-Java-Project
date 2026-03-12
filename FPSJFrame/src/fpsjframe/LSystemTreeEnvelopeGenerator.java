package fpsjframe;

/**
 * Stem → 3 branches at 120° yaw, each pitching outward from trunk → each splits same way
 *
 * Key fix: yaw FIRST with world-Y presets (1/2/3 = 0/120/240°), THEN pitch outward (+)
 * This ensures each branch leans away from trunk in its own compass direction,
 * not all leaning the same world-space way.
 *
 * Rule A: 3-way split using world-Y yaw symbols then pitch up-and-out
 *   [1+FFFA]  = face 0°,   lean out 25°, grow FFF, recurse
 *   [2+FFFA]  = face 120°, lean out 25°, grow FFF, recurse
 *   [3+FFFA]  = face 240°, lean out 25°, grow FFF, recurse
 *
 * iterations=2 → self-similar: each branch tip splits into 3 the same way
 */
public class LSystemTreeEnvelopeGenerator {

    public static String generate(int size, int iterations) {
        return LSystem.generate(
            new LSystem.Params()
                .axiom("FFFFFFA")
                .rule('A', "[1+FFFA][2+FFFA][3+FFFA]")
                .iterations(2)
                .branchAngle(28)
                .widthRatio(0.75)
                .trunkThickness(0)
                .objectName("objectTree")
                .solidLabel("tree"),
            size, size, 'O', '#');
    }

    public static void main(String[] args) {
        int size = args.length > 0 ? Integer.parseInt(args[0]) : 20;
        System.out.print(generate(size, 2));
    }
}