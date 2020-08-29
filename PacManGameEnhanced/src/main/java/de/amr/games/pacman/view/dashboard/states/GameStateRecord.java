package de.amr.games.pacman.view.dashboard.states;

import de.amr.games.pacman.controller.creatures.ghost.GhostMentalState;
import de.amr.games.pacman.controller.steering.api.Guy;
import de.amr.games.pacman.model.world.api.Direction;
import de.amr.games.pacman.model.world.components.Tile;

/**
 * Holds the actor data displayed in the game state view.
 * 
 * @author Armin Reichert
 */
class GameStateRecord {

	public Guy creature;
	public boolean included;
	public String name;
	public Tile tile;
	public Tile target;
	public Direction moveDir;
	public Direction wishDir;
	public float speed;
	public String state;
	public GhostMentalState ghostSanity;
	public long ticksRemaining;
	public long duration;
	public boolean pacManCollision;
}