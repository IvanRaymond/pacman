package de.amr.games.pacman.controller.steering.common;

import static de.amr.games.pacman.model.world.api.Direction.dirs;

import java.util.EnumMap;
import java.util.Objects;

import de.amr.easy.game.input.Keyboard;
import de.amr.games.pacman.controller.steering.api.Steering;
import de.amr.games.pacman.model.world.api.Direction;
import de.amr.games.pacman.model.world.api.MobileLifeform;

/**
 * Steering controlling a creature using the keyboard keys for UP, RIGHT, DOWN, LEFT.
 * 
 * @author Armin Reichert
 */
public class FollowingKeys implements Steering {

	private MobileLifeform creature;
	private EnumMap<Direction, Integer> keys = new EnumMap<>(Direction.class);

	public FollowingKeys(MobileLifeform creature, int upKey, int rightKey, int downKey, int leftKey) {
		this.creature = Objects.requireNonNull(creature);
		keys.put(Direction.UP, upKey);
		keys.put(Direction.RIGHT, rightKey);
		keys.put(Direction.DOWN, downKey);
		keys.put(Direction.LEFT, leftKey);
	}

	@Override
	public void steer() {
		dirs().filter(dir -> Keyboard.keyDown(keys.get(dir))).findAny().ifPresent(creature::setWishDir);
	}

	@Override
	public boolean requiresGridAlignment() {
		return true;
	}
}