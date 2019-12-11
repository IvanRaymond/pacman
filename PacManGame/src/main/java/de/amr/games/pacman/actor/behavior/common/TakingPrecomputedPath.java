package de.amr.games.pacman.actor.behavior.common;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import de.amr.games.pacman.actor.behavior.Steering;
import de.amr.games.pacman.actor.core.MazeMover;
import de.amr.games.pacman.model.Tile;

/**
 * Steering using a precomputed path which is recomputed only if the actor would
 * leave this path.
 *
 * @author Armin Reichert
 */
public abstract class TakingPrecomputedPath<T extends MazeMover> implements Steering<T> {

	protected Supplier<Tile> fnTargetTile;
	protected List<Tile> path = Collections.emptyList();

	public TakingPrecomputedPath(Supplier<Tile> fnTargetTile) {
		this.fnTargetTile = fnTargetTile;
	}

	@Override
	public void steer(T actor) {
		Tile actorTile = actor.tile();
		actor.setTargetTile(fnTargetTile.get());
		while (path.size() > 0 && !actorTile.equals(path.get(0))) {
			path.remove(0);
		}
		if (path.isEmpty() || actorTile.equals(path.get(path.size() - 1))) {
			path = computePath(actor);
			actor.setTargetPath(path);
			actor.setTargetTile(path.isEmpty() ? null : path.get(path.size() - 1));
		}
		actor.setNextDir(actor.maze().alongPath(path).orElse(actor.moveDir()));
	}

	protected abstract List<Tile> computePath(T actor);
}