package de.amr.games.pacman.controller.api;

import de.amr.easy.game.entity.Entity;
import de.amr.games.pacman.model.world.api.Direction;
import de.amr.games.pacman.model.world.core.Tile;

/**
 * Implemented by entities that can move through the world.
 * 
 * @author Armin Reichert
 */
public interface MobileCreature extends Creature {

	Entity entity();

	/**
	 * @return the current move direction
	 */
	Direction moveDir();

	/**
	 * Sets the move direction.
	 * 
	 * @param dir the wanted move direction
	 */
	void setMoveDir(Direction dir);

	/**
	 * @return the wanted move direction
	 */
	Direction wishDir();

	/**
	 * Sets the wanted move direction.
	 * 
	 * @param dir the wanted move direction
	 */
	void setWishDir(Direction dir);

	/**
	 * @return tells if a new tile has been entered with the previous move
	 */
	boolean enteredNewTile();

	/**
	 * @return the (optional) target tile
	 */
	Tile targetTile();

	/**
	 * Sets the target tile.
	 * 
	 * @param tile target tile of this entity
	 */
	void setTargetTile(Tile tile);

	/**
	 * @param dir direction
	 * @return if the entity can move into the neighbor tile towards the given direction
	 */
	boolean canCrossBorderTo(Direction dir);

	/**
	 * @param tile     some tile
	 * @param neighbor neighbor the tile
	 * @return tells if the entity can move from the given tile tile to the neighbor tile (might be
	 *         state-dependent)
	 */
	boolean canMoveBetween(Tile tile, Tile neighbor);

	/**
	 * Forces the entity to move to the given direction.
	 * 
	 * @param dir direction
	 */
	void forceMoving(Direction dir);

	/**
	 * Forces the entity to reverse its current move direction.
	 */
	default void reverseDirection() {
		forceMoving(moveDir().opposite());
	}
}