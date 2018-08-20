package de.amr.games.pacman.actor.core;

import static de.amr.easy.game.math.Vector2f.smul;
import static de.amr.easy.game.math.Vector2f.sum;
import static de.amr.games.pacman.model.Game.TS;
import static de.amr.games.pacman.model.Maze.NESW;
import static java.lang.Math.round;

import de.amr.easy.game.Application;
import de.amr.easy.game.math.Vector2f;
import de.amr.easy.grid.impl.Top4;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.Tile;

/**
 * An entity that knows how to move inside a tile-based maze.
 * 
 * @author Armin Reichert
 */
public abstract class MazeMover extends TileWorldEntity {

	private int currentDir;
	private int nextDir;

	public abstract Maze getMaze();

	public abstract boolean canEnterDoor(Tile door);

	public abstract int supplyIntendedDir();

	public abstract float getSpeed();

	public int getCurrentDir() {
		return currentDir;
	}

	public void setCurrentDir(int dir) {
		this.currentDir = dir;
	}

	public int getNextDir() {
		return nextDir;
	}

	public void setNextDir(int dir) {
		if (dir != nextDir) {
			nextDir = dir;
			Application.LOGGER.info("Next dir set to: " + dir);
		}
	}

	public boolean isTurn(int currentDir, int nextDir) {
		return nextDir == NESW.left(currentDir) || nextDir == NESW.right(currentDir);
	}

	public boolean inTeleportSpace() {
		return getMaze().inTeleportSpace(getTile());
	}

	public boolean inTunnel() {
		return getMaze().isTunnel(getTile());
	}

	public boolean inGhostHouse() {
		return getMaze().inGhostHouse(getTile());
	}

	public void move() {
		if (canMove(currentDir)) {
			tf.setVelocity(velocity(currentDir));
			tf.move();
			teleportIfPossible();
		}
	}

	public boolean canMove(int dir) {
		Vector2f v = velocity(dir);
		switch (dir) {
		case Top4.E:
			return canMoveRight(v);
		case Top4.W:
			return canMoveLeft(v);
		case Top4.N:
			return canMoveUp(v);
		case Top4.S:
			return canMoveDown(v);
		}
		throw new IllegalArgumentException("Illegal direction: " + dir);
	}

	public boolean canEnterTile(Tile tile) {
		return !getMaze().isWall(tile) || (getMaze().isDoor(tile) && canEnterDoor(tile));
	}

	private boolean canMoveRight(Vector2f v) {
		int col = Math.round(tf.getX() + getWidth() / 2) / TS;
		int row = Math.round(tf.getY() + getHeight() / 2) / TS;
		int newCol = Math.round(tf.getX() + getWidth()) / TS;
		return newCol == col || canEnterTile(new Tile(newCol, row));
	}

	private boolean canMoveLeft(Vector2f v) {
		int col = Math.round(tf.getX()) / TS;
		int row = Math.round(tf.getY() + getHeight() / 2) / TS;
		int newCol = Math.round(tf.getX() + v.x) / TS;
		return newCol == col || canEnterTile(new Tile(newCol, row));
	}

	private boolean canMoveUp(Vector2f v) {
		int col = Math.round(tf.getX() + getWidth() / 2) / TS;
		int row = Math.round(tf.getY() + getHeight() / 2) / TS;
		int newRow = Math.round(tf.getY() + v.y) / TS;
		return newRow == row || canEnterTile(new Tile(col, newRow));
	}

	private boolean canMoveDown(Vector2f v) {
		int col = Math.round(tf.getX() + getWidth() / 2) / TS;
		int row = Math.round(tf.getY() + getHeight() / 2) / TS;
		int newRow = Math.round(tf.getY() + getHeight()) / TS;
		return newRow == row || canEnterTile(new Tile(col, newRow));
	}

	public Vector2f positionAfterMove(int dir) {
		return sum(tf.getPosition(), velocity(dir));
	}

	private Vector2f velocity(int dir) {
		return smul(getSpeed(), Vector2f.of(NESW.dx(dir), NESW.dy(dir)));
	}

	/**
	 * "Teleport".
	 * 
	 * Leaves the maze on the left or right side, runs in "teleport space", reenters the maze on the
	 * opposite side.
	 */
	private void teleportIfPossible() {
		int right = getMaze().numCols() - 1;
		int len = getMaze().getTeleportLength();
		if (tf.getX() > (right + len) * TS) {
			tf.setX(0);
		} else if (tf.getX() < -len * TS) {
			tf.setX(right * TS);
		}
	}

	/**
	 * Computes the tile position after a move in the given direction.
	 * 
	 * @param dir
	 *              move direction
	 * @return the tile after a move in that direction
	 */
	public Tile tileAfterMove(int dir) {
		Tile current = getTile();
		Vector2f pos = positionAfterMove(dir);
		switch (dir) {
		case Top4.W:
			return new Tile(round(pos.x) / TS, current.row);
		case Top4.E:
			return new Tile(round(pos.x + getWidth()) / TS, current.row);
		case Top4.N:
			return new Tile(current.col, round(pos.y) / TS);
		case Top4.S:
			return new Tile(current.col, round(pos.y + getHeight()) / TS);
		}
		throw new IllegalArgumentException("Illegal direction: " + dir);
	}
}