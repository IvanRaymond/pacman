package de.amr.games.pacman.view.theme.blocks;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import de.amr.games.pacman.controller.creatures.pacman.PacMan;
import de.amr.games.pacman.controller.creatures.pacman.PacManState;
import de.amr.games.pacman.view.theme.api.IPacManRenderer;

class PacManRenderer implements IPacManRenderer {

	@Override
	public void render(Graphics2D g, PacMan pacMan) {
		if (!pacMan.isVisible()) {
			return;
		}
		smoothDrawingOn(g);
		PacManState state = pacMan.getState();
		int size = 2 * pacMan.entity.tf.width;
		switch (state) {
		case AWAKE:
		case POWERFUL:
			drawRunning(g, pacMan, size);
			break;
		case IN_BED:
		case SLEEPING:
		case DEAD:
			drawFull(g, pacMan, size);
			break;
		case COLLAPSING:
			drawCollapsed(g, pacMan, size);
			break;
		default:
			throw new IllegalArgumentException("Unknown Pac-Man state" + state);
		}
		smoothDrawingOff(g);
	}

	private void drawFull(Graphics2D g, PacMan pacMan, int size) {
		int x = (int) pacMan.entity.tf.x + (pacMan.entity.tf.width - size) / 2;
		int y = (int) pacMan.entity.tf.y + (pacMan.entity.tf.width - size) / 2;
		g.setColor(Color.YELLOW);
		g.fillOval(x, y, size, size);
	}

	private void drawRunning(Graphics2D g, PacMan pacMan, int size) {
		int x = (int) pacMan.entity.tf.x + (pacMan.entity.tf.width - size) / 2;
		int y = (int) pacMan.entity.tf.y + (pacMan.entity.tf.width - size) / 2;
		g.setColor(Color.YELLOW);
		g.fillOval(x, y, size, size);
	}

	private void drawCollapsed(Graphics2D g, PacMan pacMan, int size) {
		Stroke stroke = g.getStroke();
		float thickness = 1f;
		g.setColor(Color.YELLOW);
		for (int d = size; d > size / 8; d = d / 2) {
			int x = (int) pacMan.entity.tf.x + (pacMan.entity.tf.width - d) / 2;
			int y = (int) pacMan.entity.tf.y + (pacMan.entity.tf.width - d) / 2;
			g.setStroke(new BasicStroke(thickness));
			g.drawOval(x, y, d, d);
			thickness = thickness * 0.5f;
		}
		g.setStroke(stroke);
	}
}