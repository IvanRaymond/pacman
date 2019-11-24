package de.amr.games.pacman.actor.behavior;

import java.util.List;
import java.util.function.Supplier;

import de.amr.games.pacman.actor.MazeMover;
import de.amr.games.pacman.model.Tile;

/**
 * Steering which computes the shortest path to the target tile.
 *
 * @author Armin Reichert
 */
class TakingShortestPath extends TakingPrecomputedPath implements Steering {

	public TakingShortestPath(Supplier<Tile> fnTargetTile) {
		super(fnTargetTile);
	}

	@Override
	protected List<Tile> computePath(MazeMover actor) {
		return actor.maze.findPath(actor.currentTile(), actor.targetTile);
	}
}