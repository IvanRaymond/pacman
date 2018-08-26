package de.amr.games.pacman.actor;

import static de.amr.games.pacman.actor.PacManState.DEAD;
import static de.amr.games.pacman.actor.PacManState.DYING;
import static de.amr.games.pacman.actor.PacManState.GREEDY;
import static de.amr.games.pacman.actor.PacManState.HOME;
import static de.amr.games.pacman.actor.PacManState.HUNGRY;
import static de.amr.games.pacman.model.Game.TS;
import static de.amr.games.pacman.model.Maze.NESW;
import static de.amr.games.pacman.navigation.NavigationSystem.keepDirection;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

import de.amr.easy.game.sprite.Sprite;
import de.amr.easy.grid.impl.Top4;
import de.amr.games.pacman.controller.EventManager;
import de.amr.games.pacman.controller.StateMachineControlled;
import de.amr.games.pacman.controller.event.BonusFoundEvent;
import de.amr.games.pacman.controller.event.FoodFoundEvent;
import de.amr.games.pacman.controller.event.GameEvent;
import de.amr.games.pacman.controller.event.PacManGainsPowerEvent;
import de.amr.games.pacman.controller.event.PacManGettingWeakerEvent;
import de.amr.games.pacman.controller.event.PacManGhostCollisionEvent;
import de.amr.games.pacman.controller.event.PacManKilledEvent;
import de.amr.games.pacman.controller.event.PacManLostPowerEvent;
import de.amr.games.pacman.model.Game;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.Tile;
import de.amr.games.pacman.navigation.Navigation;
import de.amr.games.pacman.theme.PacManThemes;
import de.amr.statemachine.StateMachine;
import de.amr.statemachine.StateObject;

/**
 * The one and only.
 * 
 * @author Armin Reichert
 */
public class PacMan extends MazeMover implements StateMachineControlled<PacManState, GameEvent> {

	private final Game game;
	private final StateMachine<PacManState, GameEvent> controller;
	private final Map<PacManState, Navigation> navigationMap;
	private final EventManager<GameEvent> events;
	private boolean eventsEnabled;
	private int digestionTicks;
	private PacManWorld world;

	public PacMan(Game game) {
		this.game = game;
		events = new EventManager<>("[PacMan]");
		eventsEnabled = true;
		controller = buildStateMachine();
		navigationMap = new EnumMap<>(PacManState.class);
		createSprites();
	}

	public void initPacMan() {
		digestionTicks = 0;
		placeAtTile(getHome(), TS / 2, 0);
		setNextDir(Top4.E);
		getSprites().forEach(Sprite::resetAnimation);
		sprite = s_full;
	}
	
	public void setWorld(PacManWorld world) {
		this.world = world;
	}

	// Eventing

	public void subscribe(Consumer<GameEvent> subscriber) {
		events.subscribe(subscriber);
	}

	public EventManager<GameEvent> getEvents() {
		return events;
	}

	public void setEventsEnabled(boolean eventsEnabled) {
		this.eventsEnabled = eventsEnabled;
	}

	private void publishEvent(GameEvent event) {
		if (eventsEnabled) {
			events.publish(event);
		}
	}

	// Accessors

	@Override
	public Maze getMaze() {
		return game.getMaze();
	}

	public Tile getHome() {
		return getMaze().getPacManHome();
	}

	@Override
	public float getSpeed() {
		return game.getPacManSpeed(getState());
	}

	// Navigation and movement

	public void setNavigation(PacManState state, Navigation navigation) {
		navigationMap.put(state, navigation);
	}

	public Navigation getNavigation() {
		return navigationMap.getOrDefault(getState(), keepDirection());
	}

	@Override
	public int supplyIntendedDir() {
		return getNavigation().computeRoute(this).getDir();
	}

	@Override
	public boolean canTraverseDoor(Tile door) {
		return false;
	}

	@Override
	public void move() {
		super.move();
		sprite = s_walking_to[getCurrentDir()];
		sprite.enableAnimation(!isStuck());
	}

	// Sprites

	private Sprite sprite;

	private Sprite s_walking_to[] = new Sprite[4];
	private Sprite s_dying;
	private Sprite s_full;

