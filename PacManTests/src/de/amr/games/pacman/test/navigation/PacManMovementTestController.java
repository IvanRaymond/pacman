package de.amr.games.pacman.test.navigation;

import de.amr.easy.game.Application;
import de.amr.easy.game.assets.Assets;
import de.amr.easy.game.view.View;
import de.amr.easy.game.view.ViewController;
import de.amr.games.pacman.controller.event.FoodFoundEvent;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.PacManGame;
import de.amr.games.pacman.theme.PacManTheme;
import de.amr.games.pacman.view.play.PlayViewX;

public class PacManMovementTestController implements ViewController {

	private final PacManGame game;
	private final PlayViewX view;

	public PacManMovementTestController() {
		Maze maze = new Maze(Assets.text("maze.txt"));
		game = new PacManGame(maze);
		view = new PlayViewX(game);
		view.setShowRoutes(true);
		view.setShowGrid(true);
		view.setShowStates(false);
		view.setScoresVisible(false);
	}

	private PacManTheme getTheme() {
		return Application.app().settings.get("theme");
	}

	@Override
	public void init() {
		game.setLevel(1);
		// game.getMaze().tiles().filter(game.getMaze()::isFood).forEach(game::eatFoodAtTile);
		game.getPacMan().getEventManager().subscribe(event -> {
			if (event.getClass() == FoodFoundEvent.class) {
				FoodFoundEvent foodFound = (FoodFoundEvent) event;
				getTheme().snd_eatPill().play();
				game.getMaze().hideFood(foodFound.tile);
			}
		});
		game.getGhosts().forEach(ghost -> game.setActive(ghost, false));
		game.setActive(game.getPacMan(), true);
		game.getPacMan().init();
	}

	@Override
	public void update() {
		game.getPacMan().update();
		view.update();
	}

	@Override
	public View currentView() {
		return view;
	}
}