package de.amr.games.pacman.view.theme.arcade;

import java.awt.Graphics2D;

import de.amr.games.pacman.controller.actor.PacMan;
import de.amr.games.pacman.model.Direction;
import de.amr.games.pacman.model.world.api.World;
import de.amr.games.pacman.view.theme.IRenderer;

public class PacManRenderer extends CreatureRenderer implements IRenderer {

	private final World world;
	private final PacMan pacMan;

	public PacManRenderer(World world, PacMan pacMan) {
		this.world = world;
		this.pacMan = pacMan;
		ArcadeSprites arcadeSprites = ArcadeSprites.BUNDLE;
		Direction.dirs().forEach(dir -> sprites.set("walking-" + dir, arcadeSprites.spr_pacManWalking(dir)));
		sprites.set("dying", arcadeSprites.spr_pacManDying());
		sprites.set("full", arcadeSprites.spr_pacManFull());
		sprites.select("full");
	}

	@Override
	public void draw(Graphics2D g) {
		if (world.isChangingLevel()) {
			selectSprite("full");
		} else {
			switch (pacMan.getState()) {
			case DEAD:
				if (pacMan.collapsing) {
					selectSprite("dying");
				} else if (!sprites.selectedKey().equals("full")) {
					selectSprite("full");
					sprites.get("dying").resetAnimation();
				}
				break;
			case RUNNING:
				selectSprite("walking-" + pacMan.moveDir());
				enableAnimation(pacMan.tf.getVelocity().length() > 0);
				break;
			case SLEEPING:
				selectSprite("full");
			default:
				break;
			}
		}
		drawEntity(g, pacMan);
	}
}