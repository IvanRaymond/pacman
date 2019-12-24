package de.amr.games.pacman.actor.behavior.pacman;

import static de.amr.datastruct.StreamUtils.permute;
import static de.amr.games.pacman.model.Tile.distanceSq;

import java.util.Comparator;

import de.amr.games.pacman.actor.Ghost;
import de.amr.games.pacman.actor.PacMan;
import de.amr.games.pacman.actor.PacManGameCast;
import de.amr.games.pacman.actor.behavior.Steering;
import de.amr.games.pacman.actor.core.MazeMover;
import de.amr.games.pacman.model.Direction;
import de.amr.games.pacman.model.Tile;

public class AvoidingGhosts implements Steering<PacMan> {

	private final PacManGameCast cast;

	public AvoidingGhosts(PacManGameCast cast) {
		this.cast = cast;
	}

	@Override
	public void steer(PacMan pacMan) {
		/*@formatter:off*/
		cast.ghostsOnStage()
			.filter(ghost -> !pacMan.maze().inGhostHouse(ghost.tile()))
			.sorted(bySmallestDistanceTo(pacMan))
			.findFirst()
			.ifPresent(ghost -> {
				pacMan.setNextDir(Direction.dirs()
						.filter(pacMan::canCrossBorderTo)
						.sorted(byLargestDistanceOfNeighborTile(pacMan, ghost))
						.findAny()
						.orElse(randomAccessibleDir(pacMan)));
			});
		/*@formatter:on*/
	}

	private Comparator<Direction> byLargestDistanceOfNeighborTile(PacMan pacMan, MazeMover ghost) {
		Tile pacManTile = pacMan.tile(), ghostTile = ghost.tile();
		return (dir1, dir2) -> {
			Tile neighborTile1 = pacMan.maze().tileToDir(pacManTile, dir1),
					neighborTile2 = pacMan.maze().tileToDir(pacManTile, dir2);
			return -Integer.compare(distanceSq(neighborTile1, ghostTile), distanceSq(neighborTile2, ghostTile));
		};
	}

	private Comparator<Ghost> bySmallestDistanceTo(PacMan pacMan) {
		return (ghost1, ghost2) -> Integer.compare(pacMan.distanceSq(ghost1), pacMan.distanceSq(ghost2));
	}

	private Direction randomAccessibleDir(PacMan pacMan) {
		return permute(Direction.dirs()).filter(pacMan::canCrossBorderTo).findAny().get();
	}
}