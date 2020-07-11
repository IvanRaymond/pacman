package de.amr.games.pacman.view.theme.common;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import de.amr.easy.game.view.Pen;
import de.amr.games.pacman.model.game.Game;
import de.amr.games.pacman.model.world.core.Tile;
import de.amr.games.pacman.view.api.IRenderer;

public class ScoreRenderer implements IRenderer {

	private final Game game;
	private Font font = new Font(Font.MONOSPACED, Font.PLAIN, Tile.SIZE);
	private boolean smoothText = false;
	private int topMargin = 1;
	private int baselineOffset = Tile.SIZE;

	public ScoreRenderer(Game game) {
		this.game = game;
	}

	public void setSmoothText(boolean smoothText) {
		this.smoothText = smoothText;
	}

	public void setFont(Font font) {
		this.font = font;
	}

	public void setBaselineOffset(int baselineOffset) {
		this.baselineOffset = baselineOffset;
	}

	public void setTopMargin(int topMargin) {
		this.topMargin = topMargin;
	}

	@Override
	public void render(Graphics2D g) {
		g.translate(0, topMargin);
		try (Pen pen = new Pen(g)) {
			Color hilight = Color.YELLOW;
			int interlineSpacing = 2;
			int col;
			pen.down(baselineOffset);
			pen.font(font);
			if (smoothText) {
				pen.turnSmoothRenderingOn();
			}

			// Game score
			col = 1;
			pen.color(hilight);
			pen.drawAtGridPosition("Score".toUpperCase(), col, 0);

			pen.color(Color.WHITE);
			pen.down(interlineSpacing);
			pen.drawAtGridPosition(String.format("%7d", game.score), col, 1);
			pen.up(interlineSpacing);

			// Highscore
			col = 9;
			pen.color(hilight);
			pen.drawAtGridPosition("High Score".toUpperCase(), col, 0);
			pen.color(Color.WHITE);
			pen.down(interlineSpacing);
			pen.drawAtGridPosition(String.format("%7d", game.hiscore.points), col, 1);
			pen.color(Color.LIGHT_GRAY);
			pen.drawAtGridPosition(String.format("L%02d", game.hiscore.level), col + 7, 1);
			pen.up(interlineSpacing);

			col = 21;
			pen.color(hilight);
			pen.drawAtGridPosition(String.format("Level".toUpperCase()), col, 0);
			// Level number
			pen.color(Color.WHITE);
			pen.down(interlineSpacing);
			pen.drawAtGridPosition(String.format("%02d", game.level.number), col, 1);
			pen.up(interlineSpacing);

			// Number of remaining pellets
			// dot image
			int size = 4;
			int dotX = (col + 3) * Tile.SIZE - size - 1;
			int dotY = topMargin + Tile.SIZE + interlineSpacing + 1;
			g.setColor(Color.PINK);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.fillOval(dotX, dotY, size, size);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

			pen.color(Color.WHITE);
			pen.down(interlineSpacing);
			pen.drawAtGridPosition(String.format("%03d", game.level.remainingFoodCount()), col + 3, 1);
			pen.up(interlineSpacing);
		}
		g.translate(0, -topMargin);
	}
}