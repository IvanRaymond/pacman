package de.amr.games.pacman.model.world.api;

/**
 * Bonus food that appears for a certain time inside the world.
 * 
 * @author Armin Reichert
 */
public interface BonusFood extends Food {

	Tile location();

	int value();

	BonusFoodState state();

	void setState(BonusFoodState state);

	default boolean isActive() {
		return state() == BonusFoodState.ACTIVE;
	}

	default boolean isConsumed() {
		return state() == BonusFoodState.CONSUMED;
	}
}