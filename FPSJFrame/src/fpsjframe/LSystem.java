package fpsjframe;

import java.util.*;

/**
 * Utility class for generating 3D L-System object envelopes.
 *
 * Usage:
 *   LSystem.Params p = new LSystem.Params()
 *       .axiom("A")
 *       .rule('A', "F[1+A][2+A][3+A][4+A]")
 *       .iterations(4)
 *       .branchAngle(35)
 *       .widthRatio(0.4)
 *       .objectName("objectTree")
 *       .solidLabel("tree");
 *   String envelope = LSystem.generate(p, 20, 20, 'O', '#');
 *
 * ── Turtle symbols ──────────────────────────────────────────────────────────
 *   F      draw segment along heading
 *   + / -  pitch up / down       (around local Left axis)
 *   < / >  yaw left / right      (around local Up axis)
 *   ^ / &  roll left / right     (around local Heading axis)
 *   1 2 3 4  world-Y yaw 0/90/180/270° — use for symmetric canopies,
 *            always relative to world not the turtle's drifted Up vector
 *   [ / ]  push / pop turtle state
 *
 * ── Shape parameters (Params) ───────────────────────────────────────────────
 *   iterations     rewrite depth
 *   branchAngle    pitch/yaw angle for + - < > ^ & symbols (degrees)
 *   widthRatio     XZ spread as fraction of grid  (0.4=narrow/tall, 0.9=wide/bushy)
 *   trunkThickness segment radius (-1 = auto: max(0, 2-iterations))
 *   objectName     written into envelope header
 *   solidLabel     legend label for the solid char
 *
 * ── Envelope axis mapping (parseObject convention) ──────────────────────────
 *   slices (===) = X axis
 *   rows         = Y axis, row 0 = top = y(size-1)
 *   chars        = Z axis
 */
public class LSystem {

    // ── Params ───────────────────────────────────────────────────────────────

    public static class Params {
        String axiom           = "A";
        Map<Character,String> rules = new LinkedHashMap<>();
        int    iterations      = 4;
        double branchAngleDeg  = 35.0;
        double widthRatio      = 0.4;   // 0.4=tall/narrow, 0.9=wide/bushy
        int    trunkThickness  = -1;    // -1 = auto
        String objectName      = "object";
        String solidLabel      = "solid";

        public Params axiom(String a)              { axiom = a;              return this; }
        public Params rule(char k, String v)       { rules.put(k, v);        return this; }
        public Params iterations(int n)            { iterations = n;         return this; }
        public Params branchAngle(double deg)      { branchAngleDeg = deg;   return this; }
        public Params widthRatio(double r)         { widthRatio = r;         return this; }
        public Params trunkThickness(int t)        { trunkThickness = t;     return this; }
        public Params objectName(String n)         { objectName = n;         return this; }
        public Params solidLabel(String l)         { solidLabel = l;         return this; }
    }

    // ── L-System expansion ───────────────────────────────────────────────────

