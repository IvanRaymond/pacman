package de.amr.games.pacman.controller;

import static de.amr.easy.game.Application.loginfo;
import static de.amr.games.pacman.actor.GhostState.LOCKED;
import static de.amr.games.pacman.model.Timing.sec;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import de.amr.games.pacman.actor.Cast;
import de.amr.games.pacman.actor.Ghost;
import de.amr.games.pacman.controller.event.FoodFoundEvent;
import de.amr.games.pacman.controller.event.GhostUnlockedEvent;
import de.amr.games.pacman.model.Game;

/**
 * This class controls when and in which order locked ghosts can leave the ghost house.
 * 
 * @author Armin Reichert
 * 
 * @see <a href=
 *      "https://www.gamasutra.com/view/feature/132330/the_pacman_dossier.php?page=4">Gamasutra</a>
 */
public class House {

	private static class DotCounter {

		int dots;
		boolean enabled;
	}

	private final Cast cast;
	private final DotCounter globalCounter;
	private final int[] ghostDotCount;
	private int pacManStarvingTicks;

	public House(Cast cast) {
		this.cast = cast;
		globalCounter = new DotCounter();
		ghostDotCount = new int[4];
	}

	public void init() {
		globalCounter.enabled = false;
		globalCounter.dots = 0;
		resetGhostDotCount();
	}

	public void update() {
		if (cast.blinky.is(LOCKED)) {
			unlock(cast.blinky);
		}
		preferredLockedGhost().filter(this::canLeaveHome).ifPresent(ghost -> unlock(ghost));
		pacManStarvingTicks += 1;
	}

	public void onPacManFoundFood(FoodFoundEvent e) {
		pacManStarvingTicks = 0;
		if (globalCounter.enabled) {
			globalCounter.dots++;
			if (globalCounter.dots == 32 && cast.clyde.is(LOCKED)) {
				globalCounter.dots = 0;
				globalCounter.enabled = false;
				loginfo("Global dot counter reset and disabled (Clyde was locked when counter reached 32)");
			}
		} else {
			preferredLockedGhost().ifPresent(ghost -> {
				ghostDotCount[cast.seat(ghost)] += 1;
			});
		}
	}

	public void onLevelChange() {
		resetGhostDotCount();
	}

	public void onLifeLost() {
		globalCounter.enabled = true;
		globalCounter.dots = 0;
		loginfo("Global dot counter enabled and set to zero");
	}

	public boolean isPreferredGhost(Ghost ghost) {
		return preferredLockedGhost().map(next -> next == ghost).orElse(false);
	}

	public int globalDotCount() {
		return globalCounter.dots;
	}

	public boolean isGlobalDotCounterEnabled() {
		return globalCounter.enabled;
	}

	public int ghostDotCount(Ghost ghost) {
		return ghostDotCount[cast.seat(ghost)];
	}

	public int pacManStarvingTicks() {
		return pacManStarvingTicks;
	}

	private Game game() {
		return cast.game();
	}

	private Optional<Ghost> preferredLockedGhost() {
		return Stream.of(cast.pinky, cast.inky, cast.clyde).filter(ghost -> ghost.is(LOCKED)).findFirst();
	}

	private void unlock(Ghost ghost) {
		ghost.process(new GhostUnlockedEvent());
	}

	/**
	 * Determines if the given ghost can leave the ghost house.
	 * 
	 * @param ghost
	 *                a ghost
	 * 
	 * @see <a href=
	 *      "http://www.gamasutra.com/view/feature/132330/the_pacman_dossier.php?page=4">Pac-Man
	 *      Dossier</a>
	 */
	private boolean canLeaveHome(Ghost ghost) {
		int pacManStarvingTimeLimit = game().level().number < 5 ? sec(4) : sec(3);
		if (pacManStarvingTicks >= pacManStarvingTimeLimit) {
			loginfo("%s can leave house: Pac-Man's starving time limit (%d ticks) reached", ghost.name(),
					pacManStarvingTimeLimit);
			pacManStarvingTicks = 0;
			return true;
		}
		if (globalCounter.enabled) {
			int globalLimit = globalDotLimit(ghost);
			if (globalCounter.dots >= globalLimit) {
				loginfo("%s can leave house: global dot limit (%d) reached", ghost.name(), globalLimit);
				return true;
			}
		} else {
			int personalLimit = personalDotLimit(ghost);
			if (ghostDotCount[cast.seat(ghost)] >= personalLimit) {
				loginfo("%s can leave house: ghost's dot limit (%d) reached", ghost.name(), personalLimit);
				return true;
			}
		}
		return false;
	}

	private int personalDotLimit(Ghost ghost) {
		if (ghost == cast.pinky) {
			return 0;
		}
		if (ghost == cast.inky) {
			return game().level().number == 1 ? 30 : 0;
		}
		if (ghost == cast.clyde) {
			return game().level().number == 1 ? 60 : game().level().number == 2 ? 50 : 0;
		}
		throw new IllegalArgumentException("Ghost must be either Pinky, Inky or Clyde");
	}

	private int globalDotLimit(Ghost ghost) {
		if (ghost == cast.pinky) {
			return 7;
		}
		if (ghost == cast.inky) {
			return 17;
		}
		if (ghost == cast.clyde) {
			return 32;
		}
		throw new IllegalArgumentException("Ghost must be either Pinky, Inky or Clyde");
	}

	private void resetGhostDotCount() {
		Arrays.fill(ghostDotCount, 0);
		loginfo("Ghost dot counters reset to zero");
	}
}