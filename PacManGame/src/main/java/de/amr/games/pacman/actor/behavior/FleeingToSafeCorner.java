package de.amr.games.pacman.actor.behavior;

import static de.amr.datastruct.StreamUtils.permute;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import de.amr.games.pacman.actor.MazeMover;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.Tile;

/**
 * Lets a ghost escape to the "safest" maze corner depending on Pac-Man's
 * current position. The "safest" corner is defined by the maximum distance of
 * Pac-Man to any tile on the path from the actor's current position to the
 * corner. When the target corner is reached the next corner is computed.
 * 
 * @author Armin Reichert
 */
class FleeingToSafeCorner extends FollowingFixedPath {

	private final Maze maze;

	public FleeingToSafeCorner(Maze maze, Supplier<Tile> chaserTileSupplier) {
		super(chaserTileSupplier);
		this.maze = maze;
	}

	@Override
	public void computePath(MazeMover refugee) {
		Tile target = refugee.currentTile();
		while (target.equals(refugee.currentTile())) {
			target = safeCorner(refugee);
		}
		path = maze.findPath(refugee.currentTile(), target);
	}

	private Tile safeCorner(MazeMover refugee) {
		Tile refugeeTile = refugee.currentTile();
		Tile chaserTile = targetTileSupplier.get();
		//@formatter:off
		return permute(Stream.of(
			maze.getTopLeftCorner(), maze.getTopRightCorner(),
			maze.getBottomRightCorner(), maze.getBottomLeftCorner()
		)).filter(corner -> !corner.equals(refugeeTile))
			.sorted(byDist(refugeeTile, chaserTile).reversed())
			.findFirst().get();
		//@formatter:on
	}

	private Comparator<Tile> byDist(Tile refugeeTile, Tile chaserTile) {
		return (corner1, corner2) -> {
			double dist1 = minDistFromPath(maze.findPath(refugeeTile, corner1), chaserTile);
			double dist2 = minDistFromPath(maze.findPath(refugeeTile, corner2), chaserTile);
			return Double.compare(dist1, dist2);
		};
	}

	private int minDistFromPath(List<Tile> path, Tile tile) {
		int min = Integer.MAX_VALUE;
		for (Tile t : path) {
			int dist = maze.manhattanDist(t, tile);
			if (dist < min) {
				min = dist;
			}
		}
		return min;
	}
}