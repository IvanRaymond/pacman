package de.amr.games.pacman.actor;

import static de.amr.games.pacman.actor.GhostState.CHASING;
import static de.amr.games.pacman.actor.GhostState.DEAD;
import static de.amr.games.pacman.actor.GhostState.ENTERING_HOUSE;
import static de.amr.games.pacman.actor.GhostState.FRIGHTENED;
import static de.amr.games.pacman.actor.GhostState.LEAVING_HOUSE;
import static de.amr.games.pacman.actor.GhostState.LOCKED;
import static de.amr.games.pacman.actor.GhostState.SCATTERING;
import static de.amr.games.pacman.actor.behavior.Steerings.isHeadingFor;
import static de.amr.games.pacman.model.Direction.UP;
import static de.amr.games.pacman.model.Timing.sec;
import static de.amr.games.pacman.model.Timing.speed;

import java.util.EnumMap;
import java.util.Map;

import de.amr.easy.game.ui.sprites.Sprite;
import de.amr.games.pacman.actor.behavior.Steering;
import de.amr.games.pacman.actor.core.AbstractMazeMover;
import de.amr.games.pacman.actor.core.PacManGameActor;
import de.amr.games.pacman.controller.GhostHouseDoorMan;
import de.amr.games.pacman.controller.event.GhostKilledEvent;
import de.amr.games.pacman.controller.event.PacManGainsPowerEvent;
import de.amr.games.pacman.controller.event.PacManGameEvent;
import de.amr.games.pacman.controller.event.PacManKilledEvent;
import de.amr.games.pacman.model.Direction;
import de.amr.games.pacman.model.PacManGame;
import de.amr.games.pacman.model.Tile;
import de.amr.statemachine.client.FsmComponent;
import de.amr.statemachine.core.StateMachine;
import de.amr.statemachine.core.StateMachine.MissingTransitionBehavior;

/**
 * A ghost.
 * 
 * @author Armin Reichert
 */
public class Ghost extends AbstractMazeMover implements PacManGameActor<GhostState> {

	public Direction eyes;
	public byte seat;
	public int dotCounter;
	public GhostState nextState;
	public GhostHouseDoorMan doorMan;
	private final PacManGameCast cast;
	private final FsmComponent<GhostState, PacManGameEvent> brain;
	private final Map<GhostState, Steering<Ghost>> steering = new EnumMap<>(GhostState.class);
	private final Steering<Ghost> defaultSteering = isHeadingFor(this::targetTile);

	public Ghost(String name, PacManGameCast cast) {
		super(name);
		this.cast = cast;
		tf.setWidth(Tile.SIZE);
		tf.setHeight(Tile.SIZE);
		brain = buildBrain();
		brain.fsm().setMissingTransitionBehavior(MissingTransitionBehavior.LOG);
		brain.fsm().traceTo(PacManGame.FSM_LOGGER, () -> 60);
	}

	@Override
	public StateMachine<GhostState, PacManGameEvent> buildFsm() {
		return StateMachine.
		/*@formatter:off*/
		beginStateMachine(GhostState.class, PacManGameEvent.class)
			 
			.description(String.format("[%s]", name()))
			.initialState(LOCKED)
		
			.states()
	
				.state(LOCKED)
					.onEntry(() -> {
						visible = true;
						enteredNewTile = true;
						nextState = getState();
						placeAtTile(maze().ghostHouseSeats[seat], Tile.SIZE / 2, 0);
						setMoveDir(eyes);
						setNextDir(eyes);
						sprites.select("color-" + moveDir());
						sprites.forEach(Sprite::resetAnimation);
					})
					.onTick(() -> walkAndDisplayAs("color-" + moveDir()))
					.onExit(() -> {
						steering().triggerSteering(this);
					})
					
				.state(LEAVING_HOUSE)
					.onTick(() -> walkAndDisplayAs("color-" + moveDir()))
					.onExit(() -> setNextDir(Direction.LEFT))
				
				.state(ENTERING_HOUSE)
					.onEntry(() -> setNextDir(Direction.DOWN))
					.onTick(() -> walkAndDisplayAs("eyes-" + moveDir()))
				
				.state(SCATTERING)
					.onTick(() -> {
						walkAndDisplayAs("color-" + moveDir());
						handlePacManCollision();
					})
			
				.state(CHASING)
					.onEntry(() -> turnChasingGhostSoundOn())
					.onTick(() -> {
						walkAndDisplayAs("color-" + moveDir());
						handlePacManCollision();
					})
					.onExit(() -> turnChasingGhostSoundOff())
				
				.state(FRIGHTENED)
					.onTick(() -> {
						walkAndDisplayAs(cast.pacMan.isTired() ? "flashing" : "frightened");
						handlePacManCollision();
					})
				
				.state(DEAD)
					.timeoutAfter(sec(1)) // "dying" time
					.onEntry(() -> {
						sprites.select("value-" + game().level().ghostsKilledByEnergizer);
						setTargetTile(maze().ghostHouseSeats[0]);
						turnDeadGhostSoundOn();
					})
					.onTick(() -> {
						if (state().isTerminated()) { // "dead"
							walkAndDisplayAs("eyes-" + moveDir());
						}
					})
					.onExit(() -> {
						turnDeadGhostSoundOff();
					})
				
			.transitions()
			
				.when(LOCKED).then(LEAVING_HOUSE)
					.condition(this::canLeaveHouse)
					.act(() -> cast.pacMan.clearStarvingTime())
			
				.when(LEAVING_HOUSE).then(SCATTERING)
					.condition(() -> hasLeftTheHouse() && nextState == SCATTERING)
				
				.when(LEAVING_HOUSE).then(CHASING)
					.condition(() -> hasLeftTheHouse() && nextState == CHASING)
					
				.when(ENTERING_HOUSE).then(LEAVING_HOUSE)
					.condition(() -> nextDir() == null)
				
				.when(CHASING).then(FRIGHTENED)
					.on(PacManGainsPowerEvent.class)
					.act(this::turnAround)
				
				.when(CHASING).then(DEAD)
					.on(GhostKilledEvent.class)
				
				.when(CHASING).then(SCATTERING)
					.condition(() -> nextState == SCATTERING)
					.act(this::turnAround)
	
				.when(SCATTERING).then(FRIGHTENED)
					.on(PacManGainsPowerEvent.class)
					.act(this::turnAround)
				
				.when(SCATTERING).then(DEAD)
					.on(GhostKilledEvent.class)
				
				.when(SCATTERING).then(CHASING)
					.condition(() -> nextState == CHASING)
					.act(this::turnAround)
				
				.when(FRIGHTENED).then(CHASING)
					.condition(() -> !cast.pacMan.isKicking() && nextState == CHASING)
	
				.when(FRIGHTENED).then(SCATTERING)
					.condition(() -> !cast.pacMan.isKicking() && nextState == SCATTERING)
				
				.when(FRIGHTENED).then(DEAD)
					.on(GhostKilledEvent.class)
					
				.when(DEAD).then(ENTERING_HOUSE)
					.condition(() -> maze().inFrontOfGhostHouseDoor(tile()))
					.act(() -> placeAtTile(maze().ghostHouseSeats[0], Tile.SIZE / 2, 0))
				
		.endStateMachine();
		/*@formatter:on*/
	}

