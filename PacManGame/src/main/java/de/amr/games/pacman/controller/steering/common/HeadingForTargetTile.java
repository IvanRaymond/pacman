package de.amr.games.pacman.controller.steering.common;

import static de.amr.games.pacman.model.world.api.Direction.DOWN;
import static de.amr.games.pacman.model.world.api.Direction.LEFT;
import static de.amr.games.pacman.model.world.api.Direction.RIGHT;
import static de.amr.games.pacman.model.world.api.Direction.UP;
import static java.util.Comparator.comparing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import de.amr.games.pacman.controller.steering.api.SteeredMover;
import de.amr.games.pacman.controller.steering.api.Steering;
import de.amr.games.pacman.model.world.api.Direction;
import de.amr.games.pacman.model.world.api.World;
import de.amr.games.pacman.model.world.components.Tile;

/**
 * Steers a guy towards a target tile.
 * 
 * The detailed behavior is described
 * <a href= "http://gameinternals.com/understanding-pac-man-ghost-behavior">here</a>.
 * 
 * @author Armin Reichert
 */
public class HeadingForTargetTile implements Steering {

	private static final List<Direction> DIRECTION_ORDER = List.of(UP, LEFT, DOWN, RIGHT);

	/**
	 * Computes the next direction to take for reaching the target tile as described
	 * <a href= "http://gameinternals.com/understanding-pac-man-ghost-behavior">here.</a>
	 * <p>
	 * Note: I use method parameters for the move direction, current and target tile instead of the
	 * fields of the guy because the {@link #pathTo(Tile)} method also uses this method without actually
	 * moving the guy along the path.
	 * 
	 * @param guy     the guy moving
	 * @param moveDir current move direction
	 * @param tile    current tile
	 * @param target  target tile
	 */
	private static Direction bestDirTowardsTarget(World world, SteeredMover guy, Direction moveDir, Tile tile,
			Tile target) {
		/*@formatter:off*/
		return Direction.dirs()
			.filter(dir -> dir != moveDir.opposite())
			.filter(dir -> guy.canMoveBetween(tile, world.neighbor(tile, dir)))
			.sorted(comparing((Direction dir) -> world.neighbor(tile, dir).distance(target))
					.thenComparing(DIRECTION_ORDER::indexOf))
			.findFirst()
			.orElse(moveDir);
		/*@formatter:on*/
	}

	private final World world;
	private final SteeredMover guy;
	private final Supplier<Tile> fnTargetTile;
	private final List<Tile> path;

	private boolean pathComputed;
	private boolean forced;

	public HeadingForTargetTile(World world, SteeredMover guy, Supplier<Tile> fnTargetTile) {
		this.world = Objects.requireNonNull(world);
		this.guy = Objects.requireNonNull(guy);
		this.fnTargetTile = Objects.requireNonNull(fnTargetTile);
		this.path = new ArrayList<>();
	}

	@Override
	public Optional<Tile> targetTile() {
		return Optional.ofNullable(fnTargetTile.get());
	}

	@Override
	public void steer(SteeredMover guy) {
		if (forced || guy.enteredNewTile) {
			Tile target = fnTargetTile.get();
			if (target != null) {
				guy.wishDir = bestDirTowardsTarget(world, guy, guy.moveDir, guy.tile(), target);
				updatePath(target);
			}
			forced = false;
		}
	}

	@Override
	public boolean requiresGridAlignment() {
		return true;
	}

	@Override
	public void force() {
		forced = true;
	}

	@Override
	public boolean isPathComputed() {
		return pathComputed;
	}

	@Override
	public void setPathComputed(boolean computed) {
		pathComputed = computed;
		updatePath(fnTargetTile.get());
	}

	@Override
	public List<Tile> pathToTarget() {
		return Collections.unmodifiableList(path);
	}

	/**
	 * Computes the path the guy would traverse until either reaching the target tile, running into a
	 * cycle or entering a portal.
	 */
	private void updatePath(Tile target) {
		if (target != null) {
			path.clear();
			Direction dir = guy.moveDir;
			Tile next = guy.tile();
			while (!next.equals(target) && world.includes(next) && !path.contains(next)) {
				path.add(next);
				dir = bestDirTowardsTarget(world, guy, dir, next, target);
				next = world.neighbor(next, dir);
			}
		}
	}
}