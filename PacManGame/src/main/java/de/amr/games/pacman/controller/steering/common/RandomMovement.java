package de.amr.games.pacman.controller.steering.common;

import de.amr.games.pacman.controller.steering.api.Steering;
import de.amr.games.pacman.model.world.api.Direction;
import de.amr.games.pacman.model.world.api.MobileLifeform;

/**
 * Lets a creature move randomly but never reverse its direction.
 * 
 * @author Armin Reichert
 */
public class RandomMovement<M extends MobileLifeform> implements Steering<M> {

	private boolean forced;

	@Override
	public boolean requiresGridAlignment() {
		return true;
	}

	@Override
	public void force() {
		forced = true;
	}

	@Override
	public void steer(M mover) {
		if (forced || mover.enteredNewTile() || !mover.canCrossBorderTo(mover.moveDir())) {
			/*@formatter:off*/
			Direction.dirsShuffled()
				.filter(dir -> dir != mover.moveDir().opposite())
				.filter(mover::canCrossBorderTo)
				.findFirst()
				.ifPresent(mover::setWishDir);
			/*@formatter:on*/
			forced = false;
		}
	}
}