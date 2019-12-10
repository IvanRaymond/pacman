package de.amr.games.pacman.actor;

import static de.amr.easy.game.Application.LOGGER;
import static de.amr.games.pacman.model.Maze.NESW;
import static de.amr.graph.grid.impl.Grid4Topology.E;
import static de.amr.graph.grid.impl.Grid4Topology.N;
import static de.amr.graph.grid.impl.Grid4Topology.S;
import static de.amr.graph.grid.impl.Grid4Topology.W;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.amr.easy.game.entity.Entity;
import de.amr.easy.game.math.Vector2f;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.Tile;

/**
 * Base class for maze movers (ghosts, Pac-Man).
 * 
 * @author Armin Reichert
 */
public abstract class AbstractMazeMover extends Entity implements MazeMover {

	protected byte moveDir;
	protected byte nextDir;
	protected Tile targetTile;
	protected List<Tile> targetPath;
	protected boolean enteredNewTile;
	protected int teleportingTicks;
	protected int teleportTicksRemaining;

	@Override
	public void init() {
		moveDir = nextDir = E;
		targetTile = null;
		targetPath = Collections.emptyList();
		enteredNewTile = true;
		teleportTicksRemaining = -1;
	}

	@Override
	public int moveDir() {
		return moveDir;
	}

	@Override
	public void setMoveDir(int dir) {
		if (Maze.NESW.isValid(dir)) {
			moveDir = (byte) dir;
		}
		else {
			throw new IllegalArgumentException("Illegal direction value " + dir);
		}
	}

	@Override
	public int nextDir() {
		return nextDir;
	}

	@Override
	public void setNextDir(int dir) {
		if (Maze.NESW.isValid(dir)) {
			nextDir = (byte) dir;
		}
		else {
			throw new IllegalArgumentException("Illegal direction value " + dir);
		}
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
	public List<Tile> targetPath() {
		return targetPath;
	}

	@Override
	public void setTargetPath(List<Tile> path) {
		targetPath = new ArrayList<>(path);
	}

	/**
	 * @return <code>true</code> if the maze mover cannot move further towards its current direction
	 */
	@Override
	public boolean isStuck() {
		return tf.getVelocity().length() == 0;
	}

	/**
	 * Tells if the neighbor tile towards the given direction can be entered from the current direction.
	 * 
	 * @param dir
	 *              a direction (N, E, S, W)
	 * @return if the maze mover can enter the neighbor tile towards the given direction
	 */
	@Override
	public boolean canCrossBorderTo(int dir) {
		Tile currentTile = tile();
		return canMoveBetween(currentTile, maze().tileToDir(currentTile, dir));
	}

	/**
	 * Tells if this actor can (currently) cross the border between the given tiles.
	 * 
	 * @param tile
	 *                   current tile
	 * @param neighbor
	 *                   neighbor tile, may also be a tile outside of the board
	 * @return <code>true</code> if this maze mover can cross the border between the given tiles
	 */
	@Override
	public boolean canMoveBetween(Tile tile, Tile neighbor) {
		if (maze().isWall(neighbor)) {
			return false;
		}
		if (maze().isTunnel(neighbor)) {
			return true; // includes tiles outside board used for teleportation!
		}
		return maze().insideBoard(neighbor);
	}

	/**
	 * @param numTiles
	 *                   number of tiles
	 * @return the tile located <code>numTiles</code> tiles ahead of the actor towards his current move
	 *         direction.
	 */
	@Override
	public Tile tilesAhead(int numTiles) {
		return maze().tileToDir(tile(), moveDir, numTiles);
	}

	/**
	 * Turns back to the reverse direction and triggers new steering.
	 */
	public void turnBack() {
		nextDir = moveDir = NESW.inv(moveDir);
		enteredNewTile = true;
	}

	/**
	 * @return the maximum possible speed (in pixels/tick) for the current frame. The actual speed can
	 *         be lower to avoid moving into inaccessible tiles.
	 */
	public abstract float maxSpeed();

	/**
	 * Steers the actor by changing the intended move direction.
	 */
	protected abstract void steer();

	@Override
	public void placeAtTile(Tile tile, float xOffset, float yOffset) {
		MazeMover.super.placeAtTile(tile, xOffset, yOffset);
		enteredNewTile = !tile.equals(tile());
	}

	/**
	 * Moves or teleports the actor.
	 */
	protected void move() {
		boolean teleporting = teleport();
		if (!teleporting) {
			moveInsideMaze();
		}
	}

	/**
	 * When an actor (Ghost, Pac-Man) leaves a teleport tile towards the border, a timer is started and
	 * the actor is placed at the teleportation target and hidden (to avoid triggering events during
	 * teleportation). When the timer ends, the actor is made visible again.
	 * 
	 * @return <code>true</code> if teleportation is running
	 */
	private boolean teleport() {
		if (teleportTicksRemaining > 0) { // running
			teleportTicksRemaining -= 1;
			LOGGER.fine("Teleporting running, remaining:" + teleportTicksRemaining);
		}
		else if (teleportTicksRemaining == 0) { // completed
			teleportTicksRemaining = -1;
			show();
			LOGGER.fine("Teleporting complete");
		}
		else { // off
			int leftExit = (maze().tunnelLeftExit.col - 1) * Maze.TS;
			int rightExit = (maze().tunnelRightExit.col + 1) * Maze.TS;
			if (tf.getX() > rightExit) { // start
				teleportTicksRemaining = teleportingTicks;
				tf.setX(leftExit);
				hide();
				LOGGER.fine("Teleporting started");
			}
			else if (tf.getX() < leftExit) { // start
				teleportTicksRemaining = teleportingTicks;
				tf.setX(rightExit);
				hide();
				LOGGER.fine("Teleporting started");
			}
		}
		return teleportTicksRemaining != -1;
	}

	/**
	 * Movement inside the maze. Handles changing the direction according to the intended move
	 * direction, moving around corners without losing alignment,
	 */
	private void moveInsideMaze() {
		Tile oldTile = tile();
		float speed = possibleSpeedTo(nextDir);
		if (speed > 0) {
			if (nextDir == NESW.left(moveDir) || nextDir == NESW.right(moveDir)) {
				tf.setPosition(oldTile.col * Maze.TS, oldTile.row * Maze.TS);
			}
			moveDir = nextDir;
		}
		else {
			speed = possibleSpeedTo(moveDir);
		}
		tf.setVelocity(Vector2f.smul(speed, Vector2f.of(NESW.dx(moveDir), NESW.dy(moveDir))));
		tf.move();
		enteredNewTile = !oldTile.equals(tile());
	}

	/**
	 * Computes how many pixels this entity can move towards the given direction without crossing the
	 * border to a forbidden neighbor tile.
	 */
	private float possibleSpeedTo(int dir) {
		if (canCrossBorderTo(dir)) {
			return maxSpeed();
		}
		switch (dir) {
		case N:
			return -row() * Maze.TS + tf.getY();
		case E:
			return col() * Maze.TS - tf.getX();
		case S:
			return row() * Maze.TS - tf.getY();
		case W:
			return -col() * Maze.TS + tf.getX();
		default:
			throw new IllegalArgumentException("Illegal move direction: " + dir);
		}
	}
}