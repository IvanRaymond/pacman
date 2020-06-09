package de.amr.games.pacman.controller;

import static de.amr.easy.game.Application.app;
import static de.amr.easy.game.Application.loginfo;
import static de.amr.games.pacman.PacManApp.settings;

import java.awt.event.KeyEvent;

import de.amr.easy.game.input.Keyboard;
import de.amr.easy.game.input.Keyboard.Modifier;
import de.amr.games.pacman.controller.actor.Ghost;
import de.amr.games.pacman.controller.actor.GhostState;
import de.amr.games.pacman.controller.event.GhostKilledEvent;
import de.amr.games.pacman.controller.event.LevelCompletedEvent;
import de.amr.games.pacman.view.theme.Theme;

/**
 * Enhanced game controller with all the bells and whistles.
 * 
 * @author Armin Reichert
 */
public class EnhancedGameController extends GameController {

	public EnhancedGameController(Theme theme) {
		super(theme);
	}

	@Override
	public void update() {
		handleInput();
		super.update();
	}

	private void handleInput() {
		if (Keyboard.keyPressedOnce(Modifier.CONTROL, KeyEvent.VK_LEFT)) {
			int oldFreq = app().clock().getTargetFramerate();
			changeClockFrequency(oldFreq <= 10 ? Math.max(1, oldFreq - 1) : oldFreq - 5);
		} else if (Keyboard.keyPressedOnce(Modifier.CONTROL, KeyEvent.VK_RIGHT)) {
			int oldFreq = app().clock().getTargetFramerate();
			changeClockFrequency(oldFreq < 10 ? oldFreq + 1 : oldFreq + 5);
		}
		if (currentView == playView) {
			if (Keyboard.keyPressedOnce("b")) {
				toggleGhostOnStage(game.blinky);
			} else if (Keyboard.keyPressedOnce("c")) {
				toggleGhostOnStage(game.clyde);
			} else if (Keyboard.keyPressedOnce("d")) {
				toggleDemoMode();
			} else if (Keyboard.keyPressedOnce("e")) {
				eatAllSimplePellets();
			} else if (Keyboard.keyPressedOnce("f")) {
				toggleGhostFrightenedBehavior();
			} else if (Keyboard.keyPressedOnce("g")) {
				playView.showGrid = !playView.showGrid;
			} else if (Keyboard.keyPressedOnce("i")) {
				toggleGhostOnStage(game.inky);
			} else if (Keyboard.keyPressedOnce("k")) {
				killAllGhosts();
			} else if (Keyboard.keyPressedOnce("l")) {
				toggleStateMachineLogging();
			} else if (Keyboard.keyPressedOnce("m")) {
				toggleMakePacManImmortable();
			} else if (Keyboard.keyPressedOnce("o")) {
				togglePacManOverflowBug();
			} else if (Keyboard.keyPressedOnce("p")) {
				toggleGhostOnStage(game.pinky);
			} else if (Keyboard.keyPressedOnce("s")) {
				playView.showStates = !playView.showStates;
			} else if (Keyboard.keyPressedOnce("t")) {
				playView.showFrameRate = !playView.showFrameRate;
			} else if (Keyboard.keyPressedOnce("r")) {
				playView.showRoutes = !playView.showRoutes;
			} else if (Keyboard.keyPressedOnce("x")) {
				toggleGhostsHarmless();
			} else if (Keyboard.keyPressedOnce("+")) {
				switchToNextLevel();
			}
		}
	}

	private void togglePacManOverflowBug() {
		settings.fixOverflowBug = !settings.fixOverflowBug;
		loginfo("Overflow bug is %s", settings.fixOverflowBug ? "fixed" : "active");
	}

	private void toggleStateMachineLogging() {
		PacManStateMachineLogging.toggle();
		loginfo("State machine logging changed to %s", PacManStateMachineLogging.LOGGER.getLevel());
	}

	private void toggleGhostFrightenedBehavior() {
		if (settings.ghostsSafeCorner) {
			settings.ghostsSafeCorner = false;
			game.ghosts().forEach(ghost -> ghost.behavior(GhostState.FRIGHTENED, ghost.isMovingRandomlyWithoutTurningBack()));
			loginfo("Ghost escape behavior is: Random movement");
		} else {
			settings.ghostsSafeCorner = true;
			game.ghosts().forEach(ghost -> ghost.behavior(GhostState.FRIGHTENED, ghost.isFleeingToSafeCorner(game.pacMan,
					game.maze.cornerNW(), game.maze.cornerNE(), game.maze.cornerSW(), game.maze.cornerSE())));
			loginfo("Ghosts escape behavior is: Fleeing to safe corners");
		}
	}

	private void toggleGhostsHarmless() {
		settings.ghostsHarmless = !settings.ghostsHarmless;
		loginfo("Ghosts are %s", settings.ghostsHarmless ? "harmless" : "dangerous");
	}

	private void toggleDemoMode() {
		settings.demoMode = !settings.demoMode;
		setDemoMode(settings.demoMode);
		loginfo("Demo mode is %s", settings.demoMode ? "on" : "off");
	}

	private void toggleMakePacManImmortable() {
		settings.pacManImmortable = !settings.pacManImmortable;
		loginfo("Pac-Man immortable = %s", settings.pacManImmortable);
	}

	private void switchToNextLevel() {
		loginfo("Switching to level %d", game.level.number + 1);
		enqueue(new LevelCompletedEvent());
	}

	private void eatAllSimplePellets() {
		game.maze.playingArea().filter(game.maze::isSimplePellet).forEach(tile -> {
			game.eatFood(tile, false);
			ghostHouse.onPacManFoundFood();
			ghostHouse.update();
		});
		loginfo("All simple pellets eaten");
	}

	private void toggleGhostOnStage(Ghost ghost) {
		if (game.stage.contains(ghost)) {
			game.stage.remove(ghost);
		} else {
			game.stage.add(ghost);
		}
	}

	private void killAllGhosts() {
		game.level.ghostsKilledByEnergizer = 0;
		game.ghostsOnStage().filter(ghost -> ghost.is(GhostState.CHASING, GhostState.SCATTERING, GhostState.FRIGHTENED))
				.forEach(ghost -> {
					game.scoreKilledGhost(ghost.name);
					ghost.process(new GhostKilledEvent(ghost));
				});
		loginfo("All ghosts killed");
	}
}