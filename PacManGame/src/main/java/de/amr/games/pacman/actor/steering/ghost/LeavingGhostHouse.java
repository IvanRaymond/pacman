package de.amr.games.pacman.actor.steering.ghost;

import static de.amr.games.pacman.model.Direction.LEFT;
import static de.amr.games.pacman.model.Direction.RIGHT;
import static de.amr.games.pacman.model.Direction.UP;

import de.amr.games.pacman.actor.Ghost;
import de.amr.games.pacman.actor.steering.Steering;
import de.amr.games.pacman.model.tiles.Tile;

/**
 * Steers a ghost out of the house.
 * 
 * @author Armin Reichert
 */
public class LeavingGhostHouse implements Steering {

	private final Ghost ghost;

	public LeavingGhostHouse(Ghost ghost) {
		this.ghost = ghost;
	}

	private Tile ghostHouseExit() {
		return ghost.maze().ghostHouseSeats[0];
	}

	@Override
	public void steer() {
		Tile exit = ghostHouseExit();
		int targetX = exit.centerX(), targetY = exit.y();
		if (ghost.tf.y <= targetY) {
			ghost.tf.y = targetY;
		} else if (Math.round(ghost.tf.x) == targetX) {
			ghost.tf.x = targetX;
			ghost.setWishDir(UP);
		} else if (ghost.tf.x < targetX) {
			ghost.setWishDir(RIGHT);
		} else if (ghost.tf.x > targetX) {
			ghost.setWishDir(LEFT);
		}
	}

	@Override
	public boolean isComplete() {
		return ghost.tf.y == ghostHouseExit().y();
	}

	@Override
	public boolean requiresGridAlignment() {
		return false;
	}
}