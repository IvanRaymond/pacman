package de.amr.games.pacman.navigation;

import de.amr.games.pacman.actor.core.MazeMover;

public interface Navigation {

	MazeRoute computeRoute(MazeMover mover);
}