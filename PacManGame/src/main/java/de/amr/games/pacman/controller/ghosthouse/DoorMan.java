package de.amr.games.pacman.controller.ghosthouse;

import static de.amr.easy.game.Application.loginfo;
import static de.amr.games.pacman.controller.creatures.ghost.GhostState.ENTERING_HOUSE;
import static de.amr.games.pacman.controller.creatures.ghost.GhostState.LEAVING_HOUSE;
import static de.amr.games.pacman.controller.creatures.ghost.GhostState.LOCKED;
import static de.amr.games.pacman.controller.ghosthouse.Decision.confirmed;
import static de.amr.games.pacman.controller.ghosthouse.Decision.rejected;
import static de.amr.games.pacman.model.game.Game.sec;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import de.amr.easy.game.controller.Lifecycle;
import de.amr.games.pacman.controller.creatures.ghost.Ghost;
import de.amr.games.pacman.controller.event.GhostUnlockedEvent;
import de.amr.games.pacman.controller.world.arcade.ArcadeWorldFolks;
import de.amr.games.pacman.model.game.Game;
import de.amr.games.pacman.model.world.api.Door;
import de.amr.games.pacman.model.world.api.Door.DoorState;
import de.amr.games.pacman.model.world.api.House;
import de.amr.games.pacman.model.world.api.Tile;
import de.amr.games.pacman.model.world.api.World;

/**
 * This class controls when and in which order locked ghosts can leave the ghost house.
 * 
 * @author Armin Reichert
 * 
 * @see <a href=
 *      "https://www.gamasutra.com/view/feature/132330/the_pacman_dossier.php?page=4">Gamasutra</a>
 */
public class DoorMan implements Lifecycle {

	private final House house;
	private final ArcadeWorldFolks folks;
	private final Game game;
	private final World world;
	private final DotCounter globalCounter;
	private final int[] ghostCounters;
	private int pacManStarvingTicks;

	public DoorMan(House house, Game game, ArcadeWorldFolks folks) {
		this.house = house;
		this.folks = folks;
		this.game = game;
		world = folks.world();
		globalCounter = new DotCounter();
		ghostCounters = new int[4];
	}

	@Override
	public void init() {
		globalCounter.enabled = false;
		globalCounter.dots = 0;
		resetGhostDotCounters();
	}

	@Override
	public void update() {
		preferredLockedGhost().ifPresent(ghost -> {
			Decision decision = makeDecisionAboutReleasing(ghost);
			if (decision.confirmed) {
				loginfo(decision.reason);
				unlock(ghost);
			}
		});
		pacManStarvingTicks += 1;

		house.doors().forEach(this::closeDoor);
		house.doors().filter(this::isOpeningRequested).forEach(this::openDoor);
	}

	public void onPacManFoundFood() {
		pacManStarvingTicks = 0;
		if (globalCounter.enabled) {
			globalCounter.dots++;
			if (globalCounter.dots == 32 && folks.clyde().is(LOCKED)) {
				globalCounter.dots = 0;
				globalCounter.enabled = false;
				loginfo("Global dot counter reset and disabled (Clyde was locked when counter reached 32)");
			}
		} else {
			preferredLockedGhost().ifPresent(ghost -> {
				ghostCounters[number(ghost)] += 1;
			});
		}
	}

	public void onLifeLost() {
		globalCounter.enabled = true;
		globalCounter.dots = 0;
		loginfo("Global dot counter enabled and set to zero");
	}

	public void onLevelChange() {
		resetGhostDotCounters();
	}

	public boolean isPreferredGhost(Ghost ghost) {
		return preferredLockedGhost().orElse(null) == ghost;
	}

	/**
	 * Determines if the given ghost can leave the ghost house.
	 * 
	 * @param ghost a ghost
	 * @return if the ghost can leave
	 */
	public boolean canLeave(Ghost ghost) {
		return makeDecisionAboutReleasing(ghost).confirmed;
	}

	public int globalDotCount() {
		return globalCounter.dots;
	}

	public boolean isGlobalDotCounterEnabled() {
		return globalCounter.enabled;
	}

	public int ghostDotCount(Ghost ghost) {
		return ghostCounters[number(ghost)];
	}

