package de.amr.games.pacman.actor.core;

import static de.amr.easy.game.math.Vector2f.smul;
import static de.amr.easy.game.math.Vector2f.sum;
import static de.amr.games.pacman.model.Content.DOOR;
import static de.amr.games.pacman.model.Content.WALL;
import static de.amr.games.pacman.model.Game.TS;
import static de.amr.games.pacman.model.Maze.NESW;
import static java.lang.Math.round;

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

	private int dir;
	private int nextDir;

	public abstract Maze getMaze();

	public abstract Tile getHome();

	public abstract float getSpeed();

	public int getDir() {
		return dir;
	}

	public void setDir(int dir) {
		this.dir = dir;
	}

	public int getNextDir() {
		return nextDir;
	}

	public void setNextDir(int dir) {
		this.nextDir = dir;
	}

	public int supplyIntendedDir() {
		return nextDir;
	}

	protected boolean canWalkThroughDoor(Tile door) {
		return true;
	}

	public void move() {
		nextDir = supplyIntendedDir();
		if (canMove(nextDir)) {
			if (nextDir == NESW.left(dir) || nextDir == NESW.right(dir)) {
				placeAtTile(getTile(), 0, 0); // align on tile
			}
			dir = nextDir;
		}
		if (inTeleportSpace()) {
			teleport();
			return;
		}
		if (canMove(dir)) {
			tf.moveTo(positionAfterMove(dir));
		} else {
			placeAtTile(getTile(), 0, 0); // align on tile
		}
	}

	public boolean canMove(int targetDir) {
		Tile current = getTile(), next = computeNextTile(current, targetDir);
		if (inTeleportSpace()) {
			// in teleport space direction can only be reversed
			return targetDir == dir || targetDir == NESW.inv(dir);
		}
		if (getMaze().getContent(next) == WALL) {
			return false;
		}
		if (getMaze().getContent(next) == DOOR) {
			return canWalkThroughDoor(next);
		}
		if (targetDir == NESW.right(dir) || targetDir == NESW.left(dir)) {
			// TODO this is not nice
			return targetDir == Top4.N || targetDir == Top4.S ? getAlignmentX() <= 1 : getAlignmentY() <= 1;
		}
		return true;
	}

	public Tile computeNextTile(Tile current, int dir) {
		Vector2f nextPosition = positionAfterMove(dir);
		float x = nextPosition.x, y = nextPosition.y;
		switch (dir) {
		case Top4.W:
			return new Tile(round(x) / TS, current.row);
		case Top4.E:
			return new Tile(round(x + getWidth()) / TS, current.row);
		case Top4.N:
			return new Tile(current.col, round(y) / TS);
		case Top4.S:
			return new Tile(current.col, round(y + getHeight()) / TS);
		default:
			throw new IllegalArgumentException("Illegal direction: " + dir);
		}
	}

	private boolean inTeleportSpace() {
		return getMaze().isTeleportSpace(getTile());
	}

	/**
	 * "Teleport": leave the maze on the left or right side, run over a certain number of tiles in
	 * "teleport space", then reenter the maze on the opposite side.
	 */
	private void teleport() {
		Tile tile = getTile();
		int left = 0, right = getMaze().numCols() - 1, length = getMaze().getTeleportLength();
		if (tile.col > right + length) {
			tf.moveTo(left * TS, tile.row * TS);
		} else if (tile.col < left - length) {
			tf.moveTo(right * TS, tile.row * TS);
		} else {
			tf.moveTo(positionAfterMove(dir));
		}
	}

	private Vector2f positionAfterMove(int dir) {
		return sum(tf.getPosition(), smul(getSpeed(), Vector2f.of(NESW.dx(dir), NESW.dy(dir))));
	}
}