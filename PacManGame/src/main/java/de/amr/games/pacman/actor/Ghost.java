package de.amr.games.pacman.actor;

import static de.amr.games.pacman.actor.GhostState.CHASING;
import static de.amr.games.pacman.actor.GhostState.DEAD;
import static de.amr.games.pacman.actor.GhostState.ENTERING_HOUSE;
import static de.amr.games.pacman.actor.GhostState.FRIGHTENED;
import static de.amr.games.pacman.actor.GhostState.LEAVING_HOUSE;
import static de.amr.games.pacman.actor.GhostState.LOCKED;
import static de.amr.games.pacman.actor.GhostState.SCATTERING;
import static de.amr.games.pacman.actor.behavior.Steerings.isHeadingFor;
import static de.amr.games.pacman.model.Direction.LEFT;
import static de.amr.games.pacman.model.Direction.UP;
import static de.amr.games.pacman.model.Timing.sec;
import static de.amr.games.pacman.model.Timing.speed;

import java.util.EnumMap;
import java.util.Map;

import de.amr.easy.game.ui.sprites.Sprite;
import de.amr.games.pacman.actor.behavior.Steering;
import de.amr.games.pacman.actor.core.AbstractMazeMover;
import de.amr.games.pacman.actor.core.PacManGameActor;
import de.amr.games.pacman.controller.event.GhostKilledEvent;
import de.amr.games.pacman.controller.event.GhostUnlockedEvent;
import de.amr.games.pacman.controller.event.PacManGainsPowerEvent;
import de.amr.games.pacman.controller.event.PacManGameEvent;
import de.amr.games.pacman.controller.event.PacManGhostCollisionEvent;
import de.amr.games.pacman.controller.event.PacManLostPowerEvent;
import de.amr.games.pacman.controller.event.StartChasingEvent;
import de.amr.games.pacman.controller.event.StartScatteringEvent;
import de.amr.games.pacman.model.Direction;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.PacManGame;
import de.amr.games.pacman.model.Tile;
import de.amr.statemachine.client.FsmComponent;
import de.amr.statemachine.client.FsmControlled;
import de.amr.statemachine.core.StateMachine;
import de.amr.statemachine.core.StateMachine.MissingTransitionBehavior;

/**
 * A ghost.
 * 
 * @author Armin Reichert
 */
public class Ghost extends AbstractMazeMover implements PacManGameActor<GhostState> {

	private final Map<GhostState, Steering<Ghost>> steeringByState = new EnumMap<>(GhostState.class);
	private final Steering<Ghost> defaultSteering = isHeadingFor(this::targetTile);

	public final PacManGameCast cast;
	public final PacManGame game;
	public final FsmComponent<GhostState, PacManGameEvent> fsmComponent;
	public Direction eyes;
	public int seat;
	public GhostState nextState;

	public int dotCounter; // used by logic when ghost can leave house

	public Ghost(String name, PacManGameCast cast) {
		super(name);
		this.cast = cast;
		this.game = cast.game;
		fsmComponent = buildFsmComponent(name);
		tf.setWidth(Tile.SIZE);
		tf.setHeight(Tile.SIZE);
	}

	private FsmComponent<GhostState, PacManGameEvent> buildFsmComponent(String name) {
		StateMachine<GhostState, PacManGameEvent> fsm = buildStateMachine(name);
		fsm.setMissingTransitionBehavior(MissingTransitionBehavior.LOG);
		fsm.traceTo(PacManGame.FSM_LOGGER, () -> 60);
		return new FsmComponent<>(fsm);
	}

