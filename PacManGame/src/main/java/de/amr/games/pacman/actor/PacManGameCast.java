package de.amr.games.pacman.actor;

import static de.amr.games.pacman.actor.GhostState.CHASING;
import static de.amr.games.pacman.actor.GhostState.ENTERING_HOUSE;
import static de.amr.games.pacman.actor.GhostState.FRIGHTENED;
import static de.amr.games.pacman.actor.GhostState.LEAVING_HOUSE;
import static de.amr.games.pacman.actor.GhostState.LOCKED;
import static de.amr.games.pacman.actor.GhostState.SCATTERING;
import static de.amr.games.pacman.actor.behavior.Steerings.followsKeys;
import static de.amr.games.pacman.actor.behavior.Steerings.isHeadingFor;
import static de.amr.games.pacman.actor.behavior.Steerings.isJumpingUpAndDown;
import static de.amr.games.pacman.actor.behavior.Steerings.isLeavingGhostHouse;
import static de.amr.games.pacman.actor.behavior.Steerings.isMovingRandomlyWithoutTurningBack;
import static de.amr.games.pacman.actor.behavior.Steerings.isTakingSeat;
import static de.amr.games.pacman.model.Direction.DOWN;
import static de.amr.games.pacman.model.Direction.LEFT;
import static de.amr.games.pacman.model.Direction.UP;
import static de.amr.games.pacman.model.Direction.dirs;
import static de.amr.games.pacman.model.Tile.distanceSq;
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

import de.amr.games.pacman.actor.core.PacManGameActor;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.PacManGame;
import de.amr.games.pacman.model.Tile;
import de.amr.games.pacman.theme.GhostColor;
import de.amr.games.pacman.theme.PacManTheme;

/**
 * The cast (set of actors) in the PacMan game.
 * 
 * @author Armin Reichert
 */
public class PacManGameCast {

	private final PacManGame game;
	public final PacMan pacMan;
	public final Ghost blinky, pinky, inky, clyde;

	private PacManTheme theme;
	private Bonus bonus;
	private final Set<PacManGameActor<?>> actorsOnStage = new HashSet<>();
	private final PropertyChangeSupport changes = new PropertyChangeSupport(this);

	public PacManGameCast(PacManGame game, PacManTheme theme) {
		this.game = game;

		pacMan = new PacMan(this);
		blinky = new Ghost("Blinky", this);
		pinky = new Ghost("Pinky", this);
		inky = new Ghost("Inky", this);
		clyde = new Ghost("Clyde", this);

		// initially, all actors are off-stage
		actors().forEach(actor -> setActorOffStage(actor));

		// configure the actors

		setTheme(theme);

		pacMan.steering(followsKeys(VK_UP, VK_RIGHT, VK_DOWN, VK_LEFT));
		pacMan.setTeleportingDuration(sec(0.5f));

		blinky.eyes = LEFT;
		blinky.seat = 0;
		blinky.during(SCATTERING, isHeadingFor(maze().horizonNE));
		blinky.during(CHASING, isHeadingFor(pacMan::tile));

		inky.eyes = UP;
		inky.seat = 1;
		inky.during(SCATTERING, isHeadingFor(maze().horizonSE));
		inky.during(CHASING, isHeadingFor(() -> {
			Tile b = blinky.tile(), p = pacMan.tilesAhead(2);
			return maze().tileAt(2 * p.col - b.col, 2 * p.row - b.row);
		}));

		pinky.eyes = DOWN;
		pinky.seat = 2;
		pinky.during(SCATTERING, isHeadingFor(maze().horizonNW));
		pinky.during(CHASING, isHeadingFor(() -> pacMan.tilesAhead(4)));

		clyde.eyes = UP;
		clyde.seat = 3;
		clyde.during(SCATTERING, isHeadingFor(maze().horizonSW));
		clyde.during(CHASING,
				isHeadingFor(() -> distanceSq(clyde.tile(), pacMan.tile()) > 8 * 8 ? pacMan.tile() : maze().horizonSW));

		ghosts().forEach(ghost -> {
			ghost.setTeleportingDuration(sec(0.5f));
			ghost.during(LEAVING_HOUSE, isLeavingGhostHouse(maze()));
			ghost.during(FRIGHTENED, isMovingRandomlyWithoutTurningBack());
			if (ghost != blinky) {
				ghost.during(LOCKED, isJumpingUpAndDown(maze(), ghost.seat));
				ghost.during(ENTERING_HOUSE, isTakingSeat(ghost, ghost.seat));
			} else {
				ghost.during(ENTERING_HOUSE, isTakingSeat(ghost, 2));
			}
		});
	}

	public PacManGame game() {
		return game;
	}

	public Maze maze() {
		return game.maze();
	}

	public PacManTheme theme() {
		return theme;
	}

	public void setTheme(PacManTheme newTheme) {
		PacManTheme oldTheme = this.theme;
		this.theme = newTheme;
		clotheActors();
		changes.firePropertyChange("theme", oldTheme, newTheme);
	}

	public void addThemeListener(PropertyChangeListener subscriber) {
		changes.addPropertyChangeListener("theme", subscriber);
	}

	private void clotheActors() {
		clothePacMan();
		clotheGhosts(blinky, GhostColor.RED);
		clotheGhosts(pinky, GhostColor.PINK);
		clotheGhosts(inky, GhostColor.CYAN);
		clotheGhosts(clyde, GhostColor.ORANGE);
	}

	private void clothePacMan() {
		dirs().forEach(dir -> pacMan.sprites.set("walking-" + dir, theme.spr_pacManWalking(dir.ordinal())));
		pacMan.sprites.set("dying", theme.spr_pacManDying());
		pacMan.sprites.set("full", theme.spr_pacManFull());
		pacMan.sprites.select("full");
	}

	private void clotheGhosts(Ghost ghost, GhostColor color) {
		dirs().forEach(dir -> {
			ghost.sprites.set("color-" + dir, theme.spr_ghostColored(color, dir.ordinal()));
			ghost.sprites.set("eyes-" + dir, theme.spr_ghostEyes(dir.ordinal()));
		});
		// sprite keys: "value-1", ..., "value-4"
		for (int i = 1; i <= 4; ++i) {
			ghost.sprites.set("value-" + i, theme.spr_greenNumber(i - 1));
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

	public Stream<PacManGameActor<?>> actors() {
		return Stream.of(pacMan, blinky, pinky, inky, clyde);
	}

	public Stream<PacManGameActor<?>> actorsOnStage() {
		return actors().filter(this::onStage);
	}

	public boolean onStage(PacManGameActor<?> actor) {
		return actorsOnStage.contains(actor);
	}

	public void setActorOnStage(PacManGameActor<?> actor) {
		actor.init();
		actor.show();
		actorsOnStage.add(actor);
	}

	public void setActorOffStage(PacManGameActor<?> actor) {
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