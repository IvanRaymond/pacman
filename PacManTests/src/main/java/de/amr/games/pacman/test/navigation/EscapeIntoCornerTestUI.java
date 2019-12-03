package de.amr.games.pacman.test.navigation;

import static de.amr.games.pacman.actor.behavior.ghost.GhostSteerings.fleeingToSafeCorner;

import de.amr.easy.game.view.View;
import de.amr.easy.game.view.ViewController;
import de.amr.games.pacman.actor.GhostState;
import de.amr.games.pacman.model.PacManGame;
import de.amr.games.pacman.theme.ClassicPacManTheme;
import de.amr.games.pacman.view.play.PlayViewXtended;

public class EscapeIntoCornerTestUI extends PlayViewXtended implements ViewController {

	public EscapeIntoCornerTestUI(PacManGame game) {
		super(game, new ClassicPacManTheme());
		showRoutes = true;
		showStates = true;
		showScores = false;
	}

	@Override
	public void init() {
		super.init();
		game.levelNumber = 1;
		game.maze.removeFood();
		game.pacMan.activate();
		game.pacMan.init();
		game.blinky.activate();
		game.blinky.setSteering(GhostState.FRIGHTENED, fleeingToSafeCorner(game.pacMan));
		game.blinky.init();
		game.blinky.setState(GhostState.FRIGHTENED);
	}

	@Override
	public void update() {
		game.pacMan.update();
		game.blinky.update();
		super.update();
	}

	@Override
	public View currentView() {
		return this;
	}
}