package fpsjframe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class FPSJFrame extends JPanel implements KeyListener, Runnable {

	private final int nScreenWidth = 800;
	private final int nScreenHeight = 600;

	private double fPlayerX = 5.0, fPlayerY = 5.0, fPlayerAngle = 0.0;
	private double fFOV = Math.PI / 3.0;
	private double fDepth = 24.0;
	private double fSpeed = 5.0;
	private boolean[] keys = new boolean[256];

	// ── BIOME SYSTEM ──────────────────────────────────────────────
	private static final int BIOME_COLS = 6, BIOME_ROWS = 6, TILE_SIZE = 10;
	private static final int WORLD_W = BIOME_COLS * TILE_SIZE; // 60
	private static final int WORLD_H = BIOME_ROWS * TILE_SIZE; // 60

	private int[][] biomeMap = {
			{ 1, 1, 2, 2, 3, 3 }, { 1, 1, 2, 2, 3, 3 }, { 1, 2, 2, 3, 3, 2 },
			{ 2, 2, 3, 3, 2, 1 }, { 3, 3, 2, 2, 1, 1 }, { 3, 3, 2, 1, 1, 1 },
	};

	// 0=open 1=bush 2=tree-trunk-marker (expanded to 3x3 by getCell)
	private int[][] biomeTiles1 = {
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	};
	private int[][] biomeTiles2 = {
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 1, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 1, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 1, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	};
	private int[][] biomeTiles3 = {
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 1, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 2, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0, 2 },
			{ 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 2, 0, 0 },
	};

	// ── CELL TYPES: 0=open 1=bush 2=trunk 3=leaves ────────────────
	// A tree marker (2) in biomeTiles expands to a 3x3 voxel:
	// centre cell = TRUNK (type 2), surrounding 8 cells = LEAVES (type 3)
	private int getRawCell(int wx, int wy) {
		if (wx < 0 || wx >= WORLD_W || wy < 0 || wy >= WORLD_H)
			return 2;
		int bx = wx / TILE_SIZE, by = wy / TILE_SIZE;
		int tx = wx % TILE_SIZE, ty = wy % TILE_SIZE;
		int biome = biomeMap[by][bx];
		int[][] tiles = (biome == 1) ? biomeTiles1 : (biome == 2) ? biomeTiles2 : biomeTiles3;
		return tiles[ty][tx];
	}

	private int getCell(int wx, int wy) {
		int raw = getRawCell(wx, wy);
		if (raw != 0 && raw != 2)
			return raw; // bush or boundary
		if (raw == 2)
			return 2; // trunk centre
		// Check if any neighbour is a tree marker → this cell is leaves
		for (int dy = -1; dy <= 1; dy++)
			for (int dx = -1; dx <= 1; dx++)
				if (!(dx == 0 && dy == 0) && getRawCell(wx + dx, wy + dy) == 2)
					return 3;
		return 0;
	}

	private boolean isSolid(double wx, double wy) {
		int c = getCell((int) wx, (int) wy);
		return c != 0;
	}

	// Enemy state (inactive)
	private double[] enemyX = { 11.5, 5.5, 18.5 };
	private double[] enemyY = { 11.5, 19.5, 4.5 };
	private boolean[] enemyAlive = { true, true, true };
	private int nEnemyCount = 3;

	private int nHealth = 100, nAmmo = 30, nShootTimer = 0;
	private boolean bShooting = false, bGameOver = false, bRunning = true;

	private BufferedImage offscreen;
	private Graphics2D offG;
	private long lastTime = System.nanoTime();
	private double fps = 0;

	// ── TEXTURES ─────────────────────────────────────────────────
	// type 1=bush, 2=trunk, 3=leaves, else=brick/boundary
	private static final int TEX_W = 64, TEX_H = 64;
	private int[] texBrick = new int[TEX_W * TEX_H];
	private int[] texBush = new int[TEX_W * TEX_H];
	private int[] texTrunk = new int[TEX_W * TEX_H]; // oak bark
	private int[] texLeaves = new int[TEX_W * TEX_H]; // minecraft leaf block

	public FPSJFrame() {
		setPreferredSize(new Dimension(nScreenWidth, nScreenHeight));
		setBackground(Color.BLACK);
		setFocusable(true);
		addKeyListener(this);
		offscreen = new BufferedImage(nScreenWidth, nScreenHeight, BufferedImage.TYPE_INT_RGB);
		offG = offscreen.createGraphics();
		java.util.Random rng = new java.util.Random(42);
		generateBrickTex(rng);
		generateBushTex(rng);
		generateTrunkTex(rng);
		generateLeavesTex(rng);
		new Thread(this).start();
	}

	// ── NOISE HELPER ─────────────────────────────────────────────
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
				int r = (int) (30 + v * 40), g = (int) (100 + v * 80), b = (int) (20 + v * 30);
				if (v < 0.35f) {
					r = (int) (r * 0.6);
					g = (int) (g * 0.6);
					b = (int) (b * 0.6);
				}
				if (tx % 16 == 7 || tx % 16 == 8) {
					r = (int) (60 + v * 20);
					g = (int) (40 + v * 20);
					b = 10;
				}
				texBush[ty * TEX_W + tx] = (r << 16) | (g << 8) | b;
			}
	}

	// Oak bark — vertical ridged grain, warm brown
	private void generateTrunkTex(java.util.Random rng) {
		float[] n = makeNoise(rng, 2);
		for (int ty = 0; ty < TEX_H; ty++)
			for (int tx = 0; tx < TEX_W; tx++) {
				float v = n[ty * TEX_W + tx];
				// Vertical ridges
				float ridge = (float) (0.5 + 0.5 * Math.sin(tx * 0.9 + v * 1.5));
				int r = (int) (80 + ridge * 50 + v * 20);
				int g = (int) (50 + ridge * 30 + v * 15);
				int b = (int) (20 + ridge * 10 + v * 8);
				// Horizontal ring marks every ~12px
				if (ty % 12 < 2) {
					r = (int) (r * 0.7);
					g = (int) (g * 0.7);
					b = (int) (b * 0.7);
				}
				texTrunk[ty * TEX_W + tx] = (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
			}
	}

	// Minecraft oak leaves — pixel grid of leaf blocks with light/shadow faces
	private void generateLeavesTex(java.util.Random rng) {
		float[] n = makeNoise(rng, 1);
		// Minecraft block size = 8px in a 64px texture = 8 blocks across
		int BS = 8; // block size in pixels
		for (int ty = 0; ty < TEX_H; ty++)
			for (int tx = 0; tx < TEX_W; tx++) {
				int bx = tx / BS, by = ty / BS; // which block
				int lx = tx % BS, ly = ty % BS; // local within block
				float v = n[(by * 8 + bx) % 64 * TEX_W + (bx * 7 + 11) % 64]; // stable per-block noise
				v = n[ty * TEX_W + tx] * 0.3f + v * 0.7f; // mix in local noise too

				// Base leaf green — varies per block for "clumpy" look
				int baseG = (int) (80 + v * 60);
				int baseR = (int) (20 + v * 25);
				int baseB = (int) (10 + v * 15);

				// Top face of block: brightest
				// Side face (left/bottom border): darker — gives 3D cube feel
				boolean topFace = ly < 2;
				boolean leftFace = lx < 2;
				boolean dark = ly >= BS - 1 || lx >= BS - 1; // bottom/right pixel = shadow

				float bright = topFace ? 1.3f : leftFace ? 0.7f : dark ? 0.5f : 1.0f;
				int r = Math.min(255, (int) (baseR * bright));
				int g = Math.min(255, (int) (baseG * bright));
				int b = Math.min(255, (int) (baseB * bright));

				// Some blocks are "holes" (transparent) — about 20% for airy canopy feel
				// Use stable per-block hash
				int blockHash = (bx * 31 + by * 17 + 13) % 10;
				if (blockHash < 2) { // ~20% transparent — store magic colour
					r = 0;
					g = 200;
					b = 0; // chroma-key green = transparent
				}

				texLeaves[ty * TEX_W + tx] = (r << 16) | (g << 8) | b;
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
			fPlayerAngle -= fSpeed * 0.5 * dt;
		if (keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D])
			fPlayerAngle += fSpeed * 0.5 * dt;
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
		if (bShooting) {
			nShootTimer--;
			if (nShootTimer <= 0)
				bShooting = false;
		}
	}

	// ── RENDER ────────────────────────────────────────────────────
	private void render() {
		// Sky gradient (outdoor green-tinted)
		for (int y = 0; y < nScreenHeight / 2; y++) {
			float t = (float) y / (nScreenHeight / 2f);
			int r = (int) (15 + 25 * t), g = (int) (40 + 30 * t), b = (int) (15 + 20 * t);
			for (int x = 0; x < nScreenWidth; x++)
				offscreen.setRGB(x, y, (r << 16) | (g << 8) | b);
		}
		// Floor
		for (int y = nScreenHeight / 2; y < nScreenHeight; y++) {
			float t = (float) (y - nScreenHeight / 2) / (nScreenHeight / 2f);
			int r = (int) (40 + 20 * t), g = (int) (50 + 25 * t), b = (int) (20 + 10 * t);
			for (int x = 0; x < nScreenWidth; x++)
				offscreen.setRGB(x, y, (r << 16) | (g << 8) | b);
		}

		double[] depthBuf = new double[nScreenWidth];

		for (int x = 0; x < nScreenWidth; x++) {
			double rayA = (fPlayerAngle - fFOV / 2.0) + ((double) x / nScreenWidth) * fFOV;
			double eyeX = Math.cos(rayA), eyeY = Math.sin(rayA);

			// Collect up to 4 hits — leaves are semi-transparent like bushes
			double[] hDist = new double[4];
			int[] hType = new int[4];
			int hCount = 0;
			double dist = 0;
			int lastCell = 0;

			while (dist < fDepth && hCount < 4) {
				dist += 0.01;
				int tx = (int) (fPlayerX + eyeX * dist), ty = (int) (fPlayerY + eyeY * dist);
				if (tx < 0 || tx >= WORLD_W || ty < 0 || ty >= WORLD_H) {
					hDist[hCount] = fDepth;
					hType[hCount] = 0;
					hCount++;
					break;
				}
				int cell = getCell(tx, ty);
				if (cell != 0 && cell != lastCell) {
					hDist[hCount] = dist;
					hType[hCount] = cell;
					hCount++;
					lastCell = cell;
					// Only trunk and brick are fully opaque — stop there
					if (cell == 2 || cell == 0xFF)
						break;
					// Leaves and bushes: keep going to find what's behind
				} else if (cell == 0) {
					lastCell = 0;
				}
			}

			depthBuf[x] = hCount > 0 ? hDist[0] : fDepth;

			// Pre-compute screen bands for each hit
			// Cell heights: trunk=full, leaves=top 55% (canopy), bush=bottom 40%
			int[] cArr = new int[hCount], fArr = new int[hCount];
			for (int hi = 0; hi < hCount; hi++) {
				double d = hDist[hi];
				int c = (int) (nScreenHeight / 2.0 - nScreenHeight / d);
				int f = nScreenHeight - c;
				int type = hType[hi];
				if (type == 3) { // leaves: upper 55% as canopy block above trunk top
					int wallH = f - c;
					// Canopy occupies top 55%, sitting above the trunk top (ceiling of full wall)
					int canopyH = (int) (wallH * 0.55);
					f = c + canopyH; // canopy bottom = ceiling + canopyH
				} else if (type == 1) { // bush: bottom 40%
					int wallH = f - c;
					c = f - (int) (wallH * 0.4);
				}
				cArr[hi] = c;
				fArr[hi] = f;
			}

			// Render back-to-front
			for (int hi = hCount - 1; hi >= 0; hi--) {
				double d = hDist[hi];
				int type = hType[hi];
				int ceiling = cArr[hi], floor = fArr[hi];

				// Pick texture
				int[] tex = (type == 2) ? texTrunk : (type == 3) ? texLeaves : (type == 1) ? texBush : texBrick;

				double hitX = fPlayerX + eyeX * d, hitY = fPlayerY + eyeY * d;
				int texX;
				if (Math.abs(eyeX) > Math.abs(eyeY))
					texX = (int) ((hitY - Math.floor(hitY)) * TEX_W) & (TEX_W - 1);
				else
					texX = (int) ((hitX - Math.floor(hitX)) * TEX_W) & (TEX_W - 1);

				// Face shading: north/south faces darker than east/west (Minecraft style)
				float faceDark = (Math.abs(eyeX) > Math.abs(eyeY)) ? 1.0f : 0.75f;
				float distBright = (float) Math.max(0.07, 1.0 - d / fDepth);
				float brightness = distBright * faceDark;

				for (int y = 0; y < nScreenHeight; y++) {
					if (y <= ceiling || y > floor)
						continue;
					// Skip if occluded by closer solid hit
					boolean occ = false;
					for (int fhi = 0; fhi < hi; fhi++) {
						int ft = hType[fhi];
						if (ft != 1 && ft != 3) {
							if (y > cArr[fhi] && y <= fArr[fhi]) {
								occ = true;
								break;
							}
						} else {
							if (y > cArr[fhi] && y <= fArr[fhi]) {
								occ = true;
								break;
							}
						}
					}
					if (occ)
						continue;

					int texY = (int) (((y - ceiling) / (double) (floor - ceiling)) * TEX_H) & (TEX_H - 1);
					int tc = tex[texY * TEX_W + texX];

					// Transparency: leaves chroma-key (pure g=200,r=0,b=0) and bush sky
					if (type == 3) {
						int tr = (tc >> 16) & 0xFF, tg = (tc >> 8) & 0xFF, tb2 = tc & 0xFF;
						if (tr == 0 && tg == 200 && tb2 == 0)
							continue;
					}
					if (type == 1) {
						int tr = (tc >> 16) & 0xFF, tg = (tc >> 8) & 0xFF, tb2 = tc & 0xFF;
						if (tr < 60 && tg > 35 && tg < 80 && tb2 < 50)
							continue;
					}

					float fb = Math.max(0.07f, brightness);
					int r = Math.min(255, (int) (((tc >> 16) & 0xFF) * fb));
					int g = Math.min(255, (int) (((tc >> 8) & 0xFF) * fb));
					int b = Math.min(255, (int) ((tc & 0xFF) * fb));
					offscreen.setRGB(x, y, (r << 16) | (g << 8) | b);
				}
			}
		}

		// Wall proximity darkness
		double minD = fDepth;
		for (int ri = -2; ri <= 2; ri++) {
			double ra = fPlayerAngle + ri * 0.15;
			double rx = Math.cos(ra), ry = Math.sin(ra);
			double rd = 0;
			while (rd < 1.5) {
				rd += 0.01;
				if (getCell((int) (fPlayerX + rx * rd), (int) (fPlayerY + ry * rd)) != 0) {
					minD = Math.min(minD, rd);
					break;
				}
			}
		}
		float pa = (float) Math.max(0, Math.min(1, 1.0 - minD / 0.5));
		if (pa > 0.01f)
			for (int py = 0; py < nScreenHeight; py++)
				for (int px = 0; px < nScreenWidth; px++) {
					int col = offscreen.getRGB(px, py);
					offscreen.setRGB(px, py, (((int) (((col >> 16) & 0xFF) * (1 - pa))) << 16)
							| (((int) (((col >> 8) & 0xFF) * (1 - pa))) << 8) | ((int) ((col & 0xFF) * (1 - pa))));
				}

		offG.drawImage(offscreen, 0, 0, null);
		drawHUD(offG);
	}

	private void drawHUD(Graphics2D g) {
		g.setColor(new Color(180, 180, 180, 200));
		g.setFont(new Font("Courier New", Font.PLAIN, 11));
		g.drawString(String.format("FPS: %.0f", fps), 10, 20);
		int cx = nScreenWidth / 2, cy = nScreenHeight / 2;
		g.setColor(new Color(255, 255, 255, 180));
		g.drawLine(cx - 12, cy, cx - 4, cy);
		g.drawLine(cx + 4, cy, cx + 12, cy);
		g.drawLine(cx, cy - 12, cx, cy - 4);
		g.drawLine(cx, cy + 4, cx, cy + 12);
		g.setColor(new Color(150, 150, 150, 180));
		g.setFont(new Font("Courier New", Font.PLAIN, 11));
		g.drawString("WASD/Arrows: Move | Q/E: Strafe | R: Restart", 10, nScreenHeight - 10);
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
		int cellPx = 8, mapW = BIOME_COLS * cellPx, mapH = BIOME_ROWS * cellPx;
		int offX = nScreenWidth - mapW - 10, offY = 10;
		g.setColor(new Color(0, 0, 0, 160));
		g.fillRect(offX - 2, offY - 2, mapW + 4, mapH + 4);
		for (int by = 0; by < BIOME_ROWS; by++)
			for (int bx = 0; bx < BIOME_COLS; bx++) {
				int biome = biomeMap[by][bx];
				g.setColor(biome == 1 ? new Color(180, 200, 80)
						: biome == 2 ? new Color(60, 160, 60) : new Color(30, 100, 30));
				g.fillRect(offX + bx * cellPx, offY + by * cellPx, cellPx - 1, cellPx - 1);
			}
		int px = offX + (int) (fPlayerX / TILE_SIZE * cellPx), py = offY + (int) (fPlayerY / TILE_SIZE * cellPx);
		g.setColor(Color.WHITE);
		g.fillOval(px - 2, py - 2, 5, 5);
		g.setColor(Color.YELLOW);
		g.drawLine(px, py, (int) (px + Math.cos(fPlayerAngle) * 6), (int) (py + Math.sin(fPlayerAngle) * 6));
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

	private void restart() {
		fPlayerX = 5;
		fPlayerY = 5;
		fPlayerAngle = 0;
		nHealth = 100;
		nAmmo = 30;
		bGameOver = false;
		bShooting = false;
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
		JFrame frame = new JFrame("DOOM-J | Voxel Biomes");
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