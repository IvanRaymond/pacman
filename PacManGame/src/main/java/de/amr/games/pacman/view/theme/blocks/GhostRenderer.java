package de.amr.games.pacman.view.theme.blocks;

import static de.amr.games.pacman.view.theme.blocks.BlocksTheme.ghostColor;

import java.awt.Color;
import java.awt.Graphics2D;

import de.amr.easy.game.Application;
import de.amr.games.pacman.controller.actor.Ghost;
import de.amr.games.pacman.controller.actor.GhostState;
import de.amr.games.pacman.model.world.core.Tile;
import de.amr.games.pacman.view.core.IRenderer;

class GhostRenderer implements IRenderer {

	private final Ghost ghost;

	public GhostRenderer(Ghost ghost) {
		this.ghost = ghost;
	}

	@Override
	public void render(Graphics2D g) {
		if (!ghost.visible) {
			return;
		}
		GhostState state = ghost.getState();
		if (state == null) {
			smoothDrawingOn(g);
			drawColored(g);
			smoothDrawingOff(g);
			return;
		}
		smoothDrawingOn(g);
		switch (state) {
		case CHASING:
		case SCATTERING:
		case LOCKED:
		case LEAVING_HOUSE:
			drawColored(g);
			break;
		case FRIGHTENED:
			if (ghost.isFlashing()) {
				drawFlashing(g);
			} else {
				drawFrightened(g);
			}
			break;
		case DEAD:
		case ENTERING_HOUSE:
			if (ghost.getBounty() > 0) {
				drawPoints(g);
			} else {
				drawEyes(g);
			}
			break;
		default:
			break;
		}
		smoothDrawingOff(g);
	}

	private int tiles(double amount) {
		return (int) (amount * Tile.SIZE);
	}

	private void drawEyes(Graphics2D g) {
		int size = tiles(1);
		int x = (int) ghost.tf.x + (ghost.tf.width - size) / 2;
		int y = (int) ghost.tf.y + (ghost.tf.width - size) / 2;
		g.setColor(ghostColor(ghost));
		g.drawRect(x, y, size, size);
	}

	private void drawPoints(Graphics2D g) {
		g.setColor(Color.GREEN);
		g.setFont(BlocksTheme.FONT);
		g.drawString(ghost.getBounty() + "", (int) ghost.tf.x, (int) ghost.tf.y);
	}

	private void drawFrightened(Graphics2D g) {
		int size = tiles(1.5);
		int x = (int) ghost.tf.x + (ghost.tf.width - size) / 2;
		int y = (int) ghost.tf.y + (ghost.tf.width - size) / 2;
		g.translate(0, 2);
		g.setColor(Color.BLUE);
		g.fillRect(x, y, size, size);
		g.fillArc(x, y - Tile.SIZE / 2, size, size, 0, 180);
		g.translate(0, -2);
	}

	private void drawFlashing(Graphics2D g) {
		boolean flash = Application.app().clock().getTotalTicks() % 30 < 15;
		int size = tiles(1.5);
		int x = (int) ghost.tf.x + (ghost.tf.width - size) / 2;
		int y = (int) ghost.tf.y + (ghost.tf.width - size) / 2;
		g.translate(0, 2);
		g.setColor(flash ? Color.WHITE : Color.BLUE);
		g.fillRect(x, y, size, size);
		g.fillArc(x, y - Tile.SIZE / 2, size, size, 0, 180);
		g.translate(0, -2);
	}

	private void drawColored(Graphics2D g) {
		int size = tiles(1.5);
		int x = (int) ghost.tf.x + (ghost.tf.width - size) / 2;
		int y = (int) ghost.tf.y + (ghost.tf.width - size) / 2;
		g.translate(0, 2);
		g.setColor(ghostColor(ghost));
		g.fillRect(x, y, size, size);
		g.fillArc(x, y - Tile.SIZE / 2, size, size, 0, 180);
		g.translate(0, -2);
	}

	@Override
	public void resetAnimations() {
	}

	@Override
	public void enableAnimation(boolean enabled) {
	}
}