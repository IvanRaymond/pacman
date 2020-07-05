package de.amr.games.pacman.view.theme.blocks;

import java.awt.Color;
import java.awt.Graphics2D;

import de.amr.games.pacman.controller.actor.PacMan;
import de.amr.games.pacman.controller.actor.PacManState;
import de.amr.games.pacman.model.world.api.World;
import de.amr.games.pacman.model.world.core.Tile;
import de.amr.games.pacman.view.theme.IRenderer;

class PacManRenderer implements IRenderer {

	private final PacMan pacMan;

	public PacManRenderer(World world, PacMan pacMan) {
		this.pacMan = pacMan;
	}

	@Override
	public void draw(Graphics2D g) {
		if (!pacMan.visible) {
			return;
		}
		smoothOn(g);
		PacManState state = pacMan.getState();
		switch (state) {
		case DEAD:
			drawCollapsing(g);
			break;
		case RUNNING:
			drawRunning(g);
			break;
		case SLEEPING:
			drawFull(g);
			break;
		default:
			break;
		}
		smoothOff(g);
	}

	private void drawFull(Graphics2D g) {
		int w = pacMan.tf.width * 2, h = pacMan.tf.height * 2;
		int x = (int) pacMan.tf.x - Tile.SIZE / 2, y = (int) pacMan.tf.y - Tile.SIZE / 2;
		g.setColor(Color.YELLOW);
		g.fillOval(x, y, w, h);
	}

	private void drawRunning(Graphics2D g) {
		int w = pacMan.tf.width * 2, h = pacMan.tf.height * 2;
		int x = (int) pacMan.tf.x - Tile.SIZE / 2, y = (int) pacMan.tf.y - Tile.SIZE / 2;
		g.setColor(Color.YELLOW);
		g.fillOval(x, y, w, h);
	}

	private void drawCollapsing(Graphics2D g) {
		int w = pacMan.tf.width * 2, h = pacMan.tf.height * 2;
		int x = (int) pacMan.tf.x - Tile.SIZE / 2, y = (int) pacMan.tf.y - Tile.SIZE / 2;
		g.setColor(Color.YELLOW);
		g.drawOval(x, y, w, h);
	}
}