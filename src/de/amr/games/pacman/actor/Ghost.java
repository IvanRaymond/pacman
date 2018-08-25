package de.amr.games.pacman.actor;

import static de.amr.games.pacman.actor.GhostState.AGGRO;
import static de.amr.games.pacman.actor.GhostState.DEAD;
import static de.amr.games.pacman.actor.GhostState.DYING;
import static de.amr.games.pacman.actor.GhostState.FRIGHTENED;
import static de.amr.games.pacman.actor.GhostState.HOME;
import static de.amr.games.pacman.actor.GhostState.SAFE;
import static de.amr.games.pacman.actor.GhostState.SCATTERING;
import static de.amr.games.pacman.model.Game.TS;
import static de.amr.games.pacman.model.Maze.NESW;
import static de.amr.games.pacman.navigation.NavigationSystem.keepDirection;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;
import java.util.stream.Stream;

import de.amr.easy.game.sprite.Sprite;
import de.amr.games.pacman.controller.StateMachineControlled;
import de.amr.games.pacman.controller.event.GameEvent;
import de.amr.games.pacman.controller.event.GhostKilledEvent;
import de.amr.games.pacman.controller.event.PacManGainsPowerEvent;
import de.amr.games.pacman.controller.event.PacManGettingWeakerEvent;
import de.amr.games.pacman.controller.event.PacManLostPowerEvent;
import de.amr.games.pacman.model.Game;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.Tile;
import de.amr.games.pacman.navigation.Navigation;
import de.amr.games.pacman.theme.GhostColor;
import de.amr.games.pacman.theme.PacManTheme;
import de.amr.statemachine.StateMachine;

/**
 * A ghost.
 * 
 * @author Armin Reichert
 */
public class Ghost extends MazeMover implements StateMachineControlled<GhostState, GameEvent> {

	private final Game game;
	private final StateMachine<GhostState, GameEvent> controller;
	private final Map<GhostState, Navigation> navigationMap;
	private final GhostName name;
	private final PacMan pacMan;
	private final Tile home;
	private final Tile scatteringTarget;
	private final int initialDir;
	BooleanSupplier fnCanLeaveHouse;

	public Ghost(GhostName name, PacMan pacMan, Game game, Tile home, Tile scatteringTarget, int initialDir,
			GhostColor color) {
		this.name = name;
		this.pacMan = pacMan;
		this.game = game;
		this.home = home;
		this.scatteringTarget = scatteringTarget;
		this.initialDir = initialDir;
		fnCanLeaveHouse = () -> getStateObject().isTerminated();
		controller = buildStateMachine();
		navigationMap = new EnumMap<>(GhostState.class);
		createSprites(color);
	}

	public void initGhost() {
		placeAtTile(home, TS / 2, 0);
		setCurrentDir(initialDir);
		setNextDir(initialDir);
		getSprites().forEach(Sprite::resetAnimation);
		sprite = s_color[initialDir];
	}

	// Accessors

	@Override
	public Maze getMaze() {
		return game.getMaze();
	}

	public GhostName getName() {
		return name;
	}

	public Tile getHome() {
		return home;
	}

	public Tile getScatteringTarget() {
		return scatteringTarget;
	}

	@Override
	public float getSpeed() {
		return game.getGhostSpeed(getState(), getTile());
	}

	// Navigation and movement

	public void setNavigation(GhostState state, Navigation navigation) {
		navigationMap.put(state, navigation);
	}

	public Navigation getNavigation() {
		return navigationMap.getOrDefault(getState(), keepDirection());
	}

