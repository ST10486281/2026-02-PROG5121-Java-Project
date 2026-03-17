package fpsjframe;

/**
 * Parametric cow generator — stamps body parts at calculated positions.
 *
 * All coords in local voxel space, origin = bottom-front-left of 20³ canvas.
 * Y=0 is bottom, Y=19 is top.
 *
 * Parts:
 *   body  — wide box, centred horizontally, mid-height
 *   head  — smaller box, front of body, slightly raised
 *   neck  — connects head to body
 *   4 legs — pillars hanging below body at corners
 *   tail  — thin rod behind body
 *   horns — two short prongs above head
 */
public class CowEnvelopeGenerator {

    static final int S = 20; // canvas size
    static final char AIR   = 'O';
    static final char SOLID = '#';

    static char[][][] vox = new char[S][S][S]; // vox[x][y][z]

    public static String generate() {
        // clear
        for (int x=0;x<S;x++) for (int y=0;y<S;y++) for (int z=0;z<S;z++)
            vox[x][y][z] = AIR;

        int cx = S/2;  // centre X
        int cz = S/2;  // centre Z

        int legH      = 5;   // leg height
        int bodyY     = legH; // bottom of body sits on top of legs
        int bodyH     = 4;   // body height
        int bodyW     = 5;   // body half-width in X
        int bodyD     = 8;   // body depth in Z

        int bodyZmin  = cz - bodyD/2;
        int bodyZmax  = cz + bodyD/2;
        int bodyXmin  = cx - bodyW/2;
        int bodyXmax  = cx + bodyW/2;

        // ── body ──────────────────────────────────────────────────────────────
        box(bodyXmin, bodyY, bodyZmin,  bodyXmax, bodyY+bodyH, bodyZmax);

        // ── legs ──────────────────────────────────────────────────────────────
        int lx1 = bodyXmin + 1,  lx2 = bodyXmax - 1;
        int lz1 = bodyZmin + 1,  lz2 = bodyZmax - 1;
        int legsBottom = 1; // leave 1 cell gap from floor so it looks grounded
        pillar(lx1, legsBottom, lz1,  legH); // front-left
        pillar(lx2, legsBottom, lz1,  legH); // front-right
        pillar(lx1, legsBottom, lz2,  legH); // back-left
        pillar(lx2, legsBottom, lz2,  legH); // back-right

        // ── neck ──────────────────────────────────────────────────────────────
        int neckZ  = bodyZmin - 1;
        int neckY  = bodyY + 1;
        box(cx-1, neckY, neckZ,  cx+1, bodyY+bodyH, neckZ);

        // ── head ──────────────────────────────────────────────────────────────
        int headZ  = bodyZmin - 3;
        int headY  = bodyY + 1;
        box(cx-2, headY, headZ,  cx+2, headY+3, bodyZmin-1);

        // ── horns ─────────────────────────────────────────────────────────────
        int hornY  = headY + 4;
        int hornZ  = headZ + 1;
        set(cx-2, hornY, hornZ);
        set(cx+2, hornY, hornZ);

        // ── tail ──────────────────────────────────────────────────────────────
        int tailZ  = bodyZmax + 1;
        int tailY  = bodyY + bodyH;
        set(cx, tailY,   tailZ);
        set(cx, tailY+1, tailZ+1);
        set(cx, tailY+2, tailZ+1);

        return toEnvelope("objectCow");
    }

    // ── stamp helpers ─────────────────────────────────────────────────────────

    static void set(int x, int y, int z) {
        if (x>=0&&x<S && y>=0&&y<S && z>=0&&z<S) vox[x][y][z] = SOLID;
    }

    static void box(int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x=x0;x<=x1;x++) for (int y=y0;y<=y1;y++) for (int z=z0;z<=z1;z++)
            set(x,y,z);
    }

    static void pillar(int x, int yBot, int z, int h) {
        for (int y=yBot; y<yBot+h; y++) set(x, y, z);
    }

    // ── envelope serialiser ───────────────────────────────────────────────────
    // Format mirrors what parseObject() expects:
    //   slices separated by "===\n"  → slice index = X
    //   rows within slice            → row 0 = top = y(S-1)
    //   chars within row             → z index

    static String toEnvelope(String name) {
        StringBuilder sb = new StringBuilder();
        sb.append("name: ").append(name).append("\n");
        sb.append("type: object\n");
        sb.append("legend: O=air #=solid\n");
        sb.append("air: O\n");
        sb.append("solid: #\n");
        sb.append("---\n");
        for (int x=0; x<S; x++) {
            if (x > 0) sb.append("===\n");
            for (int row=0; row<S; row++) {
                int y = (S-1) - row; // row 0 = top
                for (int z=0; z<S; z++) sb.append(vox[x][y][z]);
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.print(generate());
    }
}