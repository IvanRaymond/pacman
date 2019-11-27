package de.amr.games.pacman.test.navigation;

import de.amr.easy.game.view.View;
import de.amr.easy.game.view.ViewController;
import de.amr.games.pacman.actor.Ghost;
import de.amr.games.pacman.actor.GhostState;
import de.amr.games.pacman.model.PacManGame;
import de.amr.games.pacman.view.play.PlayViewXtended;

public class InkyChaseTestUI extends PlayViewXtended implements ViewController {

	public InkyChaseTestUI(PacManGame game) {
		super(game);
		showRoutes = true;
		showStates = true;
		showScores = false;
	}

	@Override
	public void init() {
		super.init();
		game.level = 1;
		game.maze.removeFood();
		game.pacMan.init();
		game.activateActor(game.pinky, false);
		game.activateActor(game.clyde, false);
		game.activeGhosts().forEach(ghost -> {
			ghost.init();
			ghost.fnIsUnlocked = () -> true;
			ghost.fnNextState = () -> GhostState.CHASING;
		});
	}

	@Override
	public void update() {
		game.pacMan.update();
		game.activeGhosts().forEach(Ghost::update);
		super.update();
	}

	@Override
	public View currentView() {
		return this;
	}
}