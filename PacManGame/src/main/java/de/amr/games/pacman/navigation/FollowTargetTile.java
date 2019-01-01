package de.amr.games.pacman.navigation;

import static de.amr.games.pacman.model.Maze.NESW;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import de.amr.easy.grid.impl.Top4;
import de.amr.games.pacman.actor.MazeEntity;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.Tile;

/**
 * Attempt at implementing the original Ghost behavior as described
 * <a href="http://gameinternals.com/post/2072558330/understanding-pac-man-ghost-behavior">here</a>:
 *
 * <p>
 * <cite> The next step is understanding exactly how the ghosts attempt to reach their target tiles.
 * The ghosts’ AI is very simple and short-sighted, which makes the complex behavior of the ghosts
 * even more impressive. Ghosts only ever plan one step into the future as they move about the maze.
 * </cite>
 * </p>
 * 
 * <p>
 * <cite> Whenever a ghost enters a new tile, it looks ahead to the next tile that it will reach,
 * and makes a decision about which direction it will turn when it gets there. These decisions have
 * one very important restriction, which is that ghosts may never choose to reverse their direction
 * of travel. That is, a ghost cannot enter a tile from the left side and then decide to reverse
 * direction and move back to the left. The implication of this restriction is that whenever a ghost
 * enters a tile with only two exits, it will always continue in the same direction. </cite>
 * </p>
 * 
 * @author Armin Reichert
 */
class FollowTargetTile<T extends MazeEntity> implements Behavior<T> {

	private final Supplier<Tile> targetTileSupplier;

	public FollowTargetTile(Supplier<Tile> targetTileSupplier) {
		this.targetTileSupplier = targetTileSupplier;
	}

	@Override
	public Route getRoute(T actor) {
		final Route route = new Route();

		// where to go?
		final Tile targetTile = targetTileSupplier.get();
		Objects.requireNonNull(targetTile, "Target tile must not be NULL");
		route.setTarget(targetTile);

		final Maze maze = actor.getMaze();
		final int actorDir = actor.getMoveDir();
		final Tile actorTile = actor.getTile();

		// use graph path-finder for entering ghost house
		if (maze.isGhostHouseEntry(actorTile) && maze.inGhostHouse(targetTile)) {
			List<Tile> intoGhostHouse = maze.findPath(actorTile, targetTile);
			route.setPath(intoGhostHouse);
			route.setDir(maze.alongPath(intoGhostHouse).orElse(actorDir));
			return route;
		}

		// also use path-finder inside ghost house and for exiting ghost house
		if (maze.inGhostHouse(actorTile)) {
			if (maze.inGhostHouse(targetTile)) {
				// follow target inside ghost house
				route.setPath(maze.findPath(actorTile, targetTile));
			} else {
				// go to Blinky's home to exit ghost house
				route.setPath(maze.findPath(actorTile, maze.getBlinkyHome()));
			}
			route.setDir(maze.alongPath(route.getPath()).orElse(actorDir));
			return route;
		}

		// if stuck, check if turning left or right is possible
		if (actor.isStuck()) {
			int[] leftOrRight = { NESW.left(actorDir), NESW.right(actorDir) };
			for (int turn : leftOrRight) {
				Tile neighbor = maze.neighborTile(actorTile, turn).get();
				if (actor.canEnterTile(neighbor)) {
					route.setDir(turn);
					return route;
				}
			}
		}

		// decide where to go if the next tile is an intersection
		final Tile nextTile = actorTile.tileTowards(actorDir);
		final boolean unrestricted = maze.isUnrestrictedIntersection(nextTile);
		final boolean upwardsBlocked = maze.isUpwardsBlockedIntersection(nextTile);
		if (unrestricted || upwardsBlocked) {
			/*@formatter:off*/
			IntStream possibleDirs = NESW.dirs()
					.filter(dir -> dir != NESW.inv(actorDir)) // cannot reverse direction
					.filter(dir -> unrestricted || dir != Top4.N);
			/*@formatter:on*/
			Optional<Integer> bestDir = findBestDir(actor, nextTile, targetTile, possibleDirs);
			if (bestDir.isPresent()) {
				route.setDir(bestDir.get());
				return route;
			}
		}

		// no direction could be determined
		route.setDir(-1);
		return route;
	}

	/**
	 * Finds the "best" direction from the source tile towards the target tile. The best direction leads
	 * to the neighbor with the least straight line (Euclidean) distance to the target tile.
	 */
	private static Optional<Integer> findBestDir(MazeEntity actor, Tile sourceTile, Tile targetTile,
			IntStream possibleDirs) {
		/*@formatter:off*/
		return possibleDirs.boxed()
			.map(dir -> actor.getMaze().neighborTile(sourceTile, dir))
			.filter(Optional::isPresent).map(Optional::get)
			.filter(actor::canEnterTile)
			.sorted(byEuclideanDistSquared(targetTile))
			.map(tile -> actor.getMaze().direction(sourceTile, tile).getAsInt())
			.findFirst();
		/*@formatter:on*/
	}

	private static Comparator<Tile> byEuclideanDistSquared(Tile target) {
		return (t1, t2) -> Float.compare(euclideanDistSquared(t1, target), euclideanDistSquared(t2, target));
	}

	private static float euclideanDistSquared(Tile t1, Tile t2) {
		return (t1.col - t2.col) * (t1.col - t2.col) + (t1.row - t2.row) * (t1.row - t2.row);
	}
}