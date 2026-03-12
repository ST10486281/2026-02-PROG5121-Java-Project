package fpsjframe;

public class TreeAscii {

    public final int W, H;
    public final int[][] grid;

    public TreeAscii(int w, int h) {
        this.W = w;
        this.H = h;
        this.grid = new int[h][w];
        growBranch(w / 2.0, h - 1, -Math.PI / 2, h * 0.4, 0, 5);
    }

    private void growBranch(double ox, double oy, double angle, double length, int depth, int maxDepth) {
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
        double childLen = length * 0.65;
        double spread   = Math.PI / 4.5;
        growBranch(ex, ey, angle - spread, childLen, depth + 1, maxDepth);
        growBranch(ex, ey, angle + spread, childLen, depth + 1, maxDepth);
        if (depth < 3)
            growBranch(ex, ey, angle, childLen * 0.75, depth + 1, maxDepth);
    }

    public int get(int x, int y) {
        if (x < 0 || x >= W || y < 0 || y >= H) return 0;
        return grid[y][x];
    }

    public boolean isTree(int x, int y) {
        return get(x, y) == 1;
    }
}