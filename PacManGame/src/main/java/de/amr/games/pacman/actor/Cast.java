package de.amr.games.pacman.actor;

import static de.amr.games.pacman.actor.GhostState.CHASING;
import static de.amr.games.pacman.actor.GhostState.DEAD;
import static de.amr.games.pacman.actor.GhostState.ENTERING_HOUSE;
import static de.amr.games.pacman.actor.GhostState.FRIGHTENED;
import static de.amr.games.pacman.actor.GhostState.LEAVING_HOUSE;
import static de.amr.games.pacman.actor.GhostState.LOCKED;
import static de.amr.games.pacman.actor.GhostState.SCATTERING;
import static de.amr.games.pacman.model.Direction.DOWN;
import static de.amr.games.pacman.model.Direction.LEFT;
import static de.amr.games.pacman.model.Direction.UP;
import static de.amr.games.pacman.model.Timing.sec;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_UP;

import java.util.stream.Stream;

import de.amr.games.pacman.actor.core.MovingActor;
import de.amr.games.pacman.model.Game;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.Tile;

/**
 * The cast (set of actors) of the Pac-Man game.
 * 
 * @author Armin Reichert
 */
public class Cast {

	private final Game game;
	public final PacMan pacMan;
	public final Ghost blinky, pinky, inky, clyde;
	public final Bonus bonus;

	public Cast(Game game) {
		this.game = game;
		pacMan = new PacMan(this);
		blinky = new Ghost(this, "Blinky");
		inky = new Ghost(this, "Inky");
		pinky = new Ghost(this, "Pinky");
		clyde = new Ghost(this, "Clyde");
		bonus = new Bonus(this);
		configureActors(game.maze());
	}

	private void configureActors(Maze maze) {
		pacMan.behavior(pacMan.isFollowingKeys(VK_UP, VK_RIGHT, VK_DOWN, VK_LEFT));
		pacMan.setTeleportingDuration(sec(0.5f));

		blinky.assignSeat(0, LEFT);
		blinky.setTeleportingDuration(sec(0.5f));
		blinky.behavior(LOCKED, blinky.isHeadingFor(blinky::tile));
		blinky.behavior(ENTERING_HOUSE, blinky.isTakingSeat(maze.seatPosition(2)));
		blinky.behavior(LEAVING_HOUSE, blinky.isLeavingGhostHouse());
		blinky.behavior(FRIGHTENED, blinky.isMovingRandomlyWithoutTurningBack());
		blinky.behavior(SCATTERING, blinky.isHeadingFor(maze.horizonNE));
		blinky.behavior(CHASING, blinky.isHeadingFor(pacMan::tile));
		blinky.behavior(DEAD, blinky.isHeadingFor(() -> maze.ghostHouseSeats[0]));

		inky.assignSeat(1, UP);
		inky.setTeleportingDuration(sec(0.5f));
		inky.behavior(LOCKED, inky.isJumpingUpAndDown(maze.seatPosition(1)));
		inky.behavior(ENTERING_HOUSE, inky.isTakingSeat(maze.seatPosition(1)));
		inky.behavior(LEAVING_HOUSE, inky.isLeavingGhostHouse());
		inky.behavior(FRIGHTENED, inky.isMovingRandomlyWithoutTurningBack());
		inky.behavior(SCATTERING, inky.isHeadingFor(maze.horizonSE));
		inky.behavior(CHASING, inky.isHeadingFor(() -> {
			Tile b = blinky.tile(), p = pacMan.tilesAhead(2);
			return maze.tileAt(2 * p.col - b.col, 2 * p.row - b.row);
		}));
		inky.behavior(DEAD, inky.isHeadingFor(() -> maze.ghostHouseSeats[0]));

		pinky.assignSeat(2, DOWN);
		pinky.setTeleportingDuration(sec(0.5f));
		pinky.behavior(LOCKED, pinky.isJumpingUpAndDown(maze.seatPosition(2)));
		pinky.behavior(ENTERING_HOUSE, pinky.isTakingSeat(maze.seatPosition(2)));
		pinky.behavior(LEAVING_HOUSE, pinky.isLeavingGhostHouse());
		pinky.behavior(FRIGHTENED, pinky.isMovingRandomlyWithoutTurningBack());
		pinky.behavior(SCATTERING, pinky.isHeadingFor(maze.horizonNW));
		pinky.behavior(CHASING, pinky.isHeadingFor(() -> pacMan.tilesAhead(4)));
		pinky.behavior(DEAD, pinky.isHeadingFor(() -> maze.ghostHouseSeats[0]));

		clyde.assignSeat(3, UP);
		clyde.setTeleportingDuration(sec(0.5f));
		clyde.behavior(LOCKED, clyde.isJumpingUpAndDown(maze.seatPosition(3)));
		clyde.behavior(ENTERING_HOUSE, clyde.isTakingSeat(maze.seatPosition(3)));
		clyde.behavior(LEAVING_HOUSE, clyde.isLeavingGhostHouse());
		clyde.behavior(FRIGHTENED, clyde.isMovingRandomlyWithoutTurningBack());
		clyde.behavior(SCATTERING, clyde.isHeadingFor(maze.horizonSW));
		clyde.behavior(CHASING,
				clyde.isHeadingFor(() -> clyde.tile().distSq(pacMan.tile()) > 8 * 8 ? pacMan.tile() : maze.horizonSW));
		clyde.behavior(DEAD, clyde.isHeadingFor(() -> maze.ghostHouseSeats[0]));
	}

	public Game game() {
		return game;
	}

	public Stream<Ghost> ghosts() {
		return Stream.of(blinky, pinky, inky, clyde);
	}

	public Stream<Ghost> ghostsOnStage() {
		return ghosts().filter(Ghost::isActing);
	}

	public Stream<MovingActor<?>> movingActors() {
		return Stream.of(pacMan, blinky, pinky, inky, clyde);
	}

	public Stream<MovingActor<?>> movingActorsOnStage() {
		return movingActors().filter(MovingActor::isActing);
	}
}