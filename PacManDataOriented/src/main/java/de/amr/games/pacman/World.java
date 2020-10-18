package de.amr.games.pacman;

import static de.amr.games.pacman.Direction.DOWN;
import static de.amr.games.pacman.Direction.LEFT;
import static de.amr.games.pacman.Direction.RIGHT;
import static de.amr.games.pacman.Direction.UP;

import java.util.List;

public class World {

	public static final int TS = 8;
	public static final int HTS = TS / 2;
	public static final int WORLD_WIDTH_TILES = 28;
	public static final int WORLD_HEIGHT_TILES = 36;
	public static final int WORLD_WIDTH = WORLD_WIDTH_TILES * TS;
	public static final int WORLD_HEIGHT = WORLD_HEIGHT_TILES * TS;
	public static final V2 CLYDE_CORNER = new V2(0, WORLD_HEIGHT_TILES - 1);
	public static final V2 INKY_CORNER = new V2(WORLD_WIDTH_TILES - 1, WORLD_HEIGHT_TILES - 1);
	public static final V2 PINKY_CORNER = new V2(2, 0);
	public static final V2 BLINKY_CORNER = new V2(WORLD_WIDTH_TILES - 3, 0);
	public static final V2 PORTAL_LEFT_ENTRY = new V2(-1, 17);
	public static final V2 PORTAL_RIGHT_ENTRY = new V2(WORLD_WIDTH_TILES, 17);

	private final String[] map;

	public World() {
		map = new String[] {
		//@formatter:off
			"1111111111111111111111111111",
			"1111111111111111111111111111",
			"1111111111111111111111111111",
			"1111111111111111111111111111",
			"1222222222222112222222222221",
			"1211112111112112111112111121",
			"1211112111112112111112111121",
			"1211112111112112111112111121",
			"1222222222222222222222222221",
			"1211112112111111112112111121",
			"1211112112111111112112111121",
			"1222222112222112222112222221",
			"1111112111110110111112111111",
			"1111112111110110111112111111",
			"1111112110000000000112111111",
			"1111112110111001110112111111",
			"1111112110100000010112111111",
			"0000002000100000010002000000",
			"1111112110100000010112111111",
			"1111112110111111110112111111",
			"1111112110000000000112111111",
			"1111112110111111110112111111",
			"1111112110111111110112111111",
			"1222222222222112222222222221",
			"1211112111112112111112111121",
			"1211112111112112111112111121",
			"1222112222222002222222112221",
			"1112112112111111112112112111",
			"1112112112111111112112112111",
			"1222222112222112222112222221",
			"1211111111112112111111111121",
			"1211111111112112111111111121",
			"1222222222222222222222222221",
			"1111111111111111111111111111",
			"1111111111111111111111111111",
			"1111111111111111111111111111",
		//@formatter:on
		};
	}

	public char content(int x, int y) {
		return map[y].charAt(x);
	}

	public int index(int x, int y) {
		return y * WORLD_WIDTH_TILES + x;
	}

	public V2 tile(V2 position) {
		return new V2((int) (position.x + HTS) / TS, (int) (position.y + HTS) / TS);
	}

	public V2 offset(V2 position, V2 tile) {
		return new V2(position.x - tile.x * TS, position.y - tile.y * TS);
	}

	public V2 position(Creature guy) {
		return new V2(guy.tile.x * TS + guy.offset.x, guy.tile.y * TS + guy.offset.y);
	}

	public boolean inMapRange(int x, int y) {
		return 0 <= x && x < WORLD_WIDTH_TILES && 0 <= y && y < WORLD_HEIGHT_TILES;
	}

	public boolean isGhostHouseDoor(V2 tile) {
		return tile.y == 15 && (tile.x == 13 || tile.x == 14);
	}

	public boolean isInsideGhostHouse(V2 tile) {
		return tile.x >= 10 && tile.x <= 17 && tile.y >= 15 && tile.y <= 22;
	}

	public boolean isInsideTunnel(V2 tile) {
		return tile.y == 17 && (tile.x <= 5 || tile.x >= 21);
	}

	public boolean isUpwardsBlocked(V2 tile) {
		//@formatter:off
		return tile.x == 12 && tile.y == 13
	  	|| tile.x == 15 && tile.y == 13
		  || tile.x == 12 && tile.y == 25
		  || tile.x == 15 && tile.y == 25;
		//@formatter:on
	}

	public boolean isFoodTile(int x, int y) {
		return inMapRange(x, y) && content(x, y) == '2';
	}

	public boolean isEnergizerTile(V2 tile) {
		//@formatter:off
		return tile.x == 1  && tile.y == 6
	  	|| tile.x == 26 && tile.y == 6
		  || tile.x == 1  && tile.y == 26
		  || tile.x == 26 && tile.y == 26;
		//@formatter:on
	}

	public boolean isIntersectionTile(V2 tile) {
		int accessibleNeighbors = 0;
		for (Direction dir : List.of(DOWN, LEFT, RIGHT, UP)) {
			V2 neighbor = tile.sum(dir.vector);
			if (isAccessibleTile(neighbor)) {
				++accessibleNeighbors;
			}
		}
		return accessibleNeighbors >= 3;
	}

	public boolean isAccessibleTile(V2 tile) {
		if (isPortalTile(tile)) {
			return true;
		}
		int x = (int) tile.x, y = (int) tile.y;
		if (x >= 0 && x < WORLD_WIDTH_TILES && y > 0 && y < WORLD_HEIGHT_TILES) {
			return false;
		}
		return content(x, y) != '1';
	}

	public boolean isPortalTile(V2 tile) {
		return tile.equals(PORTAL_RIGHT_ENTRY) || tile.equals(PORTAL_LEFT_ENTRY);
	}

	public boolean isBonusTile(int x, int y) {
		return x == 13 && y == 20;
	}
}