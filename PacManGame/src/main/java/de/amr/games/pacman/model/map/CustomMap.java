package de.amr.games.pacman.model.map;

import de.amr.games.pacman.model.world.Tile;

public class CustomMap extends ArcadeMap {

	public CustomMap() {
		addPortal(Tile.at(0, 4), Tile.at(27, 32));
		addPortal(Tile.at(0, 32), Tile.at(27, 4));
	}
}