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

	public int getIntendedNextDir() {
		return nextDir;
	}

	protected boolean canWalkThroughDoor(Tile door) {
		return true;
	}

	public void move() {
		nextDir = getIntendedNextDir();
		if (canMove(nextDir)) {
			if (nextDir == NESW.left(dir) || nextDir == NESW.right(dir)) {
				placeAtTile(getTile(), 0, 0); // align on tile
			}
			dir = nextDir;
		}
		if (getMaze().isTeleportSpace(getTile())) {
			teleport();
			return;
		}
		if (canMove(dir)) {
			tf.moveTo(computePosition(dir));
		} else {
			placeAtTile(getTile(), 0, 0); // align on tile
		}
	}

	public boolean canMove(int targetDir) {
		Tile current = getTile(), next = computeNextTile(current, targetDir);
		if (getMaze().isTeleportSpace(current)) {
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
		Vector2f nextPosition = computePosition(dir);
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

	private void teleport() {
		Tile tile = getTile();
		if (tile.col > (getMaze().numCols() - 1) + getMaze().getTeleportLength()) {
			// reenter maze from the left
			tf.moveTo(0, tile.row * TS);
		} else if (tile.col < -getMaze().getTeleportLength()) {
			// reenter maze from the right
			tf.moveTo((getMaze().numCols() - 1) * TS, tile.row * TS);
		} else {
			tf.moveTo(computePosition(dir));
		}
	}

	private Vector2f computePosition(int dir) {
		Vector2f direction = Vector2f.of(NESW.dx(dir), NESW.dy(dir));
		Vector2f velocity = smul(getSpeed(), direction);
		return sum(tf.getPosition(), velocity);
	}
}