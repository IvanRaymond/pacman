package de.amr.games.pacman.actor;

import static de.amr.easy.game.Application.LOGGER;
import static de.amr.easy.game.Application.app;
import static de.amr.games.pacman.actor.PacManState.DEAD;
import static de.amr.games.pacman.actor.PacManState.DYING;
import static de.amr.games.pacman.actor.PacManState.HOME;
import static de.amr.games.pacman.actor.PacManState.HUNGRY;
import static de.amr.games.pacman.actor.PacManState.POWER;
import static de.amr.games.pacman.model.Maze.NESW;
import static de.amr.games.pacman.model.PacManGame.TS;

import java.awt.event.KeyEvent;
import java.util.Optional;
import java.util.OptionalInt;

import de.amr.easy.game.assets.Sound;
import de.amr.easy.game.input.Keyboard;
import de.amr.easy.game.ui.sprites.Sprite;
import de.amr.easy.grid.impl.Top4;
import de.amr.games.pacman.controller.EventManager;
import de.amr.games.pacman.controller.event.BonusFoundEvent;
import de.amr.games.pacman.controller.event.FoodFoundEvent;
import de.amr.games.pacman.controller.event.GameEvent;
import de.amr.games.pacman.controller.event.PacManGainsPowerEvent;
import de.amr.games.pacman.controller.event.PacManGettingWeakerEvent;
import de.amr.games.pacman.controller.event.PacManGhostCollisionEvent;
import de.amr.games.pacman.controller.event.PacManKilledEvent;
import de.amr.games.pacman.controller.event.PacManLostPowerEvent;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.PacManGame;
import de.amr.games.pacman.model.Tile;
import de.amr.games.pacman.theme.PacManTheme;
import de.amr.statemachine.State;
import de.amr.statemachine.StateMachine;

/**
 * The one and only.
 * 
 * @author Armin Reichert
 */
public class PacMan extends MazeEntity {

	private final PacManGame game;
	private final StateMachine<PacManState, GameEvent> fsm;
	private final EventManager<GameEvent> eventManager;
	private PacManWorld world;

	public PacMan(PacManGame game) {
		this.game = game;
		fsm = buildStateMachine();
		fsm.traceTo(LOGGER, app().clock::getFrequency);
		eventManager = new EventManager<>("[PacMan]");
		setSprites();
	}

	public void initPacMan() {
		placeAtTile(getMaze().getPacManHome(), TS / 2, 0);
		setNextDir(Top4.E);
		sprites.forEach(Sprite::resetAnimation);
		sprites.select("s_full");
	}

	public void setWorld(PacManWorld world) {
		this.world = world;
	}

	// Accessors

	public PacManGame getGame() {
		return game;
	}

	@Override
	public Maze getMaze() {
		return game.getMaze();
	}

	public EventManager<GameEvent> getEventManager() {
		return eventManager;
	}

	@Override
	public float getSpeed() {
		return game.getPacManSpeed(this);
	}

	public PacManTheme getTheme() {
		return app().settings.get("theme");
	}

	public boolean isLosingPower() {
		return getState() == POWER
				&& getStateObject().getTicksRemaining() < getGame().getPacManLosingPowerTicks();
	}

	public boolean hasPower() {
		return getState() == POWER;
	}

	// Movement

	@Override
	public OptionalInt supplyIntendedDir() {
		if (Keyboard.keyDown(KeyEvent.VK_UP)) {
			return OptionalInt.of(Top4.N);
		}
		if (Keyboard.keyDown(KeyEvent.VK_RIGHT)) {
			return OptionalInt.of(Top4.E);
		}
		if (Keyboard.keyDown(KeyEvent.VK_DOWN)) {
			return OptionalInt.of(Top4.S);
		}
		if (Keyboard.keyDown(KeyEvent.VK_LEFT)) {
			return OptionalInt.of(Top4.W);
		}
		return OptionalInt.empty();
	}

	@Override
	public boolean canTraverseDoor(Tile door) {
		return false;
	}

	@Override
	public void move() {
		super.move();
		updateWalkingSprite();
	}

	// Sprites

	private void setSprites() {
		NESW.dirs().forEach(dir -> sprites.set("s_walking_" + dir, getTheme().spr_pacManWalking(dir)));
		sprites.set("s_dying", getTheme().spr_pacManDying());
		sprites.set("s_full", getTheme().spr_pacManFull());
		sprites.select("s_full");
	}

	public void setFullSprite() {
		sprites.select("s_full");
	}

