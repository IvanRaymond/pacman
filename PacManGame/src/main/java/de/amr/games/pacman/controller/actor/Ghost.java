package de.amr.games.pacman.controller.actor;

import static de.amr.easy.game.Application.loginfo;
import static de.amr.games.pacman.controller.actor.GhostState.CHASING;
import static de.amr.games.pacman.controller.actor.GhostState.DEAD;
import static de.amr.games.pacman.controller.actor.GhostState.ENTERING_HOUSE;
import static de.amr.games.pacman.controller.actor.GhostState.FRIGHTENED;
import static de.amr.games.pacman.controller.actor.GhostState.LEAVING_HOUSE;
import static de.amr.games.pacman.controller.actor.GhostState.LOCKED;
import static de.amr.games.pacman.controller.actor.GhostState.SCATTERING;
import static de.amr.games.pacman.model.Direction.UP;
import static de.amr.games.pacman.model.Game.sec;
import static de.amr.games.pacman.model.Game.speed;

import java.util.EnumMap;

import de.amr.easy.game.math.Vector2f;
import de.amr.easy.game.ui.sprites.Sprite;
import de.amr.games.pacman.controller.PacManStateMachineLogging;
import de.amr.games.pacman.controller.actor.steering.Steering;
import de.amr.games.pacman.controller.actor.steering.ghost.EnteringGhostHouse;
import de.amr.games.pacman.controller.actor.steering.ghost.FleeingToSafeCorner;
import de.amr.games.pacman.controller.actor.steering.ghost.JumpingUpAndDown;
import de.amr.games.pacman.controller.actor.steering.ghost.LeavingGhostHouse;
import de.amr.games.pacman.controller.event.GhostKilledEvent;
import de.amr.games.pacman.controller.event.GhostUnlockedEvent;
import de.amr.games.pacman.controller.event.PacManGainsPowerEvent;
import de.amr.games.pacman.controller.event.PacManGameEvent;
import de.amr.games.pacman.controller.event.PacManGhostCollisionEvent;
import de.amr.games.pacman.model.Direction;
import de.amr.games.pacman.model.Game;
import de.amr.games.pacman.model.Tile;
import de.amr.games.pacman.view.theme.Theme;
import de.amr.statemachine.core.StateMachine;
import de.amr.statemachine.core.StateMachine.MissingTransitionBehavior;

/**
 * A ghost.
 * 
 * <p>
 * Ghosts are creatures with additional behaviors like entering and leaving the ghost house or
 * jumping up and down at some position.
 * 
 * @author Armin Reichert
 */
public class Ghost extends Creature<GhostState> {

	public enum Insanity {
		IMMUNE, HEALTHY, CRUISE_ELROY1, CRUISE_ELROY2
	};

	/** Tile headed for when ghost scatters out. */
	public Tile scatteringTarget;

	/** State to enter after frightening state ends. */
	public GhostState followState;

	/** Keep track of steering changes. */
	public Steering lastSteering;

	/** Insanity ("cruise elroy") level. */
	public Insanity insanity = Insanity.IMMUNE;

