package de.amr.games.pacman.actor.behavior;

import java.util.Collections;
import java.util.List;

import de.amr.games.pacman.actor.core.MazeMover;
import de.amr.games.pacman.model.Tile;

/**
 * Interface for steering of actors.
 * 
 * @param <T>
 *          type of steered entity, must implement the {@link MazeMover} interface
 * 
 * @author Armin Reichert
 */
@FunctionalInterface
public interface Steering<T extends MazeMover> {

	/**
	 * Steers the actor towards its target tile or wherever it should move in its current state.
	 * 
	 * @param actor
	 *                the steered actor
	 */
	void steer(T actor);

	/**
	 * Some steerings like {@link Steerings#headingForTargetTile()} need a trigger before they start
	 * working.
	 * 
	 * @param actor
	 *                the steered actor
	 */
	default void triggerSteering(T actor) {
		actor.setEnteredNewTile();
	}

	/**
	 * @return tells if the steering requires the actor to always stay aligned with the grid
	 */
	default boolean stayOnTrack() {
		return true;
	}

	/**
	 * @return the complete path to the target id the implementing class computes it
	 */
	default List<Tile> targetPath() {
		return Collections.emptyList();
	}

	/**
	 * Tells the steering to compute the complete path to the target tile. Steerings may ignore this.
	 * 
	 * @param b
	 *            if target path should be computed
	 */
	default void computeTargetPath(boolean b) {

	}
}