package de.amr.games.pacman.actor.behavior;

import static de.amr.graph.grid.impl.Top4.E;
import static de.amr.graph.grid.impl.Top4.N;
import static de.amr.graph.grid.impl.Top4.S;
import static de.amr.graph.grid.impl.Top4.W;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.amr.games.pacman.actor.MazeMover;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.Tile;
import de.amr.graph.grid.impl.Top4;

/**
 * A behavior which steers an actor (ghost) towards a (possibly moving) target tile. Each time the
 * {@link #steer(MazeMover)} method is called, the target tile is recomputed. There is also some
 * special logic for entering and exiting the ghost house.
 * 
 * The detailed behavior is described
 * <a href="http://gameinternals.com/understanding-pac-man-ghost-behavior">here</a>.
 * 
 * @author Armin Reichert
 */
class HeadingForTile implements Steering {

	private final Supplier<Tile> fnTargetTile;

	/**
	 * Creates a behavior which lets an actor heading for the target tile supplied by the given
	 * function.
	 * 
	 * @param fnTargetTile
	 *                       function supplying the target tile whenever the {@link #steer(MazeMover)}
	 *                       method is called
	 */
	public HeadingForTile(Supplier<Tile> fnTargetTile) {
		this.fnTargetTile = fnTargetTile;
	}

	@Override
	public void steer(MazeMover actor) {
		final Maze maze = actor.maze;
		actor.targetTile = Objects.requireNonNull(fnTargetTile.get(), "Target tile must not be NULL");
		/* Entering ghost house: move downwards at the ghost house door. */
		if (maze.inGhostHouse(actor.targetTile) && maze.inFrontOfGhostHouseDoor(actor.currentTile())) {
			actor.nextDir = Top4.S;
		}
		/* Leaving ghost house: use Blinky's home tile as (temporary) target tile. */
		else if (maze.inGhostHouse(actor.currentTile()) && !maze.inGhostHouse(actor.targetTile)) {
			actor.targetTile = maze.blinkyHome;
			actor.nextDir = computeNextDir(actor, actor.moveDir, actor.currentTile(), actor.targetTile);
		}
		/* If a new tile is entered, decide where to go as described above. */
		else if (actor.enteredNewTile) {
			actor.nextDir = computeNextDir(actor, actor.moveDir, actor.currentTile(), actor.targetTile);
		}
		actor.targetPath = actor.visualizePath ? computePath(actor) : Collections.emptyList();
	}

	/**
	 * Computes the next move direction as described
	 * <a href="http://gameinternals.com/understanding-pac-man-ghost-behavior">here.</a>
	 * 
	 * <p>
	 * Note: We use separate parameters for the actor's move direction, tile and target tile instead of
	 * the members of the actor itself because the {@link #computePath(MazeMover, Tile)} method uses
	 * this method without actually placing the actor at each tile of the path.
	 * 
	 * @param actor
	 *                      a actor (normally a ghost)
	 * @param moveDir
	 *                      the actor's current move direction
	 * @param currentTile
	 *                      the actor's current tile
	 * @param targetTile
	 *                      the actor's current target tile
	 */
	private static int computeNextDir(MazeMover actor, int moveDir, Tile currentTile, Tile targetTile) {
		/*@formatter:off*/
		Maze maze = actor.maze;
		List<Integer> dirs = Stream.of(N, W, S, E)
				.filter(dir -> dir != Maze.NESW.inv(moveDir))
				.filter(dir -> actor.canEnterTile(currentTile, maze.tileToDir(currentTile, dir)))
				.collect(Collectors.toList());
		/*@formatter:on*/
		if (dirs.isEmpty()) {
			throw new IllegalStateException("Could not determine next move direction");
		}
		if (dirs.size() > 1) {
			dirs.sort((dir1, dir2) -> {
				Tile tile1 = maze.tileToDir(currentTile, dir1), tile2 = maze.tileToDir(currentTile, dir2);
				int comparedByDistance = Integer.compare(distance(tile1, targetTile), distance(tile2, targetTile));
				if (comparedByDistance != 0) {
					return comparedByDistance;
				}
				/*
				 * If two or more potential choices are an equal distance from the target, the decision between them
				 * is made in the order of up > left > down. A decision to exit right can never be made in a
				 * situation where two tiles are equidistant to the target, since any other option has a higher
				 * priority.
				 */
				List<Integer> dirOrder = Arrays.asList(Top4.N, Top4.W, Top4.S, Top4.E);
				return Integer.compare(dirOrder.indexOf(dir1), dirOrder.indexOf(dir2));
			});
		}
		return dirs.get(0);
	}

	/**
	 * Computes the complete path the actor would traverse until it would reach the target tile, a cycle
	 * would occur or the borders of the board would be reached.
	 * 
	 * @param actor
	 *                actor for which the path is computed
	 * @return the path the actor would use
	 */
	private static List<Tile> computePath(MazeMover actor) {
		Maze maze = actor.maze;
		Tile currentTile = actor.currentTile();
		int currentDir = actor.moveDir;
		Set<Tile> path = new LinkedHashSet<>();
		path.add(currentTile);
		while (!currentTile.equals(actor.targetTile)) {
			int nextDir = computeNextDir(actor, currentDir, currentTile, actor.targetTile);
			Tile nextTile = maze.tileToDir(currentTile, nextDir);
			if (!maze.insideBoard(nextTile)) {
				break; // path leaves board
			}
			if (path.contains(nextTile)) {
				break; // cycle
			}
			path.add(nextTile);
			currentTile = nextTile;
			currentDir = nextDir;
		}
		return path.stream().collect(Collectors.toList());
	}

	/** Straight line distance (squared). */
	private static int distance(Tile t1, Tile t2) {
		int dx = t1.col - t2.col, dy = t1.row - t2.row;
		return dx * dx + dy * dy;
	}

}