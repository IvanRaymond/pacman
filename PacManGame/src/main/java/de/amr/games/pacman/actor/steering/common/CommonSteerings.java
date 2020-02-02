package de.amr.games.pacman.actor.steering.common;

import java.util.List;
import java.util.function.Supplier;

import de.amr.games.pacman.actor.steering.MazeMover;
import de.amr.games.pacman.actor.steering.Steering;
import de.amr.games.pacman.model.Tile;

/**
 * Common steerings.
 * 
 * @author Armin Reichert
 */
public interface CommonSteerings extends MazeMover {

	/**
	 * @param up
	 *                key for moving up
	 * @param right
	 *                key for moving right
	 * @param down
	 *                key for moving down
	 * @param left
	 *                key for moving left
	 * 
	 * @return steering using the given keys
	 */
	default Steering isFollowingKeys(int up, int right, int down, int left) {
		return new FollowingKeys(this, up, right, down, left);
	}

	/**
	 * Lets the actor move randomly through the maze while respecting the maze structure (for example,
	 * chasing and scattering ghost may not move upwards at dedicated tiles. Also reversing the
	 * direction is never allowed.
	 * 
	 * @return random move behavior
	 */
	default Steering isMovingRandomlyWithoutTurningBack() {
		return new MovingRandomlyWithoutTurningBack(this);
	}

	/**
	 * Lets the actor head for a variable (probably unreachable) target tile by taking the "best"
	 * direction at every intersection.
	 * 
	 * @return behavior where actor heads for the target tile
	 */
	default Steering isHeadingFor(Supplier<Tile> fnTargetTile) {
		return new HeadingForTargetTile(this, fnTargetTile);
	}

	/**
	 * Lets the actor head for a constant (probably unreachable) target tile by taking the "best"
	 * direction at every intersection.
	 * 
	 * @return behavior where actor heads for the target tile
	 */
	default Steering isHeadingFor(Tile targetTile) {
		return isHeadingFor(() -> targetTile);
	}

	/**
	 * Lets the actor follow the shortest path to the target. Depending on the actor's current state,
	 * this path might not be completely accessible for the actor.
	 * 
	 * @param fnTarget
	 *                   function supplying the target tile
	 * 
	 * @return behavior where an actor follows the shortest (using Manhattan distance) path to a target
	 *         tile
	 */
	default Steering isTakingShortestPath(Supplier<Tile> fnTarget) {
		return new TakingShortestPath(this, fnTarget);
	}

	/**
	 * Lets the actor follow a fixed path to the target. As the rules for accessing tiles are not
	 * checked, the actor may get stuck.
	 * 
	 * @param path
	 *               the path to follow
	 * 
	 * @return behavior where actor follows the given path
	 */
	default Steering isTakingFixedPath(List<Tile> path) {
		if (path.isEmpty()) {
			throw new IllegalArgumentException("Path must not be empty");
		}
		return new TakingFixedPath(this, path);
	}
}