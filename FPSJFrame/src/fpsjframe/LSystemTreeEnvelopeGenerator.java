package fpsjframe;

/**
 * Simple 2-level tree: stem → 4 branches → done.
 * No recursion beyond one branch tier.
 *
 * Axiom: FFFFFA
 * Rule A: [1+FFFB][2+FFFB][3+FFFB][4+FFFB]
 * Rule B: FF   ← just a short tip, terminates
 *
 * iterations=1 so the rule fires exactly once — one tier of branches only.
 */
public class LSystemTreeEnvelopeGenerator {

    public static String generate(int size, int iterations) {
        return LSystem.generate(
            new LSystem.Params()
                .axiom("FFFFFFA")
                .rule('A', "[1+FFFFB][2+FFFFB][3+FFFFB][4+FFFFB]")
                .rule('B', "FFF")
                .iterations(1)          // exactly one branch tier, ignore iterations arg
                .branchAngle(55)
                .widthRatio(0.50)
                .trunkThickness(0)      // thinnest possible — 1-voxel lines
                .objectName("objectTree")
                .solidLabel("tree"),
            size, size, 'O', '#');
    }

    public static void main(String[] args) {
        int size = args.length > 0 ? Integer.parseInt(args[0]) : 20;
        System.out.print(generate(size, 1));
    }
}