	private void createSprites() {
		NESW.dirs().forEach(dir -> s_walking_to[dir] = PacManThemes.THEME.pacManWalking(dir));
		s_dying = PacManThemes.THEME.pacManDying();
		s_full = PacManThemes.THEME.pacManFull();
	}

	@Override
	public Stream<Sprite> getSprites() {
		return Stream.of(Stream.of(s_walking_to), Stream.of(s_dying, s_full)).flatMap(s -> s);
	}

	@Override
	public Sprite currentSprite() {
		return sprite;
	}

	public void setFullSprite() {
		sprite = s_full;
	}

	// State machine

	@Override
	public StateMachine<PacManState, GameEvent> getStateMachine() {
		return controller;
	}

	public void traceTo(Logger logger) {
		controller.traceTo(logger, game.fnTicksPerSec);
	}

	private StateMachine<PacManState, GameEvent> buildStateMachine() {
		return
		/* @formatter:off */
		StateMachine.define(PacManState.class, GameEvent.class)
				
			.description("[Pac-Man]")
			.initialState(HOME)

			.states()

				.state(HOME)
					.onEntry(this::initPacMan)
	
				.state(HUNGRY)
					.impl(new HungryState())
					
				.state(GREEDY)
					.impl(new GreedyState())
					.timeoutAfter(game::getPacManGreedyTime)
	
				.state(DYING)
					.onEntry(() -> sprite = s_dying)
					.timeoutAfter(() -> game.sec(2))

			.transitions()

				.when(HOME).then(HUNGRY)
				
				.when(HUNGRY).then(DYING)
					.on(PacManKilledEvent.class)
	
				.when(HUNGRY).then(GREEDY)
					.on(PacManGainsPowerEvent.class)
	
				.stay(GREEDY)
					.on(PacManGainsPowerEvent.class)
					.act(() -> controller.resetTimer())
	
				.when(GREEDY).then(HUNGRY)
					.onTimeout()
					.act(() -> publishEvent(new PacManLostPowerEvent()))
	
				.when(DYING).then(DEAD)
					.onTimeout()

		.endStateMachine();
		/* @formatter:on */
	}

	private class HungryState extends StateObject<PacManState, GameEvent> {

		@Override
		public void onTick() {
			if (digestionTicks > 0) {
				--digestionTicks;
				return;
			}
			move();
			if (eventsEnabled) {
				inspectTile(getTile());
			}
		}

		protected void inspectTile(Tile tile) {
			// Ghost collision?
			/*@formatter:off*/
			Optional<Ghost> collidingGhost = world.getActiveGhosts()
				.filter(ghost -> ghost.getTile().equals(tile))
				.filter(ghost -> ghost.getState() != GhostState.DEAD)
				.filter(ghost -> ghost.getState() != GhostState.DYING)
				.filter(ghost -> ghost.getState() != GhostState.SAFE)
				.findFirst();
			/*@formatter:on*/
			if (collidingGhost.isPresent()) {
				publishEvent(new PacManGhostCollisionEvent(collidingGhost.get()));
				return;
			}

			// Unhonored bonus?
			/*@formatter:off*/
			Optional<Bonus> activeBonus = world.getBonus()
					.filter(bonus -> bonus.getTile().equals(tile))
					.filter(bonus -> !bonus.isHonored());
			/*@formatter:on*/
			if (activeBonus.isPresent()) {
				Bonus bonus = activeBonus.get();
				publishEvent(new BonusFoundEvent(bonus.getSymbol(), bonus.getValue()));
				return;
			}

			// Food?
			if (getMaze().isFood(tile)) {
				boolean energizer = getMaze().isEnergizer(tile);
				digestionTicks = game.getDigestionTicks(energizer);
				publishEvent(new FoodFoundEvent(tile, energizer));
			}
		}
	}

	private class GreedyState extends HungryState {

		@Override
		public void onTick() {
			super.onTick();
			if (getRemaining() == game.getPacManGettingWeakerRemainingTime()) {
				publishEvent(new PacManGettingWeakerEvent());
			}
		}
	}
}