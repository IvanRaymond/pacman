package de.amr.games.pacman.controller.actor.steering.common;

import java.util.ArrayList;
import java.util.List;

import de.amr.games.pacman.controller.actor.WorldMover;
import de.amr.games.pacman.model.world.Tile;

/**
 * Lets an actor follow a fixed path.
 * 
 * @author Armin Reichert
 */
public class TakingFixedPath extends TakingPrecomputedPath {

	private List<Tile> path;

	public TakingFixedPath(WorldMover actor, List<Tile> path) {
		super(actor, () -> path.get(path.size() - 1));
		this.path = new ArrayList<>(path);
	}

	@Override
	protected List<Tile> pathToTarget(WorldMover actor, Tile targetTile) {
		return path;
	}
}