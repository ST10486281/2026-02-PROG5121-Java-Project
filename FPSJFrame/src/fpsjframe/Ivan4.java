import java.util.*;

public class TreeAscii {

	static final int W = 80, H = 40;
	static int[][] grid = new int[H][W];

	public static void main(String[] args) {
		long seed = args.length > 0 ? Long.parseLong(args[0]) : 42;
		growBranch(W / 2, H - 1, -Math.PI / 2, 16, 0, 5, new Random(seed));
		print();
	}

	static void growBranch(double ox, double oy, double angle, double length,
			int depth, int maxDepth, Random rng) {
		if (depth > maxDepth || length < 1.0)
			return;

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
		double spread = Math.PI / 5.0 + rng.nextDouble() * Math.PI / 8.0;
		double wobble = (rng.nextDouble() - 0.5) * 0.2;

		growBranch(ex, ey, angle - spread + wobble, childLen, depth + 1, maxDepth, new Random(rng.nextLong()));
		growBranch(ex, ey, angle + spread + wobble, childLen, depth + 1, maxDepth, new Random(rng.nextLong()));
		if (depth < 3 && rng.nextDouble() < 0.5)
			growBranch(ex, ey, angle + wobble * 0.3, childLen * 0.75, depth + 1, maxDepth, new Random(rng.nextLong()));
	}

	static void print() {
		for (int y = 0; y < H; y++) {
			StringBuilder sb = new StringBuilder();
			for (int x = 0; x < W; x++)
				sb.append(grid[y][x]);
			System.out.println(sb);
		}
	}
}