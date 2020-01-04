package de.amr.games.pacman.test.navigation;

import static de.amr.games.pacman.actor.GhostState.CHASING;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.amr.easy.game.view.View;
import de.amr.easy.game.view.VisualController;
import de.amr.games.pacman.PacManApp;
import de.amr.games.pacman.actor.Cast;
import de.amr.games.pacman.model.Game;
import de.amr.games.pacman.model.Tile;
import de.amr.games.pacman.theme.ArcadeTheme;
import de.amr.games.pacman.theme.Theme;
import de.amr.games.pacman.view.core.Pen;
import de.amr.games.pacman.view.play.PlayView;

public class FollowTargetTilesTestApp extends PacManApp {

	public static void main(String[] args) {
		launch(new FollowTargetTilesTestApp(), args);
	}

	public FollowTargetTilesTestApp() {
		settings.title = "Follow Target Tiles";
	}

	@Override
	public void init() {
		Game game = new Game();
		Theme theme = new ArcadeTheme();
		Cast cast = new Cast(game, theme);
		setController(new FollowTargetTilesTestUI(cast));
	}
}

class FollowTargetTilesTestUI extends PlayView implements VisualController {

	private List<Tile> targets;
	private int current;

	public FollowTargetTilesTestUI(Cast cast) {
		super(cast);
		showRoutes = () -> true;
		showStates = () -> false;
		showScores = () -> false;
		showGrid = () -> true;
		targets = Arrays.asList(maze().cornerNW, maze().ghostHouseSeats[0], maze().cornerNE, maze().cornerSE,
				maze().pacManHome, maze().cornerSW);
	}

	@Override
	public Optional<View> currentView() {
		return Optional.of(this);
	}

	@Override
	public void init() {
		super.init();
		current = 0;
		maze().removeFood();
		theme().snd_ghost_chase().volume(0);
		cast().setActorOnStage(cast().blinky);
		cast().blinky.placeAt(targets.get(0));
		cast().blinky.during(CHASING, cast().blinky.isHeadingFor(() -> targets.get(current)));
		cast().blinky.setState(CHASING);
		cast().blinky.steering().force();
	}

	@Override
	public void update() {
		if (cast().blinky.tile() == targets.get(current)) {
			current += 1;
			if (current == targets.size()) {
				current = 0;
				game().enterLevel(game().level().number + 1);
				maze().removeFood();
			}
		}
		cast().blinky.update();
		super.update();
	}

	@Override
	public void draw(Graphics2D g) {
		super.draw(g);
		try (Pen pen = new Pen(g)) {
			pen.color(Color.YELLOW);
			pen.fontSize(8);
			for (int i = 0; i < targets.size(); ++i) {
				Tile target = targets.get(i);
				pen.drawAtTilePosition(target.col, target.row, "" + i);
			}
		}
	}
}