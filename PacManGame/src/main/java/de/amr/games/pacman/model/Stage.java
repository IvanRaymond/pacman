package de.amr.games.pacman.model;

import static de.amr.easy.game.Application.loginfo;

import java.util.HashSet;
import java.util.Set;

import de.amr.games.pacman.actor.MovingActor;

/**
 * Manages the actors which take part in the game.
 * 
 * @author Armin Reichert
 */
public class Stage {

	private Set<MovingActor<?>> actors = new HashSet<>();

	public boolean contains(MovingActor<?> actor) {
		return actors.contains(actor);
	}

	public void add(MovingActor<?> actor) {
		actors.add(actor);
		actor.init();
		actor.visible = true;
		loginfo("%s has been added to stage", actor.name);
	}

	public void remove(MovingActor<?> actor) {
		actors.remove(actor);
		actor.visible = false;
		loginfo("%s has been removed from stage", actor.name);
	}
}