	public Ghost(Game game, String name) {
		super(game, name, new EnumMap<>(GhostState.class));
		/*@formatter:off*/
		brain = StateMachine.beginStateMachine(GhostState.class, PacManGameEvent.class)
			 
			.description(this::toString)
			.initialState(LOCKED)
		
			.states()
	
				.state(LOCKED)
					.onEntry(() -> {
						followState = LOCKED;
						visible = true;
						if (insanity != Insanity.IMMUNE) {
							insanity = Insanity.HEALTHY;
						}
						moveDir = wishDir = seat.startDir;
						tf.setPosition(seat.position);
						enteredNewTile();
						sprites.forEach(Sprite::resetAnimation);
						showColored();
					})
					.onTick(() -> {
						move();
						// not sure if ghost locked inside house should look frightened
						if (game.pacMan.power > 0) {
							showFrightened();
						} else {
							showColored();
						}
					})
					
				.state(LEAVING_HOUSE)
					.onEntry(() -> {
						steering().init();
					})
					.onTick(() -> {
						move();
						showColored();
					})
					.onExit(() -> forceMoving(Direction.LEFT))
				
				.state(ENTERING_HOUSE)
					.onEntry(() -> {
						tf.setPosition(maze.ghostSeats[0].position);
						moveDir = wishDir = Direction.DOWN;
						steering().init();
					})
					.onTick(() -> {
						move();
						showEyes();
					})
				
				.state(SCATTERING)
					.onTick(() -> {
						updateInsanity(game);
						move();
						showColored();
						checkCollision(game.pacMan);
					})
			
				.state(CHASING)
					.onTick(() -> {
						updateInsanity(game);
						move();
						showColored();
						checkCollision(game.pacMan);
					})
				
				.state(FRIGHTENED)
					.timeoutAfter(() -> sec(game.level.pacManPowerSeconds))
					.onTick((state, t, remaining) -> {
						move();
						// one flashing animation takes 0.5 sec
						int flashTicks = sec(game.level.numFlashes * 0.5f);
						if (remaining < flashTicks) {
							showFlashing();
						} else  {
							showFrightened();
						}
						checkCollision(game.pacMan);
					})
				
				.state(DEAD)
					.timeoutAfter(sec(1)) // time while ghost is drawn as number of scored points
					.onEntry(() -> {
						showPoints(Game.POINTS_GHOST[game.level.ghostsKilledByEnergizer - 1]);
					})
					.onTick((state, t, remaining) -> {
						if (remaining == 0) { // show as eyes returning to ghost home
							move();
							showEyes();
						}
					})
				
			.transitions()
			
				.when(LOCKED).then(LEAVING_HOUSE)
					.on(GhostUnlockedEvent.class)
			
				.when(LEAVING_HOUSE).then(SCATTERING)
					.condition(() -> steering().isComplete() && followState == SCATTERING)
				
				.when(LEAVING_HOUSE).then(CHASING)
					.condition(() -> steering().isComplete() && followState == CHASING)
				
				.when(ENTERING_HOUSE).then(LEAVING_HOUSE)
					.condition(() -> steering().isComplete())
				
				.when(CHASING).then(FRIGHTENED)
					.on(PacManGainsPowerEvent.class)
					.act(() -> reverseDirection())
				
				.when(CHASING).then(DEAD)
					.on(GhostKilledEvent.class)
				
				.when(CHASING).then(SCATTERING)
					.condition(() -> followState == SCATTERING)
					.act(() -> reverseDirection())
					
				.when(SCATTERING).then(FRIGHTENED)
					.on(PacManGainsPowerEvent.class)
					.act(() -> reverseDirection())
				
				.when(SCATTERING).then(DEAD)
					.on(GhostKilledEvent.class)
				
				.when(SCATTERING).then(CHASING)
					.condition(() -> followState == CHASING)
					.act(() -> reverseDirection())
					
				.stay(FRIGHTENED)
					.on(PacManGainsPowerEvent.class)
					.act(() -> restartTimer(FRIGHTENED))
				
				.when(FRIGHTENED).then(DEAD)
					.on(GhostKilledEvent.class)
				
				.when(FRIGHTENED).then(SCATTERING)
					.onTimeout()
					.condition(() -> followState == SCATTERING)
					
				.when(FRIGHTENED).then(CHASING)
					.onTimeout()
					.condition(() -> followState == CHASING)
					
				.when(DEAD).then(ENTERING_HOUSE)
					.condition(() -> maze.atGhostHouseDoor(tile()))
					
		.endStateMachine();
		/*@formatter:on*/
		brain.setMissingTransitionBehavior(MissingTransitionBehavior.LOG);
		brain.getTracer().setLogger(PacManStateMachineLogging.LOGGER);
	}

	@Override
	public boolean canMoveBetween(Tile tile, Tile neighbor) {
		if (maze.isDoor(neighbor)) {
			return is(ENTERING_HOUSE, LEAVING_HOUSE);
		}
		if (maze.isOneWayDown(tile) && neighbor.equals(maze.neighbor(tile, UP))) {
			return !is(CHASING, SCATTERING);
		}
		return super.canMoveBetween(tile, neighbor);
	}

	public void move() {
		Steering currentSteering = steering();
		if (lastSteering != currentSteering) {
			PacManStateMachineLogging.loginfo("%s steering changed from %s to %s", this, Steering.name(lastSteering),
					Steering.name(currentSteering));
			currentSteering.init();
			currentSteering.force();
			lastSteering = currentSteering;
		}
		currentSteering.steer();
		movement.update();
	}

