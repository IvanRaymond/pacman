package de.amr.games.pacman.controller.steering.common;

import static de.amr.games.pacman.model.world.api.Direction.dirs;

import java.util.EnumMap;

import de.amr.easy.game.input.Keyboard;
import de.amr.games.pacman.controller.creatures.Guy;
import de.amr.games.pacman.controller.steering.api.Steering;
import de.amr.games.pacman.model.world.api.Direction;

/**
 * Steers a mover using the keyboard keys for UP, RIGHT, DOWN, LEFT.
 * 
 * @author Armin Reichert
 */
public class FollowingKeys implements Steering {

	private EnumMap<Direction, Integer> keys = new EnumMap<>(Direction.class);

	public FollowingKeys(int upKey, int rightKey, int downKey, int leftKey) {
		keys.put(Direction.UP, upKey);
		keys.put(Direction.RIGHT, rightKey);
		keys.put(Direction.DOWN, downKey);
		keys.put(Direction.LEFT, leftKey);
	}

	@Override
	public void steer(Guy guy) {
		dirs().filter(dir -> Keyboard.keyDown(keys.get(dir))).findAny().ifPresent(dir -> guy.wishDir = dir);
	}

	@Override
	public boolean requiresGridAlignment() {
		return true;
	}
}