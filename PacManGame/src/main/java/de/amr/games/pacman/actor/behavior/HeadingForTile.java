package de.amr.games.pacman.actor.behavior;

import static de.amr.games.pacman.model.Maze.NESW;
import static de.amr.graph.grid.impl.Top4.E;
import static de.amr.graph.grid.impl.Top4.N;
import static de.amr.graph.grid.impl.Top4.S;
import static de.amr.graph.grid.impl.Top4.W;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import de.amr.games.pacman.actor.MazeMover;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.Tile;

/**
 * A behavior which steers an actor (ghost) towards a (possibly moving) target tile. Each time the
 * {@link #steer(MazeMover)} method is called, the target tile is recomputed. There is also some
 * special logic for entering and exiting the ghost house.
 * 
 * The detailed behavior is described
 * <a href= "http://gameinternals.com/understanding-pac-man-ghost-behavior">here</a>.
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
		actor.targetTile = fnTargetTile.get();
		if (actor.targetTile == null) {
			return;
		}

		final Maze maze = actor.maze;
		final Tile actorTile = actor.currentTile();
		final boolean actorInHouse = maze.inGhostHouse(actorTile);
		final boolean targetInHouse = maze.inGhostHouse(actor.targetTile);

		/* If a new tile is entered, decide where to go as described above. */
		if (actor.enteredNewTile || actorInHouse && targetInHouse) {
			actor.nextDir = computeNextDir(actor, actor.moveDir, actor.currentTile(), actor.targetTile);
		}
		actor.targetPath = actor.visualizePath ? computePath(actor) : Collections.emptyList();
	}

	/** Directions in order used to compute the next move direction */
	private static final List<Integer> DIRS_IN_ORDER = Collections.unmodifiableList(Arrays.asList(N, W, S, E));

	/**
	 * Computes the next move direction as described
	 * <a href= "http://gameinternals.com/understanding-pac-man-ghost-behavior">here.</a>
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
		return DIRS_IN_ORDER.stream()
			.filter(dir -> dir != NESW.inv(moveDir))
			.filter(dir -> actor.canEnterTile(currentTile, maze.tileToDir(currentTile, dir)))
			.sorted((dir1, dir2) -> {
				Tile nb1 = maze.tileToDir(currentTile, dir1), nb2 = maze.tileToDir(currentTile, dir2);
				int cmpByDistance = Integer.compare(distance(nb1, targetTile), distance(nb2, targetTile));
				return cmpByDistance != 0
					? cmpByDistance
					: Integer.compare(DIRS_IN_ORDER.indexOf(dir1), DIRS_IN_ORDER.indexOf(dir2));
			})
			.findFirst().orElseThrow(IllegalStateException::new);
		/*@formatter:on*/
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