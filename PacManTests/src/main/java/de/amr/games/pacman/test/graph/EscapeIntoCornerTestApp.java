package de.amr.games.pacman.test.graph;

import static de.amr.games.pacman.actor.GhostState.FRIGHTENED;
import static de.amr.games.pacman.actor.PacManState.EATING;

import java.util.Optional;

import de.amr.easy.game.config.AppSettings;
import de.amr.easy.game.view.View;
import de.amr.easy.game.view.VisualController;
import de.amr.games.pacman.PacManApp;
import de.amr.games.pacman.actor.core.MovingActor;
import de.amr.games.pacman.model.Game;
import de.amr.games.pacman.theme.ArcadeTheme;
import de.amr.games.pacman.theme.Theme;
import de.amr.games.pacman.view.play.PlayView;

public class EscapeIntoCornerTestApp extends PacManApp {

	public static void main(String[] args) {
		launch(EscapeIntoCornerTestApp.class, args);
	}

	@Override
	protected void configure(AppSettings settings) {
		super.configure(settings);
		settings.title = "Escape Into Corner";
	}

	@Override
	public void init() {
		Game game = new Game();
		Theme theme = new ArcadeTheme();
		setController(new EscapeIntoCornerTestUI(game, theme));
	}
}

class EscapeIntoCornerTestUI extends PlayView implements VisualController {

	public EscapeIntoCornerTestUI(Game game, Theme theme) {
		super(game, theme);
		showRoutes = () -> true;
		showStates = () -> true;
		showScores = () -> false;
	}

	@Override
	public void init() {
		super.init();
		game.maze.removeFood();
		game.pacMan.setActing(true);
		game.pacMan.init();
		game.pacMan.setState(EATING);
		game.blinky.setActing(true);
		game.blinky.behavior(FRIGHTENED, game.blinky.isFleeingToSafeCorner(game.pacMan));
		game.blinky.init();
		game.blinky.setState(FRIGHTENED);
	}

	@Override
	public void update() {
		super.update();
		game.movingActorsOnStage().forEach(MovingActor::update);
	}

	@Override
	public Optional<View> currentView() {
		return Optional.of(this);
	}
}