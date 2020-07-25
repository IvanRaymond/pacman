package de.amr.games.pacman.view.theme.blocks;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import de.amr.games.pacman.controller.creatures.pacman.PacMan;
import de.amr.games.pacman.controller.creatures.pacman.PacManState;
import de.amr.games.pacman.model.world.api.Tile;
import de.amr.games.pacman.view.theme.api.IPacManRenderer;

class PacManRenderer implements IPacManRenderer {

	private final PacMan pacMan;

	public PacManRenderer(PacMan pacMan) {
		this.pacMan = pacMan;
	}

	@Override
	public void render(Graphics2D g) {
		if (!pacMan.isVisible()) {
			return;
		}
		smoothDrawingOn(g);
		PacManState state = pacMan.getState();
		switch (state) {
		case DEAD:
			drawFull(g);
			break;
		case COLLAPSING:
			drawCollapsed(g);
			break;
		case AWAKE:
			drawRunning(g);
			break;
		case TIRED:
		case SLEEPING:
			drawFull(g);
			break;
		default:
			break;
		}
		smoothDrawingOff(g);
	}

	private int tiles(double amount) {
		return (int) (amount * Tile.SIZE);
	}

	private void drawFull(Graphics2D g) {
		int size = tiles(2);
		int x = (int) pacMan.entity.tf.x + (pacMan.entity.tf.width - size) / 2;
		int y = (int) pacMan.entity.tf.y + (pacMan.entity.tf.width - size) / 2;
		g.setColor(Color.YELLOW);
		g.fillOval(x, y, size, size);
	}

	private void drawRunning(Graphics2D g) {
		int size = tiles(2);
		int x = (int) pacMan.entity.tf.x + (pacMan.entity.tf.width - size) / 2;
		int y = (int) pacMan.entity.tf.y + (pacMan.entity.tf.width - size) / 2;
		g.setColor(Color.YELLOW);
		g.fillOval(x, y, size, size);
	}

	private void drawCollapsed(Graphics2D g) {
		Stroke stroke = g.getStroke();
		float thickness = 1f;
		g.setColor(Color.YELLOW);
		for (int d = tiles(2); d > tiles(0.25f); d = d / 2) {
			int x = (int) pacMan.entity.tf.x + (pacMan.entity.tf.width - d) / 2;
			int y = (int) pacMan.entity.tf.y + (pacMan.entity.tf.width - d) / 2;
			g.setStroke(new BasicStroke(thickness));
			g.drawOval(x, y, d, d);
			thickness = thickness * 0.5f;
		}
		g.setStroke(stroke);
	}
}