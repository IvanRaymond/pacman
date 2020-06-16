package de.amr.games.pacman.model;

import java.util.Objects;
import java.util.Optional;

import de.amr.easy.game.math.Vector2f;

/**
 * The Pac-Man game world is layed out into tiles of eight pixels size each.
 * 
 * @author Armin Reichert
 */
public final class Tile {

	/** Tile size in pixels. */
	public static final byte SIZE = 8;

	/**
	 * Nicer constructor function.
	 * 
	 * @param col tile column index
	 * @param row tile row index
	 * @return new tile
	 */
	public static Tile at(int col, int row) {
		return new Tile(col, row);
	}

	/** Tile column index. Left to right, zero based. */
	public final byte col;

	/** Tile row index. Top to bottom, zero based. */
	public final byte row;

	/**
	 * Constructor function.
	 * 
	 * @param col tile column index
	 * @param row tile row index
	 * @return new tile
	 */
	public Tile(int col, int row) {
		this.col = (byte) col;
		this.row = (byte) row;
	}

	/**
	 * @return this tiles' x-coordinate
	 */
	public int x() {
		return col * SIZE;
	}

	/**
	 * @return this tiles' y-coordinate
	 */
	public int y() {
		return row * SIZE;
	}

	/**
	 * @return this tiles' center x-coordinate
	 */
	public int centerX() {
		return col * SIZE + SIZE / 2;
	}

	/**
	 * @return this tiles' center y-coordinate
	 */
	public int centerY() {
		return row * SIZE + SIZE / 2;
	}

	/**
	 * @param other other tile
	 * @return Euclidean distance to other tile measured in tiles
	 */
	public double distance(Tile other) {
		int dx = col - other.col, dy = row - other.row;
		return Math.sqrt(dx * dx + dy * dy);
	}

	/**
	 * @param other other tile
	 * @return Manhattan distance to other tile measured in tiles
	 */
	public int manhattanDistance(Tile other) {
		int dx = Math.abs(col - other.col), dy = Math.abs(row - other.row);
		return dx + dy;
	}

	/**
	 * @param min minimum column index
	 * @param max maximum column index
	 * @return {@code true} if this tile is in the given column index range (boundaries inclusive)
	 */
	public boolean inColumnRange(int min, int max) {
		return min <= col && col <= max;
	}

	/**
	 * @param min minimum row index
	 * @param max maximum row index
	 * @return {@code true} if this tile is in the given row index range (boundaries inclusive)
	 */
	public boolean inRowRange(int min, int max) {
		return min <= row && row <= max;
	}

	/**
	 * @param other other tile
	 * @return the direction towards the other tile, if it is a neighbor tile
	 */
	public Optional<Direction> dirTo(Tile other) {
		Vector2f v = Vector2f.of(other.col - col, other.row - row);
		return Direction.dirs().filter(dir -> dir.vector().equals(v)).findFirst();
	}

	@Override
	public int hashCode() {
		return Objects.hash(col, row);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tile other = (Tile) obj;
		return col == other.col && row == other.row;
	}

	@Override
	public String toString() {
		return String.format("(%d,%d)", col, row);
	}
}