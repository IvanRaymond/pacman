package de.amr.games.pacman.model;

import static de.amr.games.pacman.model.PacManGame.TS;
import static java.lang.Math.round;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.amr.easy.game.Application;
import de.amr.easy.game.assets.Assets;
import de.amr.easy.game.entity.Entity;
import de.amr.easy.game.math.Vector2f;
import de.amr.graph.core.api.UndirectedEdge;
import de.amr.graph.grid.api.GridGraph2D;
import de.amr.graph.grid.api.Topology;
import de.amr.graph.grid.impl.GridGraph;
import de.amr.graph.grid.impl.Top4;
import de.amr.graph.pathfinder.api.GraphSearch;
import de.amr.graph.pathfinder.api.Path;
import de.amr.graph.pathfinder.impl.AStarSearch;

/**
 * The original Pac-Man maze.
 * 
 * <p>
 * The maze is a 2-dimensional grid of tiles, each tile contains a character representing its
 * content. Additionally, a grid graph structure is used to allow running path finders on the graph.
 * 
 * @author Armin Reichert
 * 
 * @see GridGraph2D
 */
public class Maze {

	/** The four move directions: NORTH, EAST, SOUTH and WEST. */
	public static final Topology NESW = Top4.get();

	private static final char WALL = '#';
	private static final char DOOR = 'D';
	private static final char TUNNEL = 'T';
	private static final char TELEPORT_L = '<';
	private static final char TELEPORT_R = '>';
	private static final char SPACE = ' ';
	private static final char PELLET = '.';
	private static final char ENERGIZER = '*';
	private static final char EATEN = '%';

	private final Tile[][] board;
	private final Set<Tile> unrestrictedIS = new HashSet<>();
	private final Set<Tile> upwardsBlockedIS = new HashSet<>();

	// dedicated tiles
	private Tile pacManHome;
	private Tile blinkyHome, blinkyScatterTarget;
	private Tile pinkyHome, pinkyScatterTarget;
	private Tile inkyHome, inkyScatterTarget;
	private Tile clydeHome, clydeScatterTarget;
	private Tile bonus;
	private Tile teleportLeft, teleportRight;

	public final GridGraph<Character, Void> graph;

	private int foodTotal;
	private long pathFinderCalls;

	public Maze() {
		String[] map = Assets.text("maze.txt").split("\n");
		int numCols = map[0].length(), numRows = map.length;
		board = new Tile[numCols][numRows];
		for (int row = 0; row < numRows; ++row) {
			for (int col = 0; col < numCols; ++col) {
				Tile tile = board[col][row] = new Tile(col, row, SPACE);
				switch (map[row].charAt(col)) {
				case WALL:
					tile.content = WALL;
					break;
				case DOOR:
					tile.content = DOOR;
					break;
				case TUNNEL:
					tile.content = TUNNEL;
					break;
				case TELEPORT_L:
					tile.content = TUNNEL;
					teleportLeft = tile;
					break;
				case TELEPORT_R:
					tile.content = TUNNEL;
					teleportRight = tile;
					break;
				case PELLET:
					tile.content = PELLET;
					foodTotal += 1;
					break;
				case ENERGIZER:
					tile.content = ENERGIZER;
					foodTotal += 1;
					break;
				case 'O':
					pacManHome = tile;
					break;
				case 'B':
					blinkyHome = tile;
					break;
				case 'P':
					pinkyHome = tile;
					break;
				case 'I':
					inkyHome = tile;
					break;
				case 'C':
					clydeHome = tile;
					break;
				case 'b':
					tile.content = WALL;
					blinkyScatterTarget = tile;
					break;
				case 'p':
					tile.content = WALL;
					pinkyScatterTarget = tile;
					break;
				case 'i':
					tile.content = WALL;
					inkyScatterTarget = tile;
					break;
				case 'c':
					tile.content = WALL;
					clydeScatterTarget = tile;
					break;
				case '$':
					bonus = tile;
					break;
				default:
					break;
				}
			}
		}

		// The graph represents the maze and stores the maze content inside its vertices.
		graph = new GridGraph<>(numCols, numRows, NESW, v -> null, (u, v) -> null, UndirectedEdge::new);
		graph.setDefaultVertexLabel(v -> board[graph.col(v)][graph.row(v)].content);

		// Add graph edges
		graph.fill();
		// remove edges into walls
		graph.edges().filter(edge -> graph.get(edge.either()) == WALL || graph.get(edge.other()) == WALL)
				.forEach(graph::removeEdge);

		// sort intersections into unrestricted ones and those where ghosts cannot move upwards
		graph.vertices().filter(v -> graph.degree(v) >= 3).mapToObj(this::tile)
		//@formatter:off
				// exclude tiles above ghost house doors
				.filter(tile -> !isDoor(tileToDir(tile, Top4.S)))
				// exclude doors
				.filter(tile -> !isDoor(tile))
				// exclude tiles inside ghost house
				.filter(tile -> !inGhostHouse(tile))
				.forEach(tile -> {
					if (tile == tileToDir(blinkyHome, Top4.W)	
					 || tile == tileToDir(blinkyHome, Top4.E, 2)
					 || tile == tileToDir(pacManHome, Top4.W)
					 || tile == tileToDir(pacManHome, Top4.E, 2)) {
						upwardsBlockedIS.add(tile);
					}
					else {
						unrestrictedIS.add(tile);
					}
				});
		//@formatter:on
	}

