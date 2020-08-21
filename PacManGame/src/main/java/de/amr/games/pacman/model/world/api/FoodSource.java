package de.amr.games.pacman.model.world.api;

import java.util.Optional;

import de.amr.games.pacman.model.world.components.Tile;

/**
 * Provides food-related functionality.
 * 
 * @author Armin Reichert
 */
public interface FoodSource {

	int totalFoodCount();

	void restoreFood();

	Optional<Food> foodAt(Tile location);

	void eatFood(Tile location);

	default boolean hasFood(Tile location) {
		return foodAt(location).isPresent();
	}

	default boolean hasFood(Food food, Tile location) {
		return foodAt(location).filter(food::equals).isPresent();
	}

	boolean hasEatenFood(Tile location);

	Optional<TemporaryFood> temporaryFood();

	void showTemporaryFood(TemporaryFood food);

	void hideTemporaryFood();
}