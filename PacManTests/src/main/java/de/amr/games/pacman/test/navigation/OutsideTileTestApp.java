package de.amr.games.pacman.test.navigation;

import de.amr.easy.game.Application;
import de.amr.easy.game.config.AppSettings;
import de.amr.games.pacman.controller.actor.GhostState;
import de.amr.games.pacman.model.world.core.Tile;

/**
 * Test for heading for a tile outside of the maze.
 *
 */
public class OutsideTileTestApp extends Application {

	public static void main(String[] args) {
		launch(OutsideTileTestApp.class, args);
	}

	@Override
	protected void configure(AppSettings settings) {
		settings.width = 28 * Tile.SIZE;
		settings.height = 36 * Tile.SIZE;
		settings.scale = 2;
		settings.title = "Follow Tile Outside Maze";
	}

	@Override
	public void init() {
		setController(new OutsideTileTestUI());
	}
}

class OutsideTileTestUI extends TestUI {

	public OutsideTileTestUI() {
		view.showRoutes = true;
	}

	@Override
	public void init() {
		super.init();
		theme.snd_ghost_chase().volume(0);
		include(blinky);
		int row = world.portals().findFirst().map(portal -> portal.right.row).orElse((short) 100);
		blinky.behavior(GhostState.CHASING, blinky.headingFor(() -> Tile.at(100, row)));
		blinky.setState(GhostState.CHASING);
	}
}