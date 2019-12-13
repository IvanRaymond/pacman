package de.amr.games.pacman.model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Level symbols displayed at the right lower corner.
 * 
 * @author Armin Reichert
 */
public class LevelSymbols implements Iterable<BonusSymbol> {

	private final Deque<BonusSymbol> q = new ArrayDeque<>(7);

	public int count() {
		return q.size();
	}

	@Override
	public Iterator<BonusSymbol> iterator() {
		return q.iterator();
	}

	public void clear() {
		q.clear();
	}

	public void add(BonusSymbol symbol) {
		if (q.size() == 7) {
			q.removeLast();
		}
		q.addFirst(symbol);
	}
}