package de.amr.games.pacman.test.navigation;

import de.amr.easy.game.Application;
import de.amr.games.pacman.actor.Ensemble;
import de.amr.games.pacman.model.PacManGame;
import de.amr.games.pacman.theme.ClassicPacManTheme;
import de.amr.games.pacman.theme.PacManTheme;

public class LeaveGhostHouseTestApp extends Application {

	public static void main(String[] args) {
		launch(new LeaveGhostHouseTestApp(), args);
	}

	public LeaveGhostHouseTestApp() {
		settings.title = "Leave Ghost House";
		settings.width = 28 * PacManGame.TS;
		settings.height = 36 * PacManGame.TS;
		settings.scale = 2;
	}

	@Override
	public void init() {
		clock.setFrequency(10);
		PacManGame game = new PacManGame();
		PacManTheme theme = new ClassicPacManTheme();
		Ensemble ensemble = new Ensemble(game, game.maze, theme);
		setController(new LeaveGhostHouseTestUI(game, ensemble));
	}
}