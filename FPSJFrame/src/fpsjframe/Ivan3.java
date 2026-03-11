package fpsjframe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;

public class FPSJFrame extends JPanel implements KeyListener, Runnable {

	private final int nScreenWidth = 800, nScreenHeight = 600;
	private double fPlayerX = 5.0, fPlayerY = 15.0, fPlayerAngle = 0.0;
	private final double fFOV = Math.PI / 3.0;
	private final double fDepth = 32.0, fSpeed = 5.0;
	private boolean[] keys = new boolean[256];

	// ── WORLD: single 30x30 open biome with one fractal tree ──────
	private static final int WORLD_W = 30, WORLD_H = 30;

	// ── FRACTAL TREE OVERLAY ──────────────────────────────────────
	// Maps world cell (wx,wy) → branch depth (0=trunk, 1=main, 2=secondary, 3=twig)
	// We store depth so we can render each level at the correct height
	private HashMap<Long, Integer> treeOverlay = new HashMap<>();

	private static long cellKey(int wx, int wy) {
		return ((long) (wy + 500)) * 10000 + (wx + 500);
	}

	// ── FRACTAL BRANCH GROWER ─────────────────────────────────────
	// The raycaster is 2D — height is purely visual (wall strip height ∝
	// 1/distance).
	// A "tall" object = a world cell the ray hits up close.
	// Strategy: trunk is 1 cell. Branches spread radially in world XY from the
	// trunk,
	// each branch arm is a line of cells. Branch depth controls rendered height
	// (scale).
	// The WHOLE TREE is a 2D footprint of cells at different depths.

	private void growBranch(double ox, double oy, double angle, double length,
			int depth, int maxDepth, Random rng) {
		if (depth > maxDepth || length < 0.5)
			return;

		double dx = Math.cos(angle), dy = Math.sin(angle);
		double step = 0.4;
		double traveled = 0;

		while (traveled <= length) {
			int cx = (int) Math.round(ox + dx * traveled);
			int cy = (int) Math.round(oy + dy * traveled);
			if (cx >= 1 && cx < WORLD_W - 1 && cy >= 1 && cy < WORLD_H - 1) {
				long key = cellKey(cx, cy);
				// Only overwrite if this depth is shallower (trunk wins over twig)
				int existing = treeOverlay.getOrDefault(key, 999);
				if (depth < existing)
					treeOverlay.put(key, depth);
			}
			traveled += step;
		}

		// End point of this branch
		double ex = ox + dx * length, ey = oy + dy * length;

		// Spawn 2 child branches with spread + random wobble
		double childLen = length * (0.60 + rng.nextDouble() * 0.15);
		double spread = Math.PI / 4.5 + rng.nextDouble() * Math.PI / 9.0;
		double wobble = (rng.nextDouble() - 0.5) * 0.25;

		growBranch(ex, ey, angle - spread + wobble, childLen, depth + 1, maxDepth, rng);
		growBranch(ex, ey, angle + spread + wobble, childLen, depth + 1, maxDepth, rng);

		// 50% chance of a forward continuation branch (keeps tree from being pure
		// Y-shape)
		if (depth < 2 && rng.nextDouble() < 0.55) {
			growBranch(ex, ey, angle + wobble * 0.5, childLen * 0.75, depth + 1, maxDepth, rng);
		}
	}

	// Grow a single tree centred at world position (tx, ty)
	private void growTree(int tx, int ty, long seed) {
		Random rng = new Random(seed);
		// Trunk: 1 cell, depth 0
		treeOverlay.put(cellKey(tx, ty), 10); // 10=trunk sentinel

		// 4 main boughs radiating outward (N/S/E/W ± wobble), depth 1
		double[] mainAngles = { 0, Math.PI / 2, Math.PI, 3 * Math.PI / 2 };
		for (double baseAngle : mainAngles) {
			double angle = baseAngle + (rng.nextDouble() - 0.5) * 0.4;
			double len = 2.5 + rng.nextDouble() * 1.5;
			growBranch(tx, ty, angle, len, 1, 4, new Random(rng.nextLong()));
		}
		// 4 diagonal boughs for fullness
		double[] diagAngles = { Math.PI / 4, 3 * Math.PI / 4, 5 * Math.PI / 4, 7 * Math.PI / 4 };
		for (double baseAngle : diagAngles) {
			double angle = baseAngle + (rng.nextDouble() - 0.5) * 0.3;
			double len = 1.8 + rng.nextDouble() * 1.2;
			growBranch(tx, ty, angle, len, 1, 3, new Random(rng.nextLong()));
		}
	}

