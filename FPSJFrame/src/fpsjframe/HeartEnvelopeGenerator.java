package fpsjframe;

/**
 * Generates a heart-shaped object envelope using the formula:
 *   (x² + 9/4·y² + z² - 1)³ - x²z³ - 9/200·y²z³ = 0
 *
 * The iconic heart silhouette (two bumps at top, point at bottom) appears
 * when looking along the X axis in the formula's Y-Z plane.
 * We orient so:
 *   formula-x → voxel-x  (left/right symmetry axis)
 *   formula-z → voxel-y  (up/down — point at bottom, bumps at top)
 *   formula-y → voxel-z  (depth into the object)
 *
 * Envelope axis mapping (from parseObject):
 *   slices (===) = voxel-X
 *   rows         = voxel-Y, row 0 = top
 *   chars        = voxel-Z
 */
public class HeartEnvelopeGenerator {

    public static String generate(int sizeX, int sizeY, int sizeZ, int slices, char airChar, char solidChar) {
        StringBuilder sb = new StringBuilder();
        sb.append("name: objectHeart\n");
        sb.append("type: object\n");
        sb.append("legend: ").append(airChar).append("=air ").append(solidChar).append("=heart\n");
        sb.append("---\n");

        double scaleX = 3.0 / sizeX;
        double scaleY = 3.0 / sizeY;
        double scaleZ = 3.0 / sizeZ;

        for (int s = 0; s < slices; s++) {
            if (s > 0) sb.append("===\n");

            int xi = (slices == 1) ? sizeX / 2
                                   : (int) Math.round(s * (sizeX - 1.0) / (slices - 1));

            // formula-x maps to voxel-x
            double fx = (xi - sizeX / 2.0) * scaleX;

            for (int row = 0; row < sizeY; row++) {
                // row 0 = top = voxel-y(sizeY-1)
                int yi = (sizeY - 1) - row;
                // formula-z maps to voxel-y (upright: point at bottom = -fz, bumps at top = +fz)
                double fz = (yi - sizeY / 2.0) * scaleY;

                for (int zi = 0; zi < sizeZ; zi++) {
                    // formula-y maps to voxel-z (depth)
                    double fy = (zi - sizeZ / 2.0) * scaleZ;

                    double val = Math.pow(fx*fx + (9.0/4.0)*fy*fy + fz*fz - 1, 3)
                                 - fx*fx * fz*fz*fz
                                 - (9.0/200.0) * fy*fy * fz*fz*fz;

                    sb.append(val <= 0 ? solidChar : airChar);
                }
                sb.append('\n');
            }
        }

        return sb.toString();
    }

    public static String generate(int slices) {
        return generate(20, 20, 20, slices, 'O', '#');
    }

    public static void main(String[] args) {
        int sizeX      = args.length > 0 ? Integer.parseInt(args[0]) : 20;
        int sizeY      = args.length > 1 ? Integer.parseInt(args[1]) : 20;
        int sizeZ      = args.length > 2 ? Integer.parseInt(args[2]) : 20;
        int slices     = args.length > 3 ? Integer.parseInt(args[3]) : 20;
        char airChar   = args.length > 4 ? args[4].charAt(0) : 'O';
        char solidChar = args.length > 5 ? args[5].charAt(0) : '#';

        System.out.print(generate(sizeX, sizeY, sizeZ, slices, airChar, solidChar));
    }
}