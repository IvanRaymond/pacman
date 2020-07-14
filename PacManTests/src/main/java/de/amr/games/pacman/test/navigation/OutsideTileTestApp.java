package de.amr.games.pacman.test.navigation;

import static de.amr.games.pacman.controller.creatures.ghost.GhostState.CHASING;
import static de.amr.games.pacman.controller.steering.api.SteeringBuilder.headingForTargetTile;

import de.amr.easy.game.Application;
import de.amr.easy.game.config.AppSettings;
import de.amr.games.pacman.model.world.core.Tile;
import de.amr.games.pacman.test.TestUI;

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

	@Override
	public void init() {
		super.init();
		soundManager.snd_ghost_chase().volume(0);
		include(blinky);
		blinky.init();
		int row = world.portals().findFirst().map(portal -> portal.right.row).orElse((short) 100);
		blinky.behavior(CHASING, headingForTargetTile(blinky).tile(100, row).build());
		blinky.setState(CHASING);
		view.turnRoutesOn();
	}
}