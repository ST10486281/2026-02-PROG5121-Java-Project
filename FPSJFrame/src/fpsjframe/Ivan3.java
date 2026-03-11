package fpsjframe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;

public class FPSJFrame extends JPanel implements KeyListener, Runnable {

	private final int nScreenWidth = 800, nScreenHeight = 600;

	// Player position in 3D world
	private double fPlayerX = 5.0, fPlayerY = 15.0;
	private double fPlayerZ = 4.0; // eye height (world units)
	private double fPlayerAngle = 0.0;
	private final double fFOV = Math.PI / 3.0;
	private final double fDepth = 32.0, fSpeed = 5.0;
	private boolean[] keys = new boolean[256];

	// ── 3D VOXEL WORLD ────────────────────────────────────────────
	// World is WORLD_W × WORLD_H tiles in XY, WORLD_Z voxels tall in Z
	private static final int WORLD_W = 30, WORLD_H = 30, WORLD_Z = 8;

	// Voxel grid: voxels[z][y][x] = cell type
	// 0 = air
	// 1 = bark (trunk/thick branch)
	// 2 = thin branch
	// 3 = twig
	private byte[][][] voxels = new byte[WORLD_Z][WORLD_H][WORLD_W];

	// ── FRACTAL TREE GROWER (true 3D) ─────────────────────────────
	// Grows a branch from (ox,oy,oz) in direction (dx,dy,dz).
	// Each step stamps a voxel. At the end, spawns child branches.
	// depth: 0=trunk, 1=main branch, 2=secondary, 3=twig, 4=fine twig
	private void growBranch(double ox, double oy, double oz,
			double dx, double dy, double dz,
			double length, int depth, int maxDepth, Random rng) {
		if (depth > maxDepth || length < 0.4)
			return;

		byte type = (depth == 0) ? (byte) 1 : (depth == 1) ? (byte) 1 : (depth <= 3) ? (byte) 2 : (byte) 3;
		double step = 0.35;
		double traveled = 0;

		// Normalise direction
		double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 0.001)
			return;
		dx /= len;
		dy /= len;
		dz /= len;

		while (traveled <= length) {
			int cx = (int) Math.round(ox + dx * traveled);
			int cy = (int) Math.round(oy + dy * traveled);
			int cz = (int) Math.round(oz + dz * traveled);
			if (cx >= 1 && cx < WORLD_W - 1 && cy >= 1 && cy < WORLD_H - 1 && cz >= 0 && cz < WORLD_Z) {
				// Only overwrite with thicker type (lower number = thicker)
				if (type < voxels[cz][cy][cx] || voxels[cz][cy][cx] == 0)
					voxels[cz][cy][cx] = type;
			}
			traveled += step;
		}

		// End point
		double ex = ox + dx * length, ey = oy + dy * length, ez = oz + dz * length;

		// Child branch parameters
		double childLen = length * (0.58 + rng.nextDouble() * 0.15);
		double spread = 0.5 + rng.nextDouble() * 0.3; // radians of spread in 3D
		double wobble = (rng.nextDouble() - 0.5) * 0.25;

		// Compute a perpendicular plane for spreading children
		// Use two vectors perpendicular to the current direction
		double[] perp1 = perpendicular(dx, dy, dz);
		double[] perp2 = cross(dx, dy, dz, perp1[0], perp1[1], perp1[2]);

		// Left child — rotated by -spread in perp1 plane
		double[] c1 = rotateDir(dx, dy, dz, perp1[0], perp1[1], perp1[2], -spread + wobble);
		growBranch(ex, ey, ez, c1[0], c1[1], c1[2], childLen, depth + 1, maxDepth, rng);

		// Right child — rotated by +spread
		double[] c2 = rotateDir(dx, dy, dz, perp1[0], perp1[1], perp1[2], spread + wobble);
		growBranch(ex, ey, ez, c2[0], c2[1], c2[2], childLen, depth + 1, maxDepth, rng);

