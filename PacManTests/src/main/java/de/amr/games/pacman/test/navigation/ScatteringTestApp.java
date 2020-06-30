package de.amr.games.pacman.test.navigation;

import java.awt.Color;
import java.awt.event.KeyEvent;

import de.amr.easy.game.Application;
import de.amr.easy.game.config.AppSettings;
import de.amr.easy.game.input.Keyboard;
import de.amr.games.pacman.controller.actor.GhostState;
import de.amr.games.pacman.controller.event.GhostUnlockedEvent;
import de.amr.games.pacman.model.world.Tile;

public class ScatteringTestApp extends Application {

	public static void main(String[] args) {
		launch(ScatteringTestApp.class, args);
	}

	@Override
	protected void configure(AppSettings settings) {
		settings.width = 28 * Tile.SIZE;
		settings.height = 36 * Tile.SIZE;
		settings.scale = 2;
		settings.title = "Scattering";
	}

	@Override
	public void init() {
		setController(new ScatteringTestUI());
	}
}

class ScatteringTestUI extends TestUI {

	public ScatteringTestUI() {
		view.showRoutes = true;
		view.showGrid = true;
	}

	@Override
	public void init() {
		super.init();
		world.population().ghosts().forEach(ghost -> {
			world.putOnStage(ghost, true);
			ghost.subsequentState = GhostState.SCATTERING;
		});
		view.showMessage("Press SPACE to start", Color.WHITE);
	}

	@Override
	public void update() {
		if (Keyboard.keyPressedOnce(KeyEvent.VK_SPACE)) {
			ghostsOnStage().forEach(ghost -> ghost.process(new GhostUnlockedEvent()));
			view.clearMessage();
		}
		super.update();
	}
}