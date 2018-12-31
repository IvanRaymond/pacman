package de.amr.games.pacman.test.navigation;

import static de.amr.easy.game.Application.LOGGER;

import de.amr.easy.game.input.Mouse;
import de.amr.easy.game.view.View;
import de.amr.easy.game.view.ViewController;
import de.amr.games.pacman.actor.Ghost;
import de.amr.games.pacman.actor.GhostState;
import de.amr.games.pacman.actor.PacMan;
import de.amr.games.pacman.model.PacManGame;
import de.amr.games.pacman.model.Tile;
import de.amr.games.pacman.view.play.PlayViewX;

public class FollowMouseTestController implements ViewController {

	private final PacManGame game;
	private final PacMan pacMan;
	private final Ghost blinky;
	private final PlayViewX view;
	private Tile targetTile;

	public FollowMouseTestController() {
		game = new PacManGame();
		pacMan = game.getPacMan();
		blinky = game.getBlinky();
		view = new PlayViewX(game);
		view.setShowRoutes(true);
		view.setShowGrid(true);
		view.setShowStates(false);
		view.setScoresVisible(false);
	}

	@Override
	public void init() {
		targetTile = game.getMaze().getPacManHome();
		pacMan.placeAtTile(targetTile, 0, 0);
		game.setLevel(1);
		game.getMaze().tiles().filter(game.getMaze()::isFood).forEach(game::eatFoodAtTile);
		game.getAllGhosts().forEach(ghost -> game.setActorActive(ghost, false));
		game.setActorActive(blinky, true);
		game.setActorActive(pacMan, true);
		blinky.init();
		blinky.setState(GhostState.CHASING);
		blinky.setBehavior(GhostState.CHASING, blinky.followRoute(() -> targetTile));
	}

	@Override
	public void update() {
		readTargetTile();
		blinky.update();
		view.update();
	}

	private void readTargetTile() {
		if (Mouse.moved()) {
			int x = Mouse.getX(), y = Mouse.getY();
			targetTile = new Tile(x / PacManGame.TS, y / PacManGame.TS);
			pacMan.placeAtTile(targetTile, 0, 0);
			LOGGER.info(targetTile.toString());
		}
	}

	@Override
	public View currentView() {
		return view;
	}
}