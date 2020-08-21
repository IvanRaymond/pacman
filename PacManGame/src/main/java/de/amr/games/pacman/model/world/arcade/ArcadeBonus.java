package de.amr.games.pacman.model.world.arcade;

import de.amr.games.pacman.model.world.api.TemporaryFood;
import de.amr.games.pacman.model.world.components.Tile;

/**
 * Symbols appearing as bonus food in the Arcade game.
 * 
 * @author Armin Reichert
 */
public enum ArcadeBonus implements TemporaryFood {

	CHERRIES, STRAWBERRY, PEACH, APPLE, GRAPES, GALAXIAN, BELL, KEY;

	private Tile location;
	private int value;
	private boolean active;
	private boolean consumed;

	@Override
	public Tile location() {
		return location;
	}

	@Override
	public int value() {
		return value;
	}

	@Override
	public boolean isConsumed() {
		return consumed;
	}

	@Override
	public void consume() {
		consumed = true;
	}

	@Override
	public boolean isActive() {
		return active;
	}

	@Override
	public void activate() {
		active = true;
		consumed = false;
	}

	@Override
	public void deactivate() {
		active = false;
		consumed = false;
	}

	public void setLocation(Tile location) {
		this.location = location;
	}

	public void setValue(int value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return String.format("(%s,%s,present=%s,consumed=%s)", name(), value, active, consumed);
	}
}