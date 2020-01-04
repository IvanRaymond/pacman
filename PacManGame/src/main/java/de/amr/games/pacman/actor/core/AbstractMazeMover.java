package de.amr.games.pacman.actor.core;

import static de.amr.games.pacman.model.Direction.RIGHT;

import java.util.Objects;

import de.amr.easy.game.math.Vector2f;
import de.amr.games.pacman.model.Direction;
import de.amr.games.pacman.model.Game;
import de.amr.games.pacman.model.Tile;
import de.amr.statemachine.core.StateMachine;

/**
 * Base class for entities that can move through the maze.
 * 
 * @author Armin Reichert
 */
public abstract class AbstractMazeMover extends AbstractMazeResident implements MazeMover {

	/**
	 * Anonymous inner class implementing teleporting control.
	 * <p>
	 * When an actor (Ghost, Pac-Man) crosses the border of the board in the tunnel, a timer is started and the actor is
	 * placed at the teleportation target and hidden (to avoid triggering events during teleportation). When the timer
	 * ends, the actor is made visible again.
	 */
	private StateMachine<Boolean, Void> teleporting = new StateMachine<Boolean, Void>(Boolean.class) {

		{
			//@formatter:off
			beginStateMachine()
				.description(String.format("[%s teleporter]", name()))
				.initialState(false)
				.states()
				.transitions()
					.when(false).then(true).condition(() -> tf.getX() > exitR() + Tile.SIZE)
						.act(() -> { tf.setX(exitL()); hide(); })
					.when(false).then(true).condition(() -> tf.getX() < exitL())
						.act(() -> { tf.setX(exitR()); hide(); })
					.when(true).then(false).onTimeout()
						.act(() -> show())
			.endStateMachine();
			//@formatter:on
		}

		private int exitL() {
			return maze().portalLeft.col * Tile.SIZE;
		}

		private int exitR() {
			return maze().portalRight.col * Tile.SIZE;
		}
	};

	private Direction moveDir;
	private Direction wishDir;
	private Tile targetTile;
	private boolean enteredNewTile;

	public AbstractMazeMover(String name) {
		super(name);
		teleporting.setLogger(Game.FSM_LOGGER);
	}

	@Override
	public void init() {
		moveDir = wishDir = RIGHT;
		targetTile = null;
		enteredNewTile = true;
		teleporting.init();
	}

	/**
	 * Moves or teleports the actor one step. Handles changing the direction according to the wish direction, moving
	 * around corners without losing alignment,
	 */
	@Override
	public void step() {
		teleporting.update();
		if (isTeleporting()) {
			return; // wait for teleporting state machine timeout
		}
		Tile tileBeforeStep = tile();
		float speed = possibleSpeed(tileBeforeStep, moveDir);
		if (wishDir != null && wishDir != moveDir) {
			float wishDirSpeed = possibleSpeed(tileBeforeStep, wishDir);
			if (wishDirSpeed > 0) {
				boolean turning = (wishDir == moveDir.turnLeft() || wishDir == moveDir.turnRight());
				if (turning && steering().requiresGridAlignment()) {
					tf.setPosition(tileBeforeStep.x(), tileBeforeStep.y());
				}
				moveDir = wishDir;
				speed = wishDirSpeed;
			}
		}
		tf.setVelocity(Vector2f.smul(speed, Vector2f.of(moveDir.dx, moveDir.dy)));
		tf.move();
		enteredNewTile = !tileBeforeStep.equals(tile());
	}

	/**
	 * Computes how many pixels this entity can move towards the given direction without crossing the border to a
	 * forbidden neighbor tile.
	 */
	private float possibleSpeed(Tile currentTile, Direction dir) {
		if (canCrossBorderTo(dir)) {
			return maxSpeed();
		}
		float offsetX = tf.getX() - currentTile.x(), offsetY = tf.getY() - currentTile.y();
		switch (dir) {
		case UP:
			return offsetY;
		case DOWN:
			return -offsetY;
		case LEFT:
			return offsetX;
		case RIGHT:
			return -offsetX;
		default:
			throw new IllegalArgumentException("Illegal move direction: " + dir);
		}
	}

	@Override
	public Direction moveDir() {
		return moveDir;
	}

	@Override
	public void setMoveDir(Direction dir) {
		moveDir = Objects.requireNonNull(dir);
	}

	@Override
	public Direction wishDir() {
		return wishDir;
	}

	@Override
	public void setWishDir(Direction dir) {
		wishDir = dir;
	}

	@Override
	public boolean enteredNewTile() {
		return enteredNewTile;
	}

	@Override
	public Tile targetTile() {
		return targetTile;
	}

	@Override
	public void setTargetTile(Tile tile) {
		targetTile = tile;
	}

	@Override
	public boolean isTeleporting() {
		return teleporting.is(true);
	}

	@Override
	public boolean canCrossBorderTo(Direction dir) {
		Tile currentTile = tile();
		return canMoveBetween(currentTile, maze().tileToDir(currentTile, dir));
	}

	@Override
	public boolean canMoveBetween(Tile tile, Tile neighbor) {
		if (neighbor.isWall()) {
			return false;
		}
		if (neighbor.isTunnel()) {
			return true; // includes tiles outside board used for teleportation!
		}
		return maze().insideBoard(neighbor);
	}

	@Override
	public Tile tilesAhead(int numTiles) {
		if (numTiles < 0) {
			throw new IllegalArgumentException("Number of tiles must be positive but is " + numTiles);
		}
		return maze().tileToDir(tile(), moveDir, numTiles);
	}

	@Override
	public void placeAt(Tile tile, byte xOffset, byte yOffset) {
		super.placeAt(tile, xOffset, yOffset);
		enteredNewTile = !tile.equals(tile());
	}

	/**
	 * Sets the teleporting duration for this actor.
	 * 
	 * @param ticks
	 *                how many ticks the teleporting is running
	 */
	public void setTeleportingDuration(int ticks) {
		teleporting.state(true).setConstantTimer(ticks);
	}

	/**
	 * Turns around and triggers a new steering.
	 */
	public void turnAround() {
		wishDir = moveDir.opposite();
		steering().trigger();
	}
}