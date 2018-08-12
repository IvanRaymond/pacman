package de.amr.games.pacman.model;

import static de.amr.games.pacman.model.Content.ENERGIZER;
import static de.amr.games.pacman.model.Content.PELLET;
import static de.amr.games.pacman.model.Content.POS_BLINKY;
import static de.amr.games.pacman.model.Content.POS_BONUS;
import static de.amr.games.pacman.model.Content.POS_CLYDE;
import static de.amr.games.pacman.model.Content.POS_INKY;
import static de.amr.games.pacman.model.Content.POS_PACMAN;
import static de.amr.games.pacman.model.Content.POS_PINKY;
import static de.amr.games.pacman.model.Content.WALL;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.amr.easy.graph.api.GraphTraversal;
import de.amr.easy.graph.api.UndirectedEdge;
import de.amr.easy.graph.impl.traversal.AStarTraversal;
import de.amr.easy.grid.api.GridGraph2D;
import de.amr.easy.grid.api.Topology;
import de.amr.easy.grid.impl.GridGraph;
import de.amr.easy.grid.impl.Top4;

/**
 * The original Pac-Man maze. It is represented by a grid graph which may store content and can be
 * used by path finding algorithms.
 * 
 * @author Armin Reichert
 * 
 * @see GridGraph2D
 * @see AStarTraversal
 */
public class Maze {

	public static final Topology FOUR_DIRECTIONS = new Top4();

	public Tile pacManHome;
	public Tile blinkyHome;
	public Tile pinkyHome;
	public Tile inkyHome;
	public Tile clydeHome;
	public Tile bonusTile;

	private final String[] originalData;
	private int foodTotal;

	private final GridGraph<Character, Integer> graph;

	public Maze(String map) {
		originalData = map.split("\n");
		int numCols = originalData[0].length(), numRows = originalData.length;

		graph = new GridGraph<>(numCols, numRows, FOUR_DIRECTIONS, v -> null, (u, v) -> 1,
				UndirectedEdge::new);
		graph.setDefaultVertexLabel(v -> originalData(graph.row(v), graph.col(v)));

		foodTotal = 0;
		for (int row = 0; row < numRows; ++row) {
			for (int col = 0; col < numCols; ++col) {
				char c = originalData(row, col);
				if (c == POS_BLINKY) {
					blinkyHome = new Tile(col, row);
				} else if (c == POS_PINKY) {
					pinkyHome = new Tile(col, row);
				} else if (c == POS_INKY) {
					inkyHome = new Tile(col, row);
				} else if (c == POS_CLYDE) {
					clydeHome = new Tile(col, row);
				} else if (c == POS_BONUS) {
					bonusTile = new Tile(col, row);
				} else if (c == POS_PACMAN) {
					pacManHome = new Tile(col, row);
				} else if (c == PELLET || c == ENERGIZER) {
					foodTotal += 1;
				}
			}
		}
		graph.fill();
		graph.edges().filter(edge -> {
			int u = edge.either(), v = edge.other();
			return originalData(graph.row(u), graph.col(u)) == WALL
					|| originalData(graph.row(v), graph.col(v)) == WALL;
		}).forEach(graph::removeEdge);
	}

	private char originalData(int row, int col) {
		return originalData[row].charAt(col);
	}

	public GridGraph<Character, Integer> getGraph() {
		return graph;
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

	public boolean isValidTile(Tile tile) {
		return graph.isValidCol(tile.col) && graph.isValidRow(tile.row);
	}

	public void resetFood() {
		graph.clearVertexLabels();
	}

	public int getFoodTotal() {
		return foodTotal;
	}

	public char getContent(int col, int row) {
		return graph.get(graph.cell(col, row));
	}

	public char getContent(Tile tile) {
		return isValidTile(tile) ? graph.get(cell(tile)) : ' ';
	}

	public void setContent(Tile tile, char c) {
		graph.set(cell(tile), c);
	}

	public OptionalInt direction(Tile t1, Tile t2) {
		return graph.direction(cell(t1), cell(t2));
	}

	public boolean hasAdjacentTile(Tile t1, Tile t2) {
		return graph.adjacent(cell(t1), cell(t2));
	}

	public Optional<Tile> neighborTile(Tile tile, int dir) {
		OptionalInt neighbor = graph.neighbor(cell(tile), dir);
		return neighbor.isPresent() ? Optional.of(tile(neighbor.getAsInt())) : Optional.empty();
	}

	public Stream<Tile> getAdjacentTiles(Tile tile) {
		return graph.adj(cell(tile)).mapToObj(this::tile);
	}

	public List<Tile> findPath(Tile source, Tile target) {
		if (isValidTile(source) && isValidTile(target)) {
			GraphTraversal pathfinder = new AStarTraversal<>(graph, edge -> 1, graph::manhattan);
			// GraphTraversal pathfinder = new BreadthFirstTraversal<>(graph);
			pathfinder.traverseGraph(cell(source), cell(target));
			return pathfinder.path(cell(target)).stream().map(this::tile).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	public OptionalInt alongPath(List<Tile> path) {
		return path.size() < 2 ? OptionalInt.empty() : direction(path.get(0), path.get(1));
	}

	// convert between vertex numbers ("cells") and tiles

	public int cell(Tile tile) {
		return graph.cell(tile.col, tile.row);
	}

	public Tile tile(int cell) {
		return new Tile(graph.col(cell), graph.row(cell));
	}
}