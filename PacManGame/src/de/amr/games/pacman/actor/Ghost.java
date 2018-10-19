package de.amr.games.pacman.actor;

import static de.amr.easy.game.Application.app;
import static de.amr.games.pacman.actor.GhostState.CHASING;
import static de.amr.games.pacman.actor.GhostState.DEAD;
import static de.amr.games.pacman.actor.GhostState.DYING;
import static de.amr.games.pacman.actor.GhostState.FRIGHTENED;
import static de.amr.games.pacman.actor.GhostState.HOME;
import static de.amr.games.pacman.actor.GhostState.SAFE;
import static de.amr.games.pacman.actor.GhostState.SCATTERING;
import static de.amr.games.pacman.model.Maze.NESW;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import de.amr.easy.game.Application;
import de.amr.easy.game.ui.sprites.Sprite;
import de.amr.easy.grid.impl.Top4;
import de.amr.games.pacman.controller.event.GameEvent;
import de.amr.games.pacman.controller.event.GhostKilledEvent;
import de.amr.games.pacman.controller.event.PacManGainsPowerEvent;
import de.amr.games.pacman.controller.event.PacManGettingWeakerEvent;
import de.amr.games.pacman.controller.event.PacManLostPowerEvent;
import de.amr.games.pacman.controller.event.StartChasingEvent;
import de.amr.games.pacman.controller.event.StartScatteringEvent;
import de.amr.games.pacman.model.PacManGame;
import de.amr.games.pacman.model.Tile;
import de.amr.games.pacman.navigation.ActorBehavior;
import de.amr.games.pacman.navigation.GhostBehaviors;
import de.amr.games.pacman.theme.GhostColor;
import de.amr.games.pacman.theme.PacManTheme;
import de.amr.statemachine.State;
import de.amr.statemachine.StateMachine;

/**
 * A ghost.
 * 
 * @author Armin Reichert
 */
public class Ghost extends PacManGameActor implements GhostBehaviors {

	private final String name;
	private final StateMachine<GhostState, GameEvent> fsm;
	private final Map<GhostState, ActorBehavior<Ghost>> behaviorMap;
	private final PacMan pacMan;
	private final Tile home;
	private final Tile scatteringTarget;
	private final int initialDir;
	public Supplier<GhostState> fnNextAttackState; // chasing or scattering
	public BooleanSupplier fnCanLeaveHouse;

	public Ghost(String name, PacMan pacMan, PacManGame game, Tile home, Tile scatteringTarget, int initialDir,
			GhostColor color) {
		super(game);
		this.name = name;
		this.pacMan = pacMan;
		this.home = home;
		this.scatteringTarget = scatteringTarget;
		this.initialDir = initialDir;
		fsm = buildStateMachine(name);
		fsm.traceTo(Application.LOGGER, Application.app().clock::getFrequency);
		fnNextAttackState = () -> getState();
		fnCanLeaveHouse = () -> fsm.state().isTerminated();
		behaviorMap = new EnumMap<>(GhostState.class);
		setSprites(color);
	}

	public void initGhost() {
		placeAtTile(home, getTileSize() / 2, 0);
		setCurrentDir(initialDir);
		setNextDir(initialDir);
		sprites.forEach(Sprite::resetAnimation);
		sprites.select("s_color_" + initialDir);
	}

	private void reviveGhost() {
		setCurrentDir(Top4.N);
		setNextDir(Top4.N);
		sprites.forEach(Sprite::resetAnimation);
		sprites.select("s_color_" + getCurrentDir());
	}

	// Accessors

	public String getName() {
		return name;
	}

	public Tile getHomeTile() {
		return home;
	}

	public Tile getScatteringTarget() {
		return scatteringTarget;
	}

	public GhostState getNextAttackState() {
		GhostState nextAttackState = fnNextAttackState.get();
		return nextAttackState != null ? nextAttackState : getState();
	}

