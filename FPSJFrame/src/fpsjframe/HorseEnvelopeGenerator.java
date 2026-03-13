package fpsjframe;

/**
 * Parametric horse — more detail than the cow:
 *  - longer legs, narrower at bottom (tapered)
 *  - elongated body
 *  - distinct neck angled forward
 *  - head with snout
 *  - ears
 *  - mane along neck/back
 *  - tail
 */
public class HorseEnvelopeGenerator {

    static final int S = 20;
    static final char AIR   = 'O';
    static final char SOLID = '#';
    static char[][][] vox = new char[S][S][S];

    public static String generate() {
        for (int x=0;x<S;x++) for (int y=0;y<S;y++) for (int z=0;z<S;z++)
            vox[x][y][z] = AIR;

        int cx = S/2; // 10

        // ── legs ─────────────────────────────────────────────────────────────
        // 4 legs, tapered: 2 wide at top, 1 wide at bottom
        int legBot = 1, legTop = 6;
        int[] lx = { 7, 12, 7, 12 };  // front-left, front-right, back-left, back-right
        int[] lz = { 5,  5, 14, 14 };
        for (int i = 0; i < 4; i++) {
            // bottom half — single voxel wide
            for (int y = legBot; y < legBot+3; y++) set(lx[i], y, lz[i]);
            // top half — 2×2 where leg meets body
            for (int y = legBot+3; y <= legTop; y++) {
                set(lx[i],   y, lz[i]);
                set(lx[i]+1, y, lz[i]);
            }
        }

        // ── body ─────────────────────────────────────────────────────────────
        // wide barrel, sits on top of legs
        int bodyY0 = legTop, bodyY1 = legTop + 4;
        int bodyX0 = 6, bodyX1 = 13;
        int bodyZ0 = 4, bodyZ1 = 15;
        box(bodyX0, bodyY0, bodyZ0,  bodyX1, bodyY1, bodyZ1);

        // ── neck ─────────────────────────────────────────────────────────────
        // angled forward — steps diagonally in Z and Y
        int neckX0 = 8, neckX1 = 11;
        // 3 segments stepping forward and up
        box(neckX0, bodyY1,   bodyZ0-1, neckX1, bodyY1+1, bodyZ0-1);
        box(neckX0, bodyY1+1, bodyZ0-2, neckX1, bodyY1+2, bodyZ0-2);
        box(neckX0, bodyY1+2, bodyZ0-3, neckX1, bodyY1+3, bodyZ0-3);

        // ── head ─────────────────────────────────────────────────────────────
        int headY0 = bodyY1+1, headY1 = bodyY1+4;
        int headZ0 = bodyZ0-6, headZ1 = bodyZ0-3;
        int headX0 = 8, headX1 = 11;
        box(headX0, headY0, headZ0, headX1, headY1, headZ1);

        // snout — extends forward, slightly lower and narrower
        int snoutY0 = headY0, snoutY1 = headY0+2;
        int snoutZ0 = headZ0-2, snoutZ1 = headZ0-1;
        box(headX0+1, snoutY0, snoutZ0, headX1-1, snoutY1, snoutZ1);

        // nostrils
        set(headX0+1, snoutY0, snoutZ0);
        set(headX1-1, snoutY0, snoutZ0);

        // ── ears ─────────────────────────────────────────────────────────────
        set(headX0,   headY1+1, headZ1-1);
        set(headX1,   headY1+1, headZ1-1);
        set(headX0,   headY1+2, headZ1-1);
        set(headX1,   headY1+2, headZ1-1);

        // ── mane ─────────────────────────────────────────────────────────────
        // along top of neck and back of head
        for (int z = headZ1; z <= bodyZ0; z++)
            set(cx, bodyY1+3, z);          // along neck top
        set(cx, headY1,   headZ1);
        set(cx, headY1-1, headZ1-1);

        // ── tail ─────────────────────────────────────────────────────────────
        int tailZ = bodyZ1 + 1;
        set(cx,   bodyY1+1, tailZ);
        set(cx,   bodyY1,   tailZ+1);
        set(cx,   bodyY1-1, tailZ+2);
        set(cx,   bodyY1-2, tailZ+2);
        set(cx-1, bodyY1-3, tailZ+2);
        set(cx+1, bodyY1-3, tailZ+2);

        return toEnvelope("objectHorse");
    }

    static void set(int x, int y, int z) {
        if (x>=0&&x<S && y>=0&&y<S && z>=0&&z<S) vox[x][y][z] = SOLID;
    }

    static void box(int x0,int y0,int z0, int x1,int y1,int z1) {
        for (int x=x0;x<=x1;x++) for (int y=y0;y<=y1;y++) for (int z=z0;z<=z1;z++)
            set(x,y,z);
    }

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
                int y = (S-1) - row;
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