	@Override
	public float currentSpeed(Game game) {
		switch (getState()) {
		case LOCKED:
			return speed(maze.insideGhostHouse(tile()) ? game.level.ghostSpeed / 2 : 0);
		case LEAVING_HOUSE:
			return speed(game.level.ghostSpeed / 2);
		case ENTERING_HOUSE:
			return speed(game.level.ghostSpeed);
		case CHASING:
		case SCATTERING:
			if (maze.isTunnel(tile())) {
				return speed(game.level.ghostTunnelSpeed);
			} else {
				switch (insanity) {
				case CRUISE_ELROY2:
					return speed(game.level.elroy2Speed);
				case CRUISE_ELROY1:
					return speed(game.level.elroy1Speed);
				case HEALTHY:
				case IMMUNE:
					return speed(game.level.ghostSpeed);
				default:
					throw new IllegalArgumentException("Illegal ghost state: " + getState());
				}
			}
		case FRIGHTENED:
			return speed(maze.isTunnel(tile()) ? game.level.ghostTunnelSpeed : game.level.ghostFrightenedSpeed);
		case DEAD:
			return speed(2 * game.level.ghostSpeed);
		default:
			throw new IllegalStateException(String.format("Illegal ghost state %s", getState()));
		}
	}

	public void takeClothes(Theme theme, int color) {
		Direction.dirs().forEach(dir -> {
			sprites.set("color-" + dir, theme.spr_ghostColored(color, dir));
			sprites.set("eyes-" + dir, theme.spr_ghostEyes(dir));
		});
		sprites.set("frightened", theme.spr_ghostFrightened());
		sprites.set("flashing", theme.spr_ghostFlashing());
		for (int points : Game.POINTS_GHOST) {
			sprites.set("points-" + points, theme.spr_number(points));
		}
	}

	public void showColored() {
		sprites.select("color-" + moveDir);
	}

	public void showFrightened() {
		sprites.select("frightened");
	}

	public void showEyes() {
		sprites.select("eyes-" + moveDir);
	}

	public void showFlashing() {
		sprites.select("flashing");
	}

	public void showPoints(int points) {
		sprites.select("points-" + points);
	}

	private void checkCollision(PacMan pacMan) {
		if (isTeleporting() || pacMan.isTeleporting() || pacMan.is(PacManState.DEAD)) {
			return;
		}
		if (tile().equals(pacMan.tile())) {
			publish(new PacManGhostCollisionEvent(this));
		}
	}

	private void updateInsanity(Game game) {
		if (insanity == Insanity.IMMUNE) {
			return;
		}
		int pelletsLeft = game.remainingFoodCount();
		Insanity oldInsanity = insanity;
		if (pelletsLeft <= game.level.elroy2DotsLeft) {
			insanity = Insanity.CRUISE_ELROY2;
		} else if (pelletsLeft <= game.level.elroy1DotsLeft) {
			insanity = Insanity.CRUISE_ELROY1;
		}
		if (oldInsanity != insanity) {
			loginfo("%s's insanity changed from %s to %s, pellets left: %d", name, oldInsanity, insanity, pelletsLeft);
		}
	}

	/**
	 * Lets the ghost jump up and down at its own seat in the house.
	 * 
	 * @return behavior which lets the ghost jump
	 */
	public Steering isJumpingUpAndDown(Vector2f seatPosition) {
		return new JumpingUpAndDown(this, seatPosition.y);
	}

	/**
	 * Lets the actor avoid the attacker's path by walking to a "safe" maze corner.
	 * 
	 * @param attacker the attacking actor
	 * @param corners  list of tiles representing maze corners
	 * 
	 * @return behavior where actor flees to a "safe" maze corner
	 */
	public Steering isFleeingToSafeCorner(MazeMover attacker, Tile... corners) {
		return new FleeingToSafeCorner(this, attacker, corners);
	}

	/**
	 * Lets a ghost enter the ghost house and move to the seat with the given position.
	 * 
	 * @param seatPosition seat position
	 * 
	 * @return behavior which lets a ghost enter the house and take its seat
	 */
	public Steering isTakingSeat(Vector2f seatPosition) {
		// add 3 pixel so that ghost dives deeper into ghost house
		return new EnteringGhostHouse(this, Vector2f.of(seatPosition.x, seatPosition.y + 3));
	}

	/**
	 * Lets a ghost leave the ghost house.
	 * 
	 * @return behavior which lets a ghost leave the ghost house
	 */
	public Steering isLeavingGhostHouse() {
		return new LeavingGhostHouse(this);
	}
}