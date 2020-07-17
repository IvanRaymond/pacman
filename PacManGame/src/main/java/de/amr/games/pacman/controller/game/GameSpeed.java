package de.amr.games.pacman.controller.game;

import de.amr.games.pacman.controller.creatures.ghost.Ghost;
import de.amr.games.pacman.controller.creatures.pacman.PacMan;
import de.amr.games.pacman.controller.creatures.pacman.PacManState;
import de.amr.games.pacman.model.game.Game;

public class GameSpeed {

	/**
	 * In Shaun William's <a href="https://github.com/masonicGIT/pacman">Pac-Man remake</a> there is a
	 * speed table giving the number of steps (=pixels?) which Pac-Man is moving in 16 frames. In level
	 * 5, he uses 4 * 2 + 12 = 20 steps in 16 frames, which is 1.25 pixels/frame. The table from
	 * Gamasutra ({@link Game#LEVELS}) states that this corresponds to 100% base speed for Pac-Man at
	 * level 5. Therefore I use 1.25 pixel/frame for 100% speed.
	 */
	public static final float BASE_SPEED = 1.25f;

	/**
	 * @param fraction fraction of base speed
	 * @return speed (pixels/tick) corresponding to given fraction of base speed
	 */
	public static float speed(float fraction) {
		return fraction * BASE_SPEED;
	}

	public static float ghostSpeed(Ghost ghost, Game game) {
		switch (ghost.getState()) {
		case LOCKED:
			return speed(ghost.isInsideHouse() ? game.level.ghostSpeed / 2 : 0);
		case LEAVING_HOUSE:
			return speed(game.level.ghostSpeed / 2);
		case ENTERING_HOUSE:
			return speed(game.level.ghostSpeed);
		case CHASING:
		case SCATTERING:
			if (ghost.world().isTunnel(ghost.tileLocation())) {
				return speed(game.level.ghostTunnelSpeed);
			}
			switch (ghost.getSanity()) {
			case ELROY1:
				return speed(game.level.elroy1Speed);
			case ELROY2:
				return speed(game.level.elroy2Speed);
			case INFECTABLE:
			case IMMUNE:
				return speed(game.level.ghostSpeed);
			default:
				throw new IllegalArgumentException("Illegal ghost sanity state: " + ghost.getSanity());
			}
		case FRIGHTENED:
			return speed(
					ghost.world().isTunnel(ghost.tileLocation()) ? game.level.ghostTunnelSpeed : game.level.ghostFrightenedSpeed);
		case DEAD:
			return speed(2 * game.level.ghostSpeed);
		default:
			throw new IllegalStateException(String.format("Illegal ghost state %s", ghost.getState()));
		}
	}

	public static float pacManSpeed(PacMan pacMan, Game game) {
		if (pacMan.is(PacManState.SLEEPING, PacManState.DEAD)) {
			return 0;
		}
		return pacMan.getPower() > 0 ? speed(game.level.pacManPowerSpeed) : speed(game.level.pacManSpeed);
	}
}