	public int personalDotLimit(Ghost ghost) {
		if (ghost == folks.pinky()) {
			return 0;
		}
		if (ghost == folks.inky()) {
			return game.level.number == 1 ? 30 : 0;
		}
		if (ghost == folks.clyde()) {
			return game.level.number == 1 ? 60 : game.level.number == 2 ? 50 : 0;
		}
		throw new IllegalArgumentException("Ghost must be either Pinky, Inky or Clyde");
	}

	public int globalDotLimit(Ghost ghost) {
		if (ghost == folks.pinky()) {
			return 7;
		}
		if (ghost == folks.inky()) {
			return 17;
		}
		if (ghost == folks.clyde()) {
			return 32;
		}
		throw new IllegalArgumentException("Ghost must be either Pinky, Inky or Clyde");
	}

	public int pacManStarvingTicks() {
		return pacManStarvingTicks;
	}

	public Optional<Ghost> preferredLockedGhost() {
		return Stream.of(folks.blinky(), folks.pinky(), folks.inky(), folks.clyde()).filter(world::contains)
				.filter(ghost -> ghost.is(LOCKED)).findFirst();
	}

	private void closeDoor(Door door) {
		door.state = DoorState.CLOSED;
	}

	private void openDoor(Door door) {
		door.state = DoorState.OPEN;
	}

	private void unlock(Ghost ghost) {
		ghost.process(new GhostUnlockedEvent());
	}

	private boolean isOpeningRequested(Door door) {
		//@formatter:off
		return folks.ghosts().filter(world::contains)
				.filter(ghost -> ghost.is(ENTERING_HOUSE, LEAVING_HOUSE))
				.filter(ghost -> isGhostNearDoor(ghost, door))
				.findAny()
				.isPresent();
		//@formatter:on
	}

	private boolean isGhostNearDoor(Ghost ghost, Door door) {
		Tile fromGhostTowardsHouse = world.neighbor(ghost.tileLocation(), door.intoHouse);
		Tile fromGhostAwayFromHouse = world.neighbor(ghost.tileLocation(), door.intoHouse.opposite());
		return door.includes(ghost.tileLocation()) || door.includes(fromGhostAwayFromHouse)
				|| door.includes(fromGhostTowardsHouse);
	}

	private int pacManStarvingTimeLimit() {
		return game.level.number < 5 ? sec(4) : sec(3);
	}

	/**
	 * @param ghost a ghost
	 * @return decision why ghost can leave
	 * 
	 * @see <a href=
	 *      "http://www.gamasutra.com/view/feature/132330/the_pacman_dossier.php?page=4">Pac-Man
	 *      Dossier</a>
	 */
	private Decision makeDecisionAboutReleasing(Ghost ghost) {
		if (!ghost.is(LOCKED)) {
			return confirmed("Ghost is not locked");
		}
		if (ghost == folks.blinky()) {
			return confirmed("%s can always leave", ghost.name);
		}
		if (pacManStarvingTicks >= pacManStarvingTimeLimit()) {
			pacManStarvingTicks = 0;
			return confirmed("%s can leave house: Pac-Man's starving time limit (%d ticks) reached", ghost.name,
					pacManStarvingTimeLimit());
		}
		if (globalCounter.enabled) {
			int globalLimit = globalDotLimit(ghost);
			if (globalCounter.dots >= globalLimit) {
				return confirmed("%s can leave house: global dot limit (%d) reached", ghost.name, globalLimit);
			}
		} else {
			int personalLimit = personalDotLimit(ghost);
			if (ghostCounters[number(ghost)] >= personalLimit) {
				return confirmed("%s can leave house: ghost's dot limit (%d) reached", ghost.name, personalLimit);
			}
		}
		return rejected("");
	}

	private void resetGhostDotCounters() {
		Arrays.fill(ghostCounters, 0);
		loginfo("Ghost dot counters have been reset to zero");
	}

	private int number(Ghost ghost) {
		if (ghost == folks.blinky()) {
			return 0;
		}
		if (ghost == folks.inky()) {
			return 1;
		}
		if (ghost == folks.pinky()) {
			return 2;
		}
		if (ghost == folks.clyde()) {
			return 2;
		}
		throw new IllegalArgumentException();
	}
}