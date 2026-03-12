package fpsjframe;

/**
 * 3D L-System tree — tall, narrow, 4-way symmetric canopy.
 */
public class LSystemTreeEnvelopeGenerator {

    static final LSystem.Params PARAMS = new LSystem.Params()
        .axiom("A")
        .rule('A', "F[1+A][2+A][3+A][4+A]")
        .iterations(4)
        .branchAngle(35)
        .widthRatio(0.40)
        .objectName("objectTree")
        .solidLabel("tree");

    public static String generate(int size, int iterations) {
        return LSystem.generate(
            new LSystem.Params()
                .axiom("A")
                .rule('A', "F[1+A][2+A][3+A][4+A]")
                .iterations(iterations)
                .branchAngle(35)
                .widthRatio(0.40)
                .objectName("objectTree")
                .solidLabel("tree"),
            size, size, 'O', '#');
    }

    public static void main(String[] args) {
        int size  = args.length > 0 ? Integer.parseInt(args[0]) : 20;
        int iters = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        System.out.print(generate(size, iters));
    }
}