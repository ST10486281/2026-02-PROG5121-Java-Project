package fpsjframe;

/**
 * 3D L-System bush — short, wide, dense, multi-stemmed.
 * Canvas is always 20³; shapeSize controls how large the bush
 * actually is within that space (default 5 = knee-high shrub).
 */
public class LSystemBushEnvelopeGenerator {

    public static String generate(int canvasSize, int iterations, int shapeSize) {
        return LSystem.generate(
            new LSystem.Params()
                .axiom("B")
                .rule('B', "F[1+B][2+B][3+B][4+B][1++B][2++B][3++B][4++B]")
                .iterations(iterations)
                .branchAngle(40)
                .widthRatio(0.85)
                .shapeSize(shapeSize)
                .trunkThickness(1)
                .objectName("objectBush")
                .solidLabel("bush"),
            canvasSize, canvasSize, 'O', '#');
    }

    /** Convenience: canvas=20, iterations=2, shapeSize=5 */
    public static String generate(int canvasSize, int iterations) {
        return generate(canvasSize, iterations, 5);
    }

    public static void main(String[] args) {
        int canvas = args.length > 0 ? Integer.parseInt(args[0]) : 20;
        int iters  = args.length > 1 ? Integer.parseInt(args[1]) : 2;
        int shape  = args.length > 2 ? Integer.parseInt(args[2]) : 5;
        System.out.print(generate(canvas, iters, shape));
    }
}