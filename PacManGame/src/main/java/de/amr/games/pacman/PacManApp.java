package de.amr.games.pacman;

import java.util.ResourceBundle;
import java.util.logging.Level;

import com.beust.jcommander.Parameter;

import de.amr.easy.game.Application;
import de.amr.easy.game.config.AppSettings;
import de.amr.games.pacman.controller.GameController;
import de.amr.games.pacman.controller.PacManStateMachineLogging;
import de.amr.games.pacman.model.Tile;
import de.amr.games.pacman.theme.ArcadeTheme;
import de.amr.games.pacman.theme.Theme;

/**
 * The Pac-Man game application.
 * 
 * @see <a href="https://github.com/armin-reichert/pacman">GitHub</a>
 * 
 * @author Armin Reichert
 */
public class PacManApp extends Application {

	public static class Settings extends AppSettings {

		@Parameter(names = { "-demoMode" }, description = "Pac-Man moves automatically")
		public boolean demoMode = false;

		@Parameter(names = {
				"-ghostsFleeRandomly" }, description = "frightened ghosts flee randomly (true) or into safe corner (false)", arity = 1)
		public boolean ghostsFleeRandomly = true;

		@Parameter(names = { "-ghostsDangerous" }, description = "ghosts kill Pac-Man on collision", arity = 1)
		public boolean ghostsDangerous = true;

		@Parameter(names = { "-pathFinder" }, description = "used path finding algorithm (astar, bfs, bestfs)")
		public String pathFinder = "astar";

		@Parameter(names = {
				"-overflowBug" }, description = "enable overflow bug as in the original Arcade game", arity = 1)
		public boolean overflowBug = true;

		@Parameter(names = { "-pacManImmortable" }, description = "Pac-Man stays alive when killed by ghost")
		public boolean pacManImmortable = false;

		@Parameter(names = { "-skipIntro" }, description = "game starts without intro screen")
		public boolean skipIntro = false;

		@Parameter(names = { "-startLevel" }, description = "game starts in specified level")
		public int startLevel = 1;
	}

	public static final ResourceBundle texts = ResourceBundle.getBundle("texts");

	public static final Settings settings = new Settings();

	public static void main(String[] args) {
		launch(PacManApp.class, settings, args);
	}

	@Override
	protected void configure(AppSettings settings) {
		settings.width = 28 * Tile.SIZE;
		settings.height = 36 * Tile.SIZE;
		settings.scale = 2;
		settings.title = texts.getString("app.title");
	}

	@Override
	protected void printSettings() {
		super.printSettings();
		loginfo("\tGhosts dangerous: %s", settings.ghostsDangerous);
		loginfo("\tGhosts flee randomly: %s", settings.ghostsFleeRandomly);
		loginfo("\tPath finder: %s", settings.pathFinder);
		loginfo("\tOverflow Bug: %s", settings.overflowBug);
		loginfo("\tPacMan immortable: %s", settings.pacManImmortable);
		loginfo("\tSkip Intro: %s", settings.skipIntro);
		loginfo("\tStart level: %d", settings.startLevel);
	}

	@Override
	public void init() {
		loginfo("User language is %s", texts.getLocale().getDisplayLanguage());
		Theme theme = new ArcadeTheme(); // only theme existing yet
		setIcon(theme.spr_ghostFrightened().frame(0));
		PacManStateMachineLogging.setLevel(Level.INFO);
		GameController gameController = new GameController(theme);
		setExitHandler(app -> gameController.saveHiscore());
		setController(gameController);
	}
}