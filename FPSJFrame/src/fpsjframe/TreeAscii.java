package fpsjframe;

import java.util.Random;

public class TreeAscii {

    public final int W, H;
    public final int[][] grid;

    public TreeAscii(int canvasW, int canvasH, int treeW, int treeH) {
        this.W = canvasW;
        this.H = canvasH;
        this.grid = new int[canvasH][canvasW];

        // grow into a temp treeW x treeH grid
        int[][] temp = new int[treeH][treeW];
        growBranch(treeW / 2.0, treeH - 1, -Math.PI / 2, treeH * 0.4, 0, 5, new Random(42), temp, treeW, treeH);

        // stamp temp onto canvas: bottom-center aligned
        int offsetX = (canvasW - treeW) / 2;
        int offsetY = canvasH - treeH;
        for (int y = 0; y < treeH; y++)
            for (int x = 0; x < treeW; x++)
                if (temp[y][x] == 1) {
                    int cx = x + offsetX, cy = y + offsetY;
                    if (cx >= 0 && cx < canvasW && cy >= 0 && cy < canvasH)
                        grid[cy][cx] = 1;
                }
    }

    public TreeAscii(int w, int h) {
        this(w, h, w, h);
    }

    private void growBranch(double ox, double oy, double angle, double length,
            int depth, int maxDepth, Random rng, int[][] g, int gW, int gH) {
        if (depth > maxDepth || length < 1.0) return;
        double dx = Math.cos(angle), dy = Math.sin(angle);
        double step = 0.5, traveled = 0;
        while (traveled <= length) {
            int cx = (int) Math.round(ox + dx * traveled);
            int cy = (int) Math.round(oy + dy * traveled);
            if (cx >= 0 && cx < gW && cy >= 0 && cy < gH)
                g[cy][cx] = 1;
            traveled += step;
        }
        double ex = ox + dx * length, ey = oy + dy * length;
        double childLen = length * (0.60 + rng.nextDouble() * 0.15);
        double spread   = Math.PI / 5.0 + rng.nextDouble() * Math.PI / 8.0;
        double wobble   = (rng.nextDouble() - 0.5) * 0.2;
        growBranch(ex, ey, angle - spread + wobble, childLen, depth + 1, maxDepth, new Random(rng.nextLong()), g, gW, gH);
        growBranch(ex, ey, angle + spread + wobble, childLen, depth + 1, maxDepth, new Random(rng.nextLong()), g, gW, gH);
        if (depth < 3 && rng.nextDouble() < 0.5)
            growBranch(ex, ey, angle + wobble * 0.3, childLen * 0.75, depth + 1, maxDepth, new Random(rng.nextLong()), g, gW, gH);
    }

    public int get(int x, int y) {
        if (x < 0 || x >= W || y < 0 || y >= H) return 0;
        return grid[y][x];
    }

    public boolean isTree(int x, int y) {
        return get(x, y) == 1;
    }
}