package de.amr.games.pacman.test.navigation;

import static de.amr.games.pacman.controller.actor.GhostState.FRIGHTENED;

import java.awt.Color;
import java.awt.event.KeyEvent;

import de.amr.easy.game.Application;
import de.amr.easy.game.config.AppSettings;
import de.amr.easy.game.input.Keyboard;
import de.amr.games.pacman.model.world.core.Tile;
import de.amr.games.pacman.test.TestUI;

public class MovingRandomlyTestApp extends Application {

	public static void main(String[] args) {
		launch(MovingRandomlyTestApp.class, args);
	}

	@Override
	protected void configure(AppSettings settings) {
		settings.width = 28 * Tile.SIZE;
		settings.height = 36 * Tile.SIZE;
		settings.scale = 2;
		settings.title = "Moving Randomly";
	}

	@Override
	public void init() {
		setController(new MovingRandomlyTestUI());
	}
}

class MovingRandomlyTestUI extends TestUI {

	private boolean started;

	@Override
	public void init() {
		super.init();
		include(blinky, pinky, inky, clyde);
		ghostsOnStage().forEach(ghost -> {
			ghost.init();
			ghost.tf.setPosition(pacMan.bed().tile.x(), pacMan.bed().tile.y());
			ghost.behavior(FRIGHTENED, ghost.movingRandomly());
			ghost.state(FRIGHTENED).removeTimer();
			ghost.setState(FRIGHTENED);
		});
		view.turnRoutesOn();
		view.turnStatesOn();
		view.turnGridOn();
		view.showMessage(1, "Press SPACE", Color.WHITE);
		started = false;
	}

	@Override
	public void update() {
		if (!started && Keyboard.keyPressedOnce(KeyEvent.VK_SPACE)) {
			started = true;
			view.clearMessage(1);
		}
		if (started) {
			super.update();
		}
	}
}