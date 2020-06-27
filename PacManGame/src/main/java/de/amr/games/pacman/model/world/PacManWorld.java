package de.amr.games.pacman.model.world;

import static de.amr.games.pacman.model.map.PacManMap.B_EATEN;
import static de.amr.games.pacman.model.map.PacManMap.B_ENERGIZER;
import static de.amr.games.pacman.model.map.PacManMap.B_FOOD;
import static de.amr.games.pacman.model.map.PacManMap.B_INTERSECTION;
import static de.amr.games.pacman.model.map.PacManMap.B_TUNNEL;
import static de.amr.games.pacman.model.map.PacManMap.B_WALL;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import de.amr.games.pacman.model.Direction;
import de.amr.games.pacman.model.map.PacManMap;

/**
 * The Pac-Man game world. Reserves 3 rows above and 2 rows below the map for displaying the scores
 * and counters.
 * 
 * @author Armin Reichert
 */
public class PacManWorld implements PacManWorldStructure {

	static final int ROWS_ABOVE_MAP = 3;
	static final int ROWS_BELOW_MAP = 2;

	static int toWorld(int row) {
		return row + ROWS_ABOVE_MAP;
	}

	static int toMap(int row) {
		return row - ROWS_ABOVE_MAP;
	}

	public final Tile cornerNW, cornerNE, cornerSW, cornerSE;

	private final PacManMap map;

	public PacManWorld(PacManMap map) {
		this.map = map;
		// only used by algorithm to calculate routes to "safe" corner for fleeing ghosts
		cornerNW = Tile.at(1, ROWS_ABOVE_MAP + 1);
		cornerNE = Tile.at(width() - 2, ROWS_ABOVE_MAP + 1);
		cornerSW = Tile.at(1, toWorld(map.numRows - 2));
		cornerSE = Tile.at(width() - 2, toWorld(map.numRows - 2));
	}

	@Override
	public int width() {
		return map.width();
	}

	@Override
	public int height() {
		return ROWS_ABOVE_MAP + map.height() + ROWS_BELOW_MAP;
	}

	@Override
	public boolean insideMap(Tile tile) {
		return map.contains(toMap(tile.row), tile.col);
	}

	@Override
	public Stream<House> houses() {
		return map.houses();
	}

	@Override
	public Seat pacManSeat() {
		return map.pacManSeat();
	}

	@Override
	public Tile bonusTile() {
		return map.bonusTile();
	}

	@Override
	public Stream<Portal> portals() {
		return map.portals();
	}

	@Override
	public Stream<OneWayTile> oneWayTiles() {
		return map.oneWayTiles();
	}

	/**
	 * @return the map tiles in world coordinates
	 */
	public Stream<Tile> mapTiles() {
		return IntStream.range(toWorld(0) * map.numCols, toWorld(map.numRows + 1) * map.numCols)
				.mapToObj(i -> Tile.at(i % map.numCols, i / map.numCols));
	}

	/**
	 * @param tile reference tile
	 * @param dir  some direction
	 * @param n    number of tiles
	 * @return The tile located <code>n</code> tiles away from the reference tile towards the given
	 *         direction. This can be a tile outside of the world.
	 */
	public Tile tileToDir(Tile tile, Direction dir, int n) {
		//@formatter:off
		return portals()
				.filter(portal -> portal.contains(tile))
				.findAny()
				.map(portal -> portal.exitTile(tile, dir))
				.orElse(
						Tile.at(tile.col + n * dir.vector().roundedX(), tile.row + n * dir.vector().roundedY())
				);
		//@formatter:on
	}

	/**
	 * @param tile reference tile
	 * @param dir  some direction
	 * @return Neighbor towards the given direction. This can be a tile outside of the map.
	 */
	public Tile neighbor(Tile tile, Direction dir) {
		return tileToDir(tile, dir, 1);
	}

	public Stream<Tile> neighbors(Tile tile) {
		return Direction.dirs().map(dir -> neighbor(tile, dir));
	}

	public void eatAllFood() {
		mapTiles().forEach(this::eatFood);
	}

	public void restoreAllFood() {
		mapTiles().forEach(this::restoreFood);
	}

	public boolean isDoor(Tile tile) {
		return houses().flatMap(House::doors).anyMatch(door -> door.contains(tile));
	}

	public boolean insideHouse(Tile tile) {
		return houses().map(House::room).anyMatch(room -> room.contains(tile) || isDoor(tile));
	}

	public boolean outsideOfDoor(Tile tile) {
		for (Direction dir : Direction.values()) {
			Tile neighbor = neighbor(tile, dir);
			if (isDoor(neighbor)) {
				Door door = houses().flatMap(House::doors).filter(d -> d.contains(neighbor)).findFirst().get();
				return door.intoHouse == dir;
			}
		}
		return false;
	}

	public boolean isInaccessible(Tile tile) {
		if (insideMap(tile)) {
			return map.is1(toMap(tile.row), tile.col, B_WALL);
		}
		return !isPortal(tile);
	}

	public boolean isTunnel(Tile tile) {
		return insideMap(tile) && map.is1(toMap(tile.row), tile.col, B_TUNNEL);
	}

	public boolean isIntersection(Tile tile) {
		return insideMap(tile) && map.is1(toMap(tile.row), tile.col, B_INTERSECTION);
	}

	public boolean isFood(Tile tile) {
		return insideMap(tile) && map.is1(toMap(tile.row), tile.col, B_FOOD);
	}

	public boolean isEatenFood(Tile tile) {
		return insideMap(tile) && map.is1(toMap(tile.row), tile.col, B_EATEN);
	}

	public boolean isEnergizer(Tile tile) {
		return insideMap(tile) && map.is1(toMap(tile.row), tile.col, B_ENERGIZER);
	}

	public boolean containsSimplePellet(Tile tile) {
		return isFood(tile) && !isEnergizer(tile) && !isEatenFood(tile);
	}

	public boolean containsEnergizer(Tile tile) {
		return insideMap(tile) && map.is1(toMap(tile.row), tile.col, B_ENERGIZER)
				&& map.is0(toMap(tile.row), tile.col, B_EATEN);
	}

	public boolean containsFood(Tile tile) {
		return insideMap(tile) && map.is0(toMap(tile.row), tile.col, B_EATEN) && map.is1(toMap(tile.row), tile.col, B_FOOD);
	}

	public boolean containsEatenFood(Tile tile) {
		return insideMap(tile) && map.is1(toMap(tile.row), tile.col, B_EATEN) && map.is1(toMap(tile.row), tile.col, B_FOOD);
	}

	public void eatFood(Tile tile) {
		if (insideMap(tile)) {
			map.set1(toMap(tile.row), tile.col, B_EATEN);
		}
	}

	public void restoreFood(Tile tile) {
		if (insideMap(tile)) {
			map.set0(toMap(tile.row), tile.col, B_EATEN);
		}
	}
}