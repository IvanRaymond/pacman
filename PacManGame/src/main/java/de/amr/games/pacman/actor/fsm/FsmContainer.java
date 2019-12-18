package de.amr.games.pacman.actor.fsm;

import java.util.function.Consumer;

import de.amr.games.pacman.controller.event.PacManGameEvent;
import de.amr.statemachine.State;
import de.amr.statemachine.StateMachine;

/**
 * This interface is implemented by entities that implement the
 * {@link FsmControlled} interface by delegating to a component implementing
 * that interface.
 * 
 * @author Armin Reichert
 *
 * @param <S> state type of the finite-state machine
 */
public interface FsmContainer<S> extends FsmControlled<S> {

	/**
	 * The component (delegate) implementing the {@link FsmControlled} interface.
	 * 
	 * @return delegate component
	 */
	FsmControlled<S> fsmComponent();

	@Override
	default StateMachine<S, PacManGameEvent> fsm() {
		return fsmComponent().fsm();
	}

	@Override
	default void addGameEventListener(Consumer<PacManGameEvent> listener) {
		fsmComponent().addGameEventListener(listener);
	}

	@Override
	default void removeGameEventListener(Consumer<PacManGameEvent> listener) {
		fsmComponent().removeGameEventListener(listener);
	}

	@Override
	default void setState(S state) {
		fsmComponent().setState(state);
	}

	@Override
	default S getState() {
		return fsmComponent().getState();
	}

	@Override
	@SuppressWarnings("unchecked")
	default boolean is(S... states) {
		return fsmComponent().is(states);
	}

	@Override
	default State<S, PacManGameEvent> state() {
		return fsmComponent().state();
	}

	@Override
	default void process(PacManGameEvent event) {
		fsmComponent().process(event);
	}
}