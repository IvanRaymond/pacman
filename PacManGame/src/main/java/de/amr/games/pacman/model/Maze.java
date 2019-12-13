package de.amr.games.pacman.model;

import static de.amr.easy.game.Application.LOGGER;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.amr.graph.core.api.UndirectedEdge;
import de.amr.graph.grid.api.GridGraph2D;
import de.amr.graph.grid.impl.Grid4Topology;
import de.amr.graph.grid.impl.GridGraph;
import de.amr.graph.pathfinder.api.GraphSearch;
import de.amr.graph.pathfinder.api.Path;
import de.amr.graph.pathfinder.impl.AStarSearch;

/**
 * The original Pac-Man maze.
 * 
 * <p>
 * The maze is a 2-dimensional grid of tiles, each tile contains a character representing its
 * content. Additionally, a (grid) graph structure is used to allow running path finders on the
 * graph.
 * 
 * @author Armin Reichert
 * 
 * @see GridGraph2D
 */
public class Maze {

	/** Tile size in pixels. */
	public static final int TS = 8;

	public static final int NUM_COLS = 28;

	public static final int NUM_ROWS = 36;

	private static final String[] CONTENT = {
	/*@formatter:off*/
	"############################", 
	"############################", 
	"############################", 
	"############################", 
	"#............##............#", 
	"#.####.#####.##.#####.####.#", 
	"#*####.#####.##.#####.####*#", 
	"#.####.#####.##.#####.####.#", 
	"#..........................#", 
	"#.####.##.########.##.####.#", 
	"#.####.##.########.##.####.#", 
	"#......##....##....##......#", 
	"######.##### ## #####.######", 
	"######.##### ## #####.######", 
	"######.##    0     ##.######", 
	"######.## ###  ### ##.######", 
	"######.## #      # ##.######", 
	"tttttt.   #1 2 3 #   .tttttt", 
	"######.## #      # ##.######", 
	"######.## ######## ##.######", 
	"######.##    $     ##.######", 
	"######.## ######## ##.######", 
	"######.## ######## ##.######", 
	"#............##............#", 
	"#.####.#####.##.#####.####.#", 
	"#.####.#####.##.#####.####.#", 
	"#*..##.......P .......##..*#", 
	"###.##.##.########.##.##.###", 
	"###.##.##.########.##.##.###", 
	"#......##....##....##......#", 
	"#.##########.##.##########.#", 
	"#.##########.##.##########.#", 
	"#..........................#", 
	"############################", 
	"############################", 
	"############################"}; 
	/*@formatter:on*/

	public static final char WALL = '#', TUNNEL = 't', SPACE = ' ', PELLET = '.', ENERGIZER = '*',
			EATEN_PELLET = ':', EATEN_ENERGIZER = '~';

	public final GridGraph2D<Tile, Void> graph;

	public Tile cornerNW, cornerNE, cornerSW, cornerSE, scatterTileNE, scatterTileNW, scatterTileSE,
			scatterTileSW, tunnelExitLeft, tunnelExitRight, pacManHome, bonusTile, doorLeft, doorRight;

	public Tile[] ghostHome = new Tile[4];

	public int totalNumPellets;

	private final Tile[][] board = new Tile[NUM_COLS][NUM_ROWS];
	private final Set<Tile> intersections;
	private final Set<Tile> energizers = new HashSet<>();

	public Maze() {
		for (int row = 0; row < NUM_ROWS; ++row) {
			for (int col = 0; col < NUM_COLS; ++col) {
				char content = CONTENT[row].charAt(col);
				Tile tile = board[col][row] = new Tile((byte) col, (byte) row, content);
				if (Character.isDigit(content)) {
					ghostHome[Integer.valueOf(String.valueOf(content))] = tile;
					tile.content = SPACE;
				}
				switch (content) {
				case PELLET:
					totalNumPellets += 1;
					break;
				case ENERGIZER:
					totalNumPellets += 1;
					energizers.add(tile);
					break;
				case 'P':
					pacManHome = tile;
					tile.content = SPACE;
					break;
				case '$':
					bonusTile = tile;
					tile.content = SPACE;
					break;
				default:
					break;
				}
			}
		}

		doorLeft = board[13][15];
		doorRight = board[14][15];

		tunnelExitLeft = board[0][17];
		tunnelExitRight = board[27][17];

		// Scattering targets
		scatterTileNW = board[2][0];
		scatterTileNE = board[25][0];
		scatterTileSW = board[0][35];
		scatterTileSE = board[27][35];

		// Corners inside maze
		cornerNW = board[1][4];
		cornerNE = board[26][4];
		cornerSW = board[1][32];
		cornerSE = board[26][32];

		// Graph where each vertex holds a reference to the corresponding tile
		graph = new GridGraph<>(NUM_COLS, NUM_ROWS, Grid4Topology.get(), this::tile, (u, v) -> null,
				UndirectedEdge::new);
		graph.fill();
		//@formatter:off
		graph.edges()
			.filter(edge -> isWall(tile(edge.either())) || isWall(tile(edge.other())))
			.forEach(graph::removeEdge);

		intersections = graph.vertices()
			.filter(vertex -> graph.degree(vertex) >= 3)
			.mapToObj(this::tile)
			.filter(tile -> !inFrontOfGhostHouseDoor(tile))
			.filter(tile -> !partOfGhostHouse(tile))
			.collect(Collectors.toSet());
		//@formatter:on
	}

