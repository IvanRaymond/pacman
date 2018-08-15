package de.amr.games.pacman.routing.impl;

import static de.amr.games.pacman.model.Content.DOOR;
import static de.amr.games.pacman.model.Content.WALL;
import static de.amr.games.pacman.model.Maze.NESW;

import de.amr.games.pacman.actor.core.MazeMover;
import de.amr.games.pacman.model.Tile;
import de.amr.games.pacman.routing.MazeRoute;
import de.amr.games.pacman.routing.Navigation;

class Bounce implements Navigation {

	@Override
	public MazeRoute computeRoute(MazeMover bouncer) {
		MazeRoute route = new MazeRoute();
		route.dir = isReflected(bouncer) ? NESW.inv(bouncer.getDir()) : bouncer.getDir();
		return route;
	}

	private boolean isReflected(MazeMover bouncer) {
		Tile nextTile = bouncer.computeNextTile(bouncer.getTile(), bouncer.getDir());
		char content = bouncer.getMaze().getContent(nextTile);
		return content == WALL || content == DOOR;
	}
}