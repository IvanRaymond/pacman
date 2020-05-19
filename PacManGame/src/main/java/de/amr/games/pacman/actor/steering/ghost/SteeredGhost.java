package de.amr.games.pacman.actor.steering.ghost;

import de.amr.easy.game.math.Vector2f;
import de.amr.games.pacman.actor.Ghost;
import de.amr.games.pacman.actor.MazeMover;
import de.amr.games.pacman.actor.steering.Steering;
import de.amr.games.pacman.actor.steering.common.SteeredMazeMover;
import de.amr.games.pacman.model.Tile;

/**
 * Ghost-specific steerings.
 * 
 * @author Armin Reichert
 */
public interface SteeredGhost extends SteeredMazeMover {

	default Ghost steeredGhost() {
		return (Ghost) this;
	}

	/**
	 * Lets the ghost jump up and down at its own seat in the house.
	 * 
	 * @return behavior which lets the ghost jump
	 */
	default Steering isJumpingUpAndDown(Vector2f seatPosition) {
		return new JumpingUpAndDown(steeredGhost(), seatPosition.y);
	}

	/**
	 * Lets the actor avoid the attacker's path by walking to a "safe" maze corner.
	 * 
	 * @param attacker the attacking actor
	 * @param corners  list of tiles representing maze corners
	 * 
	 * @return behavior where actor flees to a "safe" maze corner
	 */
	default Steering isFleeingToSafeCorner(MazeMover attacker, Tile... corners) {
		return new FleeingToSafeCorner(steeredGhost(), attacker, corners);
	}

	/**
	 * Lets a ghost enter the ghost house and move to the seat with the given
	 * position.
	 * 
	 * @param seatPosition seat position
	 * 
	 * @return behavior which lets a ghost enter the house and take its seat
	 */
	default Steering isTakingSeat(Vector2f seatPosition) {
		// add 3 pixel so that ghost dives deeper into ghost house
		return new EnteringGhostHouse(steeredGhost(), Vector2f.of(seatPosition.x, seatPosition.y + 3));
	}

	/**
	 * Lets a ghost leave the ghost house.
	 * 
	 * @return behavior which lets a ghost leave the ghost house
	 */
	default Steering isLeavingGhostHouse() {
		return new LeavingGhostHouse(steeredGhost());
	}
}