    static String expand(Params p) {
        String s = p.axiom;
        for (int i = 0; i < p.iterations; i++) {
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray())
                sb.append(p.rules.getOrDefault(c, String.valueOf(c)));
            s = sb.toString();
        }
        return s;
    }

    // ── Turtle state ─────────────────────────────────────────────────────────

    static class State {
        double x,y,z, hx,hy,hz, ux,uy,uz, lx,ly,lz;
        State(double x,double y,double z,
              double hx,double hy,double hz,
              double ux,double uy,double uz,
              double lx,double ly,double lz) {
            this.x=x; this.y=y; this.z=z;
            this.hx=hx; this.hy=hy; this.hz=hz;
            this.ux=ux; this.uy=uy; this.uz=uz;
            this.lx=lx; this.ly=ly; this.lz=lz;
        }
        State copy() { return new State(x,y,z,hx,hy,hz,ux,uy,uz,lx,ly,lz); }
    }

    static double[] rod(double[] v, double[] ax, double a) {
        double c=Math.cos(a), s=Math.sin(a), d=v[0]*ax[0]+v[1]*ax[1]+v[2]*ax[2];
        double cx=ax[1]*v[2]-ax[2]*v[1], cy=ax[2]*v[0]-ax[0]*v[2], cz=ax[0]*v[1]-ax[1]*v[0];
        return new double[]{
            v[0]*c+cx*s+ax[0]*d*(1-c),
            v[1]*c+cy*s+ax[1]*d*(1-c),
            v[2]*c+cz*s+ax[2]*d*(1-c)
        };
    }

    static void rotFrame(State t, double ax, double ay, double az, double a) {
        double[] h=rod(new double[]{t.hx,t.hy,t.hz},new double[]{ax,ay,az},a);
        double[] u=rod(new double[]{t.ux,t.uy,t.uz},new double[]{ax,ay,az},a);
        double[] l=rod(new double[]{t.lx,t.ly,t.lz},new double[]{ax,ay,az},a);
        t.hx=h[0]; t.hy=h[1]; t.hz=h[2];
        t.ux=u[0]; t.uy=u[1]; t.uz=u[2];
        t.lx=l[0]; t.ly=l[1]; t.lz=l[2];
    }

    static final double[] WORLD_Y = {0,1,0};

    // ── Segment drawing ──────────────────────────────────────────────────────

    static void drawSeg(boolean[][][] v, int S,
                        double x0,double y0,double z0,
                        double x1,double y1,double z1, int thick) {
        double dx=x1-x0, dy=y1-y0, dz=z1-z0;
        int steps=(int)(Math.sqrt(dx*dx+dy*dy+dz*dz)*3)+2;
        for (int i=0; i<=steps; i++) {
            double t=(double)i/steps;
            int px=(int)Math.round(x0+t*dx), py=(int)Math.round(y0+t*dy), pz=(int)Math.round(z0+t*dz);
            for (int tx=-thick; tx<=thick; tx++)
            for (int ty=-thick; ty<=thick; ty++)
            for (int tz=-thick; tz<=thick; tz++) {
                if (tx*tx+ty*ty+tz*tz <= thick*thick+thick) {
                    int nx=px+tx, ny=py+ty, nz=pz+tz;
                    if (nx>=0&&nx<S&&ny>=0&&ny<S&&nz>=0&&nz<S) v[nx][ny][nz]=true;
                }
            }
        }
    }

    // ── Turtle walk ──────────────────────────────────────────────────────────

    static void walk(String ls, double r, boolean dry, List<double[]> pts,
                     boolean[][][] vox, int S,
                     double ox,double oy,double oz,
                     double sXZ,double sY, int thick) {

        State t = new State(0,0,0, 0,1,0, 0,0,1, 1,0,0);
        Deque<State> stack = new ArrayDeque<>();

        for (char c : ls.toCharArray()) {
            switch (c) {
                case 'F': {
                    double nx=t.x+t.hx, ny=t.y+t.hy, nz=t.z+t.hz;
                    if (dry) {
                        pts.add(new double[]{t.x,t.y,t.z});
                        pts.add(new double[]{nx,ny,nz});
                    } else {
                        drawSeg(vox,S, t.x*sXZ+ox,t.y*sY+oy,t.z*sXZ+oz,
                                       nx*sXZ+ox, ny*sY+oy, nz*sXZ+oz, thick);
                    }
                    t.x=nx; t.y=ny; t.z=nz; break;
                }
                case '+': rotFrame(t, t.lx,t.ly,t.lz,  r); break;
                case '-': rotFrame(t, t.lx,t.ly,t.lz, -r); break;
                case '<': rotFrame(t, t.ux,t.uy,t.uz,  r); break;
                case '>': rotFrame(t, t.ux,t.uy,t.uz, -r); break;
                case '^': rotFrame(t, t.hx,t.hy,t.hz,  r); break;
                case '&': rotFrame(t, t.hx,t.hy,t.hz, -r); break;
                case '1': break; // world-Y yaw 0°
                case '2': rotFrame(t,WORLD_Y[0],WORLD_Y[1],WORLD_Y[2],  Math.PI/2); break;
                case '3': rotFrame(t,WORLD_Y[0],WORLD_Y[1],WORLD_Y[2],  Math.PI);   break;
                case '4': rotFrame(t,WORLD_Y[0],WORLD_Y[1],WORLD_Y[2], -Math.PI/2); break;
                case '[': stack.push(t.copy()); break;
                case ']': t=stack.pop();        break;
            }
        }
    }

    // ── Public: generate envelope string ─────────────────────────────────────

    public static String generate(Params p, int size, int slices, char airChar, char solidChar) {
        String ls = expand(p);
        double r  = Math.toRadians(p.branchAngleDeg);

        // dry run → bounding box
        List<double[]> pts = new ArrayList<>();
        walk(ls, r, true, pts, null, 0, 0,0,0, 1,1, 0);

        double minX=0,maxX=0, minY=0,maxY=0, minZ=0,maxZ=0;
        for (double[] pt : pts) {
            minX=Math.min(minX,pt[0]); maxX=Math.max(maxX,pt[0]);
            minY=Math.min(minY,pt[1]); maxY=Math.max(maxY,pt[1]);
            minZ=Math.min(minZ,pt[2]); maxZ=Math.max(maxZ,pt[2]);
        }
        double spanXZ = Math.max(maxX-minX, maxZ-minZ); if (spanXZ==0) spanXZ=1;
        double spanY  = maxY-minY;                       if (spanY ==0) spanY =1;

        double margin = 1.5;
        double sY     = (size - margin*2) / spanY;
        double sXZ    = (size * p.widthRatio) / spanXZ;
        double ox     = size/2.0 - ((minX+maxX)/2.0)*sXZ;
        double oy     = margin   - minY*sY;
        double oz     = size/2.0 - ((minZ+maxZ)/2.0)*sXZ;

        int thick = p.trunkThickness >= 0 ? p.trunkThickness : Math.max(0, 2-p.iterations);

        // draw pass
        boolean[][][] vox = new boolean[size][size][size];
        walk(ls, r, false, null, vox, size, ox,oy,oz, sXZ,sY, thick);

        // encode
        StringBuilder sb = new StringBuilder();
        sb.append("name: ").append(p.objectName).append("\n");
        sb.append("type: object\n");
        sb.append("legend: ").append(airChar).append("=air ")
                             .append(solidChar).append("=").append(p.solidLabel).append("\n");
        sb.append("---\n");
        for (int s=0; s<slices; s++) {
            if (s>0) sb.append("===\n");
            int xi = (slices==1) ? size/2 : (int)Math.round(s*(size-1.0)/(slices-1));
            for (int row=0; row<size; row++) {
                int yi = (size-1)-row;
                for (int zi=0; zi<size; zi++)
                    sb.append(vox[xi][yi][zi] ? solidChar : airChar);
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    /** Convenience: size=20, slices=20, chars O/# */
    public static String generate(Params p) {
        return generate(p, 20, 20, 'O', '#');
    }
}