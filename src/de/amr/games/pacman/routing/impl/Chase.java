package de.amr.games.pacman.routing.impl;

import de.amr.games.pacman.actor.core.MazeMover;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.routing.MazeRoute;
import de.amr.games.pacman.routing.Navigation;

/**
 * Chasing a refugee through the maze.
 */
class Chase implements Navigation {

	private final MazeMover victim;
	private final Maze maze;

	public Chase(MazeMover victim) {
		this.victim = victim;
		maze = victim.maze;
	}

	@Override
	public MazeRoute computeRoute(MazeMover chaser) {
		MazeRoute route = new MazeRoute();
		if (maze.isTeleportSpace(victim.getTile())) {
			route.dir = chaser.getNextDir();
			return route;
		}
		route.path = chaser.maze.findPath(chaser.getTile(), victim.getTile());
		route.dir = chaser.maze.alongPath(route.path).orElse(chaser.getNextDir());
		return route;
	}
}