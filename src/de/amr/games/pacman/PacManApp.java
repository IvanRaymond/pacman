package de.amr.games.pacman;

import java.util.logging.Level;

import de.amr.easy.game.Application;
import de.amr.easy.game.ui.FullScreen;
import de.amr.games.pacman.controller.GameController;
import de.amr.games.pacman.model.Game;
import de.amr.games.pacman.theme.ClassicPacManTheme;
import de.amr.games.pacman.theme.PacManTheme;

/**
 * Pac-Man game.
 * 
 * @see <a href="https://github.com/armin-reichert/pacman">GitHub</a>
 * 
 * @author Armin Reichert
 */
public class PacManApp extends Application {

	public static PacManTheme THEME;

	public static void main(String[] args) {
		LOGGER.setLevel(Level.INFO);
		launch(new PacManApp(), args);
	}

	public PacManApp() {
		settings.width = 28 * Game.TS;
		settings.height = 36 * Game.TS;
		settings.scale = 2;
		settings.title = "Armin's Pac-Man";
		settings.fullScreenMode = FullScreen.Mode(800, 600, 32);
		settings.fullScreenOnStart = false;
		try {
			THEME = ClassicPacManTheme.class.newInstance();
			LOGGER.info(String.format("Theme '%s' created.", THEME.getClass().getSimpleName()));
		} catch (Exception e) {
			LOGGER.info("Could not create theme.");
			throw new RuntimeException(e);
		}
	}

	@Override
	public void init() {
		setController(new GameController());
	}
}