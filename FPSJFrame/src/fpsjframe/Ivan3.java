package fpsjframe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class FPSJFrame extends JPanel implements KeyListener, Runnable {

	// Screen dimensions
	private final int nScreenWidth = 800;
	private final int nScreenHeight = 600;

	// Player state
	private double fPlayerX = 5.0;
	private double fPlayerY = 5.0;
	private double fPlayerAngle = 0.0;
	private double fFOV = Math.PI / 3.0;
	private double fDepth = 24.0;
	private double fSpeed = 5.0;

	// Key states
	private boolean[] keys = new boolean[256];

	// ── BIOME SYSTEM ──────────────────────────────────────────────
	// Layer 1: 6x6 biome map. Each cell = 10x10 world units.
	// 1 = Flatland, 2 = Bushland, 3 = Treeland
	private static final int BIOME_COLS = 6;
	private static final int BIOME_ROWS = 6;
	private static final int TILE_SIZE = 10; // world units per biome cell
	private static final int WORLD_W = BIOME_COLS * TILE_SIZE; // 60
	private static final int WORLD_H = BIOME_ROWS * TILE_SIZE; // 60

	private int[][] biomeMap = {
			{ 1, 1, 2, 2, 3, 3 },
			{ 1, 1, 2, 2, 3, 3 },
			{ 1, 2, 2, 3, 3, 2 },
			{ 2, 2, 3, 3, 2, 1 },
			{ 3, 3, 2, 2, 1, 1 },
			{ 3, 3, 2, 1, 1, 1 },
	};

	// Layer 2: detail tile maps for each biome type (10x10)
	// 0 = walkable dirt/ground, 1 = bush (solid), 2 = tree (solid, trees only)
	// BIOME 1: Flatland — all open
	private int[][] biomeTiles1 = {
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	};

	// BIOME 2: Bushland — 0=dirt, 1=bush (sparse)
	private int[][] biomeTiles2 = {
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 1, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 1, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 1, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	};

	// BIOME 3: Treeland — 0=dirt, 1=bush, 2=tree (sparse)
	private int[][] biomeTiles3 = {
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 1, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 2, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 2 },
			{ 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 2, 0, 0 },
	};

	// ── MAP LOOKUP ───────────────────────────────────────────────
	// Returns: 0 = open, 1 = bush, 2 = tree
	private int getCell(int wx, int wy) {
		if (wx < 0 || wx >= WORLD_W || wy < 0 || wy >= WORLD_H)
			return 2; // out of bounds = solid
		int bx = wx / TILE_SIZE;
		int by = wy / TILE_SIZE;
		int tx = wx % TILE_SIZE;
		int ty = wy % TILE_SIZE;
		int biome = biomeMap[by][bx];
		int[][] tiles = (biome == 1) ? biomeTiles1 : (biome == 2) ? biomeTiles2 : biomeTiles3;
		return tiles[ty][tx];
	}

	// Returns true if this world cell blocks movement
	private boolean isSolid(double wx, double wy) {
		return getCell((int) wx, (int) wy) != 0;
	}

	// ── ENEMY STATE (commented out) ───────────────────────────────
	private double[] enemyX = { 11.5, 5.5, 18.5 };
	private double[] enemyY = { 11.5, 19.5, 4.5 };
	private boolean[] enemyAlive = { true, true, true };
	private int nEnemyCount = 3;

	// Game state
	private int nHealth = 100;
	private int nAmmo = 30;
	private boolean bShooting = false;
	private int nShootTimer = 0;
	private boolean bGameOver = false;
	private boolean bRunning = true;

	// Rendering
	private BufferedImage offscreen;
	private Graphics2D offG;
	private long lastTime = System.nanoTime();
	private double fps = 0;

	// ── TEXTURES ─────────────────────────────────────────────────
	private static final int TEX_W = 64;
	private static final int TEX_H = 64;
	private int[] texBrick = new int[TEX_W * TEX_H]; // wall/boundary
	private int[] texBush = new int[TEX_W * TEX_H]; // bush (green)
	private int[] texTree = new int[TEX_W * TEX_H]; // tree (brown bark)

	// Colors (still used for legacy shading arrays)
	private Color[] wallShades = new Color[10];
	private Color[] floorShades = new Color[10];

	public FPSJFrame() {
		setPreferredSize(new Dimension(nScreenWidth, nScreenHeight));
		setBackground(Color.BLACK);
		setFocusable(true);
		addKeyListener(this);

		offscreen = new BufferedImage(nScreenWidth, nScreenHeight, BufferedImage.TYPE_INT_RGB);
		offG = offscreen.createGraphics();

		for (int i = 0; i < 10; i++) {
			float b = (i + 1) / 10.0f;
			wallShades[i] = new Color((int) (180 * b), (int) (80 * b), (int) (40 * b));
			floorShades[i] = new Color((int) (60 * b), (int) (60 * b), (int) (60 * b));
		}

		java.util.Random rng = new java.util.Random(42);
		generateBrickTex(rng);
		generateBushTex(rng);
		generateTreeTex(rng);

		new Thread(this).start();
	}

	// ── TEXTURE GENERATORS ────────────────────────────────────────

	private float[] makeNoise(java.util.Random rng, int passes) {
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

	private void generateBrickTex(java.util.Random rng) {
		float[] n = makeNoise(rng, 3);
		for (int ty = 0; ty < TEX_H; ty++)
			for (int tx = 0; tx < TEX_W; tx++) {
				float v = n[ty * TEX_W + tx];
				int row = ty / 8, off = (row % 2 == 0) ? 0 : TEX_W / 2;
				int bx = (tx + off) % TEX_W;
				boolean mortar = (ty % 8 == 0) || (ty % 8 == 7) || (bx % 16 == 0) || (bx % 16 == 15);
				int r, g, b;
				if (mortar) {
					int q = (int) (60 + v * 30);
					r = q;
					g = q;
					b = q;
				} else {
					r = (int) (120 + v * 60);
					g = (int) (55 + v * 30);
					b = (int) (30 + v * 20);
				}
				texBrick[ty * TEX_W + tx] = (r << 16) | (g << 8) | b;
			}
	}

	private void generateBushTex(java.util.Random rng) {
		float[] n = makeNoise(rng, 2);
		for (int ty = 0; ty < TEX_H; ty++)
			for (int tx = 0; tx < TEX_W; tx++) {
				float v = n[ty * TEX_W + tx];
				// Leafy green with darker patches
				int r = (int) (30 + v * 40);
				int g = (int) (100 + v * 80);
				int b = (int) (20 + v * 30);
				// Add occasional darker leaf clusters
				if (v < 0.35f) {
					r = (int) (r * 0.6);
					g = (int) (g * 0.6);
					b = (int) (b * 0.6);
				}
				// Thin stem lines
				if (tx % 16 == 7 || tx % 16 == 8) {
					r = (int) (60 + v * 20);
					g = (int) (40 + v * 20);
					b = 10;
				}
				texBush[ty * TEX_W + tx] = (r << 16) | (g << 8) | b;
			}
	}

	private void generateTreeTex(java.util.Random rng) {
		float[] noise = makeNoise(rng, 2);

		// Sky background colour (matches the renderer's sky)
		int SKY_R = 25, SKY_G = 55, SKY_B = 25;

		// Paint everything as sky first
		for (int i = 0; i < TEX_W * TEX_H; i++)
			texTree[i] = (SKY_R << 16) | (SKY_G << 8) | SKY_B;

		// Helper: draw a bark pixel with noise variation
		java.util.function.BiConsumer<int[], Float> barkPx = (coord, v) -> {
			int tx = coord[0], ty = coord[1];
			if (tx < 0 || tx >= TEX_W || ty < 0 || ty >= TEX_H)
				return;
			int r = Math.min(255, (int) (90 + v * 50));
			int g = Math.min(255, (int) (55 + v * 30));
			int b = Math.min(255, (int) (20 + v * 15));
			texTree[ty * TEX_W + tx] = (r << 16) | (g << 8) | b;
		};

		// Draw a thick branch/trunk as a filled rectangle with tapered width
		// branchRect(x_centre, y_top, y_bottom, half_width)
		java.util.function.Consumer<int[]> branchRect = (p) -> {
			int cx = p[0], yt = p[1], yb = p[2], hw = p[3];
			for (int ty = yt; ty <= yb; ty++) {
				// Slight taper: wider at bottom, narrower at top
				float t = (yb == yt) ? 1f : (float) (ty - yt) / (yb - yt);
				int w = Math.max(1, (int) (hw * (0.5f + 0.5f * t)));
				for (int tx = cx - w; tx <= cx + w; tx++) {
					float v = noise[Math.max(0, Math.min(TEX_H - 1, ty)) * TEX_W
							+ Math.max(0, Math.min(TEX_W - 1, tx))];
					barkPx.accept(new int[] { tx, ty }, v);
				}
			}
		};

		// ── TRUNK (centre, bottom 40% of texture) ──────────────────
		int trunkCX = TEX_W / 2;
		int trunkTop = (int) (TEX_H * 0.55);
		int trunkBot = TEX_H - 1;
		branchRect.accept(new int[] { trunkCX, trunkTop, trunkBot, 5 });

		// ── MAIN FORK at ~55% height ─────────────────────────────
		int forkY = (int) (TEX_H * 0.55);

		// Left main branch
		for (int step = 0; step < 18; step++) {
			int bx = trunkCX - step * 2;
			int by = forkY - step * 2;
			float v = noise[Math.max(0, Math.min(TEX_H - 1, by)) * TEX_W + Math.max(0, Math.min(TEX_W - 1, bx))];
			int hw = Math.max(1, 4 - step / 6);
			for (int dy = -hw; dy <= hw; dy++)
				for (int dx = -hw; dx <= hw; dx++)
					barkPx.accept(new int[] { bx + dx, by + dy }, v);
		}
		// Right main branch
		for (int step = 0; step < 18; step++) {
			int bx = trunkCX + step * 2;
			int by = forkY - step * 2;
			float v = noise[Math.max(0, Math.min(TEX_H - 1, by)) * TEX_W + Math.max(0, Math.min(TEX_W - 1, bx))];
			int hw = Math.max(1, 4 - step / 6);
			for (int dy = -hw; dy <= hw; dy++)
				for (int dx = -hw; dx <= hw; dx++)
					barkPx.accept(new int[] { bx + dx, by + dy }, v);
		}

		// ── SECONDARY BRANCHES ────────────────────────────────────
		// Left sub-branch curving more left + up
		int lBranchStartX = trunkCX - 12, lBranchStartY = forkY - 12;
		for (int step = 0; step < 12; step++) {
			int bx = lBranchStartX - step * 2;
			int by = lBranchStartY - step;
			float v = noise[Math.max(0, Math.min(TEX_H - 1, by)) * TEX_W + Math.max(0, Math.min(TEX_W - 1, bx))];
			int hw = Math.max(1, 3 - step / 5);
			for (int dy = -hw; dy <= hw; dy++)
				for (int dx = -hw; dx <= hw; dx++)
					barkPx.accept(new int[] { bx + dx, by + dy }, v);
		}
		// Left sub-branch going more steeply up
		for (int step = 0; step < 10; step++) {
			int bx = lBranchStartX - step;
			int by = lBranchStartY - step * 2;
			float v = noise[Math.max(0, Math.min(TEX_H - 1, by)) * TEX_W + Math.max(0, Math.min(TEX_W - 1, bx))];
			int hw = Math.max(1, 2 - step / 6);
			for (int dy = -hw; dy <= hw; dy++)
				for (int dx = -hw; dx <= hw; dx++)
					barkPx.accept(new int[] { bx + dx, by + dy }, v);
		}
		// Right sub-branch curving more right + up
		int rBranchStartX = trunkCX + 12, rBranchStartY = forkY - 12;
		for (int step = 0; step < 12; step++) {
			int bx = rBranchStartX + step * 2;
			int by = rBranchStartY - step;
			float v = noise[Math.max(0, Math.min(TEX_H - 1, by)) * TEX_W + Math.max(0, Math.min(TEX_W - 1, bx))];
			int hw = Math.max(1, 3 - step / 5);
			for (int dy = -hw; dy <= hw; dy++)
				for (int dx = -hw; dx <= hw; dx++)
					barkPx.accept(new int[] { bx + dx, by + dy }, v);
		}
		// Right sub-branch going more steeply up
		for (int step = 0; step < 10; step++) {
			int bx = rBranchStartX + step;
			int by = rBranchStartY - step * 2;
			float v = noise[Math.max(0, Math.min(TEX_H - 1, by)) * TEX_W + Math.max(0, Math.min(TEX_W - 1, bx))];
			int hw = Math.max(1, 2 - step / 6);
			for (int dy = -hw; dy <= hw; dy++)
				for (int dx = -hw; dx <= hw; dx++)
					barkPx.accept(new int[] { bx + dx, by + dy }, v);
		}

		// ── TERTIARY TWIGS (thin, near top) ───────────────────────
		int[][] twigRoots = {
				{ trunkCX - 24, forkY - 22 },
				{ trunkCX - 10, forkY - 32 },
				{ trunkCX + 10, forkY - 32 },
				{ trunkCX + 24, forkY - 22 },
				{ trunkCX - 18, forkY - 16 },
				{ trunkCX + 18, forkY - 16 },
		};
		int[][] twigDirs = { { -2, -1 }, { -1, -2 }, { 1, -2 }, { 2, -1 }, { -2, -2 }, { 2, -2 } };
		for (int ti = 0; ti < twigRoots.length; ti++) {
			int wx = twigRoots[ti][0], wy = twigRoots[ti][1];
			int dx = twigDirs[ti][0], dy = twigDirs[ti][1];
			for (int step = 0; step < 8; step++) {
				int bx = wx + dx * step, by = wy + dy * step;
				float v = noise[Math.max(0, Math.min(TEX_H - 1, by)) * TEX_W + Math.max(0, Math.min(TEX_W - 1, bx))];
				barkPx.accept(new int[] { bx, by }, v);
				barkPx.accept(new int[] { bx + 1, by }, v);
			}
		}
	}

	// ── GAME LOOP ─────────────────────────────────────────────────

	@Override
	public void run() {
		while (bRunning) {
			long now = System.nanoTime();
			double fElapsedTime = (now - lastTime) / 1_000_000_000.0;
			lastTime = now;
			if (!bGameOver)
				update(fElapsedTime);
			render();
			repaint();
			fps = 1.0 / fElapsedTime;
			try {
				Thread.sleep(8);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void update(double fElapsedTime) {
		if (keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A])
			fPlayerAngle -= fSpeed * 0.5 * fElapsedTime;
		if (keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D])
			fPlayerAngle += fSpeed * 0.5 * fElapsedTime;

		double newX = fPlayerX, newY = fPlayerY;
		if (keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W]) {
			newX += Math.cos(fPlayerAngle) * fSpeed * fElapsedTime;
			newY += Math.sin(fPlayerAngle) * fSpeed * fElapsedTime;
		}
		if (keys[KeyEvent.VK_DOWN] || keys[KeyEvent.VK_S]) {
			newX -= Math.cos(fPlayerAngle) * fSpeed * fElapsedTime;
			newY -= Math.sin(fPlayerAngle) * fSpeed * fElapsedTime;
		}
		if (keys[KeyEvent.VK_Q]) {
			newX += Math.sin(fPlayerAngle) * fSpeed * fElapsedTime;
			newY -= Math.cos(fPlayerAngle) * fSpeed * fElapsedTime;
		}
		if (keys[KeyEvent.VK_E]) {
			newX -= Math.sin(fPlayerAngle) * fSpeed * fElapsedTime;
			newY += Math.cos(fPlayerAngle) * fSpeed * fElapsedTime;
		}

		if (!isSolid(newX, fPlayerY))
			fPlayerX = newX;
		if (!isSolid(fPlayerX, newY))
			fPlayerY = newY;

		if (bShooting) {
			nShootTimer--;
			if (nShootTimer <= 0)
				bShooting = false;
		}
	}

	// ── RENDER ────────────────────────────────────────────────────

	private void render() {
		// Sky
		for (int y = 0; y < nScreenHeight / 2; y++) {
			float t = (float) y / (nScreenHeight / 2.0f);
			int r = (int) (15 + 25 * t), g = (int) (40 + 30 * t), b = (int) (15 + 20 * t); // greenish outdoor sky
			for (int x = 0; x < nScreenWidth; x++)
				offscreen.setRGB(x, y, (r << 16) | (g << 8) | b);
		}
		// Floor — earthy brown/green
		for (int y = nScreenHeight / 2; y < nScreenHeight; y++) {
			float t = (float) (y - nScreenHeight / 2) / (nScreenHeight / 2.0f);
			int r = (int) (40 + 20 * t), g = (int) (50 + 25 * t), b = (int) (20 + 10 * t);
			for (int x = 0; x < nScreenWidth; x++)
				offscreen.setRGB(x, y, (r << 16) | (g << 8) | b);
		}

		double[] fDepthBuffer = new double[nScreenWidth];

		// Raycasting — each column collects up to 2 hits (bush + tree behind it)
		for (int x = 0; x < nScreenWidth; x++) {
			double fRayAngle = (fPlayerAngle - fFOV / 2.0) + ((double) x / nScreenWidth) * fFOV;
			double fEyeX = Math.cos(fRayAngle), fEyeY = Math.sin(fRayAngle);

			// Collect hits: first solid, and if it's a bush also find next solid behind it
			double[] hitDist = new double[2];
			int[] hitType = new int[2];
			int hitCount = 0;
			double fDist = 0;
			int lastCell = 0;

			while (fDist < fDepth && hitCount < 2) {
				fDist += 0.01;
				int nTestX = (int) (fPlayerX + fEyeX * fDist);
				int nTestY = (int) (fPlayerY + fEyeY * fDist);
				if (nTestX < 0 || nTestX >= WORLD_W || nTestY < 0 || nTestY >= WORLD_H) {
					// boundary — stop
					hitDist[hitCount] = fDepth;
					hitType[hitCount] = 3;
					hitCount++;
					break;
				}
				int cell = getCell(nTestX, nTestY);
				if (cell != 0 && cell != lastCell) {
					hitDist[hitCount] = fDist;
					hitType[hitCount] = cell;
					hitCount++;
					lastCell = cell;
					// Only keep going if this hit was a bush (so we can see tree behind it)
					if (cell != 1)
						break;
				} else if (cell == 0) {
					lastCell = 0; // left the previous solid, reset so we can hit the next
				}
			}

			// depth buffer = first hit (closest solid)
			fDepthBuffer[x] = hitCount > 0 ? hitDist[0] : fDepth;

			// Pre-compute each hit's screen band so we can clip correctly
			int[] ceilArr = new int[hitCount];
			int[] floorArr = new int[hitCount];
			for (int hi = 0; hi < hitCount; hi++) {
				double hDist = hitDist[hi];
				int c = (int) (nScreenHeight / 2.0 - nScreenHeight / hDist);
				int f = nScreenHeight - c;
				if (hitType[hi] == 1) {
					int wh = f - c;
					c = f - (int) (wh * 0.4);
				}
				ceilArr[hi] = c;
				floorArr[hi] = f;
			}

			// Render back-to-front. For each hit, only draw pixels NOT already covered
			// by a closer hit's solid band (so tree top shows above bush).
			for (int hi = hitCount - 1; hi >= 0; hi--) {
				double hDist = hitDist[hi];
				int hType = hitType[hi];
				int nCeiling = ceilArr[hi];
				int nFloor = floorArr[hi];

				int[] tex = (hType == 2) ? texTree : (hType == 1) ? texBush : texBrick;

				double fHitX = fPlayerX + fEyeX * hDist;
				double fHitY = fPlayerY + fEyeY * hDist;
				int texX;
				if (Math.abs(fEyeX) > Math.abs(fEyeY))
					texX = (int) ((fHitY - Math.floor(fHitY)) * TEX_W) & (TEX_W - 1);
				else
					texX = (int) ((fHitX - Math.floor(fHitX)) * TEX_W) & (TEX_W - 1);

				float distBright = (float) Math.max(0.05, 1.0 - hDist / fDepth);
				double nx = (Math.abs(fEyeX) > Math.abs(fEyeY)) ? ((fEyeX > 0) ? -1 : 1) : 0;
				double ny = (Math.abs(fEyeX) > Math.abs(fEyeY)) ? 0 : ((fEyeY > 0) ? -1 : 1);
				float angleBright = (float) (0.4 + 0.6 * Math.abs(fEyeX * nx + fEyeY * ny));
				float brightness = distBright * angleBright;

				for (int y = 0; y < nScreenHeight; y++) {
					if (y <= nCeiling || y > nFloor)
						continue;

					// Skip pixels that are inside a closer hit's solid band
					boolean occluded = false;
					for (int fhi = 0; fhi < hi; fhi++) {
						if (y > ceilArr[fhi] && y <= floorArr[fhi]) {
							occluded = true;
							break;
						}
					}
					if (occluded)
						continue;

					int texY = (int) (((y - nCeiling) / (double) (nFloor - nCeiling)) * TEX_H) & (TEX_H - 1);
					int tc = tex[texY * TEX_W + texX];
					// Tree uses sky colour as transparent — skip those pixels so branches are
					// see-through
					if (hType == 2) {
						int tr = (tc >> 16) & 0xFF, tg = (tc >> 8) & 0xFF, tb2 = tc & 0xFF;
						if (tr < 60 && tg > 35 && tg < 80 && tb2 < 50)
							continue;
					}
					double wallMid = (nCeiling + nFloor) / 2.0, wallHalfH = (nFloor - nCeiling) / 2.0 + 1;
					float vf = (float) (1.0 - 0.25 * Math.pow(Math.abs(y - wallMid) / wallHalfH, 2));
					float fb = Math.max(0.05f, brightness * vf);
					int r = Math.min(255, (int) (((tc >> 16) & 0xFF) * fb));
					int g = Math.min(255, (int) (((tc >> 8) & 0xFF) * fb));
					int b = Math.min(255, (int) ((tc & 0xFF) * fb));
					offscreen.setRGB(x, y, (r << 16) | (g << 8) | b);
				}
			}
		}

		// Wall proximity darkness overlay
		double minWallDist = fDepth;
		for (int ri = -2; ri <= 2; ri++) {
			double rayA = fPlayerAngle + ri * 0.15;
			double rEyeX = Math.cos(rayA), rEyeY = Math.sin(rayA);
			double rDist = 0;
			while (rDist < 1.5) {
				rDist += 0.01;
				int tx = (int) (fPlayerX + rEyeX * rDist);
				int ty = (int) (fPlayerY + rEyeY * rDist);
				if (getCell(tx, ty) != 0) {
					minWallDist = Math.min(minWallDist, rDist);
					break;
				}
			}
		}
		float proxAlpha = (float) Math.max(0.0, Math.min(1.0, 1.0 - minWallDist / 0.5));
		if (proxAlpha > 0.01f) {
			for (int py = 0; py < nScreenHeight; py++)
				for (int px = 0; px < nScreenWidth; px++) {
					int col = offscreen.getRGB(px, py);
					int r = (int) (((col >> 16) & 0xFF) * (1 - proxAlpha));
					int g = (int) (((col >> 8) & 0xFF) * (1 - proxAlpha));
					int b = (int) ((col & 0xFF) * (1 - proxAlpha));
					offscreen.setRGB(px, py, (r << 16) | (g << 8) | b);
				}
		}

		offG.drawImage(offscreen, 0, 0, null);
		drawHUD(offG);
	}

	// ── HUD ───────────────────────────────────────────────────────

	private void drawHUD(Graphics2D g) {
		// FPS
		g.setColor(new Color(180, 180, 180, 200));
		g.setFont(new Font("Courier New", Font.PLAIN, 11));
		g.drawString(String.format("FPS: %.0f", fps), 10, 20);

		// Crosshair
		int cx = nScreenWidth / 2, cy = nScreenHeight / 2;
		g.setColor(new Color(255, 255, 255, 180));
		g.drawLine(cx - 12, cy, cx - 4, cy);
		g.drawLine(cx + 4, cy, cx + 12, cy);
		g.drawLine(cx, cy - 12, cx, cy - 4);
		g.drawLine(cx, cy + 4, cx, cy + 12);

		// Controls
		g.setColor(new Color(150, 150, 150, 180));
		g.setFont(new Font("Courier New", Font.PLAIN, 11));
		g.drawString("WASD/Arrows: Move | Q/E: Strafe | R: Restart", 10, nScreenHeight - 10);

		// Mini biome map (top-left)
		drawMiniMap(g);

		if (bGameOver) {
			g.setColor(new Color(180, 0, 0, 200));
			g.fillRect(0, 0, nScreenWidth, nScreenHeight);
			g.setColor(Color.RED);
			g.setFont(new Font("Courier New", Font.BOLD, 80));
			g.drawString("YOU DIED", nScreenWidth / 2 - 230, nScreenHeight / 2);
			g.setColor(Color.WHITE);
			g.setFont(new Font("Courier New", Font.PLAIN, 24));
			g.drawString("Press R to restart", nScreenWidth / 2 - 120, nScreenHeight / 2 + 60);
		}
	}

	private void drawMiniMap(Graphics2D g) {
		// Draw at top-right. Each biome cell = 8px, sub-tiles = 8/10 px (approx 1px)
		int cellPx = 8; // pixels per biome cell on minimap
		int subPx = 1; // pixels per sub-tile (10 sub-tiles per biome cell → ~8px/10 ≈ shown as biome
						// colour)
		int mapW = BIOME_COLS * cellPx;
		int mapH = BIOME_ROWS * cellPx;
		int offX = nScreenWidth - mapW - 10;
		int offY = 10;

		// Background
		g.setColor(new Color(0, 0, 0, 160));
		g.fillRect(offX - 2, offY - 2, mapW + 4, mapH + 4);

		// Draw biome cells with colour coding
		for (int by = 0; by < BIOME_ROWS; by++) {
			for (int bx = 0; bx < BIOME_COLS; bx++) {
				int biome = biomeMap[by][bx];
				Color c = (biome == 1) ? new Color(180, 200, 80) : // flatland: yellow-green
						(biome == 2) ? new Color(60, 160, 60) : // bushland: mid green
								new Color(30, 100, 30); // treeland: dark green
				g.setColor(c);
				g.fillRect(offX + bx * cellPx, offY + by * cellPx, cellPx - 1, cellPx - 1);
			}
		}

		// Player dot
		int px = offX + (int) (fPlayerX / TILE_SIZE * cellPx);
		int py = offY + (int) (fPlayerY / TILE_SIZE * cellPx);
		g.setColor(Color.WHITE);
		g.fillOval(px - 2, py - 2, 5, 5);

		// Direction line
		g.setColor(Color.YELLOW);
		g.drawLine(px, py,
				(int) (px + Math.cos(fPlayerAngle) * 6),
				(int) (py + Math.sin(fPlayerAngle) * 6));

		// Legend
		g.setFont(new Font("Courier New", Font.PLAIN, 9));
		g.setColor(new Color(180, 200, 80));
		g.fillRect(offX, offY + mapH + 4, 8, 8);
		g.setColor(Color.WHITE);
		g.drawString("Flat", offX + 10, offY + mapH + 12);
		g.setColor(new Color(60, 160, 60));
		g.fillRect(offX, offY + mapH + 14, 8, 8);
		g.setColor(Color.WHITE);
		g.drawString("Bush", offX + 10, offY + mapH + 22);
		g.setColor(new Color(30, 100, 30));
		g.fillRect(offX, offY + mapH + 24, 8, 8);
		g.setColor(Color.WHITE);
		g.drawString("Trees", offX + 10, offY + mapH + 32);
	}

	// ── INPUT ─────────────────────────────────────────────────────

	private void shoot() {
		if (bShooting || nAmmo <= 0)
			return;
		bShooting = true;
		nShootTimer = 10;
		nAmmo--;
		// -- Shoot enemy detection commented out --
		// for (int e = 0; e < nEnemyCount; e++) { ... }
	}

	private void restart() {
		fPlayerX = 5.0;
		fPlayerY = 5.0;
		fPlayerAngle = 0;
		nHealth = 100;
		nAmmo = 30;
		bGameOver = false;
		bShooting = false;
		// for (int i = 0; i < nEnemyCount; i++) enemyAlive[i] = true;
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
		if (c == KeyEvent.VK_SPACE)
			shoot();
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
		JFrame frame = new JFrame("DOOM-J | Biome World");
		FPSJFrame game = new FPSJFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(game);
		frame.pack();
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		game.requestFocusInWindow();
	}
}