	private StateMachine<GhostState, PacManGameEvent> buildStateMachine(String name) {
		return StateMachine.
		/*@formatter:off*/
		beginStateMachine(GhostState.class, PacManGameEvent.class)
			 
			.description(String.format("[%s]", name))
			.initialState(LOCKED)
		
			.states()
	
				.state(LOCKED)
					.onEntry(() -> {
						visible = true;
						enteredNewTile = true;
						nextState = fsmComponent.getState();
						placeAtTile(maze().ghostHouseSeats[seat], Tile.SIZE / 2, 0);
						setMoveDir(eyes);
						setNextDir(eyes);
						sprites.select("color-" + moveDir());
						sprites.forEach(Sprite::resetAnimation);
					})
					.onTick(() -> walkAndDisplayAs("color-" + moveDir()))
					.onExit(() -> {
						enteredNewTile = true;
						cast.pacMan.clearStarvingTime();
					})
					
				.state(LEAVING_HOUSE)
					.onTick(() -> walkAndDisplayAs("color-" + moveDir()))
					.onExit(() -> setNextDir(LEFT))
				
				.state(ENTERING_HOUSE)
					.onEntry(() -> setNextDir(Direction.DOWN))
					.onTick(() -> walkAndDisplayAs("eyes-" + moveDir()))
				
				.state(SCATTERING)
					.onTick(() -> {
						walkAndDisplayAs("color-" + moveDir());
						checkPacManCollision();
					})
			
				.state(CHASING)
					.onEntry(() -> turnChasingGhostSoundOn())
					.onTick(() -> {
						walkAndDisplayAs("color-" + moveDir());
						checkPacManCollision();
					})
					.onExit(() -> turnChasingGhostSoundOff())
				
				.state(FRIGHTENED)
					.onTick(() -> {
						walkAndDisplayAs(cast.pacMan.isLosingPower() ? "flashing" : "frightened");
						checkPacManCollision();
					})
				
				.state(DEAD)
					.timeoutAfter(sec(1)) // "dying" time
					.onEntry(() -> {
						sprites.select("value-" + game.level.ghostsKilledByEnergizer);
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
					.on(GhostUnlockedEvent.class)
			
				.when(LEAVING_HOUSE).then(SCATTERING)
					.condition(() -> leftHouse() && nextState == SCATTERING)
				
				.when(LEAVING_HOUSE).then(CHASING)
					.condition(() -> leftHouse() && nextState == CHASING)
					
				.when(ENTERING_HOUSE).then(LEAVING_HOUSE)
					.condition(() -> nextDir() == null)
				
				.when(CHASING).then(FRIGHTENED)
					.on(PacManGainsPowerEvent.class)
					.act(this::turnAround)
				
				.when(CHASING).then(DEAD)
					.on(GhostKilledEvent.class)
				
				.when(CHASING).then(SCATTERING)
					.on(StartScatteringEvent.class)
					.act(this::turnAround)
	
				.when(SCATTERING).then(FRIGHTENED)
					.on(PacManGainsPowerEvent.class)
					.act(this::turnAround)
				
				.when(SCATTERING).then(DEAD)
					.on(GhostKilledEvent.class)
				
				.when(SCATTERING).then(CHASING)
					.on(StartChasingEvent.class)
					.act(this::turnAround)
				
				.when(FRIGHTENED).then(CHASING)
					.on(PacManLostPowerEvent.class)
					.condition(() -> nextState == CHASING)
	
				.when(FRIGHTENED).then(SCATTERING)
					.on(PacManLostPowerEvent.class)
					.condition(() -> nextState == SCATTERING)
				
				.when(FRIGHTENED).then(DEAD)
					.on(GhostKilledEvent.class)
					
				.when(DEAD).then(ENTERING_HOUSE)
					.condition(() -> maze().inFrontOfGhostHouseDoor(tile()))
					.act(() -> placeAtTile(maze().ghostHouseSeats[0], Tile.SIZE / 2, 0))
				
		.endStateMachine();
		/*@formatter:on*/
	}

	@Override
	public Maze maze() {
		return cast.game.maze;
	}

	@Override
	public FsmControlled<GhostState, PacManGameEvent> fsmComponent() {
		return fsmComponent;
	}

	@Override
	public void init() {
		super.init();
		fsmComponent.init();
	}

	@Override
	public void update() {
		super.update();
		fsmComponent.update();
	}

	public void during(GhostState state, Steering<Ghost> steering) {
		steeringByState.put(state, steering);
		steering.triggerSteering(this);
	}

	@Override
	public Steering<Ghost> steering() {
		return steeringByState.getOrDefault(fsmComponent.getState(), defaultSteering);
	}

	private void walkAndDisplayAs(String spriteKey) {
		steering().steer(this);
		step();
		sprites.select(spriteKey);
	}

	@Override
	public boolean canMoveBetween(Tile tile, Tile neighbor) {
		if (neighbor.isDoor()) {
			return is(ENTERING_HOUSE) || is(LEAVING_HOUSE);
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
			return outsideHouse ? 0 : speed(game.level.ghostSpeed) / 2;
		case LEAVING_HOUSE:
			//$FALL-THROUGH$
		case ENTERING_HOUSE:
			return speed(game.level.ghostSpeed) / 2;
		case CHASING:
			//$FALL-THROUGH$
		case SCATTERING:
			return inTunnel ? speed(game.level.ghostTunnelSpeed) : speed(game.level.ghostSpeed);
		case FRIGHTENED:
			return inTunnel ? speed(game.level.ghostTunnelSpeed) : speed(game.level.ghostFrightenedSpeed);
		case DEAD:
			return 2 * speed(game.level.ghostSpeed);
		default:
			throw new IllegalStateException(String.format("Illegal ghost state %s for %s", getState(), fsmComponent.name()));
		}
	}

	private void checkPacManCollision() {
		if (tile().equals(cast.pacMan.tile())) {
			fsmComponent.publish(new PacManGhostCollisionEvent(this));
		}
	}

	private boolean leftHouse() {
		Tile currentTile = tile();
		return !maze().partOfGhostHouse(currentTile) && tf.getY() - currentTile.y() == 0;
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