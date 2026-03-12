package fpsjframe;

import java.util.*;

/**
 * 3D L-System tree envelope generator.
 *
 * Rule set — "monopodial tree" style:
 *   Axiom: A
 *   A → F[+A][&A][<A][>A]
 *   (no F->FF so trunk stays fixed length; branches recurse)
 *
 * Turtle symbols:
 *   F = draw segment (heading)
 *   + = pitch up (around Left)    - = pitch down
 *   < = yaw left (around Up)      > = yaw right
 *   ^ = roll left (around Heading) & = roll right
 *   [ = push   ] = pop
 *
 * Dry-run → bounding box → rescale to fit grid.
 */
public class LSystemTreeEnvelopeGenerator {

    static String expand(String axiom, Map<Character,String> rules, int n){
        String s=axiom;
        for(int i=0;i<n;i++){
            StringBuilder sb=new StringBuilder();
            for(char c:s.toCharArray()) sb.append(rules.getOrDefault(c,String.valueOf(c)));
            s=sb.toString();
        }
        return s;
    }

    static class State {
        double x,y,z, hx,hy,hz, ux,uy,uz, lx,ly,lz;
        State(double x,double y,double z,
              double hx,double hy,double hz,
              double ux,double uy,double uz,
              double lx,double ly,double lz){
            this.x=x;this.y=y;this.z=z;
            this.hx=hx;this.hy=hy;this.hz=hz;
            this.ux=ux;this.uy=uy;this.uz=uz;
            this.lx=lx;this.ly=ly;this.lz=lz;
        }
        State copy(){return new State(x,y,z,hx,hy,hz,ux,uy,uz,lx,ly,lz);}
    }

    static double[] rod(double[] v, double[] ax, double a){
        double c=Math.cos(a),s=Math.sin(a),d=v[0]*ax[0]+v[1]*ax[1]+v[2]*ax[2];
        double cx=ax[1]*v[2]-ax[2]*v[1],cy=ax[2]*v[0]-ax[0]*v[2],cz=ax[0]*v[1]-ax[1]*v[0];
        return new double[]{v[0]*c+cx*s+ax[0]*d*(1-c),v[1]*c+cy*s+ax[1]*d*(1-c),v[2]*c+cz*s+ax[2]*d*(1-c)};
    }

    static void rotFrame(State t, double ax, double ay, double az, double a){
        double[] r=rod(new double[]{t.hx,t.hy,t.hz},new double[]{ax,ay,az},a);
        double[] u=rod(new double[]{t.ux,t.uy,t.uz},new double[]{ax,ay,az},a);
        double[] l=rod(new double[]{t.lx,t.ly,t.lz},new double[]{ax,ay,az},a);
        t.hx=r[0];t.hy=r[1];t.hz=r[2];t.ux=u[0];t.uy=u[1];t.uz=u[2];t.lx=l[0];t.ly=l[1];t.lz=l[2];
    }

    static void drawSeg(boolean[][][] v, int S,
                        double x0,double y0,double z0,
                        double x1,double y1,double z1, int thick){
        double dx=x1-x0,dy=y1-y0,dz=z1-z0;
        int steps=(int)(Math.sqrt(dx*dx+dy*dy+dz*dz)*3)+2;
        for(int i=0;i<=steps;i++){
            double t=(double)i/steps;
            int px=(int)Math.round(x0+t*dx),py=(int)Math.round(y0+t*dy),pz=(int)Math.round(z0+t*dz);
            for(int tx=-thick;tx<=thick;tx++)for(int ty=-thick;ty<=thick;ty++)for(int tz=-thick;tz<=thick;tz++){
                if(tx*tx+ty*ty+tz*tz<=thick*thick+thick){
                    int nx=px+tx,ny=py+ty,nz=pz+tz;
                    if(nx>=0&&nx<S&&ny>=0&&ny<S&&nz>=0&&nz<S) v[nx][ny][nz]=true;
                }
            }
        }
    }

