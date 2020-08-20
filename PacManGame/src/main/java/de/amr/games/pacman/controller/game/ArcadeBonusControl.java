package de.amr.games.pacman.controller.game;

import static de.amr.easy.game.Application.loginfo;
import static de.amr.games.pacman.controller.game.BonusState.ABSENT;
import static de.amr.games.pacman.controller.game.BonusState.CONSUMED;
import static de.amr.games.pacman.controller.game.BonusState.PRESENT;
import static de.amr.games.pacman.controller.game.GameController.sec;

import java.util.Random;

import de.amr.games.pacman.controller.event.BonusFoundEvent;
import de.amr.games.pacman.controller.event.PacManGameEvent;
import de.amr.games.pacman.model.game.Game;
import de.amr.games.pacman.model.world.api.World;
import de.amr.games.pacman.model.world.arcade.ArcadeBonus;
import de.amr.games.pacman.model.world.arcade.ArcadeWorld;
import de.amr.statemachine.core.StateMachine;

/**
 * Bonus food (fruits, symbols) appear at a dedicated position for around 9 seconds. When consumed,
 * the bonus is displayed for 3 seconds as a number representing its value and then it disappears.
 * 
 * @author Armin Reichert
 */
public class ArcadeBonusControl extends StateMachine<BonusState, PacManGameEvent> {

	public ArcadeBonusControl(Game game, World world) {
		super(BonusState.class);
		/*@formatter:off*/
		beginStateMachine()
			.description("Bonus Controller")
			.initialState(ABSENT)
			.states()
			
				.state(ABSENT)
					.onEntry(world::clearBonusFood)
			
				.state(PRESENT)
					.timeoutAfter(() -> sec(Game.BONUS_SECONDS + new Random().nextFloat()))
					.onEntry(() -> {
							ArcadeBonus bonus = ArcadeBonus.valueOf(game.level.bonusSymbol);
							bonus.setValue(game.level.bonusValue);
							world.showBonusFood(bonus, ArcadeWorld.BONUS_LOCATION);
							loginfo("Bonus '%s' activated for %.2f sec", bonus, state().getDuration() / 60f);
					})
				
				.state(CONSUMED).timeoutAfter(sec(3))

			.transitions()
				
				.when(PRESENT).then(CONSUMED).on(BonusFoundEvent.class)
					.act(() -> {
						world.bonusFood().ifPresent(bonusFood -> {
							ArcadeBonus bonus = (ArcadeBonus) bonusFood;
							bonus.consume();
							loginfo("Bonus '%s' consumed after %.2f sec",	bonusFood, state().getTicksConsumed() / 60f);
						});
					})
					
				.when(PRESENT).then(ABSENT).onTimeout()
					.act(() -> {
						world.bonusFood().ifPresent(bonusFood -> {
							ArcadeBonus bonus = (ArcadeBonus) bonusFood;
							bonus.consume();
							loginfo("Bonus '%s' not consumed", bonusFood);
						});
					})
				
				.when(CONSUMED).then(ABSENT).onTimeout()
		
		.endStateMachine();
		/*@formatter:on*/
		init();
	}
}