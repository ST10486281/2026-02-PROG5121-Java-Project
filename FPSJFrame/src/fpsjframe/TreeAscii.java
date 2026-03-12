package fpsjframe;

import java.util.Random;

public class TreeAscii {

    public final int W, H;
    public final int[][] grid;

    // canvasW, canvasH = size of the grid
    // treeW, treeH = how wide and tall the tree grows within that canvas
    public TreeAscii(int canvasW, int canvasH, int treeW, int treeH) {
        this.W = canvasW;
        this.H = canvasH;
        this.grid = new int[canvasH][canvasW];
        double trunkLen = treeH * 0.4;
        // clamp tree so it fits canvas
        double startX = canvasW / 2.0;
        double startY = canvasH - 1;
        growBranch(startX, startY, -Math.PI / 2, trunkLen, 0, 5, new Random(42), treeW / 2.0);
    }

    // default: tree fills the canvas
    public TreeAscii(int w, int h) {
        this(w, h, w, h);
    }

    private void growBranch(double ox, double oy, double angle, double length,
            int depth, int maxDepth, Random rng, double maxSpread) {
        if (depth > maxDepth || length < 1.0) return;
        double dx = Math.cos(angle), dy = Math.sin(angle);
        double step = 0.5, traveled = 0;
        while (traveled <= length) {
            int cx = (int) Math.round(ox + dx * traveled);
            int cy = (int) Math.round(oy + dy * traveled);
            if (cx >= 0 && cx < W && cy >= 0 && cy < H)
                grid[cy][cx] = 1;
            traveled += step;
        }
        double ex = ox + dx * length, ey = oy + dy * length;
        double childLen = length * (0.60 + rng.nextDouble() * 0.15);
        double spread   = Math.PI / 5.0 + rng.nextDouble() * Math.PI / 8.0;
        double wobble   = (rng.nextDouble() - 0.5) * 0.2;
        // scale spread angle by treeW ratio so tree stays within requested width
        double spreadScale = maxSpread / (W / 2.0);
        spread *= spreadScale;
        growBranch(ex, ey, angle - spread + wobble, childLen, depth + 1, maxDepth, new Random(rng.nextLong()), maxSpread);
        growBranch(ex, ey, angle + spread + wobble, childLen, depth + 1, maxDepth, new Random(rng.nextLong()), maxSpread);
        if (depth < 3 && rng.nextDouble() < 0.5)
            growBranch(ex, ey, angle + wobble * 0.3, childLen * 0.75, depth + 1, maxDepth, new Random(rng.nextLong()), maxSpread);
    }

    public int get(int x, int y) {
        if (x < 0 || x >= W || y < 0 || y >= H) return 0;
        return grid[y][x];
    }

    public boolean isTree(int x, int y) {
        return get(x, y) == 1;
    }
}