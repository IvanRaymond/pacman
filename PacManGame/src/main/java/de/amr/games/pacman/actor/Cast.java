package de.amr.games.pacman.actor;

import static de.amr.games.pacman.actor.GhostState.CHASING;
import static de.amr.games.pacman.actor.GhostState.ENTERING_HOUSE;
import static de.amr.games.pacman.actor.GhostState.FRIGHTENED;
import static de.amr.games.pacman.actor.GhostState.LEAVING_HOUSE;
import static de.amr.games.pacman.actor.GhostState.LOCKED;
import static de.amr.games.pacman.actor.GhostState.SCATTERING;
import static de.amr.games.pacman.model.Direction.DOWN;
import static de.amr.games.pacman.model.Direction.LEFT;
import static de.amr.games.pacman.model.Direction.UP;
import static de.amr.games.pacman.model.Direction.dirs;
import static de.amr.games.pacman.model.Timing.sec;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_UP;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import de.amr.games.pacman.actor.core.Actor;
import de.amr.games.pacman.model.Game;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.Tile;
import de.amr.games.pacman.theme.GhostColor;
import de.amr.games.pacman.theme.Theme;

/**
 * The cast (set of actors) of the Pac-Man game.
 * 
 * @author Armin Reichert
 */
public class Cast {

	public final PacMan pacMan;
	public final Ghost blinky, pinky, inky, clyde;

	private final Game game;
	private Theme theme;
	private Bonus bonus;
	private final Set<Actor<?>> actorsOnStage = new HashSet<>();
	private final PropertyChangeSupport changes = new PropertyChangeSupport(this);

	public Cast(Game game, Theme theme) {
		this.game = game;

		pacMan = new PacMan(this);

		blinky = new Ghost(this, "Blinky", 0, LEFT);
		inky = new Ghost(this, "Inky", 1, UP);
		pinky = new Ghost(this, "Pinky", 2, DOWN);
		clyde = new Ghost(this, "Clyde", 3, UP);

		// initially, all actors are off-stage
		actors().forEach(actor -> setActorOffStage(actor));

		// configure the actors

		setTheme(theme);

		pacMan.steering(pacMan.isFollowingKeys(VK_UP, VK_RIGHT, VK_DOWN, VK_LEFT));
		pacMan.steering(pacMan.isMovingRandomlyWithoutTurningBack()); //TODO remove me
		pacMan.setTeleportingDuration(sec(0.5f));

		blinky.during(SCATTERING, blinky.isHeadingFor(maze().horizonNE));
		blinky.during(CHASING, blinky.isHeadingFor(pacMan::tile));
		blinky.during(ENTERING_HOUSE, blinky.isTakingSeat(2));

		inky.during(SCATTERING, inky.isHeadingFor(maze().horizonSE));
		inky.during(CHASING, inky.isHeadingFor(() -> {
			Tile b = blinky.tile(), p = pacMan.tilesAhead(2);
			return maze().tileAt(2 * p.col - b.col, 2 * p.row - b.row);
		}));
		inky.during(LOCKED, inky.isJumpingUpAndDown());
		inky.during(ENTERING_HOUSE, inky.isTakingOwnSeat());

		pinky.during(SCATTERING, pinky.isHeadingFor(maze().horizonNW));
		pinky.during(CHASING, pinky.isHeadingFor(() -> pacMan.tilesAhead(4)));
		pinky.during(LOCKED, pinky.isJumpingUpAndDown());
		pinky.during(ENTERING_HOUSE, pinky.isTakingOwnSeat());

		clyde.during(SCATTERING, clyde.isHeadingFor(maze().horizonSW));
		clyde.during(CHASING, clyde
				.isHeadingFor(() -> Tile.distanceSq(clyde.tile(), pacMan.tile()) > 8 * 8 ? pacMan.tile() : maze().horizonSW));
		clyde.during(LOCKED, clyde.isJumpingUpAndDown());
		clyde.during(ENTERING_HOUSE, clyde.isTakingOwnSeat());

		ghosts().forEach(ghost -> {
			ghost.setTeleportingDuration(sec(0.5f));
			ghost.during(LEAVING_HOUSE, ghost.isLeavingGhostHouse());
			ghost.during(FRIGHTENED, ghost.isMovingRandomlyWithoutTurningBack());
		});
	}

	public Game game() {
		return game;
	}

	public Maze maze() {
		return game.maze();
	}

	public Theme theme() {
		return theme;
	}

	public void setTheme(Theme newTheme) {
		Theme oldTheme = this.theme;
		this.theme = newTheme;
		clotheActors();
		changes.firePropertyChange("theme", oldTheme, newTheme);
	}

	public void addThemeListener(PropertyChangeListener subscriber) {
		changes.addPropertyChangeListener("theme", subscriber);
	}

	private void clotheActors() {
		clothePacMan();
		clotheGhost(blinky, GhostColor.RED);
		clotheGhost(pinky, GhostColor.PINK);
		clotheGhost(inky, GhostColor.CYAN);
		clotheGhost(clyde, GhostColor.ORANGE);
	}

	private void clothePacMan() {
		dirs().forEach(dir -> pacMan.sprites.set("walking-" + dir, theme.spr_pacManWalking(dir.ordinal())));
		pacMan.sprites.set("dying", theme.spr_pacManDying());
		pacMan.sprites.set("full", theme.spr_pacManFull());
	}

	private void clotheGhost(Ghost ghost, GhostColor color) {
		dirs().forEach(dir -> {
			ghost.sprites.set("color-" + dir, theme.spr_ghostColored(color, dir.ordinal()));
			ghost.sprites.set("eyes-" + dir, theme.spr_ghostEyes(dir.ordinal()));
		});
		for (int number : new int[] { 200, 400, 800, 1600 }) {
			ghost.sprites.set("number-" + number, theme.spr_number(number));
		}
		ghost.sprites.set("frightened", theme.spr_ghostFrightened());
		ghost.sprites.set("flashing", theme.spr_ghostFlashing());
	}

	public Stream<Ghost> ghosts() {
		return Stream.of(blinky, pinky, inky, clyde);
	}

	public Stream<Ghost> ghostsOnStage() {
		return ghosts().filter(this::onStage);
	}

	public Stream<Actor<?>> actors() {
		return Stream.of(pacMan, blinky, pinky, inky, clyde);
	}

	public Stream<Actor<?>> actorsOnStage() {
		return actors().filter(this::onStage);
	}

	public boolean onStage(Actor<?> actor) {
		return actorsOnStage.contains(actor);
	}

	public void setActorOnStage(Actor<?> actor) {
		actor.init();
		actor.show();
		actorsOnStage.add(actor);
	}

	public void setActorOffStage(Actor<?> actor) {
		actor.hide();
		actorsOnStage.remove(actor);
	}

	public Optional<Bonus> bonus() {
		return Optional.ofNullable(bonus);
	}

	public void addBonus() {
		bonus = new Bonus(this);
		bonus.init();
	}

	public void removeBonus() {
		bonus = null;
	}
}