package de.amr.games.pacman.test.navigation;

import static de.amr.games.pacman.actor.GhostState.CHASING;
import static de.amr.games.pacman.actor.GhostState.FRIGHTENED;
import static de.amr.games.pacman.actor.behavior.ghost.GhostSteerings.followingShortestPath;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;

import de.amr.easy.game.input.Keyboard;
import de.amr.easy.game.view.View;
import de.amr.easy.game.view.ViewController;
import de.amr.games.pacman.actor.Ghost;
import de.amr.games.pacman.actor.PacManGameCast;
import de.amr.games.pacman.actor.behavior.Steering;
import de.amr.games.pacman.model.PacManGame;
import de.amr.games.pacman.model.Tile;
import de.amr.games.pacman.view.play.PlayView;

public class TakeShortestPathTestUI extends PlayView implements ViewController {

	private List<Tile> targets;
	private int currentTarget;

	public TakeShortestPathTestUI(PacManGame game, PacManGameCast cast) {
		super(game, cast);
		showRoutes = true;
		showStates = true;
		showScores = false;
		targets = Arrays.asList(game.maze.bottomRight, game.maze.bottomLeft, game.maze.tunnelLeftExit,
				game.maze.topLeft, game.maze.blinkyHome, game.maze.topRight, game.maze.tunnelRightExit,
				game.maze.pacManHome);
	}

	@Override
	public void init() {
		super.init();
		infoTextColor = Color.YELLOW;
		infoText = "SPACE toggles ghost state";
		currentTarget = 0;
		game.startLevel(1);
		game.maze.removeFood();
		cast.theme.snd_ghost_chase().volume(0);
		cast.blinky.activate();
		cast.blinky.init();
		cast.blinky.setState(CHASING);
		Steering<Ghost> followPathToCurrentTarget = followingShortestPath(() -> targets.get(currentTarget));
		cast.blinky.setSteering(CHASING, followPathToCurrentTarget);
		cast.blinky.setSteering(FRIGHTENED, followPathToCurrentTarget);
	}

	private void nextTarget() {
		currentTarget += 1;
		if (currentTarget == targets.size()) {
			currentTarget = 0;
			game.levelNumber += 1;
		}
	}

	@Override
	public void update() {
		if (Keyboard.keyPressedOnce(KeyEvent.VK_SPACE)) {
			cast.blinky.setState(cast.blinky.getState() == CHASING ? FRIGHTENED : CHASING);
		}
		cast.blinky.update();
		if (cast.blinky.tile().equals(targets.get(currentTarget))) {
			nextTarget();
		}
		super.update();
	}

	@Override
	public View currentView() {
		return this;
	}
}