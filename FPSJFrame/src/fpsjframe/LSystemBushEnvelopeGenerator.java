package fpsjframe;

/**
 * 3D L-System bush — short, wide, dense, multi-stemmed.
 *
 * Grammar: B → F[1+B][2+B][3+B][4+B][1-+B][2-+B][3-+B][4-+B]
 *   8-way branching: 4 cardinal yaws × (pitch-up and pitch-up-more)
 *   gives a dome-shaped bush with no clear trunk
 */
public class LSystemBushEnvelopeGenerator {

    public static String generate(int size, int iterations) {
        return LSystem.generate(
            new LSystem.Params()
                .axiom("B")
                // 8-way branching: N/E/S/W each with two different pitch angles
                // the double-pitch gives the rounded dome top
                .rule('B', "F[1+B][2+B][3+B][4+B][1++B][2++B][3++B][4++B]")
                .iterations(iterations)
                .branchAngle(40)   // wide splay
                .widthRatio(0.85)  // nearly as wide as tall → bushy dome
                .trunkThickness(1)
                .objectName("objectBush")
                .solidLabel("bush"),
            size, size, 'O', '#');
    }

    public static void main(String[] args) {
        int size  = args.length > 0 ? Integer.parseInt(args[0]) : 20;
        int iters = args.length > 1 ? Integer.parseInt(args[1]) : 3;
        System.out.print(generate(size, iters));
    }
}