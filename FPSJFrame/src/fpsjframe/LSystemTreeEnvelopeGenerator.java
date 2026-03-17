package fpsjframe;

/**
 * Classic 3D L-system tree:
 *   axiom: FFFFFFA
 *   A = [&FFFA]////[&FFFA]////[&FFFA]
 *
 * & = pitch down (branch angles away from vertical)
 * / = roll CCW around heading — 4 rolls × 30° = 120° between each branch
 * → 3 branches spread evenly 120° apart around the trunk in true 3D
 *
 * iterations=2: each branch tip A expands the same way → self-similar
 */
public class LSystemTreeEnvelopeGenerator {

    public static String generate(int size, int iterations) {
        return LSystem.generate(
            new LSystem.Params()
                .axiom("FFFFFFA")
                .rule('A', "[&FFFA]////[&FFFA]////[&FFFA]")
                .iterations(2)
                .branchAngle(30)   // / rolls 30° each → 4×30° = 120° between branches
                .widthRatio(0.85)
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