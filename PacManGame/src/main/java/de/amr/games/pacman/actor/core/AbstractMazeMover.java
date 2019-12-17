package de.amr.games.pacman.actor.core;

import static de.amr.games.pacman.model.Direction.RIGHT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import de.amr.easy.game.math.Vector2f;
import de.amr.games.pacman.model.Direction;
import de.amr.games.pacman.model.Tile;
import de.amr.statemachine.StateMachine;

/**
 * Base class for maze movers (ghosts, Pac-Man).
 * 
 * @author Armin Reichert
 */
public abstract class AbstractMazeMover extends AbstractMazeResident implements MazeMover {

	/**
	 * When an actor (Ghost, Pac-Man) crosses the border of the board in the tunnel,
	 * a timer is started and the actor is placed at the teleportation target and
	 * hidden (to avoid triggering events during teleportation). When the timer
	 * ends, the actor is made visible again.
	 */
	private class Teleporting extends StateMachine<Boolean, Void> {

		private int exitL() {
			return (maze().tunnelExitLeft.col - 1) * Tile.SIZE;
		}

		private int exitR() {
			return (maze().tunnelExitRight.col + 1) * Tile.SIZE;
		}

		public Teleporting() {
			super(Boolean.class);
			//@formatter:off
			beginStateMachine()
				.description(String.format("[Teleporting %s]", name()))
				.initialState(false)
				.states()
				.transitions()
					.when(false).then(true).condition(() -> tf.getX() > exitR())
						.act(() -> { tf.setX(exitL()); hide(); })
					.when(false).then(true).condition(() -> tf.getX() < exitL())
						.act(() -> { tf.setX(exitR()); hide(); })
					.when(true).then(false).onTimeout()
						.act(() -> show())
			.endStateMachine();
			//@formatter:on
		}
	}

	private Direction moveDir = Direction.RIGHT;
	private Direction nextDir;
	private Tile targetTile;
	protected List<Tile> targetPath;
	public boolean requireTargetPath;
	protected boolean enteredNewTile;
	private Teleporting teleporting = new Teleporting();

	public AbstractMazeMover(String name) {
		super(name);
	}
	
	@Override
	public void init() {
		moveDir = nextDir = RIGHT;
		targetTile = null;
		targetPath = Collections.emptyList();
		enteredNewTile = true;
		teleporting.init();
	}

	/**
	 * Moves or teleports the actor.
	 */
	@Override
	public void walkMaze() {
		teleporting.update();
		if (teleporting.is(false)) {
			moveInsideMaze();
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
	public Direction nextDir() {
		return nextDir;
	}

	@Override
	public void setNextDir(Direction dir) {
		nextDir = dir;
	}

	@Override
	public boolean enteredNewTile() {
		return enteredNewTile;
	}

	@Override
	public void setEnteredNewTile() {
		this.enteredNewTile = true;
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
	public List<Tile> targetPath() {
		return targetPath;
	}

	@Override
	public void setTargetPath(List<Tile> path) {
		path = Objects.requireNonNull(path, "Target path must not be null");
		targetPath = new ArrayList<>(path);
	}

	@Override
	public boolean requireTargetPath() {
		return requireTargetPath;
	}

	@Override
	public boolean canMoveForward() {
		return possibleSpeedTo(moveDir) > 0;
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
	public void placeAtTile(Tile tile, float xOffset, float yOffset) {
		super.placeAtTile(tile, xOffset, yOffset);
		enteredNewTile = !tile.equals(tile());
	}

	public void setTeleportingDuration(int ticks) {
		teleporting.state(true).setConstantTimer(ticks);
	}

	/**
	 * Turns around and triggers a new steering.
	 */
	public void turnAround() {
		nextDir = moveDir = moveDir.opposite();
		enteredNewTile = true;
	}

	protected boolean snapToGrid() {
		return true;
	}

	/**
	 * Movement inside the maze. Handles changing the direction according to the
	 * intended move direction, moving around corners without losing alignment,
	 */
	private void moveInsideMaze() {
		Tile oldTile = tile();
		float speed = possibleSpeedTo(nextDir);
		if (speed > 0) {
			boolean turning = (nextDir == moveDir.turnLeft() || nextDir == moveDir.turnRight());
			if (turning && snapToGrid()) {
				tf.setPosition(oldTile.col * Tile.SIZE, oldTile.row * Tile.SIZE);
			}
			moveDir = nextDir;
		} else {
			speed = possibleSpeedTo(moveDir);
		}
		tf.setVelocity(Vector2f.smul(speed, Vector2f.of(moveDir.dx, moveDir.dy)));
		tf.move();
		enteredNewTile = !oldTile.equals(tile());
	}

	/**
	 * Computes how many pixels this entity can move towards the given direction
	 * without crossing the border to a forbidden neighbor tile.
	 */
	private float possibleSpeedTo(Direction dir) {
		if (canCrossBorderTo(dir)) {
			return maxSpeed();
		}
		switch (dir) {
		case UP:
			return -tile().row * Tile.SIZE + tf.getY();
		case RIGHT:
			return tile().col * Tile.SIZE - tf.getX();
		case DOWN:
			return tile().row * Tile.SIZE - tf.getY();
		case LEFT:
			return -tile().col * Tile.SIZE + tf.getX();
		default:
			throw new IllegalArgumentException("Illegal move direction: " + dir);
		}
	}
}