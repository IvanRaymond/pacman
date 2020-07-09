package de.amr.games.pacman.controller.actor;

import static de.amr.games.pacman.controller.actor.GhostState.CHASING;
import static de.amr.games.pacman.controller.actor.GhostState.DEAD;
import static de.amr.games.pacman.controller.actor.GhostState.ENTERING_HOUSE;
import static de.amr.games.pacman.controller.actor.GhostState.FRIGHTENED;
import static de.amr.games.pacman.controller.actor.GhostState.LEAVING_HOUSE;
import static de.amr.games.pacman.controller.actor.GhostState.LOCKED;
import static de.amr.games.pacman.controller.actor.GhostState.SCATTERING;
import static de.amr.games.pacman.model.game.Game.sec;
import static de.amr.games.pacman.model.world.Direction.DOWN;
import static de.amr.games.pacman.model.world.Direction.LEFT;
import static de.amr.games.pacman.model.world.Direction.RIGHT;
import static de.amr.games.pacman.model.world.Direction.UP;

import java.util.EnumMap;
import java.util.Optional;

import de.amr.games.pacman.controller.PacManStateMachineLogging;
import de.amr.games.pacman.controller.actor.steering.Steering;
import de.amr.games.pacman.controller.actor.steering.ghost.FleeingToSafeCorner;
import de.amr.games.pacman.controller.actor.steering.ghost.GoingToBed;
import de.amr.games.pacman.controller.event.GhostKilledEvent;
import de.amr.games.pacman.controller.event.GhostUnlockedEvent;
import de.amr.games.pacman.controller.event.PacManGainsPowerEvent;
import de.amr.games.pacman.controller.event.PacManGameEvent;
import de.amr.games.pacman.controller.event.PacManGhostCollisionEvent;
import de.amr.games.pacman.model.game.Game;
import de.amr.games.pacman.model.world.Direction;
import de.amr.games.pacman.model.world.core.Bed;
import de.amr.games.pacman.model.world.core.OneWayTile;
import de.amr.games.pacman.model.world.core.Tile;
import de.amr.games.pacman.view.core.IRenderer;
import de.amr.games.pacman.view.theme.Themes;
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

	private GhostSanityControl sanityControl;

	/** State to enter after frightening state ends. */
	public GhostState subsequentState;

	/** Keeps track of steering changes. */
	public Steering previousSteering;

	/** Ghost color as defined in {@link Themes}. */
	public int color;

	/** Value when eaten */
	public int points;

	public boolean flashing;

	private IRenderer renderer;

	public Ghost(String name) {
		super(name, new EnumMap<>(GhostState.class));
		/*@formatter:off*/
		brain = StateMachine.beginStateMachine(GhostState.class, PacManGameEvent.class)
			 
			.description(this::toString)
			.initialState(LOCKED)
		
			.states()
	
				.state(LOCKED)
					.onEntry(() -> {
						subsequentState = LOCKED;
						visible = true;
						flashing = false;
						points = 0;
						world.putIntoBed(this);
						enteredNewTile();
						if (sanityControl != null) {
							sanityControl.init();
						}
					})
					.onTick(() -> {
						move();
					})
					
				.state(LEAVING_HOUSE)
					.onEntry(() -> steering().init())
					.onTick(() -> {
						move();
					})
					.onExit(() -> forceMoving(Direction.LEFT))
				
				.state(ENTERING_HOUSE)
					.onEntry(() -> steering().init())
					.onTick(() -> {
						move();
					})
				
				.state(SCATTERING)
					.onTick(() -> {
						updateSanity();
						move();
						checkPacManCollision();
					})
			
				.state(CHASING)
					.onTick(() -> {
						updateSanity();
						move();
						checkPacManCollision();
					})
				
				.state(FRIGHTENED)
					.onTick((state, t, remaining) -> {
						move();
						// one flashing animation takes 0.5 sec
						int flashTicks = sec(game.level.numFlashes * 0.5f);
						flashing = remaining < flashTicks;
						checkPacManCollision();
					})
				
				.state(DEAD)
					.onTick(() -> {
							move();
					})
				
			.transitions()
			
				.when(LOCKED).then(LEAVING_HOUSE)
					.on(GhostUnlockedEvent.class)
			
				.when(LEAVING_HOUSE).then(SCATTERING)
					.condition(() -> hasLeftGhostHouse() && subsequentState == SCATTERING)
				
				.when(LEAVING_HOUSE).then(CHASING)
					.condition(() -> hasLeftGhostHouse() && subsequentState == CHASING)
				
				.when(ENTERING_HOUSE).then(LEAVING_HOUSE)
					.condition(() -> steering().isComplete())
				
				.when(CHASING).then(FRIGHTENED)
					.on(PacManGainsPowerEvent.class)
					.act(() -> reverseDirection())
				
				.when(CHASING).then(DEAD)
					.on(GhostKilledEvent.class)
				
				.when(CHASING).then(SCATTERING)
					.condition(() -> subsequentState == SCATTERING)
					.act(() -> reverseDirection())
					
				.when(SCATTERING).then(FRIGHTENED)
					.on(PacManGainsPowerEvent.class)
					.act(() -> reverseDirection())
				
				.when(SCATTERING).then(DEAD)
					.on(GhostKilledEvent.class)
				
				.when(SCATTERING).then(CHASING)
					.condition(() -> subsequentState == CHASING)
					.act(() -> reverseDirection())
					
				.stay(FRIGHTENED)
					.on(PacManGainsPowerEvent.class)
					.act(() -> restartTimer(FRIGHTENED))
				
				.when(FRIGHTENED).then(DEAD)
					.on(GhostKilledEvent.class)
				
				.when(FRIGHTENED).then(SCATTERING)
					.onTimeout()
					.condition(() -> subsequentState == SCATTERING)
					
				.when(FRIGHTENED).then(CHASING)
					.onTimeout()
					.condition(() -> subsequentState == CHASING)
					
				.when(DEAD).then(ENTERING_HOUSE)
					.condition(() -> world.isJustBeforeDoor(tile()))
					
		.endStateMachine();
		/*@formatter:on*/
		brain.setMissingTransitionBehavior(MissingTransitionBehavior.LOG);
		brain.getTracer().setLogger(PacManStateMachineLogging.LOGGER);
	}

	public void setRenderer(IRenderer renderer) {
		this.renderer = renderer;
	}

	public IRenderer getRenderer() {
		return renderer;
	}

	@Override
	public void takePartIn(Game game) {
		this.game = game;

		// frightened time is defined by the game level
		state(FRIGHTENED).setTimer(() -> sec(game.level.pacManPowerSeconds));

		// when dead, the ghost first appears as a number (its value) for one second, then it
		// appears as eyes returning to the ghost house
		state(DEAD).setTimer(sec(1));
		state(DEAD).setOnEntry(() -> points = game.killedGhostPoints());
		state(DEAD).setOnTick((s, consumed, remaining) -> {
			if (remaining == 0) {
				points = 0;
				move();
			}
		});

		// Blinky can get insane ("cruise elroy")
		if (name.equals("Blinky")) {
			sanityControl = new GhostSanityControl(game, "Blinky", GhostSanity.INFECTABLE);
			sanityControl.getTracer().setLogger(PacManStateMachineLogging.LOGGER);
		}
	}

	public GhostSanity getSanity() {
		return sanityControl != null ? sanityControl.getState() : GhostSanity.IMMUNE;
	}

	private void updateSanity() {
		if (sanityControl != null) {
			sanityControl.update();
		}
	}

	/**
	 * Lets the ghost jump up and down on its bed.
	 */
	public void bouncingOnBed(Bed bed) {
		float dy = tf.y + Tile.SIZE / 2 - bed.center.y;
		if (dy < -4) {
			setWishDir(DOWN);
		} else if (dy > 3) {
			setWishDir(UP);
		}
	}

	/**
	 * lets a ghost leave the ghost house
	 */
	public void leavingGhostHouse() {
		Tile exit = world.theHouse().bed(0).tile;
		int targetX = exit.centerX(), targetY = exit.y();
		if (tf.y <= targetY) {
			tf.y = targetY;
		} else if (Math.round(tf.x) == targetX) {
			tf.x = targetX;
			setWishDir(UP);
		} else if (tf.x < targetX) {
			setWishDir(RIGHT);
		} else if (tf.x > targetX) {
			setWishDir(LEFT);
		}
	}

	private boolean hasLeftGhostHouse() {
		return tf.y == world.theHouse().bed(0).tile.y();
	}

	/**
	 * Lets the actor avoid the attacker's path by walking to a "safe" maze corner.
	 * 
	 * @param attacker the attacking actor
	 * @return steering where actor flees to a "safe" maze corner
	 */
	public Steering fleeingToSafeCorner(WorldMover attacker) {
		return new FleeingToSafeCorner(this, attacker, world.capeNW(), world.capeNE(), world.capeSW(), world.capeSE());
	}

	/**
	 * @return steering for bringing ghost back to ghost house entry
	 */
	public Steering returningToHouse() {
		return headingFor(() -> world.theHouse().bed(0).tile);
	}

	/**
	 * @return steering which lets ghost enter the house going to bed
	 */
	public Steering goingToBed(Bed bed) {
		return new GoingToBed(this, bed);
	}

	@Override
	public boolean canMoveBetween(Tile tile, Tile neighbor) {
		if (world.isDoor(neighbor)) {
			return is(ENTERING_HOUSE, LEAVING_HOUSE);
		}

		Optional<OneWayTile> maybeOneWay = world.oneWayTiles().filter(oneWay -> oneWay.tile.equals(neighbor)).findFirst();
		if (maybeOneWay.isPresent()) {
			OneWayTile oneWay = maybeOneWay.get();
			Direction toNeighbor = tile.dirTo(neighbor).get();
			if (toNeighbor.equals(oneWay.dir.opposite()) && is(CHASING, SCATTERING)) {
				return false;
			}
		}
		return super.canMoveBetween(tile, neighbor);
	}

	public void move() {
		Steering currentSteering = steering();
		if (previousSteering != currentSteering) {
			currentSteering.init();
			currentSteering.force();
			previousSteering = currentSteering;
		}
		currentSteering.steer();
		movement.update();
	}

	public boolean isInsideHouse() {
		return world.insideHouseOrDoor(tile());
	}

	private void checkPacManCollision() {
		PacMan pacMan = world.population().pacMan();
		if (tile().equals(pacMan.tile()) && !isTeleporting() && !pacMan.isTeleporting() && !pacMan.is(PacManState.DEAD)) {
			publish(new PacManGhostCollisionEvent(this));
		}
	}
}