	@Override
	public float getSpeed() {
		return getGame().getGhostSpeed(this);
	}

	public PacManTheme getTheme() {
		return app().settings.get("theme");
	}

	// Behavior

	public void setBehavior(GhostState state, ActorBehavior<Ghost> behavior) {
		behaviorMap.put(state, behavior);
	}

	public ActorBehavior<Ghost> getBehavior() {
		return behaviorMap.getOrDefault(getState(), keepDirection());
	}

	@Override
	public int supplyIntendedDir() {
		return getBehavior().getRoute(this).getDir();
	}

	@Override
	public boolean canTraverseDoor(Tile door) {
		if (getState() == GhostState.SAFE) {
			return false;
		}
		if (getState() == GhostState.DEAD) {
			return true;
		}
		return inGhostHouse();
	}

	private boolean canLeaveHouse() {
		return fnCanLeaveHouse.getAsBoolean();
	}

	private boolean isPacManGreedy() {
		return pacMan.getState() == PacManState.GREEDY;
	}

	// Sprites

	private void setSprites(GhostColor color) {
		NESW.dirs().forEach(dir -> {
			sprites.set("s_color_" + dir, getTheme().spr_ghostColored(color, dir));
			sprites.set("s_eyes_" + dir, getTheme().spr_ghostEyes(dir));
		});
		for (int i = 0; i < 4; ++i) {
			sprites.set("s_value" + i, getTheme().spr_greenNumber(i));
		}
		sprites.set("s_frightened", getTheme().spr_ghostFrightened());
		sprites.set("s_flashing", getTheme().spr_ghostFlashing());
	}

	// State machine

	@Override
	public void init() {
		fsm.init();
	}

	@Override
	public void update() {
		fsm.update();
	}

	public GhostState getState() {
		return fsm.getState();
	}

	public State<GhostState, GameEvent> getStateObject() {
		return fsm.state();
	}

	public void setState(GhostState state) {
		fsm.setState(state);
	}

	public void processEvent(GameEvent event) {
		fsm.process(event);
	}

