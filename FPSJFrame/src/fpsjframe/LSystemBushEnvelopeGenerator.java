package fpsjframe;

/**
 * Same 3D grammar as tree but:
 *  - shorter stems (FF not FFF)
 *  - steeper pitch down (&) so branches spread wide and low
 *  - smaller shapeSize so it sits as a ground shrub
 *  - iterations=2 for same self-similar 3-way split
 */
public class LSystemBushEnvelopeGenerator {

    public static String generate(int canvasSize, int iterations) {
        return LSystem.generate(
            new LSystem.Params()
                .axiom("FFA")
                .rule('A', "[&FFA]////[&FFA]////[&FFA]")
                .iterations(2)
                .branchAngle(30)   // 4×30°=120° between branches, & pitches down 30°
                .widthRatio(0.90)
                .shapeSize(8)      // compact shrub in lower portion of canvas
                .trunkThickness(0)
                .objectName("objectBush")
                .solidLabel("bush"),
            canvasSize, canvasSize, 'O', '#');
    }

    public static void main(String[] args) {
        int size = args.length > 0 ? Integer.parseInt(args[0]) : 20;
        System.out.print(generate(size, 2));
    }
}