package de.amr.games.pacman.controller.actor.steering.common;

import static de.amr.games.pacman.model.world.Direction.DOWN;
import static de.amr.games.pacman.model.world.Direction.LEFT;
import static de.amr.games.pacman.model.world.Direction.RIGHT;
import static de.amr.games.pacman.model.world.Direction.UP;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;
import java.util.function.Supplier;

import de.amr.games.pacman.controller.actor.WorldMover;
import de.amr.games.pacman.controller.actor.steering.PathProvidingSteering;
import de.amr.games.pacman.model.world.Direction;
import de.amr.games.pacman.model.world.api.World;
import de.amr.games.pacman.model.world.core.Tile;

/**
 * Steers an actor towards a target tile.
 * 
 * The detailed behavior is described
 * <a href= "http://gameinternals.com/understanding-pac-man-ghost-behavior">here</a>.
 * 
 * @author Armin Reichert
 */
public class HeadingForTargetTile implements PathProvidingSteering {

	/**
	 * Computes the next move direction as described
	 * <a href= "http://gameinternals.com/understanding-pac-man-ghost-behavior">here.</a>
	 * <p>
	 * When a ghost is on a portal tile and the steering has just changed, it may happen that no
	 * direction can be computed. In that case we keep the move direction.
	 * <p>
	 * Note: We use separate parameters for the actor's move direction, current tile and target tile
	 * instead of the members of the actor itself because the {@link #pathTo(Tile)} method uses this
	 * method without actually placing the actor at each tile of the path.
	 * 
	 * @param mover   actor moving through the maze
	 * @param moveDir current move direction
	 * @param tile    current tile
	 * @param target  target tile
	 */
	private static Direction bestDir(WorldMover mover, Direction moveDir, Tile tile, Tile target) {
		Function<Direction, Double> fnNeighborDistToTarget = dir -> mover.world().neighbor(tile, dir).distance(target);
		/*@formatter:off*/
		return Direction.dirs()
			.filter(dir -> dir != moveDir.opposite())
			.filter(dir -> mover.canMoveBetween(tile, mover.world().neighbor(tile, dir)))
			.sorted(comparing(fnNeighborDistToTarget).thenComparingInt(asList(UP, LEFT, DOWN, RIGHT)::indexOf))
			.findFirst()
			.orElse(mover.moveDir());
		/*@formatter:on*/
	}

	private final WorldMover mover;
	private final Supplier<Tile> fnTargetTile;
	private final ConcurrentLinkedDeque<Tile> path = new ConcurrentLinkedDeque<>();
	private boolean forced;
	private boolean pathComputed;

	public HeadingForTargetTile(WorldMover mover, Supplier<Tile> fnTargetTile) {
		this.mover = Objects.requireNonNull(mover);
		this.fnTargetTile = Objects.requireNonNull(fnTargetTile);
	}

	@Override
	public void steer() {
		if (mover.enteredNewTile() || forced) {
			forced = false;
			mover.setTargetTile(fnTargetTile.get());
			if (mover.targetTile() != null) {
				mover.setWishDir(bestDir(mover, mover.moveDir(), mover.tile(), mover.targetTile()));
				if (pathComputed) {
					computePath();
				}
			} else {
				path.clear();
			}
		}
	}

	/**
	 * Computes the path the entity would traverse until reaching the target tile, a cycle would occur
	 * or the path would leave the map.
	 */
	private void computePath() {
		World world = mover.world();
		Tile currentTile = mover.tile(), targetTile = mover.targetTile();
		Direction currentDir = mover.moveDir();
		path.clear();
		path.add(currentTile);
		while (!currentTile.equals(targetTile)) {
			Direction dir = bestDir(mover, currentDir, currentTile, targetTile);
			Tile nextTile = world.neighbor(currentTile, dir);
			if (!world.includes(nextTile) || path.contains(nextTile)) {
				return;
			}
			path.add(nextTile);
			currentTile = nextTile;
			currentDir = dir;
		}
	}

	@Override
	public void force() {
		forced = true;
	}

	@Override
	public void setPathComputed(boolean computed) {
		if (pathComputed != computed) {
			path.clear();
		}
		pathComputed = computed;
	}

	@Override
	public boolean isPathComputed() {
		return pathComputed;
	}

	@Override
	public boolean requiresGridAlignment() {
		return true;
	}

	@Override
	public List<Tile> pathToTarget() {
		return new ArrayList<>(path);
	}
}