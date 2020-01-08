package de.amr.games.pacman.actor.core;

import de.amr.easy.game.entity.Entity;
import de.amr.easy.game.view.View;
import de.amr.games.pacman.controller.event.PacManGameEvent;
import de.amr.statemachine.client.FsmContainer;

/**
 * An actor (ghost, Pac-Man, bonus) is a visible entity controlled by a
 * contained finite-state machine.
 * 
 * @author Armin Reichert
 *
 * @param <S> state (identifier) type
 */
public interface Actor<S> extends FsmContainer<S, PacManGameEvent>, View {

	Entity entity();
}