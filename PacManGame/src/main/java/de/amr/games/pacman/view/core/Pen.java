package de.amr.games.pacman.view.core;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

import de.amr.games.pacman.model.Tile;

/**
 * Helper for drawing texts in grid.
 * 
 * @author Armin Reichert
 */
public class Pen implements AutoCloseable {

	private final Graphics2D g;
	private Font font = new Font(Font.DIALOG, Font.PLAIN, 10);
	private Color color = Color.BLUE;

	public Pen(Graphics2D g) {
		this.g = (Graphics2D) g.create();
	}

	@Override
	public void close() {
		g.dispose();
	}

	public void color(Color c) {
		color = c;
	}

	public void font(Font f) {
		font = f;
	}

	public void fontSize(float size) {
		font = font.deriveFont(size);
	}

	public void smooth(Runnable ops) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		ops.run();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
	}

	public void drawAtTilePosition(int col, int row, String s) {
		g.setColor(color);
		g.setFont(font);
		Rectangle2D box = g.getFontMetrics().getStringBounds(s, g);
		float dy = Math.round((Tile.SIZE / 2 + box.getHeight()) / 2);
		g.drawString(s, col * Tile.SIZE, row * Tile.SIZE + dy);
	}

	public void drawAtPosition(float x, float y, String s) {
		g.setColor(color);
		g.setFont(font);
		Rectangle2D box = g.getFontMetrics().getStringBounds(s, g);
		float dy = Math.round((Tile.SIZE / 2 + box.getHeight()) / 2);
		g.drawString(s, x, y + dy);
	}

	public void hcenter(String s, int viewWidth, int row) {
		g.setColor(color);
		g.setFont(font);
		g.drawString(s, (viewWidth - g.getFontMetrics().stringWidth(s)) / 2, row * Tile.SIZE);
	}
}