package de.amr.games.pacman.controller.steering.common;

import de.amr.games.pacman.controller.creatures.Creature;
import de.amr.games.pacman.controller.steering.api.Steering;
import de.amr.games.pacman.model.world.api.Direction;
import de.amr.games.pacman.model.world.core.MobileLifeform;

/**
 * Lets a creature move randomly but never reverse its direction.
 * 
 * @author Armin Reichert
 */
public class RandomMovement implements Steering {

	private Creature<?> guy;
	private boolean forced;

	public RandomMovement(Creature<?> guy) {
		this.guy = guy;
	}

	@Override
	public boolean requiresGridAlignment() {
		return true;
	}

	@Override
	public void force() {
		forced = true;
	}

	@Override
	public void steer(MobileLifeform entity) {
		if (forced || entity.enteredNewTile || !guy.canCrossBorderTo(entity.moveDir)) {
			/*@formatter:off*/
			Direction.dirsShuffled()
				.filter(dir -> dir != entity.moveDir.opposite())
				.filter(guy::canCrossBorderTo)
				.findFirst()
				.ifPresent(dir -> entity.wishDir = dir);
			/*@formatter:on*/
			forced = false;
		}
	}
}