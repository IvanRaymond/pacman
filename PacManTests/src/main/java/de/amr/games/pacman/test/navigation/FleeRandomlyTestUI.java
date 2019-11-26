package de.amr.games.pacman.test.navigation;

import static de.amr.games.pacman.actor.GhostState.FRIGHTENED;

import de.amr.easy.game.view.View;
import de.amr.easy.game.view.ViewController;
import de.amr.games.pacman.actor.Ghost;
import de.amr.games.pacman.model.PacManGame;
import de.amr.games.pacman.view.play.PlayViewXtended;

public class FleeRandomlyTestUI extends PlayViewXtended implements ViewController {

	public FleeRandomlyTestUI(PacManGame game) {
		super(game);
		showRoutes = true;
		showStates = true;
		scoresVisible = false;
	}

	@Override
	public void init() {
		super.init();
		game.level = 1;
		game.maze.removeFood();
		game.setActive(game.pacMan, false);
		game.ghosts().forEach(ghost -> {
			ghost.init();
			ghost.setState(FRIGHTENED);
		});
		game.blinky.placeAtTile(game.maze.topLeft, 0, 0);
		game.pinky.placeAtTile(game.maze.topRight, 0, 0);
		game.inky.placeAtTile(game.maze.bottomLeft, 0, 0);
		game.clyde.placeAtTile(game.maze.bottomRight, 0, 0);
	}

	@Override
	public void update() {
		game.activeGhosts().forEach(Ghost::update);
		super.update();
	}

	@Override
	public View currentView() {
		return this;
	}
}