package de.amr.games.pacman.controller.actor;

import static de.amr.games.pacman.controller.actor.Creature.Movement.MOVING_INSIDE_MAZE;
import static de.amr.games.pacman.controller.actor.Creature.Movement.TELEPORTING;
import static de.amr.games.pacman.model.Direction.RIGHT;
import static de.amr.games.pacman.model.Game.sec;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import de.amr.easy.game.entity.Entity;
import de.amr.easy.game.math.Vector2f;
import de.amr.easy.game.ui.sprites.SpriteMap;
import de.amr.games.pacman.controller.PacManStateMachineLogging;
import de.amr.games.pacman.controller.actor.steering.Steering;
import de.amr.games.pacman.controller.actor.steering.common.FollowingKeys;
import de.amr.games.pacman.controller.actor.steering.common.HeadingForTargetTile;
import de.amr.games.pacman.controller.actor.steering.common.MovingRandomlyWithoutTurningBack;
import de.amr.games.pacman.controller.actor.steering.common.TakingFixedPath;
import de.amr.games.pacman.controller.actor.steering.common.TakingShortestPath;
import de.amr.games.pacman.controller.event.PacManGameEvent;
import de.amr.games.pacman.model.Direction;
import de.amr.games.pacman.model.Game;
import de.amr.games.pacman.model.Tile;
import de.amr.games.pacman.model.world.PacManWorld;
import de.amr.games.pacman.model.world.Portal;
import de.amr.games.pacman.model.world.Seat;
import de.amr.statemachine.api.Fsm;
import de.amr.statemachine.api.FsmContainer;
import de.amr.statemachine.core.StateMachine;

/**
 * A creature (ghost, Pac-Man) is an entity that can move through the maze and uses finite-state
 * machine to control its behavior.
 * 
 * @param <STATE> state (identifier) type
 * 
 * @author Armin Reichert
 */
public abstract class Creature<STATE> extends Entity implements MazeMover, FsmContainer<STATE, PacManGameEvent> {

	enum Movement {
		MOVING_INSIDE_MAZE, TELEPORTING;
	}

	public final Game game;
	public final PacManWorld world;
	public final String name;
	public final SpriteMap sprites = new SpriteMap();
	public Seat seat;

	protected Fsm<STATE, PacManGameEvent> brain;
	protected Map<STATE, Steering> steerings;
	protected StateMachine<Movement, Void> movement;
	protected Direction moveDir;
	protected Direction wishDir;
	protected Tile targetTile;
	protected boolean enteredNewTile;
	protected Portal portalEntered;

	public Creature(Game game, String name, Map<STATE, Steering> steerings) {
		this.game = game;
		this.world = game.world;
		this.name = name;
		this.steerings = steerings;
		tf.width = Tile.SIZE;
		tf.height = Tile.SIZE;
		movement = StateMachine
		//@formatter:off
			.beginStateMachine(Movement.class, Void.class)
				.description(String.format("[%s movement]", name))
				.initialState(MOVING_INSIDE_MAZE)
				.states()
					.state(MOVING_INSIDE_MAZE)
						.onTick(() -> {
							moveInsideMaze();
							checkPortalEntered();
						})
					.state(TELEPORTING)
				.transitions()
					.when(MOVING_INSIDE_MAZE).then(TELEPORTING).condition(this::portalEntered)
					.when(TELEPORTING).then(MOVING_INSIDE_MAZE).onTimeout().act(this::teleport)
			.endStateMachine();
		//@formatter:on
		setTeleportingDuration(sec(0.5f));
		movement.getTracer().setLogger(PacManStateMachineLogging.LOGGER);
	}

	private boolean portalEntered() {
		return portalEntered != null;
	}

	private void checkPortalEntered() {
		for (Portal portal : world.portals()) {
			if (portal.contains(tile())) {
				portalEntered = portal;
				visible = false;
				break;
			}
		}
	}

	private void teleport() {
		portalEntered.teleport(this, tile(), moveDir);
		portalEntered = null;
		visible = true;
	}

