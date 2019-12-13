package de.amr.games.pacman.test.navigation;

import de.amr.easy.game.Application;
import de.amr.games.pacman.actor.PacManGameCast;
import de.amr.games.pacman.model.PacManGame;
import de.amr.games.pacman.model.Tile;
import de.amr.games.pacman.theme.ClassicPacManTheme;
import de.amr.games.pacman.theme.PacManTheme;

public class FollowMouseTestApp extends Application {

	public static void main(String[] args) {
		launch(new FollowMouseTestApp(), args);
	}

	public FollowMouseTestApp() {
		settings.title = "Follow Mouse";
		settings.width = 28 * Tile.SIZE;
		settings.height = 36 * Tile.SIZE;
		settings.scale = 2;
	}

	@Override
	public void init() {
		PacManGame game = new PacManGame();
		PacManTheme theme = new ClassicPacManTheme();
		PacManGameCast cast = new PacManGameCast(game, theme);
		setController(new FollowMouseTestUI(cast));
	}
}