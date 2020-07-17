package de.amr.games.pacman.model.world.api;

import de.amr.easy.game.controller.Lifecycle;

/**
 * A lifeform inside its world.
 * 
 * @author Armin Reichert
 */
public interface Lifeform extends Lifecycle {

	Tile tileLocation();

	void placeAt(Tile tile, float offsetX, float offsetY);

	default void placeAt(Tile tile) {
		placeAt(tile, 0, 0);
	}

	World world();

	default boolean isInsideWorld() {
		return world().contains(this);
	}

	boolean isVisible();

	void setVisible(boolean visible);
}