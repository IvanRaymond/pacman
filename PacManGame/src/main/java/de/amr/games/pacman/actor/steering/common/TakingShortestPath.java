package de.amr.games.pacman.actor.steering.common;

import java.util.List;
import java.util.function.Supplier;

import de.amr.games.pacman.actor.steering.MazeMover;
import de.amr.games.pacman.model.MazeGraph;
import de.amr.games.pacman.model.Tile;

/**
 * Lets an actor follow the shortest path (using graph path finding) to the
 * target tile.
 *
 * @author Armin Reichert
 */
public class TakingShortestPath extends TakingPrecomputedPath {

	protected MazeGraph graph;

	public TakingShortestPath(MazeMover actor, Supplier<Tile> fnTargetTile) {
		super(actor, fnTargetTile);
		graph = new MazeGraph(maze);
	}

	@Override
	protected List<Tile> pathToTarget(MazeMover actor, Tile targetTile) {
		return graph.shortestPath(actor.tile(), targetTile);
	}
}