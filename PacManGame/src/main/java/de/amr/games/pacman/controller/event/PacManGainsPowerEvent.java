package de.amr.games.pacman.controller.event;

public class PacManGainsPowerEvent implements PacManGameEvent {

	public final long duration;

	public PacManGainsPowerEvent(long ticks) {
		this.duration = ticks;
	}
}
