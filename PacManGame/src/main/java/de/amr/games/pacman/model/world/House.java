package de.amr.games.pacman.model.world;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A house.
 * 
 * @author Armin Reichert
 */
public class House {

	private final Set<Tile> room;
	private final List<Door> doors;
	private final List<Bed> seats;

	public House(Set<Tile> room, List<Door> doors, List<Bed> seats) {
		this.room = room;
		this.doors = doors;
		this.seats = seats;
	}

	public Stream<Door> doors() {
		return doors.stream();
	}

	public Set<Tile> room() {
		return Collections.unmodifiableSet(room);
	}

	public Stream<Bed> seats() {
		return seats.stream();
	}

	public Bed seat(int i) {
		return seats.get(i);
	}
}