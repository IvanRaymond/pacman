package de.amr.games.pacman.actor;

import static de.amr.easy.game.Application.app;
import static de.amr.games.pacman.actor.BonusState.ACTIVE;
import static de.amr.games.pacman.actor.BonusState.CONSUMED;
import static de.amr.games.pacman.actor.BonusState.INACTIVE;
import static java.util.Arrays.binarySearch;

import java.awt.Graphics2D;
import java.util.logging.Logger;

import de.amr.easy.game.entity.Entity;
import de.amr.games.pacman.actor.core.MazeResident;
import de.amr.games.pacman.actor.fsm.StateMachineComponent;
import de.amr.games.pacman.actor.fsm.StateMachineContainer;
import de.amr.games.pacman.actor.fsm.StateMachineControlled;
import de.amr.games.pacman.controller.event.BonusFoundEvent;
import de.amr.games.pacman.controller.event.PacManGameEvent;
import de.amr.games.pacman.model.BonusSymbol;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.PacManGame;
import de.amr.statemachine.StateMachine;

/**
 * Bonus symbol (fruit or other symbol) that appears at the maze bonus position
 * for around 9 seconds. When consumed, the bonus is displayed for 3 seconds as
 * a number representing its value and then disappears.
 * 
 * @author Armin Reichert
 */
public class Bonus extends Entity implements MazeResident, StateMachineContainer<BonusState> {

	public final PacManGameCast cast;
	public final StateMachineComponent<BonusState> fsmComponent;
	public final BonusSymbol symbol;
	public final int value;

	public Bonus(PacManGameCast cast) {
		this.cast = cast;
		fsmComponent = buildFsmComponent("Bonus");
		tf.setWidth(Maze.TS);
		tf.setHeight(Maze.TS);
		symbol = cast.game.level.bonusSymbol;
		value = cast.game.level.bonusValue;
		sprites.set("symbol", cast.theme.spr_bonusSymbol(symbol));
		sprites.set("number", cast.theme.spr_pinkNumber(binarySearch(PacManGame.BONUS_NUMBERS, value)));
	}

	@Override
	public Entity entity() {
		return this;
	}

	@Override
	public Maze maze() {
		return cast.game.maze;
	}

	@Override
	public StateMachineControlled<BonusState> fsmComponent() {
		return fsmComponent;
	}

	private StateMachineComponent<BonusState> buildFsmComponent(String name) {
		StateMachine<BonusState, PacManGameEvent> fsm = buildStateMachine();
		fsm.traceTo(Logger.getLogger("StateMachineLogger"), app().clock::getFrequency);
		return new StateMachineComponent<>(name, fsm);
	}

	private StateMachine<BonusState, PacManGameEvent> buildStateMachine() {
		return StateMachine.
		/*@formatter:off*/
		beginStateMachine(BonusState.class, PacManGameEvent.class)
			.description("[Bonus]")
			.initialState(ACTIVE)
			.states()
				.state(ACTIVE)
					.timeoutAfter(cast.game.level::bonusActiveTicks)
					.onEntry(() -> {
						placeAtTile(cast.game.maze.bonusTile, Maze.TS / 2, 0);
						activate();
						sprites.select("symbol");
					})
				.state(CONSUMED)
					.timeoutAfter(cast.game.level::bonusConsumedTicks)
					.onEntry(() -> {
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
	public void init() {
		super.init();
		fsmComponent.init();
	}

	@Override
	public void update() {
		super.update();
		fsmComponent.update();
	}

	@Override
	public void draw(Graphics2D g) {
		sprites.current().ifPresent(sprite -> {
			// center sprite over collision box
			float dx = tf.getX() + tf.getWidth() / 2 - sprite.getWidth() / 2;
			float dy = tf.getY() + tf.getHeight() / 2 - sprite.getHeight() / 2;
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