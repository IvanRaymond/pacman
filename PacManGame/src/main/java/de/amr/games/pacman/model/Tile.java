package de.amr.games.pacman.model;

/**
 * The Pac-Man game world consists of an unbounded grid of tiles. The board tiles are created
 * exactly once when the board/maze is created. Therefore tiles inside the board can be compared by
 * identity where in the general case, tiles have to be compared using equals(). The tile content is
 * not relevant when comparing tiles.
 * 
 * @author Armin Reichert
 */
public class Tile {

	/** Tile size in pixels. */
	public static final int SIZE = 8;

	public static final char WALL = '#';
	public static final char TUNNEL = 't';
	public static final char SPACE = ' ';
	public static final char PELLET = '.';
	public static final char ENERGIZER = '*';
	public static final char EATEN_PELLET = ':';
	public static final char EATEN_ENERGIZER = '~';

	/** Straight line distance (squared). */
	public static int distanceSq(Tile t1, Tile t2) {
		int dx = t1.col - t2.col, dy = t1.row - t2.row;
		return dx * dx + dy * dy;
	}

	public final byte col;
	public final byte row;
	public char content;

	Tile(byte col, byte row, char content) {
		this.col = col;
		this.row = row;
		this.content = content;
	}

	public boolean containsFood() {
		return containsPellet() || containsEnergizer();
	}

	public boolean containsPellet() {
		return content == PELLET;
	}

	public boolean containsEnergizer() {
		return content == ENERGIZER;
	}

	public boolean containsEatenFood() {
		return content == EATEN_PELLET || content == EATEN_ENERGIZER;
	}

	@Override
	public int hashCode() {
		int sum = col + row;
		return sum * (sum + 1) / 2 + col;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Tile) {
			Tile other = (Tile) obj;
			return col == other.col && row == other.row;
		}
		return super.equals(obj);
	}

	@Override
	public String toString() {
		return String.format("(%d,%d,'%c')", col, row, content);
	}
}