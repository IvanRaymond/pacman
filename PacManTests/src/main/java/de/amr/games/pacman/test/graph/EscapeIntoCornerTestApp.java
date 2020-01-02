package de.amr.games.pacman.test.graph;

import static de.amr.games.pacman.actor.GhostState.FRIGHTENED;
import static de.amr.games.pacman.actor.PacManState.ALIVE;

import java.util.Optional;

import de.amr.easy.game.view.View;
import de.amr.easy.game.view.VisualController;
import de.amr.games.pacman.PacManApp;
import de.amr.games.pacman.actor.Cast;
import de.amr.games.pacman.actor.core.MazeResident;
import de.amr.games.pacman.model.Game;
import de.amr.games.pacman.theme.ArcadeTheme;
import de.amr.games.pacman.theme.Theme;
import de.amr.games.pacman.view.play.PlayView;

public class EscapeIntoCornerTestApp extends PacManApp {

	public static void main(String[] args) {
		launch(new EscapeIntoCornerTestApp(), args);
	}

	public EscapeIntoCornerTestApp() {
		settings.title = "Escape Into Corner";
	}

	@Override
	public void init() {
		Game game = new Game();
		Theme theme = new ArcadeTheme();
		Cast ensemble = new Cast(game, theme);
		setController(new EscapeIntoCornerTestUI(game, ensemble));
	}
}

class EscapeIntoCornerTestUI extends PlayView implements VisualController {

	public EscapeIntoCornerTestUI(Game game, Cast cast) {
		super(cast);
		showRoutes = () -> true;
		showStates = () -> true;
		showScores = () -> false;
	}

	@Override
	public void init() {
		super.init();
		maze().removeFood();
		cast().setActorOnStage(cast().pacMan);
		cast().pacMan.init();
		cast().pacMan.setState(ALIVE);
		cast().setActorOnStage(cast().blinky);
		cast().blinky.during(FRIGHTENED, cast().blinky.isFleeingToSafeCorner(cast().pacMan));
		cast().blinky.init();
		cast().blinky.setState(FRIGHTENED);
	}

	@Override
	public void update() {
		super.update();
		cast().actorsOnStage().forEach(MazeResident::update);
	}

	@Override
	public Optional<View> currentView() {
		return Optional.of(this);
	}
}