	private StateMachine<GhostState, GameEvent> buildStateMachine(String ghostName) {
		/*@formatter:off*/
		return StateMachine.beginStateMachine(GhostState.class, GameEvent.class)
			 
			.description(String.format("[%s]", ghostName))
			.initialState(HOME)
		
			.states()

				.state(HOME)
					.onEntry(this::initGhost)
				
				.state(SAFE)
					.timeoutAfter(() -> getGame().getGhostSafeTime(this))
					.onTick(() -> {
						move();	
						sprites.select("s_color_" + getCurrentDir()); 
					})
				
				.state(SCATTERING)
					.onTick(() -> {
						move();	
						sprites.select("s_color_" + getCurrentDir()); 
					})
			
				.state(CHASING)
					.onEntry(() -> getTheme().snd_ghost_chase().loop())
					.onExit(() -> getTheme().snd_ghost_chase().stop())
					.onTick(() -> {	
						move();	
						sprites.select("s_color_" + getCurrentDir()); 
					})
				
				.state(FRIGHTENED)
					.onEntry(() -> {
						sprites.select("s_frightened"); 
						getBehavior().computePath(this); 
					})
					.onTick(this::move)
				
				.state(DYING)
					.timeoutAfter(getGame()::getGhostDyingTime)
					.onEntry(() -> {
						sprites.select("s_value" + getGame().getGhostsKilledByEnergizer()); 
						getGame().addGhostKilled();
					})
				
				.state(DEAD)
					.onEntry(() -> {
						getBehavior().computePath(this);
						getTheme().snd_ghost_dead().loop();
					})
					.onTick(() -> {	
						move();
						sprites.select("s_eyes_" + getCurrentDir());
					})
					.onExit(() -> {
						if (getGame().getActiveGhosts().filter(ghost -> ghost != this)
								.noneMatch(ghost -> ghost.getState() == DEAD)) {
							getTheme().snd_ghost_dead().stop();
						}
					})
					
			.transitions()

				.when(HOME).then(SAFE)
				
				.stay(HOME)
					.on(StartScatteringEvent.class)
				
				.stay(HOME)
					.on(StartChasingEvent.class)

				.stay(SAFE)
					.on(StartChasingEvent.class)
					.condition(() -> !canLeaveHouse())
				
				.when(SAFE).then(CHASING)
					.on(StartChasingEvent.class)
					.condition(() -> canLeaveHouse())
				
				.stay(SAFE)
					.on(StartScatteringEvent.class)
					.condition(() -> !canLeaveHouse())
					
				.when(SAFE).then(SCATTERING)
					.on(StartScatteringEvent.class)
					.condition(() -> canLeaveHouse())
					
				.stay(SAFE).on(PacManGainsPowerEvent.class)
				.stay(SAFE).on(PacManGettingWeakerEvent.class)
				.stay(SAFE).on(PacManLostPowerEvent.class)
				.stay(SAFE).on(GhostKilledEvent.class)
				
				.when(SAFE).then(FRIGHTENED)
					.condition(() -> canLeaveHouse() && isPacManGreedy())

				.when(SAFE).then(SCATTERING)
					.condition(() -> canLeaveHouse() && getNextAttackState() == SCATTERING)
				
				.when(SAFE).then(CHASING)
					.condition(() -> canLeaveHouse() && getNextAttackState() == CHASING)
				
				.stay(CHASING).on(StartChasingEvent.class)
				.when(CHASING).then(FRIGHTENED).on(PacManGainsPowerEvent.class)
				.when(CHASING).then(DYING).on(GhostKilledEvent.class) // cheating-mode
				.when(CHASING).then(SCATTERING).on(StartScatteringEvent.class)

				.stay(SCATTERING).on(StartScatteringEvent.class)
				.stay(SCATTERING).on(PacManGettingWeakerEvent.class)
				.stay(SCATTERING).on(PacManLostPowerEvent.class)
				.when(SCATTERING).then(FRIGHTENED).on(PacManGainsPowerEvent.class)
				.when(SCATTERING).then(DYING).on(GhostKilledEvent.class) // cheating-mode
				.when(SCATTERING).then(CHASING).on(StartChasingEvent.class)
				
				.stay(FRIGHTENED).on(PacManGainsPowerEvent.class)
				.stay(FRIGHTENED).on(PacManGettingWeakerEvent.class).act(e -> sprites.select("s_flashing"))
				.stay(FRIGHTENED).on(StartScatteringEvent.class)
				.stay(FRIGHTENED).on(StartChasingEvent.class)
				
				.when(FRIGHTENED).then(CHASING).on(PacManLostPowerEvent.class)
				.when(FRIGHTENED).then(DYING).on(GhostKilledEvent.class)
					
				.when(DYING).then(DEAD).onTimeout()
				.stay(DYING).on(PacManGainsPowerEvent.class) // cheating-mode
				.stay(DYING).on(PacManGettingWeakerEvent.class) // cheating-mode
				.stay(DYING).on(PacManLostPowerEvent.class) // cheating-mode
				.stay(DYING).on(GhostKilledEvent.class) // cheating-mode
				.stay(DYING).on(StartScatteringEvent.class)
				.stay(DYING).on(StartChasingEvent.class)
					
				.when(DEAD).then(SAFE)
					.condition(() -> inGhostHouse())
					.act(this::reviveGhost)
				
				.stay(DEAD).on(PacManGainsPowerEvent.class)
				.stay(DEAD).on(PacManGettingWeakerEvent.class)
				.stay(DEAD).on(PacManLostPowerEvent.class)
				.stay(DEAD).on(GhostKilledEvent.class) // cheating-mode
				.stay(DEAD).on(StartScatteringEvent.class)
				.stay(DEAD).on(StartChasingEvent.class)

		.endStateMachine();
		/*@formatter:on*/
	}
}