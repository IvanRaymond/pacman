package de.amr.games.pacman;

import java.awt.Color;

public class Ghost extends Creature {

	public final Color color;
	public final V2i scatterTile;
	public V2i targetTile;
	public boolean frightened;
	public boolean enteringHouse;
	public boolean leavingHouse;
	public int bounty;
	public long bountyTimer;

	public Ghost(String name, Color color, V2i homeTile, V2i scatterTile) {
		super(name, homeTile);
		this.color = color;
		this.scatterTile = scatterTile;
	}

	@Override
	public String toString() {
		return String.format("%8s tile=%s offset=(%.2f,%.2f) target=%s", name, tile, offsetX, offsetY, targetTile);
	}
}