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
	private double fPlayerX = 1.5;
	private double fPlayerY = 7.5;
	private double fPlayerAngle = 0.0;
	private double fFOV = Math.PI / 3.0; // 60 degrees
	private double fDepth = 16.0;
	private double fSpeed = 5.0;

	// Key states
	private boolean[] keys = new boolean[256];

	// Map
	private final int nMapWidth = 24;
	private final int nMapHeight = 24;
	private String[] map = {
			"########################",
			"#....##....#.....##....#",
			"#.#####....#.....#####.#",
			"#.#...#....#.....#...#.#",
			"#.#........#.........#.#",
			"#.#...#....#.....#...#.#",
			"#.#####....#.....#####.#",
			"#......................#",
			"#.......########.......#",
			"#.......#......#.......#",
			"#.......#.#..#.#.......#",
			"#######.#......#.......#",
			"#.......#......#.#######",
			"#.......#.#..#.#.......#",
			"#.......#......#.......#",
			"#.......########.......#",
			"#......................#",
			"#.######....#...######.#",
			"#.#....#....#...#....#.#",
			"#.#.........#........#.#",
			"#.#....#....#...#....#.#",
			"#.######....#...######.#",
			"#....##.....#....##....#",
			"########################"
	};

	// Enemy state
	private double[] enemyX = { 11.5, 5.5, 18.5, 11.5, 3.5, 19.5, 11.5 };
	private double[] enemyY = { 11.5, 19.5, 4.5, 19.5, 4.5, 19.5, 4.5 };
	private boolean[] enemyAlive = { true, true, true, true, true, true, true };
	private int nEnemyCount = 7;

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

	// Colors
	private Color[] wallShades = new Color[10];
	private Color[] floorShades = new Color[10];

	public FPSJFrame() {
		setPreferredSize(new Dimension(nScreenWidth, nScreenHeight));
		setBackground(Color.BLACK);
		setFocusable(true);
		addKeyListener(this);

		offscreen = new BufferedImage(nScreenWidth, nScreenHeight, BufferedImage.TYPE_INT_RGB);
		offG = offscreen.createGraphics();

		// Precompute wall shades (red-brown doom style)
		for (int i = 0; i < 10; i++) {
			float brightness = (i + 1) / 10.0f;
			wallShades[i] = new Color((int) (180 * brightness), (int) (80 * brightness), (int) (40 * brightness));
		}
		// Floor shades
		for (int i = 0; i < 10; i++) {
			float brightness = (i + 1) / 10.0f;
			floorShades[i] = new Color((int) (60 * brightness), (int) (60 * brightness), (int) (60 * brightness));
		}

		Thread thread = new Thread(this);
		thread.start();
	}

	@Override
	public void run() {
		while (bRunning) {
			long now = System.nanoTime();
			double fElapsedTime = (now - lastTime) / 1_000_000_000.0;
			lastTime = now;

			if (!bGameOver) {
				update(fElapsedTime);
			}

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
		// Rotate left
		if (keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A]) {
			fPlayerAngle -= fSpeed * 0.5 * fElapsedTime;
		}
		// Rotate right
		if (keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D]) {
			fPlayerAngle += fSpeed * 0.5 * fElapsedTime;
		}

		double newX = fPlayerX;
		double newY = fPlayerY;

		// Move forward
		if (keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W]) {
			newX += Math.cos(fPlayerAngle) * fSpeed * fElapsedTime;
			newY += Math.sin(fPlayerAngle) * fSpeed * fElapsedTime;
		}
		// Move backward
		if (keys[KeyEvent.VK_DOWN] || keys[KeyEvent.VK_S]) {
			newX -= Math.cos(fPlayerAngle) * fSpeed * fElapsedTime;
			newY -= Math.sin(fPlayerAngle) * fSpeed * fElapsedTime;
		}

		// Strafe left
		if (keys[KeyEvent.VK_Q]) {
			newX += Math.sin(fPlayerAngle) * fSpeed * fElapsedTime;
			newY -= Math.cos(fPlayerAngle) * fSpeed * fElapsedTime;
		}
		// Strafe right
		if (keys[KeyEvent.VK_E]) {
			newX -= Math.sin(fPlayerAngle) * fSpeed * fElapsedTime;
			newY += Math.cos(fPlayerAngle) * fSpeed * fElapsedTime;
		}

		// Collision detection
		if (newX >= 0 && newX < nMapWidth && newY >= 0 && newY < nMapHeight) {
			if (map[(int) newY].charAt((int) newX) != '#') {
				fPlayerX = newX;
				fPlayerY = newY;
			}
		}

		// Shooting timer
		if (bShooting) {
			nShootTimer--;
			if (nShootTimer <= 0)
				bShooting = false;
		}

		// Enemy AI - simple tracking
		for (int e = 0; e < nEnemyCount; e++) {
			if (!enemyAlive[e])
				continue;

			double dx = fPlayerX - enemyX[e];
			double dy = fPlayerY - enemyY[e];
			double dist = Math.sqrt(dx * dx + dy * dy);

			if (dist < 8.0) {
				double moveSpeed = 1.2 * fElapsedTime;
				double newEX = enemyX[e] + (dx / dist) * moveSpeed;
				double newEY = enemyY[e] + (dy / dist) * moveSpeed;

				if (newEX >= 0 && newEX < nMapWidth && newEY >= 0 && newEY < nMapHeight) {
					if (map[(int) newEY].charAt((int) newEX) != '#') {
						enemyX[e] = newEX;
						enemyY[e] = newEY;
					}
				}

				// Enemy attacks player
				if (dist < 0.8) {
					nHealth -= (int) (20 * fElapsedTime);
					if (nHealth <= 0) {
						nHealth = 0;
						bGameOver = true;
					}
				}
			}
		}
	}

	private void render() {
		// Sky (dark gradient)
		for (int y = 0; y < nScreenHeight / 2; y++) {
			float t = (float) y / (nScreenHeight / 2.0f);
			int r = (int) (20 + 20 * t);
			int g = (int) (10 + 10 * t);
			int b = (int) (30 + 20 * t);
			for (int x = 0; x < nScreenWidth; x++) {
				offscreen.setRGB(x, y, new Color(r, g, b).getRGB());
			}
		}

		// Floor (dark grey gradient)
		for (int y = nScreenHeight / 2; y < nScreenHeight; y++) {
			float t = (float) (y - nScreenHeight / 2) / (nScreenHeight / 2.0f);
			int shade = (int) (30 + 40 * t);
			for (int x = 0; x < nScreenWidth; x++) {
				offscreen.setRGB(x, y, new Color(shade, shade, shade).getRGB());
			}
		}

		// Depth buffer for sprites
		double[] fDepthBuffer = new double[nScreenWidth];

		// Raycasting
		for (int x = 0; x < nScreenWidth; x++) {
			double fRayAngle = (fPlayerAngle - fFOV / 2.0) + ((double) x / nScreenWidth) * fFOV;

			double fStepSize = 0.005;
			double fDistanceToWall = 0;
			boolean bHitWall = false;

			double fEyeX = Math.cos(fRayAngle);
			double fEyeY = Math.sin(fRayAngle);

			while (!bHitWall && fDistanceToWall < fDepth) {
				fDistanceToWall += fStepSize;

				int nTestX = (int) (fPlayerX + fEyeX * fDistanceToWall);
				int nTestY = (int) (fPlayerY + fEyeY * fDistanceToWall);

				if (nTestX < 0 || nTestX >= nMapWidth || nTestY < 0 || nTestY >= nMapHeight) {
					bHitWall = true;
					fDistanceToWall = fDepth;
				} else if (map[nTestY].charAt(nTestX) == '#') {
					bHitWall = true;

					// -- Wall edge outlines (commented out to hide lines) --
					// boolean bBoundary = false;
					// for (int tx = 0; tx < 2; tx++) {
					// for (int ty = 0; ty < 2; ty++) {
					// double vx = (double) nTestX + tx - fPlayerX;
					// double vy = (double) nTestY + ty - fPlayerY;
					// double d = Math.sqrt(vx * vx + vy * vy);
					// double dot = (fEyeX * vx / d) + (fEyeY * vy / d);
					// if (Math.acos(Math.min(1.0, dot)) < 0.04 / fDistanceToWall) {
					// bBoundary = true;
					// }
					// }
					// }
					// if (bBoundary) wallColor = Color.BLACK;
				}
			}

			fDepthBuffer[x] = fDistanceToWall;

			int nCeiling = (int) (nScreenHeight / 2.0 - nScreenHeight / fDistanceToWall);
			int nFloor = nScreenHeight - nCeiling;

			// Wall shading based on distance
			int shadeIndex = (int) Math.min(fDistanceToWall / fDepth * 10, 9);
			Color wallColor = wallShades[9 - shadeIndex];

			for (int y = 0; y < nScreenHeight; y++) {
				if (y <= nCeiling) {
					// ceiling - already drawn
				} else if (y > nCeiling && y <= nFloor) {
					offscreen.setRGB(x, y, wallColor.getRGB());
				}
				// floor - already drawn
			}
		}

		// Draw enemies as sprites
		for (int e = 0; e < nEnemyCount; e++) {
			if (!enemyAlive[e])
				continue;

			double dx = enemyX[e] - fPlayerX;
			double dy = enemyY[e] - fPlayerY;
			double fDistToEnemy = Math.sqrt(dx * dx + dy * dy);

			double fEnemyAngle = Math.atan2(dy, dx) - fPlayerAngle;
			// Normalize angle
			while (fEnemyAngle < -Math.PI)
				fEnemyAngle += 2 * Math.PI;
			while (fEnemyAngle > Math.PI)
				fEnemyAngle -= 2 * Math.PI;

			boolean bInFOV = Math.abs(fEnemyAngle) < fFOV / 2.0 + 0.05;

			if (bInFOV && fDistToEnemy >= 0.5 && fDistToEnemy < fDepth) {
				double fEnemyHeight = Math.min(nScreenHeight / fDistToEnemy, nScreenHeight);
				double fEnemyCeiling = nScreenHeight / 2.0 - fEnemyHeight / 2.0;
				double fEnemyFloor = nScreenHeight / 2.0 + fEnemyHeight / 2.0;

				double fEnemyAspect = fEnemyHeight / 2.0;
				double fEnemyMiddle = (0.5 * (fEnemyAngle / (fFOV / 2.0)) + 0.5) * nScreenWidth;

				// Draw enemy sprite (simple monster silhouette)
				for (int ex = (int) (fEnemyMiddle - fEnemyAspect); ex < (int) (fEnemyMiddle + fEnemyAspect); ex++) {
					if (ex < 0 || ex >= nScreenWidth)
						continue;
					if (fDistToEnemy >= fDepthBuffer[ex])
						continue;

					for (int ey = (int) fEnemyCeiling; ey < (int) fEnemyFloor; ey++) {
						if (ey < 0 || ey >= nScreenHeight)
							continue;

						double tx = (ex - (fEnemyMiddle - fEnemyAspect)) / (fEnemyAspect * 2);
						double ty = (ey - fEnemyCeiling) / (fEnemyFloor - fEnemyCeiling);

						// Simple monster shape
						Color c = getEnemyPixel(tx, ty, fDistToEnemy);
						if (c != null) {
							offscreen.setRGB(ex, ey, c.getRGB());
						}
					}
				}
			}
		}

		// HUD
		offG.drawImage(offscreen, 0, 0, null);
		drawHUD(offG);
	}

	private Color getEnemyPixel(double tx, double ty, double dist) {
		// Simple pixel art demon shape
		// Head (upper quarter)
		float bright = (float) Math.max(0.2, 1.0 - dist / fDepth);

		if (ty < 0.3) {
			// Head area
			if (tx > 0.25 && tx < 0.75) {
				// Face - red-brown
				if (ty > 0.05 && ty < 0.25) {
					// Eyes
					if ((tx > 0.3 && tx < 0.42) || (tx > 0.58 && tx < 0.7)) {
						if (ty > 0.1 && ty < 0.2) {
							return new Color((int) (255 * bright), (int) (50 * bright), 0);
						}
					}
					return new Color((int) (120 * bright), (int) (60 * bright), (int) (30 * bright));
				}
				// Horns
				if (ty < 0.05) {
					if ((tx > 0.25 && tx < 0.38) || (tx > 0.62 && tx < 0.75)) {
						return new Color((int) (80 * bright), (int) (40 * bright), (int) (20 * bright));
					}
				}
			}
		} else if (ty < 0.7) {
			// Body
			if (tx > 0.2 && tx < 0.8) {
				// Arms
				if ((tx > 0.2 && tx < 0.32) || (tx > 0.68 && tx < 0.8)) {
					return new Color((int) (100 * bright), (int) (50 * bright), (int) (25 * bright));
				}
				// Torso
				if (tx > 0.32 && tx < 0.68) {
					return new Color((int) (140 * bright), (int) (70 * bright), (int) (35 * bright));
				}
			}
		} else {
			// Legs
			if ((tx > 0.28 && tx < 0.48) || (tx > 0.52 && tx < 0.72)) {
				return new Color((int) (100 * bright), (int) (50 * bright), (int) (25 * bright));
			}
		}

		return null; // transparent
	}

	private void drawHUD(Graphics2D g) {
		// HUD background bar
		g.setColor(new Color(0, 0, 0, 180));
		g.fillRect(0, nScreenHeight - 80, nScreenWidth, 80);

		// Health bar
		g.setColor(new Color(60, 0, 0));
		g.fillRect(20, nScreenHeight - 55, 200, 25);
		g.setColor(nHealth > 50 ? new Color(200, 50, 50) : new Color(255, 0, 0));
		g.fillRect(20, nScreenHeight - 55, (int) (nHealth * 2), 25);
		g.setColor(Color.WHITE);
		g.setFont(new Font("Courier New", Font.BOLD, 16));
		g.drawString("HP: " + nHealth, 25, nScreenHeight - 35);

		// Ammo
		g.setColor(new Color(200, 180, 50));
		g.setFont(new Font("Courier New", Font.BOLD, 16));
		g.drawString("AMMO: " + nAmmo, nScreenWidth - 150, nScreenHeight - 35);

		// Weapon sprite (simple gun shape)
		drawGun(g);

		// Crosshair
		int cx = nScreenWidth / 2;
		int cy = nScreenHeight / 2;
		g.setColor(new Color(255, 255, 255, 180));
		g.drawLine(cx - 12, cy, cx - 4, cy);
		g.drawLine(cx + 4, cy, cx + 12, cy);
		g.drawLine(cx, cy - 12, cx, cy - 4);
		g.drawLine(cx, cy + 4, cx, cy + 12);

		// FPS counter
		g.setColor(new Color(150, 150, 150, 200));
		g.setFont(new Font("Courier New", Font.PLAIN, 11));
		g.drawString(String.format("FPS: %.0f", fps), 10, 20);

		// Mini map
		drawMiniMap(g);

		// Shoot flash
		if (bShooting && nShootTimer > 3) {
			g.setColor(new Color(255, 200, 0, 80));
			g.fillRect(0, 0, nScreenWidth, nScreenHeight);
		}

		// Game over
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

		// Controls help
		g.setColor(new Color(150, 150, 150, 180));
		g.setFont(new Font("Courier New", Font.PLAIN, 11));
		g.drawString("WASD/Arrows: Move | Q/E: Strafe | SPACE: Shoot | R: Restart", 10, nScreenHeight - 10);
	}

	private void drawGun(Graphics2D g) {
		int gx = nScreenWidth / 2 + (bShooting ? 5 : 0);
		int gy = nScreenHeight - (bShooting ? 60 : 80);

		// Gun barrel
		g.setColor(new Color(80, 80, 80));
		g.fillRect(gx - 8, gy - 40, 16, 60);
		g.setColor(new Color(50, 50, 50));
		g.fillRect(gx - 4, gy - 50, 8, 20);

		// Gun handle
		g.setColor(new Color(100, 70, 40));
		g.fillRect(gx - 15, gy - 10, 30, 40);

		// Trigger guard
		g.setColor(new Color(80, 80, 80));
		g.drawArc(gx - 10, gy + 5, 20, 20, 0, -180);

		// Muzzle flash
		if (bShooting && nShootTimer > 5) {
			g.setColor(new Color(255, 220, 50, 200));
			g.fillOval(gx - 15, gy - 75, 30, 30);
			g.setColor(new Color(255, 100, 0, 150));
			g.fillOval(gx - 20, gy - 85, 40, 40);
		}
	}

	private void drawMiniMap(Graphics2D g) {
		int mapScale = 6;
		int mapOffX = nScreenWidth - nMapWidth * mapScale - 10;
		int mapOffY = 10;

		// Background
		g.setColor(new Color(0, 0, 0, 160));
		g.fillRect(mapOffX - 2, mapOffY - 2, nMapWidth * mapScale + 4, nMapHeight * mapScale + 4);

		// Draw map tiles
		for (int my = 0; my < nMapHeight; my++) {
			for (int mx = 0; mx < nMapWidth; mx++) {
				if (map[my].charAt(mx) == '#') {
					g.setColor(new Color(160, 80, 40));
				} else {
					g.setColor(new Color(40, 40, 40));
				}
				g.fillRect(mapOffX + mx * mapScale, mapOffY + my * mapScale, mapScale - 1, mapScale - 1);
			}
		}

		// Draw enemies on minimap
		for (int e = 0; e < nEnemyCount; e++) {
			if (!enemyAlive[e])
				continue;
			g.setColor(Color.RED);
			g.fillOval(mapOffX + (int) (enemyX[e] * mapScale) - 2,
					mapOffY + (int) (enemyY[e] * mapScale) - 2, 5, 5);
		}

		// Draw player
		g.setColor(Color.GREEN);
		g.fillOval(mapOffX + (int) (fPlayerX * mapScale) - 3,
				mapOffY + (int) (fPlayerY * mapScale) - 3, 6, 6);

		// Draw player direction
		g.setColor(Color.YELLOW);
		g.drawLine(mapOffX + (int) (fPlayerX * mapScale),
				mapOffY + (int) (fPlayerY * mapScale),
				mapOffX + (int) ((fPlayerX + Math.cos(fPlayerAngle) * 1.5) * mapScale),
				mapOffY + (int) ((fPlayerY + Math.sin(fPlayerAngle) * 1.5) * mapScale));
	}

	private void shoot() {
		if (bShooting || nAmmo <= 0)
			return;
		bShooting = true;
		nShootTimer = 10;
		nAmmo--;

		// Raycast straight forward to check hits
		double fEyeX = Math.cos(fPlayerAngle);
		double fEyeY = Math.sin(fPlayerAngle);

		for (int e = 0; e < nEnemyCount; e++) {
			if (!enemyAlive[e])
				continue;

			double dx = enemyX[e] - fPlayerX;
			double dy = enemyY[e] - fPlayerY;
			double dist = Math.sqrt(dx * dx + dy * dy);
			double dot = (fEyeX * dx / dist) + (fEyeY * dy / dist);

			if (dot > 0.97 && dist < fDepth) {
				enemyAlive[e] = false;
			}
		}
	}

	private void restart() {
		fPlayerX = 1.5;
		fPlayerY = 7.5;
		fPlayerAngle = 0;
		nHealth = 100;
		nAmmo = 30;
		bGameOver = false;
		bShooting = false;
		for (int i = 0; i < nEnemyCount; i++)
			enemyAlive[i] = true;
		enemyX = new double[] { 5.5, 10.5, 12.5 };
		enemyY = new double[] { 5.5, 9.5, 3.5 };
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.drawImage(offscreen, 0, 0, null);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int code = e.getKeyCode();
		if (code < 256)
			keys[code] = true;
		if (code == KeyEvent.VK_SPACE)
			shoot();
		if (code == KeyEvent.VK_R)
			restart();
	}

	@Override
	public void keyReleased(KeyEvent e) {
		int code = e.getKeyCode();
		if (code < 256)
			keys[code] = false;
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("DOOM-J  |  DOOM-like Raycaster in Java");
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