	public int numCols() {
		return graph.numCols();
	}

	public int numRows() {
		return graph.numRows();
	}

	public Stream<Tile> tiles() {
		return graph.vertices().mapToObj(this::tile);
	}

	/**
	 * @param col
	 *              a column index
	 * @param row
	 *              a row index
	 * @return a board tile or a new tile if the coordinates are outside of the board
	 */
	public Tile tileAt(int col, int row) {
		return graph.isValidCol(col) && graph.isValidRow(row) ? board[col][row] : new Tile(col, row, TUNNEL);
	}

	/**
	 * @param tile
	 *               reference tile
	 * @param dir
	 *               some direction
	 * @param n
	 *               number of tiles
	 * @return tile that lies <code>n</code> tiles away from the given tile towards the given direction.
	 *         This can be a tile outside of the board!
	 */
	public Tile tileToDir(Tile tile, int dir, int n) {
		return tileAt(tile.col + n * NESW.dx(dir), tile.row + n * NESW.dy(dir));
	}

	/**
	 * @param tile
	 *               reference tile
	 * @param dir
	 *               some direction
	 * @return neighbor towards the given direction. This can be an invalid tile position.
	 */
	public Tile tileToDir(Tile tile, int dir) {
		return tileToDir(tile, dir, 1);
	}

	private int cell(Tile tile) {
		if (insideBoard(tile)) {
			return graph.cell(tile.col, tile.row);
		}
		throw new IllegalArgumentException("Illegal tile: " + tile);
	}

	private Tile tile(int cell) {
		return tileAt(graph.col(cell), graph.row(cell));
	}

	/**
	 * @param entity
	 *                 a game entity
	 * @return the tile where this entity is located
	 */
	public Tile tilePosition(Entity entity) {
		Vector2f center = entity.tf.getCenter();
		return tileAt(round(center.x) / TS, round(center.y) / TS);
	}

	public boolean insideBoard(Tile tile) {
		return graph.isValidCol(tile.col) && graph.isValidRow(tile.row);
	}

	public Tile getTopLeftCorner() {
		return tileAt(1, 4);
	}

	public Tile getTopRightCorner() {
		return tileAt(numCols() - 2, 4);
	}

	public Tile getBottomLeftCorner() {
		return tileAt(1, numRows() - 4);
	}

	public Tile getBottomRightCorner() {
		return tileAt(numCols() - 2, numRows() - 4);
	}

	public Tile getBlinkyScatterTarget() {
		return blinkyScatterTarget;
	}

	public Tile getPinkyScatterTarget() {
		return pinkyScatterTarget;
	}

	public Tile getInkyScatterTarget() {
		return inkyScatterTarget;
	}

	public Tile getClydeScatterTarget() {
		return clydeScatterTarget;
	}

