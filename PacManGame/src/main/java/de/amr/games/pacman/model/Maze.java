package de.amr.games.pacman.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The maze, a 2-dimensional grid of tiles.
 * 
 * @author Armin Reichert
 */
public class Maze {

	/** Tile size in pixels. */
	public static final int TS = 8;

	public static final int NUM_COLS = 28;
	public static final int NUM_ROWS = 36;

	public static final char WALL = '#', TUNNEL = 't', SPACE = ' ', PELLET = '.', ENERGIZER = '*',
			EATEN_PELLET = ':', EATEN_ENERGIZER = '~';

	public final Tile[][] tiles = new Tile[NUM_COLS][NUM_ROWS];

	public Tile cornerNW, cornerNE, cornerSW, cornerSE, scatterTileNE, scatterTileNW, scatterTileSE,
			scatterTileSW, tunnelExitLeft, tunnelExitRight, pacManHome, bonusTile, doorLeft, doorRight;

	public Tile[] ghostHome = new Tile[4];

	public int totalNumPellets;

	private final Set<Tile> intersections;
	private final Set<Tile> energizers = new HashSet<>();

	public Maze(String[] board) {
		for (int row = 0; row < NUM_ROWS; ++row) {
			for (int col = 0; col < NUM_COLS; ++col) {
				char c = board[row].charAt(col);
				Tile tile = tiles[col][row] = new Tile((byte) col, (byte) row, c);
				if (Character.isDigit(c)) {
					ghostHome[Integer.valueOf(String.valueOf(c))] = tile;
					tile.content = SPACE;
				}
				switch (c) {
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

		doorLeft = tiles[13][15];
		doorRight = tiles[14][15];

		tunnelExitLeft = tiles[0][17];
		tunnelExitRight = tiles[27][17];

		// Scattering targets
		scatterTileNW = tiles[2][0];
		scatterTileNE = tiles[25][0];
		scatterTileSW = tiles[0][35];
		scatterTileSE = tiles[27][35];

		// Corners inside maze
		cornerNW = tiles[1][4];
		cornerNE = tiles[26][4];
		cornerSW = tiles[1][32];
		cornerSE = tiles[26][32];

		intersections = tiles()
		/*@formatter:off*/
				.filter(tile -> numFreeNeighborTiles(tile) > 2)
				.filter(tile -> !inFrontOfGhostHouseDoor(tile))
				.filter(tile -> !partOfGhostHouse(tile))
				.collect(Collectors.toSet());
		/*@formatter:on*/

		System.out.println();
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
		return Arrays.stream(tiles).flatMap(Arrays::stream);
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
		return insideBoard(col, row) ? tiles[col][row]
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
		return tile == tiles[12][14] || tile == tiles[12][26] || tile == tiles[15][14] || tile == tiles[15][26];
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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int row = 0; row < NUM_ROWS; ++row) {
			for (int col = 0; col < NUM_COLS; ++col) {
				sb.append(tiles[col][row].content);
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}