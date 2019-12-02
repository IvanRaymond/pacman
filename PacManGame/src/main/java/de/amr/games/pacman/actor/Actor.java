package de.amr.games.pacman.actor;

import static de.amr.easy.game.Application.LOGGER;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import de.amr.games.pacman.controller.event.FoodFoundEvent;
import de.amr.games.pacman.controller.event.PacManGameEvent;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.PacManGame;
import de.amr.statemachine.State;
import de.amr.statemachine.StateMachine;

/**
 * Base class for Pac-Man and the ghosts.
 * <p>
 * An entity controlled by a finite-state machine with the capability of registering event handlers
 * for game events and publishing game events.
 * 
 * @author Armin Reichert
 *
 * @param <S>
 *          state (label) type of the FSM
 */
public abstract class Actor<S> extends MazeMover {

	public final String name;
	public final PacManGame game;
	private boolean active;
	protected final Set<Consumer<PacManGameEvent>> listeners;
	protected StateMachine<S, PacManGameEvent> fsm;

	public Actor(String name, PacManGame game, Maze maze) {
		super(maze);
		this.name = name;
		this.game = game;
		active = false;
		listeners = new LinkedHashSet<>();
	}

	public void activate(boolean newState) {
		if (active != newState) {
			active = newState;
			if (active) {
				init();
				show();
				LOGGER.info(() -> name + " activated");
			}
			else {
				hide();
				LOGGER.info(() -> name + " deactivated");
			}
		}
	}

	public boolean isActive() {
		return active;
	}

	public void addListener(Consumer<PacManGameEvent> listener) {
		listeners.add(listener);
	}

	public void removeListener(Consumer<PacManGameEvent> listener) {
		listeners.remove(listener);
	}

	public void publish(PacManGameEvent event) {
		if (!(event instanceof FoodFoundEvent)) {
			LOGGER.info(() -> String.format("%s reports '%s'", name, event));
		}
		listeners.forEach(listener -> listener.accept(event));
	}

	public S getState() {
		return fsm.getState();
	}

	@SuppressWarnings("unchecked")
	public boolean oneOf(S... states) {
		return Arrays.stream(states).anyMatch(state -> state == getState());
	}

	public void setState(S state) {
		fsm.setState(state);
	}

	public State<S, PacManGameEvent> state() {
		return fsm.state();
	}

	public void process(PacManGameEvent event) {
		fsm.process(event);
	}

	@Override
	public void init() {
		super.init();
		fsm.init();
	}

	@Override
	public void update() {
		super.update();
		fsm.update();
	}
}