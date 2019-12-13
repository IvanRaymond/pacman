package de.amr.games.pacman.test.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.PacManGame;

public class BoardTests {

	@Test
	public void testBoardLoading() {
		Maze maze = new Maze(PacManGame.BOARD);

		assertEquals(28, Maze.NUM_COLS);
		assertEquals(36, Maze.NUM_ROWS);

		assertNotNull(maze.pacManHome);
		assertNotNull(maze.ghostHome[0]);
		assertNotNull(maze.ghostHome[1]);
		assertNotNull(maze.ghostHome[2]);
		assertNotNull(maze.ghostHome[3]);
		assertNotNull(maze.tunnelExitLeft);
		assertNotNull(maze.tunnelExitRight);
		assertNotNull(maze.bonusTile);
		assertNotNull(maze.cornerNW);
		assertNotNull(maze.cornerNE);
		assertNotNull(maze.cornerSW);
		assertNotNull(maze.cornerSE);
		assertNotNull(maze.scatterTileNE);
		assertNotNull(maze.scatterTileNW);
		assertNotNull(maze.scatterTileSW);
		assertNotNull(maze.scatterTileSE);

		assertEquals(4, maze.tiles().filter(tile -> maze.containsEnergizer(tile)).count());
		assertEquals(240, maze.tiles().filter(tile -> maze.containsPellet(tile)).count());

		assertTrue(maze.isWall(maze.tileAt(0, 3)));
		assertTrue(maze.isDoor(maze.tileAt(13, 15)));
		assertTrue(maze.containsPellet(maze.tileAt(1, 4)));
		assertTrue(maze.containsEnergizer(maze.tileAt(1, 6)));

		assertEquals(maze.tileAt(0, 0), maze.tileAt(0, 0));
	}
}