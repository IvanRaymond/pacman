package de.amr.games.pacman.actor;

import static de.amr.games.pacman.actor.GhostName.Blinky;
import static de.amr.games.pacman.actor.GhostName.Clyde;
import static de.amr.games.pacman.actor.GhostName.Inky;
import static de.amr.games.pacman.actor.GhostName.Pinky;
import static de.amr.games.pacman.actor.GhostState.AGGRO;
import static de.amr.games.pacman.actor.GhostState.DEAD;
import static de.amr.games.pacman.actor.GhostState.FRIGHTENED;
import static de.amr.games.pacman.actor.GhostState.SAFE;
import static de.amr.games.pacman.actor.GhostState.SCATTERING;
import static de.amr.games.pacman.navigation.NavigationSystem.ambush;
import static de.amr.games.pacman.navigation.NavigationSystem.bounce;
import static de.amr.games.pacman.navigation.NavigationSystem.chase;
import static de.amr.games.pacman.navigation.NavigationSystem.clydeChaseBehavior;
import static de.amr.games.pacman.navigation.NavigationSystem.flee;
import static de.amr.games.pacman.navigation.NavigationSystem.followKeyboard;
import static de.amr.games.pacman.navigation.NavigationSystem.followPath;
import static de.amr.games.pacman.navigation.NavigationSystem.followTargetTile;
import static de.amr.games.pacman.navigation.NavigationSystem.inkyChaseBehavior;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_UP;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import de.amr.easy.grid.impl.Top4;
import de.amr.games.pacman.model.BonusSymbol;
import de.amr.games.pacman.model.Game;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.navigation.Navigation;
import de.amr.games.pacman.view.PacManSprites.GhostColor;

/**
 * Factory and container for the game actors.
 * 
 * @author Armin Reichert
 */
public class Cast implements PacManWorld {

	private final PacMan pacMan;
	private final Ghost blinky, pinky, inky, clyde;
	private final Set<Ghost> activeGhosts = new HashSet<>(4);
	private Bonus bonus;

	public Cast(Game game) {
		Maze maze = game.getMaze();
		pacMan = new PacMan(game, this);
		blinky = new Ghost(Blinky, pacMan, game, maze.getBlinkyHome(), maze.getBlinkyScatteringTarget(),
				Top4.E, GhostColor.RED);
		pinky = new Ghost(Pinky, pacMan, game, maze.getPinkyHome(), maze.getPinkyScatteringTarget(),
				Top4.S, GhostColor.PINK);
		inky = new Ghost(Inky, pacMan, game, maze.getInkyHome(), maze.getInkyScatteringTarget(), Top4.N,
				GhostColor.TURQUOISE);
		clyde = new Ghost(Clyde, pacMan, game, maze.getClydeHome(), maze.getClydeScatteringTarget(),
				Top4.N, GhostColor.ORANGE);
		activeGhosts.addAll(Arrays.asList(blinky, pinky, inky, clyde));

		// configure actor behavior
		Navigation keySteering = followKeyboard(VK_UP, VK_RIGHT, VK_DOWN, VK_LEFT);
		pacMan.setNavigation(PacManState.HUNGRY, keySteering);
		pacMan.setNavigation(PacManState.GREEDY, keySteering);

		// common ghost behavior
		Stream.of(blinky, pinky, inky, clyde).forEach(ghost -> {
			ghost.setNavigation(FRIGHTENED, flee(pacMan));
			ghost.setNavigation(SCATTERING, followTargetTile(() -> ghost.getScatteringTarget()));
			ghost.setNavigation(DEAD, followPath(ghost.getHome()));
			ghost.setNavigation(SAFE, bounce());
		});

		// individual ghost behavior
		blinky.setNavigation(AGGRO, chase(pacMan));
		pinky.setNavigation(AGGRO, ambush(pacMan));
		inky.setNavigation(AGGRO, inkyChaseBehavior(blinky, pacMan));
		clyde.setNavigation(AGGRO, clydeChaseBehavior(clyde, pacMan));
		clyde.fnCanLeaveHouse = () -> game.getLevel() > 1
				|| game.getFoodRemaining() < (66 * maze.getFoodTotal() / 100);
	}

	public void init() {
		pacMan.init();
		activeGhosts.forEach(Ghost::init);
		removeBonus();
	}

	@Override
	public Ghost getBlinky() {
		return blinky;
	}

	@Override
	public Ghost getPinky() {
		return pinky;
	}

	@Override
	public Ghost getInky() {
		return inky;
	}

	@Override
	public Ghost getClyde() {
		return clyde;
	}

	@Override
	public PacMan getPacMan() {
		return pacMan;
	}

	public boolean isActive(Ghost ghost) {
		return activeGhosts.contains(ghost);
	}

	public void setActive(Ghost ghost, boolean active) {
		if (active == isActive(ghost)) {
			return;
		}
		if (active) {
			activeGhosts.add(ghost);
			ghost.init();
		} else {
			activeGhosts.remove(ghost);
		}
	}

	@Override
	public Stream<Ghost> getActiveGhosts() {
		return activeGhosts.stream();
	}

	public Stream<Ghost> getGhosts() {
		return Stream.of(blinky, pinky, inky, clyde);
	}

	public void addBonus(BonusSymbol symbol, int value) {
		bonus = new Bonus(symbol, value);
	}

	public void removeBonus() {
		bonus = null;
	}

	@Override
	public Optional<Bonus> getBonus() {
		return Optional.ofNullable(bonus);
	}
}