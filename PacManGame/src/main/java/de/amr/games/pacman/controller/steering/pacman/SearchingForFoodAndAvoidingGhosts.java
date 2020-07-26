package de.amr.games.pacman.controller.steering.pacman;

import static de.amr.games.pacman.controller.creatures.ghost.GhostState.CHASING;
import static de.amr.games.pacman.controller.creatures.ghost.GhostState.SCATTERING;
import static java.util.Comparator.comparingInt;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import de.amr.games.pacman.controller.creatures.Folks;
import de.amr.games.pacman.controller.creatures.ghost.Ghost;
import de.amr.games.pacman.controller.creatures.pacman.PacMan;
import de.amr.games.pacman.controller.steering.api.PathProvidingSteering;
import de.amr.games.pacman.model.world.api.Direction;
import de.amr.games.pacman.model.world.api.MobileLifeform;
import de.amr.games.pacman.model.world.api.Tile;
import de.amr.games.pacman.model.world.components.BonusState;
import de.amr.games.pacman.model.world.core.WorldGraph;
import de.amr.games.pacman.model.world.core.WorldGraph.PathFinder;

/**
 * Steering used by PacMan in demo mode.
 * 
 * @author Armin Reichert
 */
public class SearchingForFoodAndAvoidingGhosts implements PathProvidingSteering<PacMan> {

	private final Folks folks;
	private final PacMan me;
	private WorldGraph graph;
	private double distance;
	private Direction newDir;
	private Tile targetTile;

	public SearchingForFoodAndAvoidingGhosts(Folks folks) {
		this.folks = folks;
		me = folks.pacMan;
		graph = new WorldGraph(folks.world);
		graph.setPathFinder(PathFinder.ASTAR);
	}

	@Override
	public boolean requiresGridAlignment() {
		return true;
	}

	@Override
	public boolean isPathComputationEnabled() {
		return targetTile != null;
	}

	@Override
	public void setPathComputationEnabled(boolean enabled) {
	}

	@Override
	public void steer(PacMan pacMan) {
		if (!pacMan.enteredNewTile() && pacMan.canCrossBorderTo(pacMan.moveDir()) || pacMan.isTeleporting()) {
			return;
		}

		// is dangerous ghost just in front of pacMan and is moving in the same direction?
		Ghost enemy = dangerousGhostInRange(1).filter(ghost -> pacMan.moveDir() == ghost.moveDir()).orElse(null);
		if (enemy != null) {
			pacMan.reverseDirection();
			return;
		}

		// is dangerous ghost coming directly towards pacMan?
		enemy = dangerousGhostInRange(3).filter(ghost -> pacMan.moveDir() == ghost.moveDir().opposite()).orElse(null);
		if (enemy != null) {
			pacMan.reverseDirection();
			return;
		}

		// determine direction for finding food
		targetTile = null;
		distance = Integer.MAX_VALUE;
		//@formatter:off
		aheadThenRightThenLeft()
			.filter(pacMan::canCrossBorderTo)
			.forEach(dir -> {
				Tile neighbor = pacMan.neighbor(dir);
				preferredFoodLocationFrom(neighbor).ifPresent(foodLocation -> {
					double d = neighbor.distance(foodLocation);
					if (d < distance) {
						newDir = dir;
						distance = d;
						targetTile = foodLocation;
					}
				});
			});
		//@formatter:on
		if (newDir != pacMan.moveDir().opposite()) {
			pacMan.setWishDir(newDir);
			return;
		}
	}

	@Override
	public List<Tile> pathToTarget(MobileLifeform pacMan) {
		return targetTile != null ? graph.shortestPath(pacMan.tileLocation(), targetTile) : Collections.emptyList();
	}

	private Stream<Ghost> dangerousGhosts() {
		return folks.ghostsInWorld().filter(ghost -> ghost.is(CHASING, SCATTERING));
	}

	private Optional<Ghost> dangerousGhostInRange(int numTiles) {
		//@formatter:off
		return dangerousGhosts()
			.filter(ghost -> shortestPathLength(ghost.tileLocation(), me.tileLocation()) <= numTiles)
			.findAny();
		//@formatter:on
	}

	private Stream<Direction> aheadThenRightThenLeft() {
		return Stream.of(me.moveDir(), me.moveDir().right(), me.moveDir().left());
	}

//	private int distance(Stream<Ghost> ghosts) {
//		return ghosts.mapToInt(ghost -> distance(pacMan.tile(), ghost.tile())).min().orElse(Integer.MAX_VALUE);
//	}

	private int shortestPathLength(Tile from, Tile to) {
		return graph.shortestPath(from, to).size();
	}

	private Stream<Tile> foodTiles() {
		return me.world().habitat().filter(me.world()::containsFood);
	}

	private Optional<Tile> preferredFoodLocationFrom(Tile here) {
		//@formatter:off
		double nearestEnemyDist = nearestDistanceToDangerousGhost(here);
		if (nearestEnemyDist == Double.MAX_VALUE) {
			return activeBonusAtMostAway(here, 30)
					.or(() -> nearestFoodFrom(here));
		} else {
			return activeBonusAtMostAway(here, 10)
					.or(() -> energizerAtMostAway(here, (int)nearestEnemyDist))
					.or(() -> nearestFoodFrom(here));
		}
		//@formatter:on
	}

	private double nearestDistanceToDangerousGhost(Tile here) {
		return dangerousGhosts().map(ghost -> here.distance(ghost.tileLocation())).min(Double::compareTo)
				.orElse(Double.MAX_VALUE);
	}

	private Optional<Tile> activeBonusAtMostAway(Tile here, int maxDistance) {
		//@formatter:off
		return me.world().getBonus()
			.filter(bonus -> bonus.state == BonusState.ACTIVE)
			.filter(bonus -> here.manhattanDistance(bonus.location) <= maxDistance)
			.map(bonus -> bonus.location);
		//@formatter:on
	}

	private Optional<Tile> energizerAtMostAway(Tile here, int distance) {
		//@formatter:off
		return foodTiles()
				.filter(me.world()::containsEnergizer)
				.filter(energizer -> here.manhattanDistance(energizer) <= distance)
				.findFirst();
		//@formatter:on
	}

	private Optional<Tile> nearestFoodFrom(Tile here) {
		//@formatter:off
		return foodTiles()
				.sorted(comparingInt(food -> here.manhattanDistance(food)))
				.findFirst();
		//@formatter:on
	}

}