	// ── CELL LOOKUP ───────────────────────────────────────────────
	// Returns: -1=out of bounds, 0=open, 0–4=branch depth
	private int getCell(int wx, int wy) {
		if (wx <= 0 || wx >= WORLD_W - 1 || wy <= 0 || wy >= WORLD_H - 1)
			return -1; // boundary wall
		Integer v = treeOverlay.get(cellKey(wx, wy));
		return (v != null) ? v : 0;
	}

	// Wall for collision: boundary and trunk only
	private boolean isSolid(double wx, double wy) {
		int c = getCell((int) wx, (int) wy);
		return c < 0 || c == 0; // boundary walls and trunk block movement
	}

	// ── GAME STATE ────────────────────────────────────────────────
	private boolean bGameOver = false, bRunning = true;
	private BufferedImage offscreen;
	private Graphics2D offG;
	private long lastTime = System.nanoTime();
	private double fps = 0;

	// ── TEXTURES ─────────────────────────────────────────────────
	private static final int TEX_W = 64, TEX_H = 64;
	private int[] texGround = new int[TEX_W * TEX_H]; // dirt floor (reused for ground colour)
	private int[] texBark = new int[TEX_W * TEX_H]; // trunk — dark ridged bark
	private int[] texBough = new int[TEX_W * TEX_H]; // main branch — medium bark
	private int[] texTwig = new int[TEX_W * TEX_H]; // secondary/twig — light smooth bark

