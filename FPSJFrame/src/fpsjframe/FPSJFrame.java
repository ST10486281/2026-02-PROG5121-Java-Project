package fpsjframe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class FPSJFrame extends JPanel implements KeyListener, Runnable {

    private final int nScreenWidth = 800, nScreenHeight = 600;
    private double fPlayerX = 5.0, fPlayerY = 5.0, fPlayerAngle = 0.0;
    private double fFOV = Math.PI / 3.0, fDepth = 24.0, fSpeed = 5.0;
    private char cLightnessCell = '0'; // cell type that means 'open' — being near this keeps it bright
    private boolean[] keys = new boolean[256];

    // ── BIOME SYSTEM ──────────────────────────────────────────────
    private BiomeSystem biomes = new BiomeSystem("maps");
    private int getCell(int wx, int wy)        { return biomes.getCell(wx, wy); }
    private int getRawCell(int wx, int wy)     { return biomes.getRawCell(wx, wy); }
    private boolean isSolid(double wx, double wy) { return biomes.isSolid(wx, wy); }

    // ── GAME STATE ────────────────────────────────────────────────
    private int nHealth=100, nAmmo=30, nShootTimer=0;
    private boolean bShooting=false, bGameOver=false, bRunning=true;
    private BufferedImage offscreen; private Graphics2D offG;
    private long lastTime=System.nanoTime(); private double fps=0;

    // ── TEXTURES ─────────────────────────────────────────────────
    private static final int TEX_W=64, TEX_H=64;
    private int[] texBrick=new int[TEX_W*TEX_H];
    private int[] texBush =new int[TEX_W*TEX_H];
    private int[] texTree =new int[TEX_W*TEX_H];
    private Color[] wallShades=new Color[10], floorShades=new Color[10];

    public FPSJFrame() {
        setPreferredSize(new Dimension(nScreenWidth,nScreenHeight));
        setBackground(Color.BLACK); setFocusable(true); addKeyListener(this);
        offscreen=new BufferedImage(nScreenWidth,nScreenHeight,BufferedImage.TYPE_INT_RGB);
        offG=offscreen.createGraphics();
        for(int i=0;i<10;i++){float b=(i+1)/10f;wallShades[i]=new Color((int)(180*b),(int)(80*b),(int)(40*b));floorShades[i]=new Color((int)(60*b),(int)(60*b),(int)(60*b));}
        java.util.Random rng=new java.util.Random(42);
        generateBrickTex(rng); generateBushTex(rng); generateTreeTex(rng);
        new Thread(this).start();
    }

    private float[] makeNoise(java.util.Random rng, int passes) {
        float[] n=new float[TEX_W*TEX_H];
        for(int i=0;i<n.length;i++) n[i]=rng.nextFloat();
        for(int p=0;p<passes;p++){float[] t=new float[TEX_W*TEX_H];for(int ty=0;ty<TEX_H;ty++)for(int tx=0;tx<TEX_W;tx++){float s=0;int c=0;for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++){s+=n[((ty+dy+TEX_H)%TEX_H)*TEX_W+((tx+dx+TEX_W)%TEX_W)];c++;}t[ty*TEX_W+tx]=s/c;}n=t;}
        return n;
    }

    private void generateBrickTex(java.util.Random rng){float[] n=makeNoise(rng,3);for(int ty=0;ty<TEX_H;ty++)for(int tx=0;tx<TEX_W;tx++){float v=n[ty*TEX_W+tx];int row=ty/8,off=(row%2==0)?0:TEX_W/2,bx=(tx+off)%TEX_W;boolean m=(ty%8==0)||(ty%8==7)||(bx%16==0)||(bx%16==15);int r,g,b;if(m){int q=(int)(60+v*30);r=q;g=q;b=q;}else{r=(int)(120+v*60);g=(int)(55+v*30);b=(int)(30+v*20);}texBrick[ty*TEX_W+tx]=(r<<16)|(g<<8)|b;}}
    private void generateBushTex(java.util.Random rng){float[] n=makeNoise(rng,2);for(int ty=0;ty<TEX_H;ty++)for(int tx=0;tx<TEX_W;tx++){float v=n[ty*TEX_W+tx];int r=(int)(30+v*40),g=(int)(100+v*80),b=(int)(20+v*30);if(v<0.35f){r=(int)(r*0.6);g=(int)(g*0.6);b=(int)(b*0.6);}if(tx%16==7||tx%16==8){r=(int)(60+v*20);g=(int)(40+v*20);b=10;}texBush[ty*TEX_W+tx]=(r<<16)|(g<<8)|b;}}
    private void generateTreeTex(java.util.Random rng){float[] n=makeNoise(rng,2),n2=makeNoise(rng,1);for(int ty=0;ty<TEX_H;ty++)for(int tx=0;tx<TEX_W;tx++){float v=n[ty*TEX_W+tx],v2=n2[ty*TEX_W+tx];boolean dk=(tx%12<4);int r,g,b;if(dk){r=(int)(55+v*30);g=(int)(35+v*20);b=(int)(15+v*10);}else{r=(int)(100+v*50);g=(int)(65+v*30);b=(int)(25+v*20);}if(ty%10<2){r=(int)(r*0.7);g=(int)(g*0.7);b=(int)(b*0.7);}r=Math.min(255,(int)(r+v2*20-10));g=Math.min(255,(int)(g+v2*15-7));texTree[ty*TEX_W+tx]=(r<<16)|(g<<8)|b;}}

    @Override public void run(){while(bRunning){long now=System.nanoTime();double dt=(now-lastTime)/1e9;lastTime=now;if(!bGameOver)update(dt);render();repaint();fps=1.0/dt;try{Thread.sleep(8);}catch(InterruptedException e){Thread.currentThread().interrupt();}}}

    private void update(double dt){
        if(keys[KeyEvent.VK_LEFT]||keys[KeyEvent.VK_A])  fPlayerAngle-=fSpeed*0.5*dt;
        if(keys[KeyEvent.VK_RIGHT]||keys[KeyEvent.VK_D]) fPlayerAngle+=fSpeed*0.5*dt;
        double nx=fPlayerX,ny=fPlayerY;
        if(keys[KeyEvent.VK_UP]||keys[KeyEvent.VK_W])   {nx+=Math.cos(fPlayerAngle)*fSpeed*dt;ny+=Math.sin(fPlayerAngle)*fSpeed*dt;}
        if(keys[KeyEvent.VK_DOWN]||keys[KeyEvent.VK_S]) {nx-=Math.cos(fPlayerAngle)*fSpeed*dt;ny-=Math.sin(fPlayerAngle)*fSpeed*dt;}
        if(keys[KeyEvent.VK_Q]){nx+=Math.sin(fPlayerAngle)*fSpeed*dt;ny-=Math.cos(fPlayerAngle)*fSpeed*dt;}
        if(keys[KeyEvent.VK_E]){nx-=Math.sin(fPlayerAngle)*fSpeed*dt;ny+=Math.cos(fPlayerAngle)*fSpeed*dt;}
        if(!isSolid(nx,fPlayerY))fPlayerX=nx;
        if(!isSolid(fPlayerX,ny))fPlayerY=ny;
        if(bShooting){nShootTimer--;if(nShootTimer<=0)bShooting=false;}
    }

    private void render(){
        int W=BiomeSystem.WORLD_W, H=BiomeSystem.WORLD_H;
        // sky
        for(int y=0;y<nScreenHeight/2;y++){float t=(float)y/(nScreenHeight/2f);int r=(int)(15+25*t),g=(int)(40+30*t),b=(int)(15+20*t);for(int x=0;x<nScreenWidth;x++)offscreen.setRGB(x,y,(r<<16)|(g<<8)|b);}
        // floor
        for(int y=nScreenHeight/2;y<nScreenHeight;y++){float t=(float)(y-nScreenHeight/2)/(nScreenHeight/2f);int r=(int)(40+20*t),g=(int)(50+25*t),b=(int)(20+10*t);for(int x=0;x<nScreenWidth;x++)offscreen.setRGB(x,y,(r<<16)|(g<<8)|b);}
        for(int x=0;x<nScreenWidth;x++){
            double rayA=(fPlayerAngle-fFOV/2.0)+((double)x/nScreenWidth)*fFOV;
            double eyeX=Math.cos(rayA),eyeY=Math.sin(rayA);
            double dist=0;
            int lastWX=-1, lastWY=-1;
            boolean hitSolid=false;
            while(dist<fDepth && !hitSolid){
                dist+=0.02;
                int wx=(int)(fPlayerX+eyeX*dist), wy=(int)(fPlayerY+eyeY*dist);
                if(wx<0||wx>=W||wy<0||wy>=H) break;
                if(wx==lastWX&&wy==lastWY) continue;
                lastWX=wx; lastWY=wy;
                BlockEnvelope block=biomes.getBlock(wx,wy);
                if(block==null) continue;
                double tileX=wx, tileY=wy;
                double subStep=1.0/BlockEnvelope.SIZE;
                double entryDist=dist-0.02;
                for(double sd=entryDist; sd<entryDist+1.5 && !hitSolid; sd+=subStep*0.5){
                    double sx=fPlayerX+eyeX*sd, sy2=fPlayerY+eyeY*sd;
                    if((int)sx!=wx||(int)sy2!=wy) continue;
                    double fx=sx-tileX, fy=sy2-tileY;
                    int bCol=(int)(fx*BlockEnvelope.SIZE);
                    int bRow=(int)(fy*BlockEnvelope.SIZE);
                    bCol=Math.max(0,Math.min(BlockEnvelope.SIZE-1,bCol));
                    bRow=Math.max(0,Math.min(BlockEnvelope.SIZE-1,bRow));
                    if(!block.isSolid(bCol,bRow)) continue;
                    hitSolid=true;
                    float distB=(float)Math.max(0.05,1.0-sd/fDepth);
                    double nx2=(Math.abs(eyeX)>Math.abs(eyeY))?((eyeX>0)?-1:1):0;
                    double ny2=(Math.abs(eyeX)>Math.abs(eyeY))?0:((eyeY>0)?-1:1);
                    float brightness=distB*(float)(0.4+0.6*Math.abs(eyeX*nx2+eyeY*ny2));
                    int wallH=Math.max(1,(int)(nScreenHeight/sd));
                    int sliceH=Math.max(1, wallH/BlockEnvelope.SIZE);
                    int blockTop=nScreenHeight/2 - wallH/2;
                    int screenTop   =blockTop + bRow*sliceH;
                    int screenBottom=screenTop + sliceH;
                    int base=160-(bRow*8);
                    int r=(int)(base*brightness);
                    int g=(int)((base*0.55)*brightness);
                    int b=(int)((base*0.25)*brightness);
                    r=Math.min(255,Math.max(0,r)); g=Math.min(255,Math.max(0,g)); b=Math.min(255,Math.max(0,b));
                    int col=(r<<16)|(g<<8)|b;
                    for(int sy=screenTop;sy<screenBottom;sy++)
                        if(sy>=0&&sy<nScreenHeight) offscreen.setRGB(x,sy,col);
                }
            }
        }
        double minD=fDepth;
        for(int ri=-2;ri<=2;ri++){double ra=fPlayerAngle+ri*0.15,rx=Math.cos(ra),ry=Math.sin(ra),rd=0;while(rd<1.5){rd+=0.01;if(getRawCell((int)(fPlayerX+rx*rd),(int)(fPlayerY+ry*rd))!=cLightnessCell){minD=Math.min(minD,rd);break;}}}
        float pa=(float)Math.max(0,Math.min(1,1.0-minD/0.5));
        if(pa>0.01f)for(int py=0;py<nScreenHeight;py++)for(int px=0;px<nScreenWidth;px++){int col=offscreen.getRGB(px,py);offscreen.setRGB(px,py,(((int)(((col>>16)&0xFF)*(1-pa)))<<16)|(((int)(((col>>8)&0xFF)*(1-pa)))<<8)|((int)((col&0xFF)*(1-pa))));}
        offG.drawImage(offscreen,0,0,null); drawHUD(offG);
    }

    private void drawHUD(Graphics2D g){
        g.setColor(new Color(180,180,180,200));g.setFont(new Font("Courier New",Font.PLAIN,11));g.drawString(String.format("FPS: %.0f",fps),10,20);
        int cx=nScreenWidth/2,cy=nScreenHeight/2;g.setColor(new Color(255,255,255,180));
        g.drawLine(cx-12,cy,cx-4,cy);g.drawLine(cx+4,cy,cx+12,cy);g.drawLine(cx,cy-12,cx,cy-4);g.drawLine(cx,cy+4,cx,cy+12);
        g.setColor(new Color(150,150,150,180));g.setFont(new Font("Courier New",Font.PLAIN,11));
        g.drawString("WASD/Arrows: Move | Q/E: Strafe | R: Restart",10,nScreenHeight-10);
        drawMiniMap(g);
        if(bGameOver){g.setColor(new Color(180,0,0,200));g.fillRect(0,0,nScreenWidth,nScreenHeight);g.setColor(Color.RED);g.setFont(new Font("Courier New",Font.BOLD,80));g.drawString("YOU DIED",nScreenWidth/2-230,nScreenHeight/2);g.setColor(Color.WHITE);g.setFont(new Font("Courier New",Font.PLAIN,24));g.drawString("Press R to restart",nScreenWidth/2-120,nScreenHeight/2+60);}
    }

    private void drawMiniMap(Graphics2D g){
        int cPx=8,mW=BiomeSystem.WORLD_COLS*cPx,mH=BiomeSystem.WORLD_ROWS*cPx,oX=nScreenWidth-mW-10,oY=10;
        g.setColor(new Color(0,0,0,160));g.fillRect(oX-2,oY-2,mW+4,mH+4);
        for(int row=0;row<BiomeSystem.WORLD_ROWS;row++)for(int col=0;col<BiomeSystem.WORLD_COLS;col++){String bi=biomes.getBiomeName(col,row);g.setColor(bi.equals("bushland")?new Color(180,200,80):bi.equals("treeland")?new Color(60,160,60):bi.equals("testland")?new Color(200,100,200):new Color(30,100,30));g.fillRect(oX+col*cPx,oY+row*cPx,cPx-1,cPx-1);}
        int px=oX+(int)(fPlayerX/BiomeSystem.CHUNK_SIZE*cPx),py=oY+(int)(fPlayerY/BiomeSystem.CHUNK_SIZE*cPx);
        g.setColor(Color.WHITE);g.fillOval(px-2,py-2,5,5);g.setColor(Color.YELLOW);g.drawLine(px,py,(int)(px+Math.cos(fPlayerAngle)*6),(int)(py+Math.sin(fPlayerAngle)*6));
        g.setFont(new Font("Courier New",Font.PLAIN,9));
        g.setColor(new Color(180,200,80));g.fillRect(oX,oY+mH+4,8,8);g.setColor(Color.WHITE);g.drawString("Flat",oX+10,oY+mH+12);
        g.setColor(new Color(60,160,60));g.fillRect(oX,oY+mH+14,8,8);g.setColor(Color.WHITE);g.drawString("Bush",oX+10,oY+mH+22);
        g.setColor(new Color(30,100,30));g.fillRect(oX,oY+mH+24,8,8);g.setColor(Color.WHITE);g.drawString("Trees",oX+10,oY+mH+32);
    }

    private void restart(){fPlayerX=5;fPlayerY=5;fPlayerAngle=0;nHealth=100;nAmmo=30;bGameOver=false;bShooting=false;}
    @Override protected void paintComponent(Graphics g){super.paintComponent(g);g.drawImage(offscreen,0,0,null);}
    @Override public void keyPressed(KeyEvent e){int c=e.getKeyCode();if(c<256)keys[c]=true;if(c==KeyEvent.VK_R)restart();}
    @Override public void keyReleased(KeyEvent e){int c=e.getKeyCode();if(c<256)keys[c]=false;}
    @Override public void keyTyped(KeyEvent e){}

    public static void main(String[] args){
        JFrame frame=new JFrame("DOOM-J | Biome World");FPSJFrame game=new FPSJFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);frame.add(game);frame.pack();
        frame.setResizable(false);frame.setLocationRelativeTo(null);frame.setVisible(true);game.requestFocusInWindow();
    }
}