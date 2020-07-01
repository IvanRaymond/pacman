package de.amr.games.pacman.test.navigation.graph;

import static de.amr.games.pacman.controller.actor.GhostState.FRIGHTENED;
import static de.amr.games.pacman.controller.actor.PacManState.EATING;

import de.amr.easy.game.Application;
import de.amr.easy.game.config.AppSettings;
import de.amr.games.pacman.model.world.core.Tile;
import de.amr.games.pacman.test.navigation.TestUI;

public class EscapeIntoCornerTestApp extends Application {

	public static void main(String[] args) {
		launch(EscapeIntoCornerTestApp.class, args);
	}

	@Override
	protected void configure(AppSettings settings) {
		settings.width = 28 * Tile.SIZE;
		settings.height = 36 * Tile.SIZE;
		settings.scale = 2;
		settings.title = "Escape Into Corner";
	}

	@Override
	public void init() {
		setController(new EscapeIntoCornerTestUI());
	}
}

class EscapeIntoCornerTestUI extends TestUI {

	public EscapeIntoCornerTestUI() {
		view.showRoutes = true;
		view.showStates = true;
	}

	@Override
	public void init() {
		super.init();
		putOnStage(pacMan, blinky);
		pacMan.setState(EATING);
		blinky.behavior(FRIGHTENED, blinky.isFleeingToSafeCorner(pacMan));
		blinky.setState(FRIGHTENED);
	}
}