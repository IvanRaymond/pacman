package de.amr.games.pacman.controller.actor;

import static de.amr.games.pacman.model.Direction.RIGHT;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import de.amr.easy.game.entity.Entity;
import de.amr.easy.game.math.Vector2f;
import de.amr.easy.game.ui.sprites.SpriteMap;
import de.amr.games.pacman.controller.actor.steering.MovementControl;
import de.amr.games.pacman.controller.actor.steering.MovementType;
import de.amr.games.pacman.controller.actor.steering.Steering;
import de.amr.games.pacman.controller.actor.steering.common.FollowingKeys;
import de.amr.games.pacman.controller.actor.steering.common.HeadingForTargetTile;
import de.amr.games.pacman.controller.actor.steering.common.RandomMovement;
import de.amr.games.pacman.controller.actor.steering.common.TakingFixedPath;
import de.amr.games.pacman.controller.actor.steering.common.TakingShortestPath;
import de.amr.games.pacman.controller.event.PacManGameEvent;
import de.amr.games.pacman.model.Direction;
import de.amr.games.pacman.model.Game;
import de.amr.games.pacman.model.world.Bed;
import de.amr.games.pacman.model.world.World;
import de.amr.games.pacman.model.world.Tile;
import de.amr.statemachine.api.Fsm;
import de.amr.statemachine.api.FsmContainer;

/**
 * A creature (ghost, Pac-Man) is an entity that can move through the world and has a finite-state
 * machine to control its behavior.
 * 
 * @param <STATE> state (identifier) type
 * 
 * @author Armin Reichert
 */
public abstract class Creature<STATE> extends Entity implements WorldMover, FsmContainer<STATE, PacManGameEvent> {

	public final String name;
	public final SpriteMap sprites = new SpriteMap();

	protected Game game;
	protected World world;
	protected Bed bed;
	protected Fsm<STATE, PacManGameEvent> brain;
	protected Map<STATE, Steering> steerings;
	protected MovementControl movement;
	protected Supplier<Float> fnSpeedLimit;
	protected Direction moveDir;
	protected Direction wishDir;
	protected Tile targetTile;
	protected boolean enteredNewTile;

	public Creature(String name, Map<STATE, Steering> steerings) {
		this.name = name;
		this.movement = new MovementControl(this);
		this.steerings = steerings;
		tf.width = Tile.SIZE;
		tf.height = Tile.SIZE;
	}

	public void setWorld(World world) {
		this.world = world;
	}

	public float getSpeedLimit() {
		return fnSpeedLimit.get();
	}

	public void setSpeedLimit(Supplier<Float> fnSpeedLimit) {
		this.fnSpeedLimit = fnSpeedLimit;
	}

	public void assignBed(Bed bed) {
		this.bed = bed;
	}

	public Bed bed() {
		return bed;
	}

	/**
	 * @return how fast (px/s) this creature can move at most
	 */
	public final float speedLimit() {
		return fnSpeedLimit.get();
	}

	/**
	 * @return the current steering for this actor.
	 */
	public Steering steering() {
		return steerings.getOrDefault(getState(), () -> {
			// do nothing
		});
	}

	/**
	 * Returns the steering for the given state.
	 * 
	 * @param state state
	 * @return steering defined for this state
	 */
	public Steering steering(STATE state) {
		if (steerings.containsKey(state)) {
			return steerings.get(state);
		}
		throw new IllegalArgumentException(String.format("%s: No steering found for state %s", this, state));
	}

	/**
	 * Defines the steering for the given state.
	 * 
	 * @param state    state
	 * @param steering steering defined for this state
	 */
	public void behavior(STATE state, Steering steering) {
		steerings.put(state, steering);
	}

	@Override
	public World world() {
		return world;
	}

	@Override
	public String toString() {
		Tile tile = tile();
		return String.format("(%s, col:%d, row:%d, %s)", name, tile.col, tile.row, getState());
	}

	@Override
	public Fsm<STATE, PacManGameEvent> fsm() {
		return brain;
	}

	@Override
	public void init() {
		moveDir = wishDir = RIGHT;
		targetTile = null;
		enteredNewTile = true;
		brain.init();
		movement.init();
	}

