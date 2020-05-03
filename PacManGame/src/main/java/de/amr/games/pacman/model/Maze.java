package de.amr.games.pacman.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.amr.easy.game.math.Vector2f;
import de.amr.games.pacman.model.tiles.Pellet;
import de.amr.games.pacman.model.tiles.Space;
import de.amr.games.pacman.model.tiles.Tile;
import de.amr.games.pacman.model.tiles.Wall;

/**
 * The Pac-Man game world.
 * 
 * @author Armin Reichert
 */
public class Maze {

	static final char WALL = '#';
	static final char SPACE = ' ';
	static final char PELLET = '.';
	static final char ENERGIZER = 'E';

	static final String[] MAP = {
	/*@formatter:off*/
	"############################", 
	"############################", 
	"############################", 
	"############################", 
	"#............##............#", 
	"#.####.#####.##.#####.####.#", 
	"#E####.#####.##.#####.####E#", 
	"#.####.#####.##.#####.####.#", 
	"#..........................#", 
	"#.####.##.########.##.####.#", 
	"#.####.##.########.##.####.#", 
	"#......##....##....##......#", 
	"######.##### ## #####.######", 
	"######.##### ## #####.######", 
	"######.##          ##.######", 
	"######.## ###  ### ##.######", 
	"######.## #      # ##.######", 
	"      .   #      #   .      ", 
	"######.## #      # ##.######", 
	"######.## ######## ##.######", 
	"######.##          ##.######", 
	"######.## ######## ##.######", 
	"######.## ######## ##.######", 
	"#............##............#", 
	"#.####.#####.##.#####.####.#", 
	"#.####.#####.##.#####.####.#", 
	"#E..##.......  .......##..E#", 
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

	public final int numRows;
	public final int numCols;
	public final int totalFoodCount;

	public final Tile pacManHome;
	public final Tile ghostHouseSeats[] = new Tile[4];
	public final Tile bonusTile;
	public final Tile cornerNW, cornerNE, cornerSW, cornerSE;
	public final Tile horizonNE, horizonNW, horizonSE, horizonSW;
	public final Tile portalLeft, portalRight;
	public final Tile doorLeft, doorRight;
	public final List<Pellet> energizers = new ArrayList<>();

	private final Tile[][] map;
	private final Set<Tile> intersections;

	public Maze() {
		numRows = MAP.length;
		numCols = MAP[0].length();
		map = new Tile[numCols][numRows];
		int foodCount = 0;
		for (int row = 0; row < numRows; ++row) {
			for (int col = 0; col < numCols; ++col) {
				char c = MAP[row].charAt(col);
				switch (c) {
				case SPACE:
					map[col][row] = new Space(col, row);
					break;
				case WALL:
					map[col][row] = new Wall(col, row);
					break;
				case PELLET:
					map[col][row] = new Pellet(col, row);
					foodCount += 1;
					break;
				case ENERGIZER:
					Pellet pellet = new Pellet(col, row);
					pellet.energizer = true;
					map[col][row] = pellet;
					energizers.add(pellet);
					foodCount += 1;
					break;
				default:
					throw new IllegalArgumentException("Unknown tile content: " + c);
				}
			}
		}
		totalFoodCount = foodCount;

		// Ghost house
		doorLeft = map[13][15];
		doorRight = map[14][15];
		ghostHouseSeats[0] = map[13][14];
		ghostHouseSeats[1] = map[11][17];
		ghostHouseSeats[2] = map[13][17];
		ghostHouseSeats[3] = map[15][17];

		pacManHome = map[13][26];
		bonusTile = map[13][20];

		portalLeft = new Space(-1, 17);
		portalRight = new Space(28, 17);

		// Scattering targets
		horizonNW = map[2][0];
		horizonNE = map[25][0];
		horizonSW = map[0][35];
		horizonSE = map[27][35];

		// Corners inside maze
		cornerNW = map[1][4];
		cornerNE = map[26][4];
		cornerSW = map[1][32];
		cornerSE = map[26][32];

		intersections = tiles()
		/*@formatter:off*/
				.filter(tile -> numFreeNeighborTiles(tile) > 2)
				.filter(tile -> !inFrontOfGhostHouseDoor(tile))
				.filter(tile -> !partOfGhostHouse(tile))
				.collect(Collectors.toSet());
		/*@formatter:on*/
	}

	private long numFreeNeighborTiles(Tile tile) {
		/*@formatter:off*/
		return Direction.dirs()
				.map(dir -> tileToDir(tile, dir))
				.filter(this::insideBoard)
				.filter(neighbor -> !isWall(neighbor) && !isDoor(neighbor))
				.count();
		/*@formatter:on*/
	}

	public Stream<Tile> tiles() {
		return Arrays.stream(map).flatMap(Arrays::stream);
	}

	/**
	 * Returns the tile at the given tile position. This is either a tile inside the
	 * board, a portal tile or a wall outside. Tiles inside the board and the two
	 * portal tiles are created once so equality can be tested using
	 * <code>==</code>. Other tiles are created on-demand and must be compared using
	 * {@link Object#equals(Object)}. For tiles outside of the board, the column and
	 * row index must fit into a byte.
	 * 
	 * @param col a column index
	 * @param row a row index
	 * @return the tile with the given coordinates.
	 */
	public Tile tileAt(int col, int row) {
		if (insideBoard(col, row)) {
			return map[col][row];
		}
		if (portalLeft.col == col && portalLeft.row == row) {
			return portalLeft;
		}
		if (portalRight.col == col && portalRight.row == row) {
			return portalRight;
		}
		return new Wall(col, row);
	}

	/**
	 * @param tile reference tile
	 * @param dir  some direction
	 * @param n    number of tiles
	 * @return the tile located <code>n</code> tiles away from the reference tile
	 *         towards the given direction. This can be a tile outside of the board!
	 */
	public Tile tileToDir(Tile tile, Direction dir, int n) {
		if (tile.equals(portalLeft) && dir == Direction.LEFT) {
			return portalRight;
		}
		if (tile.equals(portalRight) && dir == Direction.RIGHT) {
			return portalLeft;
		}
		Vector2f v = dir.vector();
		return tileAt(tile.col + n * v.roundedX(), tile.row + n * v.roundedY());
	}

	/**
	 * @param tile reference tile
	 * @param dir  some direction
	 * @return neighbor towards the given direction. This can be a tile outside of
	 *         the board!
	 */
	public Tile tileToDir(Tile tile, Direction dir) {
		return tileToDir(tile, dir, 1);
	}

	public boolean insideBoard(int col, int row) {
		return 0 <= col && col < numCols && 0 <= row && row < numRows;
	}

	public boolean insideBoard(Tile tile) {
		return insideBoard(tile.col, tile.row);
	}

	public boolean isDoor(Tile tile) {
		return tile == doorLeft || tile == doorRight;
	}

	public boolean isWall(Tile tile) {
		return tile instanceof Wall;
	}

	public boolean isTunnel(Tile tile) {
		return tile.row == 17 && (-1 <= tile.col && tile.col <= 5 || tile.col >= 22 && tile.col <= 28);
	}

	public boolean isSpace(Tile tile) {
		return tile instanceof Space;
	}

	public boolean isNormalPellet(Tile tile) {
		if (tile instanceof Pellet) {
			Pellet pellet = (Pellet) tile;
			return !pellet.energizer && !pellet.eaten;
		}
		return false;
	}

	public boolean isEatenNormalPellet(Tile tile) {
		if (tile instanceof Pellet) {
			Pellet pellet = (Pellet) tile;
			return !pellet.energizer && pellet.eaten;
		}
		return false;
	}

	public boolean isEnergizer(Tile tile) {
		if (tile instanceof Pellet) {
			Pellet pellet = (Pellet) tile;
			return pellet.energizer && !pellet.eaten;
		}
		return false;
	}

	public boolean isEatenEnergizer(Tile tile) {
		if (tile instanceof Pellet) {
			Pellet pellet = (Pellet) tile;
			return pellet.energizer && pellet.eaten;
		}
		return false;
	}

	public void removeFood(Tile tile) {
		if (tile instanceof Pellet) {
			((Pellet) tile).eaten = true;
		}
	}

	public void removeFood() {
		tiles().forEach(this::removeFood);
	}

	public void restoreFood(Tile tile) {
		if (tile instanceof Pellet) {
			((Pellet) tile).eaten = false;
		}
	}

	public void restoreFood() {
		tiles().forEach(this::restoreFood);
	}

	public boolean inFrontOfGhostHouseDoor(Tile tile) {
		return isDoor(tileToDir(tile, Direction.DOWN));
	}

	public Optional<Direction> direction(Tile t1, Tile t2) {
		Vector2f v = Vector2f.of(t2.col - t1.col, t2.row - t1.row);
		return Direction.dirs().filter(dir -> dir.vector().equals(v)).findFirst();
	}

	public Vector2f seatPosition(int seat) {
		return Vector2f.of(ghostHouseSeats[seat].centerX(), ghostHouseSeats[seat].y());
	}

	public boolean partOfGhostHouse(Tile tile) {
		return 15 <= tile.row && tile.row <= 19 && 10 <= tile.col && tile.col <= 17;
	}

	public boolean inGhostHouse(Tile tile) {
		return partOfGhostHouse(tile) && isSpace(tile);
	}

	public boolean isIntersection(Tile tile) {
		return intersections.contains(tile);
	}

	public boolean isNoUpIntersection(Tile tile) {
		return tile == map[12][14] || tile == map[12][26] || tile == map[15][14] || tile == map[15][26];
	}
}