	private int vertex(Tile tile) {
		return graph.cell(tile.col, tile.row);
	}

	private Tile tile(int vertex) {
		return board[graph.col(vertex)][graph.row(vertex)];
	}

	public Stream<Tile> tiles() {
		return graph.vertices().mapToObj(this::tile);
	}

	public Stream<Tile> energizerTiles() {
		return energizers.stream();
	}

	/**
	 * @param col
	 *              a column index
	 * @param row
	 *              a row index
	 * @return the tile with the given coordinates. Tiles outside of the board are either tunnel tiles
	 *         (if in the same row than the board tunnel tiles) or walls otherwise.
	 */
	public Tile tileAt(int col, int row) {
		return insideBoard(col, row) ? board[col][row]
				: new Tile((byte) col, (byte) row, row == tunnelExitLeft.row ? TUNNEL : WALL);
	}

	/**
	 * @param tile
	 *               reference tile
	 * @param dir
	 *               some direction
	 * @param n
	 *               number of tiles
	 * @return the tile located <code>n</code> tiles away from the reference tile towards the given
	 *         direction. This can be a tile outside of the board!
	 */
	public Tile tileToDir(Tile tile, Direction dir, int n) {
		return tileAt(tile.col + n * dir.dx, tile.row + n * dir.dy);
	}

	/**
	 * @param tile
	 *               reference tile
	 * @param dir
	 *               some direction
	 * @return neighbor towards the given direction. This can be a tile outside of the board!
	 */
	public Tile tileToDir(Tile tile, Direction dir) {
		return tileToDir(tile, dir, 1);
	}

	public boolean insideBoard(int col, int row) {
		return 0 <= col && col < NUM_COLS && 0 <= row && row < NUM_ROWS;
	}

	public boolean insideBoard(Tile tile) {
		return insideBoard(tile.col, tile.row);
	}

	public boolean isTunnel(Tile tile) {
		return tile.content == TUNNEL;
	}

	public boolean isWall(Tile tile) {
		return tile.content == WALL;
	}

	public boolean isDoor(Tile tile) {
		return tile == doorLeft || tile == doorRight;
	}

	public boolean inFrontOfGhostHouseDoor(Tile tile) {
		return isDoor(tileToDir(tile, Direction.DOWN));
	}

	public boolean partOfGhostHouse(Tile tile) {
		return 15 <= tile.row && tile.row <= 19 && 10 <= tile.col && tile.col <= 17;
	}

	public boolean inGhostHouse(Tile tile) {
		return partOfGhostHouse(tile) && tile.content == SPACE;
	}

	public boolean isIntersection(Tile tile) {
		return intersections.contains(tile);
	}

	public boolean isNoUpIntersection(Tile tile) {
		return tile == board[12][14] || tile == board[12][26] || tile == board[15][14] || tile == board[15][26];
	}

	// food

	public boolean containsFood(Tile tile) {
		return containsPellet(tile) || containsEnergizer(tile);
	}

	public boolean containsPellet(Tile tile) {
		return tile.content == PELLET;
	}

	public boolean containsEnergizer(Tile tile) {
		return tile.content == ENERGIZER;
	}

	public boolean containsEatenFood(Tile tile) {
		return tile.content == EATEN_PELLET || tile.content == EATEN_ENERGIZER;
	}

	public void removeFood(Tile tile) {
		if (tile.content == PELLET) {
			tile.content = EATEN_PELLET;
		}
		else if (tile.content == ENERGIZER) {
			tile.content = EATEN_ENERGIZER;
		}
		else {
			throw new IllegalArgumentException(String.format("Tile %s does not contain food", tile));
		}
	}

	public void restoreFood(Tile tile) {
		if (tile.content == EATEN_PELLET) {
			tile.content = PELLET;
		}
		else if (tile.content == EATEN_ENERGIZER) {
			tile.content = ENERGIZER;
		}
		else {
			throw new IllegalArgumentException(String.format("Tile %s does not contain eaten food", tile));
		}
	}

	public void restoreFood() {
		tiles().filter(this::containsEatenFood).forEach(this::restoreFood);
	}

	public void removeFood() {
		tiles().filter(this::containsFood).forEach(this::removeFood);
	}

	// navigation and path finding

	public Optional<Direction> directionBetween(Tile t1, Tile t2) {
		int dx = t2.col - t1.col, dy = t2.row - t1.row;
		return Direction.dirs().filter(dir -> dir.dx == dx && dir.dy == dy).findFirst();
	}

	private int pathFinderCalls;

	public List<Tile> findPath(Tile source, Tile target) {
		if (insideBoard(source) && insideBoard(target)) {
			GraphSearch pathfinder = new AStarSearch(graph, (u, v) -> 1, graph::manhattan);
			Path path = pathfinder.findPath(vertex(source), vertex(target));
			pathFinderCalls += 1;
			if (pathFinderCalls % 100 == 0) {
				LOGGER.info(String.format("%d'th pathfinding executed", pathFinderCalls));
			}
			return path.vertexStream().map(this::tile).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	public Optional<Direction> alongPath(List<Tile> path) {
		return path.size() < 2 ? Optional.empty() : directionBetween(path.get(0), path.get(1));
	}

	// misc

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int row = 0; row < NUM_ROWS; ++row) {
			for (int col = 0; col < NUM_COLS; ++col) {
				sb.append(board[col][row].content);
			}
			sb.append("\n");
		}
		return sb.toString();
	}

}