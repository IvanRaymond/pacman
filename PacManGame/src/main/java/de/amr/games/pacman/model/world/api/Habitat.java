package de.amr.games.pacman.model.world.api;

import java.util.stream.Stream;

import de.amr.games.pacman.controller.actor.Creature;
import de.amr.games.pacman.model.world.core.Tile;

/**
 * Where the creatures live.
 * 
 * @author Armin Reichert
 */
public interface Habitat extends FoodContainer {

	Stream<Tile> habitatTiles();

	Population population();

	boolean included(Creature<?> creature);

	void include(Creature<?> creature);

	void exclude(Creature<?> creature);
}