	public FPSJFrame() {
		setPreferredSize(new Dimension(nScreenWidth, nScreenHeight));
		setBackground(Color.BLACK);
		setFocusable(true);
		addKeyListener(this);
		offscreen = new BufferedImage(nScreenWidth, nScreenHeight, BufferedImage.TYPE_INT_RGB);
		offG = offscreen.createGraphics();
		Random rng = new Random(42);
		generateBarkTex(texBark, rng, 70, 42, 15, 3); // dark trunk
		generateBarkTex(texBough, rng, 100, 62, 22, 2); // mid branch
		generateBarkTex(texTwig, rng, 130, 85, 35, 1); // light twig
		// Grow ONE tree at the centre of the world
		growTree(15, 15, 12345L);
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

	// Parametric bark texture — ridged vertical grain, horizontal rings
	private void generateBarkTex(int[] tex, Random rng, int baseR, int baseG, int baseB, int ringGap) {
		float[] n = makeNoise(rng, 2);
		for (int ty = 0; ty < TEX_H; ty++)
			for (int tx = 0; tx < TEX_W; tx++) {
				float v = n[ty * TEX_W + tx];
				float ridge = (float) (0.5 + 0.5 * Math.sin(tx * 0.9 + v * 2.0));
				int r = Math.min(255, (int) (baseR * (0.7 + 0.3 * ridge) + v * 20));
				int g = Math.min(255, (int) (baseG * (0.7 + 0.3 * ridge) + v * 12));
				int b = Math.min(255, (int) (baseB * (0.7 + 0.3 * ridge) + v * 8));
				// Horizontal ring grooves
				int ringSize = 8 + ringGap * 2;
				if (ty % ringSize < 2) {
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
			if (!bGameOver)
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
	}

	// ── RENDER ────────────────────────────────────────────────────
	// Height scale per branch depth:
	// 0 = trunk → 1.00 (full wall)
	// 1 = main bough → 0.75
	// 2 = secondary → 0.50
	// 3 = twig → 0.30
	// 4 = fine twig → 0.18
	private static final double[] HEIGHT_SCALE = { 1.0, 0.75, 0.50, 0.30, 0.18, 0.12, 0.08, 0.08, 0.08, 0.08, 1.0 }; // index
																														// 10=trunk

	private void render() {
		// Sky — blue-grey
		for (int y = 0; y < nScreenHeight / 2; y++) {
			float t = (float) y / (nScreenHeight / 2f);
			int r = (int) (80 + 40 * t), g = (int) (120 + 50 * t), b = (int) (160 + 50 * t);
			for (int x = 0; x < nScreenWidth; x++)
				offscreen.setRGB(x, y, (r << 16) | (g << 8) | b);
		}
		// Floor — earthy
		for (int y = nScreenHeight / 2; y < nScreenHeight; y++) {
			float t = (float) (y - nScreenHeight / 2) / (nScreenHeight / 2f);
			int r = (int) (60 + 30 * t), g = (int) (45 + 20 * t), b = (int) (20 + 10 * t);
			for (int x = 0; x < nScreenWidth; x++)
				offscreen.setRGB(x, y, (r << 16) | (g << 8) | b);
		}

		for (int x = 0; x < nScreenWidth; x++) {
			double rayA = (fPlayerAngle - fFOV / 2.0) + ((double) x / nScreenWidth) * fFOV;
			double eyeX = Math.cos(rayA), eyeY = Math.sin(rayA);

			// March ray — collect ALL hits up to depth 8 (branches are see-through)
			double[] hDist = new double[8];
			int[] hDepth = new int[8];
			int hCount = 0;
			double dist = 0;
			int lastCell = 0;
			int lastCellX = -1, lastCellY = -1;

			while (dist < fDepth && hCount < 8) {
				dist += 0.02;
				int tx = (int) (fPlayerX + eyeX * dist), ty = (int) (fPlayerY + eyeY * dist);
				if (tx == lastCellX && ty == lastCellY)
					continue;
				lastCellX = tx;
				lastCellY = ty;
				int cell = getCell(tx, ty);
				if (cell < 0) { // boundary
					hDist[hCount] = dist;
					hDepth[hCount] = -1;
					hCount++;
					break;
				}
				if (cell == 10) { // TRUNK — opaque, stop
					hDist[hCount] = dist;
					hDepth[hCount] = 10;
					hCount++;
					break;
				}
				if (cell > 0 && cell != lastCell) { // branch cell (depth 1–4)
					hDist[hCount] = dist;
					hDepth[hCount] = cell;
					hCount++;
					lastCell = cell;
					// keep marching — branches are semi-transparent
				} else if (cell == 0 && lastCell > 0) {
					lastCell = 0; // exited a branch region, reset so we can hit next
				} else if (cell == 0) {
					lastCell = 0;
				}

			}

			// Render each hit back-to-front
			// Pre-compute screen bands
			int[] cArr = new int[hCount], fArr = new int[hCount];
			for (int hi = 0; hi < hCount; hi++) {
				double d = hDist[hi];
				int dep = hDepth[hi];
				int c = (int) (nScreenHeight / 2.0 - nScreenHeight / d);
				int f = nScreenHeight - c;
				if (dep > 0 && dep < HEIGHT_SCALE.length) {
					double sc = HEIGHT_SCALE[dep];
					int wallH = f - c;
					c = f - (int) (wallH * sc);
				} else if (dep == -1) { // boundary
					// full height
				}
				cArr[hi] = c;
				fArr[hi] = f;
			}

			for (int hi = hCount - 1; hi >= 0; hi--) {
				double d = hDist[hi];
				int dep = hDepth[hi];
				if (dep == 0)
					continue; // skip open air (shouldn't be in hits)
				int ceiling = cArr[hi], floor = fArr[hi];
				int[] tex = (dep == 10) ? texBark : (dep == 1) ? texBough : texTwig;
				double hitX = fPlayerX + eyeX * d, hitY = fPlayerY + eyeY * d;
				int texX;
				if (Math.abs(eyeX) > Math.abs(eyeY))
					texX = (int) ((hitY - Math.floor(hitY)) * TEX_W) & (TEX_W - 1);
				else
					texX = (int) ((hitX - Math.floor(hitX)) * TEX_W) & (TEX_W - 1);

				float face = (Math.abs(eyeX) > Math.abs(eyeY)) ? 1.0f : 0.75f;
				float distB = (float) Math.max(0.08, 1.0 - d / fDepth);
				float bright = distB * face;

				for (int y = ceiling; y <= floor && y < nScreenHeight; y++) {
					if (y < 0)
						continue;
					boolean occ = false;
					for (int fhi = 0; fhi < hi; fhi++)
						if (y >= cArr[fhi] && y <= fArr[fhi] && hDepth[fhi] > 0) {
							occ = true;
							break;
						}
					if (occ)
						continue;
					int texY = (int) (((y - ceiling) / (double) (floor - ceiling + 1)) * TEX_H) & (TEX_H - 1);
					int tc = tex[texY * TEX_W + texX];
					float fb = Math.max(0.08f, bright);
					int r = Math.min(255, (int) (((tc >> 16) & 0xFF) * fb));
					int g = Math.min(255, (int) (((tc >> 8) & 0xFF) * fb));
					int b = Math.min(255, (int) ((tc & 0xFF) * fb));
					offscreen.setRGB(x, y, (r << 16) | (g << 8) | b);
				}
			}
		}

		offG.drawImage(offscreen, 0, 0, null);
		drawHUD(offG);
	}

	private void drawHUD(Graphics2D g) {
		// FPS
		g.setColor(new Color(220, 220, 220, 220));
		g.setFont(new Font("Courier New", Font.PLAIN, 12));
		g.drawString(
				String.format("FPS:%.0f  pos:(%.1f,%.1f)  tree cells:%d", fps, fPlayerX, fPlayerY, treeOverlay.size()),
				10, 20);
		// Crosshair
		int cx = nScreenWidth / 2, cy = nScreenHeight / 2;
		g.setColor(new Color(255, 255, 255, 200));
		g.drawLine(cx - 10, cy, cx - 3, cy);
		g.drawLine(cx + 3, cy, cx + 10, cy);
		g.drawLine(cx, cy - 10, cx, cy - 3);
		g.drawLine(cx, cy + 3, cx, cy + 10);
		g.setColor(new Color(180, 180, 180, 180));
		g.setFont(new Font("Courier New", Font.PLAIN, 11));
		g.drawString("WASD=move  Q/E=strafe  R=restart", 10, nScreenHeight - 10);
		// Minimap
		drawMiniMap(g);
	}

	private void drawMiniMap(Graphics2D g) {
		int scale = 8, mW = WORLD_W * scale, mH = WORLD_H * scale;
		// Too big — show at 4px per cell, top-right
		int ps = 4;
		int ox = nScreenWidth - WORLD_W * ps - 10, oy = 10;
		g.setColor(new Color(0, 0, 0, 150));
		g.fillRect(ox, oy, WORLD_W * ps, WORLD_H * ps);
		// Draw tree cells
		for (Map.Entry<Long, Integer> e : treeOverlay.entrySet()) {
			long k = e.getKey();
			int dep = e.getValue();
			int wx = (int) ((k % 10000) - 500), wy = (int) ((k / 10000) - 500);
			if (wx < 0 || wx >= WORLD_W || wy < 0 || wy >= WORLD_H)
				continue;
			g.setColor(dep == 0 ? new Color(60, 30, 10)
					: dep == 1 ? new Color(100, 55, 20) : dep == 2 ? new Color(140, 80, 35) : new Color(170, 110, 50));
			g.fillRect(ox + wx * ps, oy + wy * ps, ps, ps);
		}
		// Player
		int px = ox + (int) (fPlayerX * ps), py = oy + (int) (fPlayerY * ps);
		g.setColor(Color.WHITE);
		g.fillOval(px - 2, py - 2, 5, 5);
		g.setColor(Color.YELLOW);
		g.drawLine(px, py, (int) (px + Math.cos(fPlayerAngle) * 8), (int) (py + Math.sin(fPlayerAngle) * 8));
	}

	private void restart() {
		fPlayerX = 5;
		fPlayerY = 15;
		fPlayerAngle = 0;
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
		if (c == KeyEvent.VK_R)
			restart();
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
		JFrame f = new JFrame("Fractal Tree Skeleton");
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