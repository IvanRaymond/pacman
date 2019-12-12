package de.amr.games.pacman.test.navigation;

import static de.amr.games.pacman.actor.GhostState.CHASING;

import de.amr.easy.game.input.Mouse;
import de.amr.easy.game.view.View;
import de.amr.easy.game.view.VisualController;
import de.amr.games.pacman.actor.PacManGameCast;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.Tile;
import de.amr.games.pacman.view.play.PlayView;

public class FollowMouseTestUI extends PlayView implements VisualController {

	private Tile mouseTile;

	public FollowMouseTestUI(PacManGameCast cast) {
		super(cast);
		showRoutes = true;
		showGrid = true;
		showStates = false;
		showScores = false;
	}

	@Override
	public View currentView() {
		return this;
	}

	@Override
	public void init() {
		super.init();
		game.reset();
		game.maze.removeFood();
		cast.theme.snd_ghost_chase().volume(0);
		cast.activate(cast.blinky);
		cast.blinky.init();
		cast.blinky.fnChasingTarget = () -> mouseTile;
		cast.blinky.setState(CHASING);
		mouseTile = game.maze.tileAt(Mouse.getX() / Maze.TS, Mouse.getY() / Maze.TS);
	}

	@Override
	public void update() {
		if (Mouse.moved()) {
			mouseTile = game.maze.tileAt(Mouse.getX() / Maze.TS, Mouse.getY() / Maze.TS);
		}
		cast.blinky.update();
		super.update();
	}
}