	/**
	 * @param game the game
	 * @return how many pixels this creature can move at most in the current frame
	 */
	public abstract float currentSpeed(Game game);

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
	public PacManWorld world() {
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
		return movement.is(TELEPORTING);
	}

	public void setTeleportingDuration(int ticks) {
		movement.state(TELEPORTING).setTimer(ticks);
	}

	@Override
	public Tile tile() {
		Vector2f center = tf.getCenter();
		int col = (int) (center.x >= 0 ? center.x / Tile.SIZE : Math.floor(center.x / Tile.SIZE));
		int row = (int) (center.y >= 0 ? center.y / Tile.SIZE : Math.floor(center.y / Tile.SIZE));
		return Tile.col_row(col, row);
	}

	@Override
	public boolean enteredNewTile() {
		return enteredNewTile;
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
		return !world.isInaccessible(neighbor);
	}

	@Override
	public void forceMoving(Direction dir) {
		setWishDir(dir);
		movement.update();
	}

	/**
	 * Computes how many pixels this creature can move towards the given direction at its current speed
	 * before entering an inaccessible neighbor tile.
	 * 
	 * @param tile tile from where to move
	 * @param dir  move direction
	 */
	private float maxMoveDistance(Tile tile, Direction dir) {
		float speed = currentSpeed(game);
		if (canCrossBorderTo(dir)) {
			return speed;
		}
		float offsetX = tf.x - tile.x(), offsetY = tf.y - tile.y();
		switch (dir) {
		case UP:
			return Math.min(offsetY, speed);
		case DOWN:
			return Math.min(-offsetY, speed);
		case LEFT:
			return Math.min(offsetX, speed);
		case RIGHT:
			return Math.min(-offsetX, speed);
		default:
			throw new IllegalArgumentException("Illegal move direction: " + dir);
		}
	}

	private void moveInsideMaze() {
		Tile tile = tile();
		float speed = maxMoveDistance(tile, moveDir);
		if (wishDir != null && wishDir != moveDir) {
			float wishDirSpeed = maxMoveDistance(tile, wishDir);
			if (wishDirSpeed > 0) {
				boolean corner = (wishDir == moveDir.left() || wishDir == moveDir.right());
				if (corner && steering().requiresGridAlignment()) {
					placeAt(tile);
				}
				moveDir = wishDir;
				speed = wishDirSpeed;
			}
		}
		tf.setVelocity(Vector2f.smul(speed, moveDir.vector()));
		tf.move();
		enteredNewTile = !tile.equals(tile());
	}

	/**
	 * @param up    key for moving up
	 * @param right key for moving right
	 * @param down  key for moving down
	 * @param left  key for moving left
	 * 
	 * @return steering using the given keys
	 */
	public Steering isFollowingKeys(int up, int right, int down, int left) {
		return new FollowingKeys(this, up, right, down, left);
	}

	/**
	 * Lets the actor move randomly through the maze while respecting the maze structure (for example,
	 * chasing and scattering ghost may not move upwards at dedicated tiles. Also reversing the
	 * direction is never allowed.
	 * 
	 * @return random move behavior
	 */
	public Steering isMovingRandomlyWithoutTurningBack() {
		return new MovingRandomlyWithoutTurningBack(this);
	}

	/**
	 * Lets the actor head for a variable (probably unreachable) target tile by taking the "best"
	 * direction at every intersection.
	 * 
	 * @return behavior where actor heads for the target tile
	 */
	public Steering isHeadingFor(Supplier<Tile> fnTargetTile) {
		return new HeadingForTargetTile(this, fnTargetTile);
	}

	/**
	 * Lets the actor head for a constant (probably unreachable) target tile by taking the "best"
	 * direction at every intersection.
	 * 
	 * @return behavior where actor heads for the target tile
	 */
	public Steering isHeadingFor(Tile targetTile) {
		return isHeadingFor(() -> targetTile);
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
	public Steering isTakingShortestPath(Supplier<Tile> fnTarget) {
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
	public Steering isTakingFixedPath(List<Tile> path) {
		if (path.isEmpty()) {
			throw new IllegalArgumentException("Path must not be empty");
		}
		return new TakingFixedPath(this, path);
	}

}