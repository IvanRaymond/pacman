package de.amr.games.pacman.actor;

import static de.amr.easy.game.Application.app;
import static de.amr.games.pacman.actor.BonusState.ACTIVE;
import static de.amr.games.pacman.actor.BonusState.CONSUMED;
import static de.amr.games.pacman.actor.BonusState.INACTIVE;

import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.logging.Logger;

import de.amr.games.pacman.controller.event.BonusFoundEvent;
import de.amr.games.pacman.controller.event.PacManGameEvent;
import de.amr.games.pacman.model.BonusSymbol;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.PacManGame;
import de.amr.statemachine.StateMachine;

/**
 * Bonus symbol (fruit or other symbol) that appears at the maze bonus position for around 9
 * seconds. When consumed, the bonus is displayed for 3 seconds as a number representing its value
 * and then disappears.
 * 
 * @author Armin Reichert
 */
public class Bonus extends MazeResident implements Actor<BonusState> {

	private final ActorPrototype<BonusState> _actor;
	public final PacManGameCast cast;
	public final BonusSymbol symbol;
	public final int value;

	public Bonus(PacManGameCast cast, int activeTime, int consumedTime) {
		super(cast.game.maze);
		this.cast = cast;
		this.symbol = cast.game.level.bonusSymbol;
		this.value = cast.game.level.bonusValue;
		_actor = new ActorPrototype<>("Bonus", buildStateMachine(activeTime, consumedTime));
		_actor.fsm.traceTo(Logger.getLogger("StateMachineLogger"), app().clock::getFrequency);
		init();
	}

	private StateMachine<BonusState, PacManGameEvent> buildStateMachine(int activeTime, int consumedTime) {
		return StateMachine.
		/*@formatter:off*/
		beginStateMachine(BonusState.class, PacManGameEvent.class)
			.description("[Bonus]")
			.initialState(ACTIVE)
			.states()
				.state(ACTIVE)
					.timeoutAfter(activeTime)
					.onEntry(() -> {
						sprites.set("symbol", cast.theme.spr_bonusSymbol(symbol));
						sprites.select("symbol");
					})
				.state(CONSUMED)
					.timeoutAfter(consumedTime)
					.onEntry(() -> {
						sprites.set("number", cast.theme.spr_pinkNumber(numberIndex(value)));
						sprites.select("number");
					})
				.state(INACTIVE)
					.onEntry(cast::removeBonus)
			.transitions()
				.when(ACTIVE).then(CONSUMED).on(BonusFoundEvent.class)
				.when(ACTIVE).then(INACTIVE).onTimeout()
				.when(CONSUMED).then(INACTIVE).onTimeout()
		.endStateMachine();
		/*@formatter:on*/
	}

	@Override
	public Actor<BonusState> _actor() {
		return _actor;
	}

	@Override
	public void init() {
		super.init();
		_actor.init();
	}

	@Override
	public void update() {
		super.update();
		_actor.update();
	}

	private int numberIndex(int value) {
		int index = Arrays.binarySearch(PacManGame.BONUS_NUMBERS, value);
		if (index >= 0) {
			return index;
		}
		throw new IllegalArgumentException("Illegal bonus value: " + value);
	}

	@Override
	public void draw(Graphics2D g) {
		sprites.current().ifPresent(sprite -> {
			// center sprite over collision box
			float dx = tf.getX() - (sprite.getWidth() - Maze.TS) / 2;
			float dy = tf.getY() - (sprite.getHeight() - Maze.TS) / 2;
			g.translate(dx, dy);
			sprite.draw(g);
			g.translate(-dx, -dy);
		});
	}

	@Override
	public String toString() {
		return String.format("Bonus(%s,%d)", symbol, value);
	}
}