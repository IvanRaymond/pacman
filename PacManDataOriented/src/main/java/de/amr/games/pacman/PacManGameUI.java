package de.amr.games.pacman;

import static de.amr.games.pacman.PacManGame.HTS;
import static de.amr.games.pacman.PacManGame.TS;
import static de.amr.games.pacman.PacManGame.WORLD_HEIGHT;
import static de.amr.games.pacman.PacManGame.WORLD_HEIGHT_TILES;
import static de.amr.games.pacman.PacManGame.WORLD_WIDTH;
import static de.amr.games.pacman.PacManGame.WORLD_WIDTH_TILES;
import static de.amr.games.pacman.PacManGame.levelData;
import static de.amr.games.pacman.PacManGame.position;
import static de.amr.games.pacman.PacManGame.vec;

import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

public class PacManGameUI {

	public boolean debugDraw;
	public float scaling = 2;

	private Canvas canvas;
	private BufferedImage imageMaze;
	private BufferedImage spriteSheet;
	private Map<String, BufferedImage> levelSymbols;
	private Font scoreFont;

	public PacManGameUI(PacManGame game) {

		loadResources();

		JFrame window = new JFrame("Pac-Man");
		window.setResizable(false);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				game.pressedKeys.set(e.getKeyCode());
			}

			@Override
			public void keyReleased(KeyEvent e) {
				game.pressedKeys.clear(e.getKeyCode());
			}
		});

		canvas = new Canvas();
		canvas.setSize((int) (WORLD_WIDTH * scaling), (int) (WORLD_HEIGHT * scaling));
		canvas.setFocusable(false);

		window.add(canvas);
		window.pack();
		window.setLocationRelativeTo(null);
		window.setVisible(true);
		window.requestFocus();

		canvas.createBufferStrategy(2);
	}

	public void loadResources() {
		try {
			spriteSheet = image("/sprites.png");
			imageMaze = image("/maze_full.png");
			levelSymbols = new HashMap<>();
			levelSymbols.put("CHERRIES", sheet(2, 3));
			levelSymbols.put("STRAWBERRY", sheet(3, 3));
			levelSymbols.put("PEACH", sheet(4, 3));
			levelSymbols.put("APPLE", sheet(5, 3));
			levelSymbols.put("GRAPES", sheet(6, 3));
			levelSymbols.put("GALAXIAN", sheet(7, 3));
			levelSymbols.put("BELL", sheet(8, 3));
			levelSymbols.put("KEY", sheet(9, 3));
			scoreFont = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/PressStart2P-Regular.ttf"))
					.deriveFont((float) TS);
		} catch (Exception x) {
			x.printStackTrace();
		}
	}

	private BufferedImage sheet(int x, int y) {
		return spriteSheet.getSubimage(x * 16, y * 16, 16, 16);
	}

	private BufferedImage image(String path) throws IOException {
		return ImageIO.read(getClass().getResourceAsStream(path));
	}

	public void render(PacManGame game) {
		BufferStrategy strategy = canvas.getBufferStrategy();
		do {
			do {
				Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
				g.scale(scaling, scaling);
				draw(game, g);
				g.dispose();
			} while (strategy.contentsRestored());
			strategy.show();
		} while (strategy.contentsLost());
	}

	private void draw(PacManGame game, Graphics2D g) {
		drawScore(g, game);
		drawMaze(g, game);
		drawPacMan(g, game, game.pacMan);
		for (int i = 0; i < game.ghosts.length; ++i) {
			drawGhost(g, game, i);
		}
		drawLevelCounter(g, game);
	}

	private void drawScore(Graphics2D g, PacManGame game) {
		g.setFont(scoreFont);
		g.setColor(Color.WHITE);
		g.drawString(String.format("SCORE %d", game.points), 16, 16);
	}

	private void drawLevelCounter(Graphics2D g, PacManGame game) {
		int x = WORLD_WIDTH - 3 * TS;
		int y = WORLD_HEIGHT - 2 * TS;
		BufferedImage symbol = levelSymbols.get(levelData(game.level).get(0));
		g.drawImage(symbol, x, y, null);
	}

	private void drawMaze(Graphics2D g, PacManGame game) {
		g = (Graphics2D) g.create();
		g.drawImage(imageMaze, 0, 3 * TS, null);
		for (int x = 0; x < WORLD_WIDTH_TILES; ++x) {
			for (int y = 0; y < WORLD_HEIGHT_TILES; ++y) {
				V2 tile = vec(x, y);
				if (game.hasEatenFood(x, y)) {
					g.setColor(Color.BLACK);
					g.fillRect(x * TS, y * TS, TS, TS);
				} else if (game.isEnergizerTile(tile)) {
					if (game.framesTotal % 20 < 10) {
						g.setColor(Color.BLACK);
						g.fillRect(x * TS, y * TS, TS, TS);
					}
				}
			}
		}

		if (debugDraw) {
			g.setColor(new Color(200, 200, 200, 100));
			g.setStroke(new BasicStroke(0.1f));
			for (int row = 1; row < WORLD_HEIGHT_TILES; ++row) {
				g.drawLine(0, row * TS, WORLD_WIDTH, row * TS);
			}
			for (int col = 1; col < WORLD_WIDTH_TILES; ++col) {
				g.drawLine(col * TS, 0, col * TS, WORLD_HEIGHT);
			}
			g.setColor(Color.WHITE);
			g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 8));
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.drawString(String.format("%d frames/sec", game.fps), 2 * TS, 3 * TS);
			g.drawString(String.format("%s", game.state), 12 * TS, 3 * TS);
		}
		g.dispose();
	}

	private void drawPacMan(Graphics2D g, PacManGame game, Creature pacMan) {
		int dirIndex = dirIndex(pacMan.dir);
		long interval = game.framesTotal % 30;
		int frame = (int) interval / 10;
		BufferedImage sprite;
		if (pacMan.stuck) {
			sprite = spriteSheet.getSubimage(0, dirIndex * 16, 16, 16);
		} else if (frame == 2) {
			sprite = spriteSheet.getSubimage(2 * 16, 0, 16, 16);
		} else {
			sprite = spriteSheet.getSubimage(frame * 16, dirIndex * 16, 16, 16);
		}
		V2 position = position(pacMan);
		g.drawImage(sprite, (int) position.x - 4, (int) position.y - 4, null);
//	g.fillRect((int) position.x, (int) position.y, (int) pacMan.size.x, (int) pacMan.size.y);
	}

	private void drawGhost(Graphics2D g, PacManGame game, int ghostIndex) {
		Creature ghost = game.ghosts[ghostIndex];
		int dirIndex = dirIndex(ghost.dir);
		int frame = game.framesTotal % 60 < 30 ? 0 : 1;
		BufferedImage sprite;
		if (game.pacManPowerTime > 0) {
			sprite = spriteSheet.getSubimage((8 + frame) * 16, 4 * 16, 16, 16);
		} else {
			sprite = spriteSheet.getSubimage((2 * dirIndex + frame) * 16, (4 + ghostIndex) * 16, 16, 16);
		}
		V2 position = position(ghost);
		g.setColor(ghost.color);
		g.drawImage(sprite, (int) position.x - 4, (int) position.y - 4, null);
		if (debugDraw) {
//		g.fillRect((int) position.x, (int) position.y, (int) ghost.size.x, (int) ghost.size.y);
			g.drawRect((int) ghost.scatterTile.x * TS, (int) ghost.scatterTile.y * TS, TS, TS);
			g.fillRect((int) ghost.targetTile.x * TS + TS / 4, (int) ghost.targetTile.y * TS + TS / 4, HTS, HTS);
		}
	}

	private int dirIndex(V2 dir) {
		if (V2.RIGHT.equals(dir)) {
			return 0;
		} else if (V2.LEFT.equals(dir)) {
			return 1;
		} else if (V2.UP.equals(dir)) {
			return 2;
		} else if (V2.DOWN.equals(dir)) {
			return 3;
		} else {
			return 0; // TODO
		}
	}
}