		// Occasional forward continuation (60% chance for depth < 3)
		if (depth < 3 && rng.nextDouble() < 0.6) {
			double[] c3 = rotateDir(dx, dy, dz, perp2[0], perp2[1], perp2[2], wobble * 0.5);
			growBranch(ex, ey, ez, c3[0], c3[1], c3[2], childLen * 0.75, depth + 1, maxDepth, rng);
		}
	}

	// Grow a tree at world position (tx, ty), trunk base at z=0
	private void growTree(int tx, int ty, long seed) {
		Random rng = new Random(seed);
		// Trunk: straight up (0,0,1) with slight random lean
		double leanX = (rng.nextDouble() - 0.5) * 0.15;
		double leanY = (rng.nextDouble() - 0.5) * 0.15;
		growBranch(tx, ty, 0, leanX, leanY, 1.0, 4.0 + rng.nextDouble() * 1.0, 0, 3, rng);
	}

	// ── 3D VECTOR HELPERS ─────────────────────────────────────────
	private double[] perpendicular(double dx, double dy, double dz) {
		// Find a vector perpendicular to (dx,dy,dz)
		double[] ref = (Math.abs(dx) < 0.9) ? new double[] { 1, 0, 0 } : new double[] { 0, 1, 0 };
		return normalise(cross(dx, dy, dz, ref[0], ref[1], ref[2]));
	}

	private double[] cross(double ax, double ay, double az, double bx, double by, double bz) {
		return new double[] { ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx };
	}

	private double[] normalise(double[] v) {
		double l = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
		if (l < 0.0001)
			return new double[] { 0, 0, 1 };
		return new double[] { v[0] / l, v[1] / l, v[2] / l };
	}

	// Rotate direction d around axis a by angle theta (Rodrigues)
	private double[] rotateDir(double dx, double dy, double dz,
			double ax, double ay, double az, double theta) {
		double c = Math.cos(theta), s = Math.sin(theta);
		double dot = dx * ax + dy * ay + dz * az;
		double[] cross = cross(ax, ay, az, dx, dy, dz);
		return new double[] {
				dx * c + cross[0] * s + ax * dot * (1 - c),
				dy * c + cross[1] * s + ay * dot * (1 - c),
				dz * c + cross[2] * s + az * dot * (1 - c)
		};
	}

	// ── CELL QUERY ────────────────────────────────────────────────
	private byte getVoxel(int x, int y, int z) {
		if (x <= 0 || x >= WORLD_W - 1 || y <= 0 || y >= WORLD_H - 1)
			return (byte) -1; // boundary
		if (z < 0 || z >= WORLD_Z)
			return 0;
		return voxels[z][y][x];
	}

	// Solid for movement (only boundary + trunk base z=0 cells)
	private boolean isSolid(double wx, double wy) {
		int x = (int) wx, y = (int) wy;
		if (x <= 0 || x >= WORLD_W - 1 || y <= 0 || y >= WORLD_H - 1)
			return true;
		return false; // player walks through branches freely
	}

	// ── GAME STATE ────────────────────────────────────────────────
	private boolean bRunning = true;
	private BufferedImage offscreen;
	private Graphics2D offG;
	private long lastTime = System.nanoTime();
	private double fps = 0;

	// ── TEXTURES ─────────────────────────────────────────────────
	private static final int TEX_W = 64, TEX_H = 64;
	private int[] texBark = new int[TEX_W * TEX_H]; // trunk & main branch
	private int[] texTwig = new int[TEX_W * TEX_H]; // secondary/twig

	public FPSJFrame() {
		setPreferredSize(new Dimension(nScreenWidth, nScreenHeight));
		setBackground(Color.BLACK);
		setFocusable(true);
		addKeyListener(this);
		offscreen = new BufferedImage(nScreenWidth, nScreenHeight, BufferedImage.TYPE_INT_RGB);
		offG = offscreen.createGraphics();
		Random rng = new Random(42);
		generateBarkTex(texBark, rng, 75, 45, 18, 14);
		generateBarkTex(texTwig, rng, 120, 80, 32, 8);
		growTree(15, 15, 12345L);
		// Count voxels
		int count = 0;
		for (int z = 0; z < WORLD_Z; z++)
			for (int y = 0; y < WORLD_H; y++)
				for (int x = 0; x < WORLD_W; x++)
					if (voxels[z][y][x] > 0)
						count++;
		System.out.println("Tree voxels: " + count);
		new Thread(this).start();
	}

	private float[] makeNoise(Random rng, int passes) {
		float[] n = new float[TEX_W * TEX_H];
		for (int i = 0; i < n.length; i++)
			n[i] = rng.nextFloat();
		for (int p = 0; p < passes; p++) {
			float[] t = new float[TEX_W * TEX_H];
			for (int ty = 0; ty < TEX_H; ty++)
				for (int tx = 0; tx < TEX_W; tx++) {
					float s = 0;
					int c = 0;
					for (int dy = -2; dy <= 2; dy++)
						for (int dx = -2; dx <= 2; dx++) {
							s += n[((ty + dy + TEX_H) % TEX_H) * TEX_W + ((tx + dx + TEX_W) % TEX_W)];
							c++;
						}
					t[ty * TEX_W + tx] = s / c;
				}
			n = t;
		}
		return n;
	}

	private void generateBarkTex(int[] tex, Random rng, int br, int bg, int bb, int ring) {
		float[] n = makeNoise(rng, 2);
		for (int ty = 0; ty < TEX_H; ty++)
			for (int tx = 0; tx < TEX_W; tx++) {
				float v = n[ty * TEX_W + tx];
				float ridge = (float) (0.5 + 0.5 * Math.sin(tx * 0.9 + v * 2.0));
				int r = Math.min(255, (int) (br * (0.7 + 0.3 * ridge) + v * 20));
				int g = Math.min(255, (int) (bg * (0.7 + 0.3 * ridge) + v * 12));
				int b = Math.min(255, (int) (bb * (0.7 + 0.3 * ridge) + v * 8));
				if (ty % ring < 2) {
					r = (int) (r * 0.6);
					g = (int) (g * 0.6);
					b = (int) (b * 0.6);
				}
				tex[ty * TEX_W + tx] = (r << 16) | (g << 8) | b;
			}
	}

	// ── GAME LOOP ─────────────────────────────────────────────────
	@Override
	public void run() {
		while (bRunning) {
			long now = System.nanoTime();
			double dt = (now - lastTime) / 1e9;
			lastTime = now;
			update(dt);
			render();
			repaint();
			fps = 1.0 / dt;
			try {
				Thread.sleep(8);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void update(double dt) {
		if (keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A])
			fPlayerAngle -= 2.0 * dt;
		if (keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D])
			fPlayerAngle += 2.0 * dt;
		double nx = fPlayerX, ny = fPlayerY;
		if (keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W]) {
			nx += Math.cos(fPlayerAngle) * fSpeed * dt;
			ny += Math.sin(fPlayerAngle) * fSpeed * dt;
		}
		if (keys[KeyEvent.VK_DOWN] || keys[KeyEvent.VK_S]) {
			nx -= Math.cos(fPlayerAngle) * fSpeed * dt;
			ny -= Math.sin(fPlayerAngle) * fSpeed * dt;
		}
		if (keys[KeyEvent.VK_Q]) {
			nx += Math.sin(fPlayerAngle) * fSpeed * dt;
			ny -= Math.cos(fPlayerAngle) * fSpeed * dt;
		}
		if (keys[KeyEvent.VK_E]) {
			nx -= Math.sin(fPlayerAngle) * fSpeed * dt;
			ny += Math.cos(fPlayerAngle) * fSpeed * dt;
		}
		if (!isSolid(nx, fPlayerY))
			fPlayerX = nx;
		if (!isSolid(fPlayerX, ny))
			fPlayerY = ny;
		if (keys[KeyEvent.VK_R]) {
			fPlayerX = 5;
			fPlayerY = 15;
			fPlayerAngle = 0;
		}
	}

	// ── 3D VOXEL RENDERER ─────────────────────────────────────────
	// For each screen column x:
	// 1. Cast ray in XY at angle
	// 2. Step through XY cells using DDA
	// 3. For each XY cell hit, scan Z levels 0..WORLD_Z-1
	// 4. For each solid voxel at (cx,cy,cz), project its top and bottom faces
	// to screen Y using: screenY = H/2 - (worldZ - eyeZ) * projDist / rayDist
	// 5. Fill that screen column slice with the bark texture
	// Track a per-pixel Y coverage array to avoid overwriting closer voxels.

	private void render() {
		// Sky
		for (int y = 0; y < nScreenHeight / 2; y++) {
			float t = (float) y / (nScreenHeight / 2f);
			int r = (int) (80 + 40 * t), g = (int) (120 + 50 * t), b = (int) (155 + 50 * t);
			for (int x = 0; x < nScreenWidth; x++)
				offscreen.setRGB(x, y, (r << 16) | (g << 8) | b);
		}
		// Floor
		for (int y = nScreenHeight / 2; y < nScreenHeight; y++) {
			float t = (float) (y - nScreenHeight / 2) / (nScreenHeight / 2f);
			int r = (int) (55 + 25 * t), g = (int) (42 + 18 * t), b = (int) (18 + 8 * t);
			for (int x = 0; x < nScreenWidth; x++)
				offscreen.setRGB(x, y, (r << 16) | (g << 8) | b);
		}

		// Projection constant: at distance 1, a 1-unit-tall object spans projDist
		// pixels
		double projDist = (nScreenHeight / 2.0) / Math.tan(fFOV / 2.0);

		for (int x = 0; x < nScreenWidth; x++) {
			double rayA = (fPlayerAngle - fFOV / 2.0) + ((double) x / nScreenWidth) * fFOV;
			double eyeX = Math.cos(rayA), eyeY = Math.sin(rayA);

			// DDA setup in XY
			int mapX = (int) fPlayerX, mapY = (int) fPlayerY;
			double deltaDistX = Math.abs(1.0 / eyeX), deltaDistY = Math.abs(1.0 / eyeY);
			double sideDistX, sideDistY;
			int stepX, stepY;
			if (eyeX < 0) {
				stepX = -1;
				sideDistX = (fPlayerX - mapX) * deltaDistX;
			} else {
				stepX = 1;
				sideDistX = (mapX + 1.0 - fPlayerX) * deltaDistX;
			}
			if (eyeY < 0) {
				stepY = -1;
				sideDistY = (fPlayerY - mapY) * deltaDistY;
			} else {
				stepY = 1;
				sideDistY = (mapY + 1.0 - fPlayerY) * deltaDistY;
			}

			// Per-pixel Y floor: tracks lowest undrawn screen row (starts at 0=top)
			// We draw front-to-back, so we fill from the top downward and skip already
			// drawn pixels
			int[] yFloor = new int[nScreenHeight]; // yFloor[screenY] = drawn? (use a simpler approach)
			// Actually: track min/max already painted per screen column
			// Use a boolean array: drawn[screenY] = true if already filled
			boolean[] drawn = new boolean[nScreenHeight];

			double dist = 0;
			boolean hitBoundary = false;

			while (dist < fDepth && !hitBoundary) {
				// Advance DDA
				boolean sideX;
				if (sideDistX < sideDistY) {
					sideDistX += deltaDistX;
					mapX += stepX;
					sideX = true;
					dist = sideDistX - deltaDistX;
				} else {
					sideDistY += deltaDistY;
					mapY += stepY;
					sideX = false;
					dist = sideDistY - deltaDistY;
				}

				if (mapX <= 0 || mapX >= WORLD_W - 1 || mapY <= 0 || mapY >= WORLD_H - 1) {
					hitBoundary = true;
					break;
				}

				// Check all Z levels in this XY column
				// Scan bottom to top so we draw higher voxels last (they appear higher on
				// screen)
				// Actually scan top to bottom in screen space: higher Z = higher on screen =
				// smaller Y
				// Draw back-to-front isn't needed here since each voxel occupies a distinct Z
				// band.
				// Just scan Z and compute screen projection for each solid voxel.

				// Correct ray distance (perpendicular, not Euclidean — fish-eye correction)
				double perpDist = sideX ? (sideDistX - deltaDistX) : (sideDistY - deltaDistY);
				if (perpDist <= 0.001)
					continue;

				// Face normal for lighting
				float faceBright = sideX ? 1.0f : 0.72f;
				float distBright = (float) Math.max(0.08, 1.0 - perpDist / fDepth);
				float brightness = faceBright * distBright;

				// Texture X from fractional hit position
				double wallX = sideX ? (fPlayerY + perpDist * eyeY) : (fPlayerX + perpDist * eyeX);
				wallX -= Math.floor(wallX);
				int texX = (int) (wallX * TEX_W) & (TEX_W - 1);

				for (int cz = WORLD_Z - 1; cz >= 0; cz--) {
					byte vox = getVoxel(mapX, mapY, cz);
					if (vox <= 0)
						continue;

					int[] tex = (vox == 1) ? texBark : texTwig;

					// World Z of voxel top and bottom
					double worldZTop = cz + 1.0;
					double worldZBottom = cz;

					// Project to screen Y (higher worldZ = higher on screen = lower screenY)
					// screenY = H/2 - (worldZ - eyeZ) * projDist / perpDist
					int screenYTop = (int) (nScreenHeight / 2.0 - (worldZTop - fPlayerZ) * projDist / perpDist);
					int screenYBottom = (int) (nScreenHeight / 2.0 - (worldZBottom - fPlayerZ) * projDist / perpDist);

					screenYTop = Math.max(0, screenYTop);
					screenYBottom = Math.min(nScreenHeight - 1, screenYBottom);

					for (int sy = screenYTop; sy <= screenYBottom; sy++) {
						if (drawn[sy])
							continue;
						drawn[sy] = true;

						// Texture Y from vertical position within voxel
						double frac = (sy - screenYTop) / (double) Math.max(1, screenYBottom - screenYTop);
						int texY = (int) (frac * TEX_H) & (TEX_H - 1);
						int tc = tex[texY * TEX_W + texX];

						float fb = Math.max(0.08f, brightness);
						int r = Math.min(255, (int) (((tc >> 16) & 0xFF) * fb));
						int g = Math.min(255, (int) (((tc >> 8) & 0xFF) * fb));
						int b = Math.min(255, (int) ((tc & 0xFF) * fb));
						offscreen.setRGB(x, sy, (r << 16) | (g << 8) | b);
					}
				}
			}
		}

		offG.drawImage(offscreen, 0, 0, null);
		drawHUD(offG);
	}

	private void drawHUD(Graphics2D g) {
		g.setColor(new Color(220, 220, 220, 220));
		g.setFont(new Font("Courier New", Font.PLAIN, 12));
		g.drawString(String.format("FPS:%.0f  pos:(%.1f, %.1f)  eyeZ:%.1f", fps, fPlayerX, fPlayerY, fPlayerZ), 10, 20);
		int cx = nScreenWidth / 2, cy = nScreenHeight / 2;
		g.setColor(new Color(255, 255, 255, 200));
		g.drawLine(cx - 10, cy, cx - 3, cy);
		g.drawLine(cx + 3, cy, cx + 10, cy);
		g.drawLine(cx, cy - 10, cx, cy - 3);
		g.drawLine(cx, cy + 3, cx, cy + 10);
		g.setColor(new Color(180, 180, 180, 180));
		g.setFont(new Font("Courier New", Font.PLAIN, 11));
		g.drawString("WASD=move  Q/E=strafe  R=restart", 10, nScreenHeight - 10);
		drawMiniMap(g);
	}

	private void drawMiniMap(Graphics2D g) {
		int ps = 4, ox = nScreenWidth - WORLD_W * ps - 10, oy = 10;
		g.setColor(new Color(0, 0, 0, 150));
		g.fillRect(ox, oy, WORLD_W * ps, WORLD_H * ps);
		// Draw XY footprint of tree (any Z level)
		for (int my = 0; my < WORLD_H; my++)
			for (int mx = 0; mx < WORLD_W; mx++) {
				byte best = 0;
				for (int mz = 0; mz < WORLD_Z; mz++)
					if (voxels[mz][my][mx] > 0 && (best == 0 || voxels[mz][my][mx] < best))
						best = voxels[mz][my][mx];
				if (best > 0) {
					g.setColor(best == 1 ? new Color(80, 45, 15)
							: best == 2 ? new Color(120, 70, 25) : new Color(160, 100, 40));
					g.fillRect(ox + mx * ps, oy + my * ps, ps, ps);
				}
			}
		int px = ox + (int) (fPlayerX * ps), py = oy + (int) (fPlayerY * ps);
		g.setColor(Color.WHITE);
		g.fillOval(px - 2, py - 2, 5, 5);
		g.setColor(Color.YELLOW);
		g.drawLine(px, py, (int) (px + Math.cos(fPlayerAngle) * 8), (int) (py + Math.sin(fPlayerAngle) * 8));
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.drawImage(offscreen, 0, 0, null);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int c = e.getKeyCode();
		if (c < 256)
			keys[c] = true;
	}

	@Override
	public void keyReleased(KeyEvent e) {
		int c = e.getKeyCode();
		if (c < 256)
			keys[c] = false;
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	public static void main(String[] args) {
		JFrame f = new JFrame("3D Voxel Tree");
		FPSJFrame game = new FPSJFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.add(game);
		f.pack();
		f.setResizable(false);
		f.setLocationRelativeTo(null);
		f.setVisible(true);
		game.requestFocusInWindow();
	}
}