	public void placeAt(Tile tile) {
		placeAt(tile, 0, 0);
	}

	public void placeAt(Tile tile, float xOffset, float yOffset) {
		tf.setPosition(tile.x() + xOffset, tile.y() + yOffset);
		enteredNewTile = !tile.equals(tile());
	}

	public boolean isTeleporting() {
		return movement.is(MovementType.TELEPORTING);
	}

	@Override
	public Tile tile() {
		Vector2f center = tf.getCenter();
		int col = (int) (center.x >= 0 ? center.x / Tile.SIZE : Math.floor(center.x / Tile.SIZE));
		int row = (int) (center.y >= 0 ? center.y / Tile.SIZE : Math.floor(center.y / Tile.SIZE));
		return Tile.at(col, row);
	}

	@Override
	public boolean enteredNewTile() {
		return enteredNewTile;
	}

	public void setEnteredNewTile(boolean enteredNewTile) {
		this.enteredNewTile = enteredNewTile;
	}

	@Override
	public Direction moveDir() {
		return moveDir;
	}

	public void setMoveDir(Direction dir) {
		moveDir = Objects.requireNonNull(dir);
	}

	@Override
	public Direction wishDir() {
		return wishDir;
	}

	@Override
	public void setWishDir(Direction dir) {
		wishDir = dir;
	}

	@Override
	public Tile targetTile() {
		return targetTile;
	}

	@Override
	public void setTargetTile(Tile tile) {
		targetTile = tile;
	}

	@Override
	public boolean canCrossBorderTo(Direction dir) {
		Tile currentTile = tile(), neighbor = world.neighbor(currentTile, dir);
		return canMoveBetween(currentTile, neighbor);
	}

	@Override
	public boolean canMoveBetween(Tile tile, Tile neighbor) {
		return world.isAccessible(neighbor);
	}

	@Override
	public void forceMoving(Direction dir) {
		setWishDir(dir);
		movement.update();
	}

	/**
	 * @param up    key for moving up
	 * @param right key for moving right
	 * @param down  key for moving down
	 * @param left  key for moving left
	 * 
	 * @return steering using the given keys
	 */
	public Steering followingKeys(int up, int right, int down, int left) {
		return new FollowingKeys(this, up, right, down, left);
	}

	/**
	 * Lets the actor move randomly through the maze while respecting the maze structure (for example,
	 * chasing and scattering ghost may not move upwards at dedicated tiles. Also reversing the
	 * direction is never allowed.
	 * 
	 * @return random move behavior
	 */
	public Steering movingRandomly() {
		return new RandomMovement(this);
	}

	/**
	 * Lets the actor head for a variable (probably unreachable) target tile by taking the "best"
	 * direction at every intersection.
	 * 
	 * @return behavior where actor heads for the target tile
	 */
	public Steering headingFor(Supplier<Tile> fnTargetTile) {
		return new HeadingForTargetTile(this, fnTargetTile);
	}

	/**
	 * Lets the actor head for a constant (probably unreachable) target tile by taking the "best"
	 * direction at every intersection.
	 * 
	 * @return behavior where actor heads for the target tile
	 */
	public Steering headingFor(Tile targetTile) {
		return headingFor(() -> targetTile);
	}

	/**
	 * Lets the actor follow the shortest path to the target. Depending on the actor's current state,
	 * this path might not be completely accessible for the actor.
	 * 
	 * @param fnTarget function supplying the target tile
	 * 
	 * @return behavior where an actor follows the shortest (using Manhattan distance) path to a target
	 *         tile
	 */
	public Steering takingShortestPath(Supplier<Tile> fnTarget) {
		return new TakingShortestPath(this, fnTarget);
	}

	/**
	 * Lets the actor follow a fixed path to the target. As the rules for accessing tiles are not
	 * checked, the actor may get stuck.
	 * 
	 * @param path the path to follow
	 * 
	 * @return behavior where actor follows the given path
	 */
	public Steering takingFixedPath(List<Tile> path) {
		if (path.isEmpty()) {
			throw new IllegalArgumentException("Path must not be empty");
		}
		return new TakingFixedPath(this, path);
	}
}