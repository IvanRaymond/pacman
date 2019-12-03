package de.amr.games.pacman.test.navigation;

import static de.amr.games.pacman.actor.GhostState.CHASING;

import de.amr.easy.game.view.View;
import de.amr.easy.game.view.ViewController;
import de.amr.games.pacman.model.PacManGame;
import de.amr.games.pacman.theme.ClassicPacManTheme;
import de.amr.games.pacman.view.play.PlayViewXtended;

public class OutsideTileTestUI extends PlayViewXtended implements ViewController {

	public OutsideTileTestUI(PacManGame game) {
		super(game, new ClassicPacManTheme());
		showRoutes = true;
		showStates = true;
		showScores = false;
	}

	@Override
	public void init() {
		super.init();
		game.levelNumber = 1;
		theme.snd_ghost_chase().volume(0);
		game.blinky.activate();
		game.blinky.fnChasingTarget = () -> game.maze.tileAt(100, game.maze.tunnelRightExit.row);
		game.blinky.init();
		game.blinky.setState(CHASING);
	}

	@Override
	public void update() {
		game.blinky.update();
		super.update();
	}

	@Override
	public View currentView() {
		return this;
	}
}