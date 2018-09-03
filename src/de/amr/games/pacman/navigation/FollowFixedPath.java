package de.amr.games.pacman.navigation;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import de.amr.games.pacman.actor.Actor;
import de.amr.games.pacman.model.Tile;

//TODO: does not yet work 100%
class FollowFixedPath<T extends Actor> implements Navigation<T> {

	protected Supplier<Tile> targetTileSupplier;
	protected List<Tile> path = Collections.emptyList();

	public FollowFixedPath(Supplier<Tile> targetTileSupplier) {
		this.targetTileSupplier = targetTileSupplier;
	}

	protected FollowFixedPath() {

	}

	@Override
	public MazeRoute computeRoute(T mover) {
		if (path.size() > 0 && !path.get(0).equals(mover.getTile())) {
			path.remove(0);
		}
		MazeRoute route = new MazeRoute();
		route.setPath(path);
		route.setDir(mover.getMaze().alongPath(path).orElse(mover.getCurrentDir()));
		return route;
	}

	@Override
	public void computeStaticRoute(T mover) {
		path = mover.getMaze().findPath(mover.getTile(), targetTileSupplier.get());
	}
}