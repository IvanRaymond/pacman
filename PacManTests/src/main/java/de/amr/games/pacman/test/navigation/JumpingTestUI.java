package de.amr.games.pacman.test.navigation;

import de.amr.easy.game.view.View;
import de.amr.easy.game.view.VisualController;
import de.amr.games.pacman.actor.Ghost;
import de.amr.games.pacman.actor.PacManGameCast;
import de.amr.games.pacman.view.play.PlayView;

public class JumpingTestUI extends PlayView implements VisualController {

	public JumpingTestUI(PacManGameCast cast) {
		super(cast);
		showRoutes = false;
		showStates = true;
		showScores = false;
	}

	@Override
	public void init() {
		super.init();
		game.newGame();
		game.maze.removeFood();
		cast.ghosts().forEach(ghost -> {
			ghost.activate();
			ghost.init();
		});
	}

	@Override
	public void update() {
		cast.activeGhosts().forEach(Ghost::update);
		super.update();
	}

	@Override
	public View currentView() {
		return this;
	}
}