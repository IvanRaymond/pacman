package de.amr.games.pacman.actor.behavior.pacman;

import static de.amr.datastruct.StreamUtils.permute;
import static de.amr.games.pacman.model.Maze.NESW;
import static de.amr.games.pacman.model.Tile.distanceSq;

import java.util.Comparator;

import de.amr.games.pacman.actor.Ghost;
import de.amr.games.pacman.actor.PacMan;
import de.amr.games.pacman.actor.behavior.Steering;
import de.amr.games.pacman.model.Tile;

public class AvoidGhosts implements Steering<PacMan> {

	@Override
	public void steer(PacMan pacMan) {
		/*@formatter:off*/
		pacMan.game.activeGhosts()
			.filter(ghost -> !pacMan.maze.inGhostHouse(ghost.currentTile()))
			.sorted(bySmallestDistanceTo(pacMan))
			.findFirst()
			.ifPresent(ghost -> {
				pacMan.nextDir = NESW.dirs().boxed()
						.filter(pacMan::canCrossBorderTo)
						.sorted(byLargestDistanceOfNeighborTile(pacMan, ghost))
						.findAny()
						.orElse(randomAccessibleDir(pacMan));
			});
		/*@formatter:on*/
	}

	private Comparator<Integer> byLargestDistanceOfNeighborTile(PacMan pacMan, Ghost ghost) {
		Tile pacManTile = pacMan.currentTile(), ghostTile = ghost.currentTile();
		return (dir1, dir2) -> {
			Tile neighborTile1 = pacMan.maze.tileToDir(pacManTile, dir1),
					neighborTile2 = pacMan.maze.tileToDir(pacManTile, dir2);
			return -Integer.compare(distanceSq(neighborTile1, ghostTile), distanceSq(neighborTile2, ghostTile));
		};
	}

	private Comparator<Ghost> bySmallestDistanceTo(PacMan pacMan) {
		return (ghost1, ghost2) -> Integer.compare(pacMan.distanceSq(ghost1), pacMan.distanceSq(ghost2));
	}

	private int randomAccessibleDir(PacMan pacMan) {
		return permute(NESW.dirs()).filter(pacMan::canCrossBorderTo).findAny().getAsInt();
	}
}