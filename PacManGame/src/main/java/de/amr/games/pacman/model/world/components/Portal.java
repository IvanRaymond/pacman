package de.amr.games.pacman.model.world.components;

import de.amr.games.pacman.model.world.api.Tile;

/**
 * A portal.
 * <p>
 * A horizontal portal connects a tile on the right edge of the world with a corresponding tile on
 * the left edge, a vertical portal connects a tile on the upper edge with a tile on the lower edge.
 * 
 * @author Armin Reichert
 */
public class Portal {

	/** left or top tile */
	public final Tile either;

	/** right or bottom tile */
	public final Tile other;

	/** If this is a horizontal or vertical portal */
	public final boolean vertical;

	public Portal(Tile either, Tile other, boolean vertical) {
		this.either = either;
		this.other = other;
		this.vertical = vertical;
	}

	public boolean includes(Tile tile) {
		return tile.equals(either) || tile.equals(other);
	}
}