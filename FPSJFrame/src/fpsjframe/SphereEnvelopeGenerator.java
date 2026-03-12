package fpsjframe;

/**
 * Generates a filled-sphere object envelope procedurally.
 *
 * Envelope axis mapping (from parseObject):
 *   slices (separated by ===) = X axis
 *   rows within a slice       = Y axis, row 0 = TOP = y(OBJ_SIZE-1)
 *   chars within a row        = Z axis
 *
 * The sphere is centred in the volume and scaled to fit.
 */
public class SphereEnvelopeGenerator {

    public static String generate(int sizeX, int sizeY, int sizeZ, int slices, char airChar, char solidChar) {
        StringBuilder sb = new StringBuilder();
        sb.append("name: objectSphere\n");
        sb.append("type: object\n");
        sb.append("legend: ").append(airChar).append("=air ").append(solidChar).append("=sphere\n");
        sb.append("---\n");

        double cx = (sizeX - 1) / 2.0;
        double cy = (sizeY - 1) / 2.0;
        double cz = (sizeZ - 1) / 2.0;
        double rx = cx, ry = cy, rz = cz;

        for (int s = 0; s < slices; s++) {
            if (s > 0) sb.append("===\n");

            // Map slice index to X position across the full sizeX range
            int x = (slices == 1) ? (int) Math.round(cx)
                                  : (int) Math.round(s * (sizeX - 1.0) / (slices - 1));

            // rows top-to-bottom = y(sizeY-1) down to y=0
            for (int row = 0; row < sizeY; row++) {
                int y = (sizeY - 1) - row;  // row 0 = top = y(sizeY-1)
                for (int z = 0; z < sizeZ; z++) {
                    double dx = (x - cx) / rx;
                    double dy = (y - cy) / ry;
                    double dz = (z - cz) / rz;
                    sb.append(dx*dx + dy*dy + dz*dz <= 1.0 ? solidChar : airChar);
                }
                sb.append('\n');
            }
        }

        return sb.toString();
    }

    // Defaults: 20x20x20, 20 slices
    public static String generate(int slices) {
        return generate(20, 20, 20, slices, 'O', '#');
    }

    public static void main(String[] args) {
        int sizeX     = args.length > 0 ? Integer.parseInt(args[0]) : 20;
        int sizeY     = args.length > 1 ? Integer.parseInt(args[1]) : 20;
        int sizeZ     = args.length > 2 ? Integer.parseInt(args[2]) : 20;
        int slices    = args.length > 3 ? Integer.parseInt(args[3]) : 20;
        char airChar  = args.length > 4 ? args[4].charAt(0) : 'O';
        char solidChar = args.length > 5 ? args[5].charAt(0) : '#';

        System.out.print(generate(sizeX, sizeY, sizeZ, slices, airChar, solidChar));
    }
}