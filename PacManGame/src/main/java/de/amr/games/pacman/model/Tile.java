package de.amr.games.pacman.model;

/**
 * A tile.
 * 
 * @author Armin Reichert
 */
public class Tile {

	public static final Tile UNDEFINED = new Tile(-1, -1);

	public final int col;
	public final int row;
	public char content;

	/* Only called from Maze. */
	Tile(int col, int row) {
		this.col = col;
		this.row = row;
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