	@Override
	public int supplyIntendedDir() {
		return getNavigation().computeRoute(this).dir;
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

	// Sprites

	private Sprite sprite;

	private Sprite s_color[] = new Sprite[4];
	private Sprite s_eyes[] = new Sprite[4];
	private Sprite s_frightened;
	private Sprite s_flashing;
	private Sprite s_numbers[] = new Sprite[4];

	private void createSprites(GhostColor color) {
		NESW.dirs().forEach(dir -> {
			s_color[dir] = PacManTheme.ASSETS.ghostColored(color, dir);
			s_eyes[dir] = PacManTheme.ASSETS.ghostEyes(dir);
		});
		for (int i = 0; i < 4; ++i) {
			s_numbers[i] = PacManTheme.ASSETS.greenNumber(i);
		}
		s_frightened = PacManTheme.ASSETS.ghostFrightened();
		s_flashing = PacManTheme.ASSETS.ghostFlashing();
	}

	@Override
	public Stream<Sprite> getSprites() {
		return Stream.of(Stream.of(s_color), Stream.of(s_numbers), Stream.of(s_eyes), Stream.of(s_frightened, s_flashing))
				.flatMap(s -> s);
	}

	@Override
	public Sprite currentSprite() {
		return sprite;
	}

	// State machine

	@Override
	public StateMachine<GhostState, GameEvent> getStateMachine() {
		return controller;
	}

	public void traceTo(Logger logger) {
		controller.traceTo(logger, game.fnTicksPerSec);
	}

	private StateMachine<GhostState, GameEvent> buildStateMachine() {
		return
		/*@formatter:off*/
		StateMachine.define(GhostState.class, GameEvent.class)
			 
			.description(String.format("[Ghost %s]", getName()))
			.initialState(HOME)
		
			.states()

					.state(HOME)
						.onEntry(this::initGhost)
					
					.state(SAFE)
						.timeoutAfter(() -> game.sec(2))
						.onTick(() -> {
							if (getName() != GhostName.Blinky) {
								move();	
								sprite = s_color[getCurrentDir()]; 
							}
						})
					
					.state(AGGRO)
						.onTick(() -> {	move();	sprite = s_color[getCurrentDir()]; })
					
					.state(FRIGHTENED)
						.onEntry(() -> {
							sprite = s_frightened; 
							getNavigation().prepareRoute(this); 
						})
						.onTick(() -> move())
					
					.state(DYING)
						.timeoutAfter(game::getGhostDyingTime)
						.onEntry(() -> {
							sprite = s_numbers[game.getGhostsKilledByEnergizer()]; 
							game.addGhostKilled();
						})
					
					.state(DEAD)
						.onEntry(() -> getNavigation().prepareRoute(this))
						.onTick(() -> {	
							move();
							sprite = s_eyes[getCurrentDir()];
						})
					
					.state(SCATTERING)
						.onTick(() -> {
							move();	
							sprite = s_color[getCurrentDir()]; 
						})
				
			.transitions()

					.when(HOME).then(SAFE)

					.when(SAFE).then(AGGRO)
						.condition(() -> fnCanLeaveHouse.getAsBoolean() && pacMan.getState() != PacManState.GREEDY)
						
					.when(SAFE).then(FRIGHTENED)
						.condition(() -> fnCanLeaveHouse.getAsBoolean() && pacMan.getState() == PacManState.GREEDY)

					.stay(SAFE).on(PacManGainsPowerEvent.class)
					.stay(SAFE).on(PacManGettingWeakerEvent.class)
					.stay(SAFE).on(PacManLostPowerEvent.class)
					.stay(SAFE).on(GhostKilledEvent.class)
						
					.when(AGGRO).then(FRIGHTENED).on(PacManGainsPowerEvent.class)
					.when(AGGRO).then(DEAD).on(GhostKilledEvent.class) // cheating-mode
						
					.stay(FRIGHTENED).on(PacManGainsPowerEvent.class)
					.stay(FRIGHTENED).on(PacManGettingWeakerEvent.class).act(e -> sprite = s_flashing)
					.when(FRIGHTENED).then(AGGRO).on(PacManLostPowerEvent.class)
					.when(FRIGHTENED).then(DYING).on(GhostKilledEvent.class)
						
					.when(DYING).then(DEAD).onTimeout()
					.stay(DYING).on(PacManGainsPowerEvent.class) // cheating-mode
					.stay(DYING).on(PacManGettingWeakerEvent.class) // cheating-mode
						
					.when(DEAD).then(SAFE)
						.condition(() -> (getName() == GhostName.Blinky && getTile().equals(home)) || inGhostHouse())
						.act(this::initGhost)
					
					.stay(DEAD).on(PacManGainsPowerEvent.class)
					.stay(DEAD).on(PacManGettingWeakerEvent.class)
					.stay(DEAD).on(PacManLostPowerEvent.class)
					.stay(DEAD).on(GhostKilledEvent.class) // cheating-mode

		.endStateMachine();
		/*@formatter:on*/
	}
}