	public Tile getPacManHome() {
		return pacManHome;
	}

	public Tile getBlinkyHome() {
		return blinkyHome;
	}

	public Tile getPinkyHome() {
		return pinkyHome;
	}

	public Tile getInkyHome() {
		return inkyHome;
	}

	public Tile getClydeHome() {
		return clydeHome;
	}

	public Tile getGhostRevivalTile() {
		return pinkyHome;
	}

	public Tile getBonusTile() {
		return bonus;
	}

	public Tile getTeleportLeft() {
		return teleportLeft;
	}

	public Tile getTeleportRight() {
		return teleportRight;
	}

	private char getContent(Tile tile) {
		return insideBoard(tile) ? graph.get(cell(tile)) : TUNNEL;
	}

	public boolean inTunnel(Tile tile) {
		return getContent(tile) == TUNNEL;
	}

	public boolean isWall(Tile tile) {
		return getContent(tile) == WALL;
	}

	public boolean isDoor(Tile tile) {
		return getContent(tile) == DOOR;
	}

	public boolean isGhostHouseEntry(Tile tile) {
		return insideBoard(tile) && isDoor(neighborTile(tile, Top4.S).get());
	}

	public boolean isIntersection(Tile tile) {
		return isUnrestrictedIntersection(tile) || isUpwardsBlockedIntersection(tile);
	}

	public boolean isUnrestrictedIntersection(Tile tile) {
		return unrestrictedIS.contains(tile);
	}

	public boolean isUpwardsBlockedIntersection(Tile tile) {
		return upwardsBlockedIS.contains(tile);
	}

	public boolean inGhostHouse(Tile tile) {
		return Math.abs(tile.row - inkyHome.row) <= 1 && tile.col >= inkyHome.col
				&& tile.col <= clydeHome.col + 1;
	}

	public boolean containsPellet(Tile tile) {
		return getContent(tile) == PELLET;
	}

	public boolean containsEnergizer(Tile tile) {
		return getContent(tile) == ENERGIZER;
	}

	public boolean containsFood(Tile tile) {
		return containsPellet(tile) || containsEnergizer(tile);
	}

	public boolean containsEatenFood(Tile tile) {
		return getContent(tile) == EATEN;
	}

	public void resetFood() {
		graph.clearVertexLabels();
	}

	public int getFoodTotal() {
		return foodTotal;
	}

	public void removeFood() {
		graph.vertices().filter(cell -> graph.get(cell) == PELLET || graph.get(cell) == ENERGIZER)
				.forEach(cell -> graph.set(cell, EATEN));
	}

	public void removeFood(Tile tile) {
		graph.set(cell(tile), EATEN);
	}

	public OptionalInt direction(Tile t1, Tile t2) {
		return graph.direction(cell(t1), cell(t2));
	}

	public Optional<Tile> neighborTile(Tile tile, int dir) {
		OptionalInt neighbor = graph.neighbor(cell(tile), dir);
		return neighbor.isPresent() ? Optional.of(tile(neighbor.getAsInt())) : Optional.empty();
	}

	public Stream<Tile> getAdjacentTiles(Tile tile) {
		return graph.adj(cell(tile)).mapToObj(this::tile);
	}

	public List<Tile> findPath(Tile source, Tile target) {
		if (insideBoard(source) && insideBoard(target)) {
			GraphSearch pathfinder = new AStarSearch(graph, (u, v) -> 1, graph::manhattan);
			Path path = pathfinder.findPath(cell(source), cell(target));
			pathFinderCalls += 1;
			if (pathFinderCalls % 100 == 0) {
				Application.LOGGER.info(String.format("%d'th pathfinding executed", pathFinderCalls));
			}
			return path.vertexStream().boxed().map(this::tile).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	public double euclidean(Tile t1, Tile t2) {
		return graph.euclidean(cell(t1), cell(t2));
	}

	public double manhattan(Tile t1, Tile t2) {
		return graph.manhattan(cell(t1), cell(t2));
	}

	public OptionalInt alongPath(List<Tile> path) {
		return path.size() < 2 ? OptionalInt.empty() : direction(path.get(0), path.get(1));
	}

}