	@Override
	public PacManGameCast cast() {
		return cast;
	}

	@Override
	public FsmComponent<GhostState, PacManGameEvent> fsmComponent() {
		return brain;
	}

	@Override
	public void init() {
		super.init();
		brain.init();
	}

	@Override
	public void update() {
		super.update();
		brain.update();
	}

	public void during(GhostState state, Steering<Ghost> steeringInState) {
		steering.put(state, steeringInState);
		steeringInState.triggerSteering(this);
	}

	@Override
	public Steering<Ghost> steering() {
		return steering.getOrDefault(getState(), defaultSteering);
	}

	private boolean canLeaveHouse() {
		return doorMan == null || doorMan.isReleasing(this);
	}

	private void walkAndDisplayAs(String spriteKey) {
		steering().steer(this);
		step();
		sprites.select(spriteKey);
	}

	@Override
	public boolean canMoveBetween(Tile tile, Tile neighbor) {
		if (maze().isDoor(neighbor)) {
			return is(ENTERING_HOUSE, LEAVING_HOUSE);
		}
		if (maze().isNoUpIntersection(tile) && neighbor == maze().tileToDir(tile, UP)) {
			return !is(CHASING, SCATTERING);
		}
		return super.canMoveBetween(tile, neighbor);
	}

	@Override
	/* TODO: Some values are still guessed */
	public float maxSpeed() {
		boolean inTunnel = tile().isTunnel();
		boolean outsideHouse = !maze().inGhostHouse(tile());
		switch (getState()) {
		case LOCKED:
			return outsideHouse ? 0 : speed(game().level().ghostSpeed) / 2;
		case LEAVING_HOUSE:
			//$FALL-THROUGH$
		case ENTERING_HOUSE:
			return speed(game().level().ghostSpeed) / 2;
		case CHASING:
			//$FALL-THROUGH$
		case SCATTERING:
			return inTunnel ? speed(game().level().ghostTunnelSpeed) : speed(game().level().ghostSpeed);
		case FRIGHTENED:
			return inTunnel ? speed(game().level().ghostTunnelSpeed) : speed(game().level().ghostFrightenedSpeed);
		case DEAD:
			return 2 * speed(game().level().ghostSpeed);
		default:
			throw new IllegalStateException(String.format("Illegal ghost state %s for %s", getState(), name()));
		}
	}

	private void handlePacManCollision() {
		if (tile().equals(cast.pacMan.tile())) {
			publish(cast.pacMan.isKicking() ? new GhostKilledEvent(this) : new PacManKilledEvent(this));
		}
	}

	private boolean hasLeftTheHouse() {
		Tile currentTile = tile();
		return !maze().partOfGhostHouse(currentTile) && tf.getPosition().roundedY() == currentTile.y();
	}

	public void turnChasingGhostSoundOn() {
		if (!cast.theme().snd_ghost_chase().isRunning()) {
			cast.theme().snd_ghost_chase().loop();
		}
	}

	public void turnChasingGhostSoundOff() {
		// if caller is the last chasing ghost, turn sound off
		if (cast.ghostsOnStage().filter(ghost -> this != ghost).noneMatch(ghost -> ghost.is(CHASING))) {
			cast.theme().snd_ghost_chase().stop();
		}
	}

	public void turnDeadGhostSoundOn() {
		if (!cast.theme().snd_ghost_dead().isRunning()) {
			cast.theme().snd_ghost_dead().loop();
		}
	}

	public void turnDeadGhostSoundOff() {
		// if caller is the last dead ghost, turn sound off
		if (cast.ghostsOnStage().filter(ghost -> this != ghost).noneMatch(ghost -> ghost.is(DEAD))) {
			cast.theme().snd_ghost_dead().stop();
		}
	}
}