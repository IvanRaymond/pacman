package de.amr.games.pacman.test.navigation;

import java.awt.event.KeyEvent;

import de.amr.easy.game.input.Keyboard;
import de.amr.easy.game.view.View;
import de.amr.easy.game.view.VisualController;
import de.amr.games.pacman.actor.PacManGameCast;
import de.amr.games.pacman.actor.behavior.common.Steerings;
import de.amr.games.pacman.actor.core.MazeResident;
import de.amr.games.pacman.controller.event.FoodFoundEvent;
import de.amr.games.pacman.view.play.PlayView;

public class PacManMovementTestUI extends PlayView implements VisualController {

	public PacManMovementTestUI(PacManGameCast cast) {
		super(cast);
		showScores = false;
	}

	@Override
	public void init() {
		super.init();
		game.startLevel(1);
		cast.pacMan.addGameEventListener(event -> {
			if (event.getClass() == FoodFoundEvent.class) {
				FoodFoundEvent foodFound = (FoodFoundEvent) event;
				cast.theme.snd_eatPill().play();
				game.maze.removeFood(foodFound.tile);
				if (game.maze.tiles().filter(game.maze::containsFood).count() == 0) {
					game.maze.restoreFood();
				}
			}
		});
		cast.activate(cast.pacMan);
		cast.pacMan.init();
	}

	@Override
	public void update() {
		handleSteeringChange();
		cast.activeActors().forEach(MazeResident::update);
		super.update();
	}

	private void handleSteeringChange() {
		if (Keyboard.keyPressedOnce(KeyEvent.VK_M)) {
			cast.pacMan.steering = Steerings.steeredByKeys(KeyEvent.VK_UP, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN,
					KeyEvent.VK_LEFT);
		} else if (Keyboard.keyPressedOnce(KeyEvent.VK_N)) {
			cast.pacMan.steering = Steerings.steeredByKeys(KeyEvent.VK_NUMPAD8, KeyEvent.VK_NUMPAD6, KeyEvent.VK_NUMPAD2,
					KeyEvent.VK_NUMPAD4);
		} else if (Keyboard.keyPressedOnce(KeyEvent.VK_A)) {
			cast.pacMan.steering = Steerings.avoidingGhosts();
		} else if (Keyboard.keyPressedOnce(KeyEvent.VK_R)) {
			cast.pacMan.steering = Steerings.movingRandomlyNoReversing();
//			cast.pacMan.enteredNewTile = true;
		}
	}

	@Override
	public View currentView() {
		return this;
	}
}