    /** dry=true → collect raw points; dry=false → draw into vox */
    static void walk(String ls, double r, double step,
                     boolean dry, List<double[]> pts,
                     boolean[][][] vox, int S,
                     double ox,double oy,double oz, double scale, int thick){
        // heading=+Y, up=+Z, left=+X
        State t=new State(0,0,0, 0,1,0, 0,0,1, 1,0,0);
        Deque<State> stack=new ArrayDeque<>();
        for(char c:ls.toCharArray()){
            switch(c){
                case 'F':{
                    double nx=t.x+t.hx*step,ny=t.y+t.hy*step,nz=t.z+t.hz*step;
                    if(dry){pts.add(new double[]{t.x,t.y,t.z});pts.add(new double[]{nx,ny,nz});}
                    else drawSeg(vox,S,t.x*scale+ox,t.y*scale+oy,t.z*scale+oz,
                                       nx*scale+ox,ny*scale+oy,nz*scale+oz,thick);
                    t.x=nx;t.y=ny;t.z=nz;break;
                }
                case '+':{rotFrame(t,t.lx,t.ly,t.lz, r);break;}
                case '-':{rotFrame(t,t.lx,t.ly,t.lz,-r);break;}
                case '<':{rotFrame(t,t.ux,t.uy,t.uz, r);break;}
                case '>':{rotFrame(t,t.ux,t.uy,t.uz,-r);break;}
                case '^':{rotFrame(t,t.hx,t.hy,t.hz, r);break;}
                case '&':{rotFrame(t,t.hx,t.hy,t.hz,-r);break;}
                case '[':{stack.push(t.copy());break;}
                case ']':{t=stack.pop();break;}
            }
        }
    }

    public static String generate(int size, int iterations, double angleDeg,
                                  int slices, char air, char solid){
        // Rule: trunk segment, then 4 diagonal branches rotated 90° apart
        // This gives a full canopy that spreads in X and Z
        Map<Character,String> rules=new LinkedHashMap<>();
        rules.put('A',"F[+A][&A][<+A][>&A]");

        String ls=expand("A",rules,iterations);
        double r=Math.toRadians(angleDeg);

        // Dry run
        List<double[]> pts=new ArrayList<>();
        walk(ls,r,1.0,true,pts,null,0, 0,0,0,1,0);

        double minX=0,maxX=0,minY=0,maxY=0,minZ=0,maxZ=0;
        for(double[] p:pts){
            minX=Math.min(minX,p[0]);maxX=Math.max(maxX,p[0]);
            minY=Math.min(minY,p[1]);maxY=Math.max(maxY,p[1]);
            minZ=Math.min(minZ,p[2]);maxZ=Math.max(maxZ,p[2]);
        }
        double span=Math.max(maxX-minX,Math.max(maxY-minY,maxZ-minZ));
        if(span==0) span=1;

        double margin=1.5;
        double scale=(size-margin*2)/span;
        double ox=size/2.0-((minX+maxX)/2.0)*scale;
        double oy=margin-minY*scale;
        double oz=size/2.0-((minZ+maxZ)/2.0)*scale;

        int thick=Math.max(0,2-iterations);
        boolean[][][] vox=new boolean[size][size][size];
        walk(ls,r,1.0,false,null,vox,size,ox,oy,oz,scale,thick);

        StringBuilder sb=new StringBuilder();
        sb.append("name: objectTree\ntype: object\n");
        sb.append("legend: ").append(air).append("=air ").append(solid).append("=tree\n");
        sb.append("---\n");
        for(int s=0;s<slices;s++){
            if(s>0) sb.append("===\n");
            int xi=(slices==1)?size/2:(int)Math.round(s*(size-1.0)/(slices-1));
            for(int row=0;row<size;row++){
                int yi=(size-1)-row;
                for(int zi=0;zi<size;zi++) sb.append(vox[xi][yi][zi]?solid:air);
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    public static String generate(int size, int iterations){
        return generate(size,iterations,35.0,size,'O','#');
    }

    public static void main(String[] args){
        int    size  =args.length>0?Integer.parseInt(args[0]):20;
        int    iters =args.length>1?Integer.parseInt(args[1]):4;
        double angle =args.length>2?Double.parseDouble(args[2]):35.0;
        int    slices=args.length>3?Integer.parseInt(args[3]):size;
        char   a     =args.length>4?args[4].charAt(0):'O';
        char   s     =args.length>5?args[5].charAt(0):'#';
        System.out.print(generate(size,iters,angle,slices,a,s));
    }
}