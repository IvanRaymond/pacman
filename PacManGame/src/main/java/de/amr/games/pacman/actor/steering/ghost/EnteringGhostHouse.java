package de.amr.games.pacman.actor.steering.ghost;

import static de.amr.games.pacman.actor.steering.ghost.EnteringGhostHouse.EnteringHouseState.AT_DOOR;
import static de.amr.games.pacman.actor.steering.ghost.EnteringGhostHouse.EnteringHouseState.AT_PLACE;
import static de.amr.games.pacman.actor.steering.ghost.EnteringGhostHouse.EnteringHouseState.FALLING;
import static de.amr.games.pacman.actor.steering.ghost.EnteringGhostHouse.EnteringHouseState.MOVING_LEFT;
import static de.amr.games.pacman.actor.steering.ghost.EnteringGhostHouse.EnteringHouseState.MOVING_RIGHT;

import de.amr.easy.game.math.Vector2f;
import de.amr.games.pacman.actor.Ghost;
import de.amr.games.pacman.actor.steering.Steering;
import de.amr.games.pacman.actor.steering.ghost.EnteringGhostHouse.EnteringHouseState;
import de.amr.games.pacman.model.Direction;
import de.amr.games.pacman.model.Game;
import de.amr.statemachine.core.StateMachine;

/**
 * A state machine steering a ghost when entering the ghost house.
 * 
 * @author Armin Reichert
 */
public class EnteringGhostHouse extends StateMachine<EnteringHouseState, Void> implements Steering {

	public enum EnteringHouseState {
		AT_DOOR, FALLING, MOVING_LEFT, MOVING_RIGHT, AT_PLACE
	}

	public EnteringGhostHouse(Ghost ghost, Vector2f seatPosition) {
		super(EnteringHouseState.class);
		/*@formatter:off*/
		beginStateMachine()
			.initialState(AT_DOOR)
			.description(String.format("[%s entering house]", ghost.name()))

			.states()
			
				.state(AT_DOOR)
					.onEntry(() -> {
						// target tile is only used for route visualization
						ghost.setTargetTile(ghost.maze().tileAt(seatPosition.roundedX(), seatPosition.roundedY()));
						ghost.setWishDir(Direction.DOWN);
					})
					
			.transitions()
	
				.when(AT_DOOR).then(FALLING)
					.act(() -> ghost.setWishDir(Direction.DOWN))
	
				.when(FALLING).then(MOVING_LEFT)
					.condition(() -> ghost.tf.getY() >= seatPosition.y && ghost.tf.getX() > seatPosition.x)
					.act(() -> ghost.setWishDir(Direction.LEFT))
				
				.when(FALLING).then(MOVING_RIGHT)
					.condition(() -> ghost.tf.getY() >= seatPosition.y && ghost.tf.getX() < seatPosition.x)
					.act(() -> ghost.setWishDir(Direction.RIGHT))
	
				.when(FALLING).then(AT_PLACE)
					.condition(() -> ghost.tf.getY() >= seatPosition.y && ghost.tf.getX() == seatPosition.x)
				
				.when(MOVING_LEFT).then(AT_PLACE)
					.condition(() -> ghost.tf.getX() <= seatPosition.x)
					
				.when(MOVING_RIGHT).then(AT_PLACE)
					.condition(() -> ghost.tf.getX() >= seatPosition.x)
					
		.endStateMachine();
		/*@formatter:on*/
		setLogger(Game.FSM_LOGGER);
	}

	@Override
	public void steer() {
		update();
	}

	@Override
	public boolean isComplete() {
		return is(AT_PLACE);
	}

	@Override
	public void force() {
	}

	@Override
	public boolean requiresGridAlignment() {
		return false;
	}

	@Override
	public void enableTargetPathComputation(boolean b) {
	}
}