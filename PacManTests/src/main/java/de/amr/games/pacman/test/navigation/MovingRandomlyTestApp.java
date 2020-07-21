package de.amr.games.pacman.test.navigation;

import static de.amr.games.pacman.controller.creatures.ghost.GhostState.FRIGHTENED;
import static de.amr.games.pacman.controller.steering.api.AnimalMaster.you;

import java.awt.Color;
import java.awt.event.KeyEvent;

import de.amr.easy.game.Application;
import de.amr.easy.game.config.AppSettings;
import de.amr.easy.game.input.Keyboard;
import de.amr.games.pacman.model.world.api.Tile;
import de.amr.games.pacman.model.world.components.Bed;
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
			you(ghost).when(FRIGHTENED).moveRandomly().ok();
			ghost.state(FRIGHTENED).removeTimer();
			ghost.setState(FRIGHTENED);
			Bed bed = world.pacManBed();
			ghost.entity.tf.centerBoth(bed.col() * Tile.SIZE, bed.row() * Tile.SIZE, bed.width() * Tile.SIZE,
					bed.height() * Tile.SIZE);
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