	private void updateWalkingSprite() {
		sprites.select("s_walking_" + getMoveDir());
		sprites.current().enableAnimation(!isStuck());
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

	public PacManState getState() {
		return fsm.getState();
	}

	public State<PacManState, GameEvent> getStateObject() {
		return fsm.state();
	}

	public void processEvent(GameEvent event) {
		fsm.process(event);
	}

	private StateMachine<PacManState, GameEvent> buildStateMachine() {
		return StateMachine.
		/* @formatter:off */
		beginStateMachine(PacManState.class, GameEvent.class)
				
			.description("[Pac-Man]")
			.initialState(HOME)

			.states()

				.state(HOME)
					.onEntry(this::initPacMan)
					.timeoutAfter(() -> app().clock.sec(1.5f))
	
				.state(HUNGRY)
					.impl(new HungryState())
					
				.state(POWER)
					.impl(new PowerState())
					.timeoutAfter(game::getPacManPowerTime)
	
				.state(DYING)
					.impl(new DyingState())

			.transitions()

				.when(HOME).then(HUNGRY).onTimeout()
				
				.when(HUNGRY).then(DYING)
					.on(PacManKilledEvent.class)
	
				.when(HUNGRY).then(POWER)
					.on(PacManGainsPowerEvent.class)
	
				.stay(POWER)
					.on(PacManGainsPowerEvent.class)
					.act(fsm::resetTimer)
	
				.when(POWER).then(HUNGRY)
					.onTimeout()
					.act(() -> getEventManager().publish(new PacManLostPowerEvent()))
	
				.when(DYING).then(DEAD)
					.onTimeout()

		.endStateMachine();
		/* @formatter:on */
	}

	private class HungryState extends State<PacManState, GameEvent> {

		private int digestionTicks;

		@Override
		public void onEntry() {
			digestionTicks = 0;
		}

		@Override
		public void onTick() {
			if (mustDigest()) {
				digest();
			} else {
				move();
				inspectWorld();
			}
		}

		private boolean mustDigest() {
			return digestionTicks > 0;
		}

		private void digest() {
			digestionTicks -= 1;
		}

		protected void inspectWorld() {
			if (world == null || !getEventManager().isEnabled()) {
				return;
			}
			Tile tile = getTile();

			/*@formatter:off*/
			Optional<Ghost> collidingGhost = world.getGhosts()
				.filter(ghost -> ghost.getState() != GhostState.DEAD)
				.filter(ghost -> ghost.getState() != GhostState.DYING)
				.filter(ghost -> ghost.getState() != GhostState.LOCKED)
				.filter(ghost -> ghost.getTile().equals(tile))
				.findFirst();
			/*@formatter:on*/
			if (collidingGhost.isPresent()) {
				getEventManager().publish(new PacManGhostCollisionEvent(collidingGhost.get()));
				return;
			}

			/*@formatter:off*/
			Optional<Bonus> activeBonus = world.getBonus()
					.filter(bonus -> bonus.getTile().equals(tile))
					.filter(bonus -> !bonus.isHonored());
			/*@formatter:on*/
			if (activeBonus.isPresent()) {
				Bonus bonus = activeBonus.get();
				getEventManager().publish(new BonusFoundEvent(bonus.getSymbol(), bonus.getValue()));
				return;
			}

			if (getMaze().isFood(tile)) {
				boolean energizer = getMaze().isEnergizer(tile);
				digestionTicks = game.getDigestionTicks(energizer);
				getEventManager().publish(new FoodFoundEvent(tile, energizer));
			}
		}
	}

	private class PowerState extends HungryState {

		@Override
		public void onEntry() {
			getTheme().snd_waza().loop();
		}

		@Override
		public void onExit() {
			getTheme().snd_waza().stop();
		}

		@Override
		public void onTick() {
			super.onTick();
			if (getTicksRemaining() == game.getPacManLosingPowerTicks()) {
				getEventManager().publish(new PacManGettingWeakerEvent());
			}
		}
	}

	private class DyingState extends State<PacManState, GameEvent> {

		private int paralyzedTime;

		{
			setTimer(() -> app().clock.sec(3));
		}

		@Override
		public void onEntry() {
			paralyzedTime = app().clock.sec(1);
			sprites.current().enableAnimation(false);
			setFullSprite();
			getTheme().snd_clips_all().forEach(Sound::stop);
		}

		@Override
		public void onTick() {
			if (paralyzedTime > 0) {
				paralyzedTime -= 1;
				if (paralyzedTime == 0) {
					getGame().getActiveGhosts().forEach(ghost -> ghost.setVisible(false));
					sprites.select("s_dying");
					getTheme().snd_die().play();
				}
			}
		}
	}

}