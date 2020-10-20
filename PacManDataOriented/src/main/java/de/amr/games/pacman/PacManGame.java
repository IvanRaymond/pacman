package de.amr.games.pacman;

import static de.amr.games.pacman.Direction.DOWN;
import static de.amr.games.pacman.Direction.LEFT;
import static de.amr.games.pacman.Direction.RIGHT;
import static de.amr.games.pacman.Direction.UP;
import static de.amr.games.pacman.V2.distance;
import static de.amr.games.pacman.World.BLINKY_CORNER;
import static de.amr.games.pacman.World.CLYDE_CORNER;
import static de.amr.games.pacman.World.HTS;
import static de.amr.games.pacman.World.INKY_CORNER;
import static de.amr.games.pacman.World.PINKY_CORNER;
import static de.amr.games.pacman.World.WORLD_HEIGHT_TILES;
import static de.amr.games.pacman.World.WORLD_WIDTH_TILES;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.KeyEvent;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

/**
 * My attempt at writing a minimal Pac-Man game with faithful behavior.
 * 
 * @author Armin Reichert
 */
public class PacManGame {

	public static void main(String[] args) {
		PacManGame game = new PacManGame();
		EventQueue.invokeLater(() -> {
			game.ui = new PacManGameUI(game, 2);
			game.initGame();
			new Thread(game::gameLoop, "GameLoop").start();
		});
	}

	private static final int BLINKY = 0;
	private static final int PINKY = 1;
	private static final int INKY = 2;
	private static final int CLYDE = 3;

	private static final int TOTAL_FOOD_COUNT = 244;
	private static final V2 BETWEEN_TILES = new V2(HTS, 0);

	public static final int FPS = 60;

	public static void log(String msg, Object... args) {
		System.err.println(String.format("%-20s: %s", LocalTime.now(), String.format(msg, args)));
	}

	public static int sec(float seconds) {
		return (int) (seconds * FPS);
	}

	private static final List<LevelData> LEVEL_DATA = List.of(
	/*@formatter:off*/
	LevelData.of("Cherries",   100,  80,  71,  75, 40,  20,  80, 10,  85,  90, 79, 50, 6, 5),
	LevelData.of("Strawberry", 300,  90,  79,  85, 45,  30,  90, 15,  95,  95, 83, 55, 5, 5),
	LevelData.of("Peach",      500,  90,  79,  85, 45,  40,  90, 20,  95,  95, 83, 55, 4, 5),
	LevelData.of("Peach",      500,  90,  79,  85, 50,  40, 100, 20,  95,  95, 83, 55, 3, 5),
	LevelData.of("Apple",      700, 100,  87,  95, 50,  40, 100, 20, 105, 100, 87, 60, 2, 5),
	LevelData.of("Apple",      700, 100,  87,  95, 50,  50, 100, 25, 105, 100, 87, 60, 5, 5),
	LevelData.of("Grapes",    1000, 100,  87,  95, 50,  50, 100, 25, 105, 100, 87, 60, 2, 5),
	LevelData.of("Grapes",    1000, 100,  87,  95, 50,  50, 100, 25, 105, 100, 87, 60, 2, 5),
	LevelData.of("Galaxian",  2000, 100,  87,  95, 50,  60, 100, 30, 105, 100, 87, 60, 1, 3),
	LevelData.of("Galaxian",  2000, 100,  87,  95, 50,  60, 100, 30, 105, 100, 87, 60, 5, 5),
	LevelData.of("Bell",      3000, 100,  87,  95, 50,  60, 100, 30, 105, 100, 87, 60, 2, 5),
	LevelData.of("Bell",      3000, 100,  87,  95, 50,  80, 100, 40, 105, 100, 87, 60, 1, 3),
	LevelData.of("Key",       5000, 100,  87,  95, 50,  80, 100, 40, 105, 100, 87, 60, 1, 3),
	LevelData.of("Key",       5000, 100,  87,  95, 50,  80, 100, 40, 105, 100, 87, 60, 3, 5),
	LevelData.of("Key",       5000, 100,  87,  95, 50, 100, 100, 50, 105, 100, 87, 60, 1, 3),
	LevelData.of("Key",       5000, 100,  87,  95, 50, 100, 100, 50, 105,   0,  0,  0, 1, 3),
	LevelData.of("Key",       5000, 100,  87,  95, 50, 100, 100, 50, 105, 100, 87, 60, 0, 0),
	LevelData.of("Key",       5000, 100,  87,  95, 50, 100, 100, 50, 105,   0,   0, 0, 1, 0),
	LevelData.of("Key",       5000, 100,  87,  95, 50, 120, 100, 60, 105,   0,   0, 0, 0, 0),
	LevelData.of("Key",       5000, 100,  87,  95, 50, 120, 100, 60, 105,   0,   0, 0, 0, 0),
	LevelData.of("Key",       5000,  90,  79,  95, 50, 120, 100, 60, 105,   0,   0, 0, 0, 0)
	//@formatter:on
	);

	public static LevelData levelData(int level) {
		return level <= 21 ? LEVEL_DATA.get(level - 1) : LEVEL_DATA.get(20);
	}

	public LevelData levelData() {
		return levelData(level);
	}

	private static final long[][] SCATTERING_TIMES = {
		//@formatter:off
		{ sec(7), sec(7), sec(5), sec(5) },
		{ sec(7), sec(7), sec(5), 1      },
		{ sec(5), sec(5), sec(5), 1      },
		//@formatter:on
	};

	private static final long[][] CHASING_TIMES = {
		//@formatter:off
		{ sec(20), sec(20), sec(20),   Long.MAX_VALUE },
		{ sec(20), sec(20), sec(1033), Long.MAX_VALUE },
		{ sec(5),  sec(5),  sec(1037), Long.MAX_VALUE },
		//@formatter:on
	};

	private static int waveTimes(int level) {
		return level == 1 ? 0 : level <= 4 ? 1 : 2;
	}

	public final World world = new World();
	public final BitSet eatenFood = new BitSet(244);
	public final Creature pacMan;
	public final Creature[] ghosts = new Creature[4];

	public GameState state;
	public PacManGameUI ui;
	public String messageText;
	public long fps;
	public long framesTotal;
	public int level;
	public int attackWave;
	public int foodRemaining;
	public int lives;
	public int points;
	public int ghostsKilledUsingEnergizer;
	public int mazeFlashes;
	public long pacManPowerTimer;
	public long readyStateTimer;
	public long scatteringStateTimer;
	public long chasingStateTimer;
	public long levelChangeStateTimer;
	public long pacManDyingStateTimer;
	public long bonusAvailableTimer;
	public long bonusConsumedTimer;

	public PacManGame() {
		pacMan = new Creature("Pac-Man", Color.YELLOW, new V2(13, 26), null);
		ghosts[BLINKY] = new Creature("Blinky", Color.RED, new V2(13, 14), BLINKY_CORNER);
		ghosts[PINKY] = new Creature("Pinky", Color.PINK, new V2(13, 17), PINKY_CORNER);
		ghosts[INKY] = new Creature("Inky", Color.CYAN, new V2(11, 17), INKY_CORNER);
		ghosts[CLYDE] = new Creature("Clyde", Color.ORANGE, new V2(15, 17), CLYDE_CORNER);
	}

	private void initGame() {
		points = 0;
		lives = 3;
		initLevel(1);
		enterReadyState();
	}

	private void initLevel(int n) {
		level = n;
		eatenFood.clear();
		foodRemaining = TOTAL_FOOD_COUNT;
		attackWave = 0;
		mazeFlashes = 0;
		ghostsKilledUsingEnergizer = 0;
		pacManPowerTimer = 0;
		readyStateTimer = 0;
		scatteringStateTimer = 0;
		chasingStateTimer = 0;
		levelChangeStateTimer = 0;
		pacManDyingStateTimer = 0;
		bonusAvailableTimer = 0;
		bonusConsumedTimer = 0;
	}

	private void initEntities() {
		pacMan.speed = 0;
		pacMan.dir = pacMan.wishDir = RIGHT;
		pacMan.tile = pacMan.homeTile;
		pacMan.offset = BETWEEN_TILES;
		pacMan.stuck = false;
		pacMan.dead = false;
		pacMan.visible = true;
		pacMan.forcedOnTrack = true;

		for (Creature ghost : ghosts) {
			ghost.speed = 0;
			ghost.tile = ghost.homeTile;
			ghost.offset = BETWEEN_TILES;
			ghost.targetTile = null;
			ghost.tileChanged = true;
			ghost.stuck = false;
			ghost.forcedTurningBack = false;
			ghost.forcedOnTrack = false;
			ghost.dead = false;
			ghost.frightened = false;
			ghost.visible = true;
			ghost.enteringHouse = false;
			ghost.leavingHouse = false;
			ghost.bounty = 0;
			ghost.bountyTimer = 0;
		}
		ghosts[BLINKY].dir = ghosts[BLINKY].wishDir = LEFT;
		ghosts[PINKY].dir = ghosts[PINKY].wishDir = DOWN;
		ghosts[INKY].dir = ghosts[INKY].wishDir = UP;
		ghosts[CLYDE].dir = ghosts[CLYDE].wishDir = UP;

		bonusAvailableTimer = 0;
		bonusConsumedTimer = 0;
	}

	private void gameLoop() {
		final long intendedFrameDuration = 1_000_000_000 / FPS;
		long fpsCountStart = 0;
		long frames = 0;
		while (true) {
			long frameStartTime = System.nanoTime();
			update();
			ui.render();
			long frameEndTime = System.nanoTime();
			long frameDuration = frameEndTime - frameStartTime;
			long sleep = Math.max(intendedFrameDuration - frameDuration, 0);
			if (sleep > 0) {
				try {
					Thread.sleep(sleep / 1_000_000); // milliseconds
				} catch (InterruptedException x) {
					x.printStackTrace();
				}
			}

			++frames;
			++framesTotal;
			if (frameEndTime - fpsCountStart >= 1_000_000_000) {
				fps = frames;
				frames = 0;
				fpsCountStart = System.nanoTime();
			}
		}
	}

	private void readInput() {
		if (ui.keyPressed(KeyEvent.VK_LEFT)) {
			pacMan.wishDir = LEFT;
		} else if (ui.keyPressed(KeyEvent.VK_RIGHT)) {
			pacMan.wishDir = RIGHT;
		} else if (ui.keyPressed(KeyEvent.VK_UP)) {
			pacMan.wishDir = UP;
		} else if (ui.keyPressed(KeyEvent.VK_DOWN)) {
			pacMan.wishDir = DOWN;
		} else if (ui.keyPressed(KeyEvent.VK_D)) {
			ui.debugDraw = !ui.debugDraw;
		} else if (ui.keyPressed(KeyEvent.VK_E)) {
			eatAllFood();
		} else if (ui.keyPressed(KeyEvent.VK_X)) {
			ghostsKilledUsingEnergizer = 0;
			for (Creature ghost : ghosts) {
				killGhost(ghost);
			}
		}
	}

	private void update() {
		readInput();
		if (state == GameState.READY) {
			runReadyState();
		} else if (state == GameState.CHASING) {
			runChasingState();
		} else if (state == GameState.SCATTERING) {
			runScatteringState();
		} else if (state == GameState.CHANGING_LEVEL) {
			runChangingLevelState();
		} else if (state == GameState.PACMAN_DYING) {
			runPacManDyingState();
		} else if (state == GameState.GAME_OVER) {
			runGameOverState();
		}
	}

	private void runReadyState() {
		if (readyStateTimer == 0) {
			exitReadyState();
			enterScatteringState();
			return;
		}
		for (int i = 1; i <= 3; ++i) {
			bounce(ghosts[i]);
		}
		--readyStateTimer;
	}

	private void enterReadyState() {
		state = GameState.READY;
		readyStateTimer = sec(3);
		ui.yellowText();
		messageText = "Ready!";
		initEntities();
	}

	private void exitReadyState() {
		messageText = null;
		for (Creature ghost : ghosts) {
			ghost.leavingHouse = true;
		}
		ghosts[0].leavingHouse = false;
		ghosts[0].forcedOnTrack = true;
	}

	private void runScatteringState() {
		if (pacMan.dead) {
			enterPacManDyingState();
			return;
		}
		if (foodRemaining == 0) {
			enterChangingLevelState();
			return;
		}
		if (scatteringStateTimer == 0) {
			enterChasingState();
			return;
		}
		if (pacManPowerTimer == 0) {
			--scatteringStateTimer;
		}
		updatePacMan();
		updateGhosts();
		updateBonus();
	}

	private void enterScatteringState() {
		state = GameState.SCATTERING;
		scatteringStateTimer = SCATTERING_TIMES[waveTimes(level)][attackWave];
		forceGhostsTurningBack();
	}

	private void runChasingState() {
		if (pacMan.dead) {
			enterPacManDyingState();
			return;
		}
		if (foodRemaining == 0) {
			enterChangingLevelState();
			return;
		}
		if (chasingStateTimer == 0) {
			++attackWave;
			enterScatteringState();
			return;
		}
		if (pacManPowerTimer == 0) {
			--chasingStateTimer;
		}
		updatePacMan();
		updateGhosts();
		updateBonus();
	}

	private void enterChasingState() {
		state = GameState.CHASING;
		chasingStateTimer = CHASING_TIMES[waveTimes(level)][attackWave];
		forceGhostsTurningBack();
	}

	private void runPacManDyingState() {
		if (pacManDyingStateTimer == 0) {
			exitPacManDyingState();
			if (lives > 0) {
				enterReadyState();
			} else {
				enterGameOverState();
			}
			return;
		}
		if (pacManDyingStateTimer == sec(2.5f) + 88) {
			for (Creature ghost : ghosts) {
				ghost.visible = false;
			}
		}
		pacManDyingStateTimer--;
	}

	private void enterPacManDyingState() {
		state = GameState.PACMAN_DYING;
		// 11 animation frames, 8 ticks each, 2 seconds before animation, 2 seconds after
		pacManDyingStateTimer = sec(2) + 88 + sec(2);
	}

	private void exitPacManDyingState() {
		for (Creature ghost : ghosts) {
			ghost.visible = true;
		}
	}

	private void runChangingLevelState() {
		if (levelChangeStateTimer == 0) {
			log("Level %d complete, entering level %d", level, level + 1);
			initLevel(++level);
			enterReadyState();
			return;
		}
		--levelChangeStateTimer;
	}

	private void enterChangingLevelState() {
		state = GameState.CHANGING_LEVEL;
		levelChangeStateTimer = sec(7);
		mazeFlashes = levelData().numFlashes();
		log("Maze flashes: %d", mazeFlashes);
		for (Creature ghost : ghosts) {
			ghost.visible = false;
		}
	}

	private void runGameOverState() {
		if (ui.keyPressed(KeyEvent.VK_SPACE)) {
			exitGameOverState();
			initGame();
		}
	}

	private void enterGameOverState() {
		state = GameState.GAME_OVER;
		ui.redText();
		messageText = "Game Over!";
	}

	private void exitGameOverState() {
		messageText = null;
	}

	private void updatePacMan() {
		pacMan.speed = levelData().pacManSpeed();
		move(pacMan);

		// Pac-man power expiring?
		if (pacManPowerTimer > 0) {
			pacManPowerTimer--;
			if (pacManPowerTimer == 0) {
				for (Creature ghost : ghosts) {
					ghost.frightened = false;
				}
			}
		}

		// food found?
		int x = pacMan.tile.x_int(), y = pacMan.tile.y_int();
		if (world.isFoodTile(x, y) && !hasEatenFood(x, y)) {
			eatenFood.set(world.index(x, y));
			foodRemaining--;
			points += 10;
			// energizer found?
			if (world.isEnergizerTile(pacMan.tile)) {
				points += 40;
				pacManPowerTimer = sec(levelData().ghostFrightenedSeconds());
				log("Pac-Man got power for %d seconds", levelData().ghostFrightenedSeconds());
				for (Creature ghost : ghosts) {
					ghost.frightened = !ghost.dead;
				}
				ghostsKilledUsingEnergizer = 0;
				forceGhostsTurningBack();
			}
			// bonus reached?
			if (bonusAvailableTimer == 0 && (foodRemaining == 70 || foodRemaining == 170)) {
				bonusAvailableTimer = sec(9) + new Random().nextInt(FPS);
			}
		}
		// bonus found?
		if (bonusAvailableTimer > 0 && world.isBonusTile(x, y)) {
			bonusAvailableTimer = 0;
			bonusConsumedTimer = sec(3);
			points += levelData().bonusPoints();
			log("Pac-Man found bonus %s of value %d", levelData().bonusSymbol(), levelData().bonusPoints());
		}
		// meeting ghost?
		for (Creature ghost : ghosts) {
			if (!pacMan.tile.equals(ghost.tile)) {
				continue;
			}
			// killing ghost?
			if (ghost.frightened) {
				killGhost(ghost);
			}
			// getting killed by ghost?
			if (pacManPowerTimer == 0 && !ghost.dead) {
				log("Pac-Man killed by %s at location %s", ghost.name, ghost.tile);
				pacMan.dead = true;
				--lives;
				break;
			}
		}
	}

	private void updateBonus() {
		if (bonusAvailableTimer > 0) {
			--bonusAvailableTimer;
		}
		if (bonusConsumedTimer > 0) {
			--bonusConsumedTimer;
		}
	}

	private void killGhost(Creature ghost) {
		ghost.dead = true;
		ghost.frightened = false;
		ghost.targetTile = ghosts[0].homeTile;
		ghostsKilledUsingEnergizer++;
		ghost.bounty = (int) Math.pow(2, ghostsKilledUsingEnergizer) * 100;
		ghost.bountyTimer = sec(0.5f);
		log("Ghost %s killed at location %s, Pac-Man wins %d points", ghost.name, ghost.tile, ghost.bounty);
	}

	private void updateGhosts() {
		for (int i = 0; i < 4; ++i) {
			Creature ghost = ghosts[i];
			log("%s", ghost);
			if (ghost.bountyTimer > 0) {
				--ghost.bountyTimer;
			} else if (ghost.enteringHouse) {
				letGhostEnterHouse(ghost);
			} else if (ghost.leavingHouse) {
				letGhostLeaveHouse(ghost);
			} else if (ghost.dead) {
				letGhostReturnHome(ghost);
			} else if (state == GameState.SCATTERING) {
				ghost.targetTile = ghost.scatterTile;
				letGhostHeadForTargetTile(ghost);
			} else if (state == GameState.CHASING) {
				ghost.targetTile = computeChasingTarget(i);
				letGhostHeadForTargetTile(ghost);
			}
		}
	}

	private V2 computeChasingTarget(int ghostIndex) {
		switch (ghostIndex) {
		case BLINKY:
			return pacMan.tile;
		case PINKY:
			// simulate offset bug when Pac-Man is looking UP
			return pacMan.dir.equals(UP) ? pacMan.tile.sum(pacMan.dir.vector.scaled(4)).sum(LEFT.vector.scaled(4))
					: pacMan.tile.sum(pacMan.dir.vector.scaled(4));
		case INKY:
			return pacMan.tile.sum(pacMan.dir.vector.scaled(2)).scaled(2).sum(ghosts[BLINKY].tile.scaled(-1));
		case CLYDE:
			return distance(ghosts[CLYDE].tile, pacMan.tile) > 8 ? pacMan.tile : ghosts[CLYDE].scatterTile;
		default:
			throw new IllegalArgumentException("Unknown ghost index: " + ghostIndex);
		}
	}

	private void letGhostHeadForTargetTile(Creature ghost) {
		updateGhostDir(ghost);
		updateGhostSpeed(ghost);
		move(ghost);
	}

	private void letGhostReturnHome(Creature ghost) {
		// house entry reached?
		if (ghost.tile.equals(ghosts[BLINKY].homeTile) && Math.abs(ghost.offset.x - HTS) <= 2) {
			ghost.offset = new V2(HTS, 0);
			ghost.targetTile = ghost == ghosts[BLINKY] ? ghosts[PINKY].homeTile : ghost.homeTile;
			ghost.dir = ghost.wishDir = DOWN;
			ghost.forcedOnTrack = false;
			ghost.enteringHouse = true;
			log("%s entering house", ghost);
			return;
		}
		letGhostHeadForTargetTile(ghost);
	}

	private void letGhostEnterHouse(Creature ghost) {
		// reached target in house?
		if (ghost.tile.equals(ghost.targetTile) && ghost.offset.y >= 0 && Math.abs(ghost.offset.x - HTS) <= 2) {
			ghost.dead = false;
			ghost.dir = ghost.wishDir = ghost.wishDir.inverse();
			ghost.enteringHouse = false;
			ghost.leavingHouse = true;
			log("%s leaving house", ghost);
			return;
		}
		if (ghost.tile.equals(ghosts[PINKY].homeTile) && ghost.offset.y >= 0) {
			ghost.dir = ghost.wishDir = ghost.homeTile.x < ghosts[PINKY].homeTile.x ? LEFT : RIGHT;
		}
		updateGhostSpeed(ghost);
		move(ghost, ghost.wishDir);
		log("%s entering house", ghost);
	}

	private void letGhostLeaveHouse(Creature ghost) {
		// has left house?
		if (ghost.tile.equals(ghosts[BLINKY].homeTile) && Math.abs(ghost.offset.y) <= 1) {
			ghost.leavingHouse = false;
			ghost.wishDir = LEFT;
			ghost.forcedOnTrack = true;
			ghost.offset = BETWEEN_TILES;
			return;
		}
		// has reached middle of house?
		if (ghost.tile.equals(ghosts[PINKY].homeTile) && Math.abs(ghost.offset.x - 3) <= 1) {
			ghost.wishDir = UP;
			ghost.offset = BETWEEN_TILES;
			updateGhostSpeed(ghost);
			move(ghost);
			return;
		}
		// keep bouncing until ghost can move towards middle of house
		if (ghost.wishDir.equals(UP) || ghost.wishDir.equals(DOWN)) {
			if (ghost.tile.equals(ghost.homeTile)) {
				ghost.offset = BETWEEN_TILES;
				ghost.wishDir = ghost.homeTile.x < ghosts[PINKY].homeTile.x ? RIGHT : LEFT;
				return;
			}
			bounce(ghost);
			return;
		}
		updateGhostSpeed(ghost);
		move(ghost);
	}

	private void updateGhostSpeed(Creature ghost) {
		if (ghost.bountyTimer > 0) {
			ghost.speed = 0;
		} else if (ghost.enteringHouse) {
			ghost.speed = levelData().ghostSpeed();
		} else if (ghost.leavingHouse) {
			ghost.speed = 0.5f * levelData().ghostSpeed();
		} else if (ghost.dead) {
			ghost.speed = 1f * levelData().ghostSpeed();
		} else if (world.isInsideTunnel(ghost.tile)) {
			ghost.speed = levelData().ghostTunnelSpeed();
		} else if (ghost.frightened) {
			ghost.speed = levelData().frightenedGhostSpeed();
		} else {
			ghost.speed = levelData().ghostSpeed();
			if (ghost == ghosts[0]) {
				checkElroySpeed(ghost);
			}
		}
	}

	private void checkElroySpeed(Creature blinky) {
		if (foodRemaining <= levelData().elroy2DotsLeft()) {
			blinky.speed = levelData().elroy2Speed();
		} else if (foodRemaining <= levelData().elroy1DotsLeft()) {
			blinky.speed = levelData().elroy1Speed();
		}
	}

	private void updateGhostDir(Creature ghost) {
		if (ghost.targetTile == null) {
			return;
		}
		if (!ghost.stuck && !ghost.tileChanged) {
			return;
		}
		if (world.isPortalTile(ghost.tile)) {
			return;
		}
		if (ghost.forcedTurningBack) {
			ghost.wishDir = ghost.wishDir.inverse();
			ghost.forcedTurningBack = false;
			return;
		}
		if (pacManPowerTimer > 0 && world.isIntersectionTile(ghost.tile)) {
			ghost.wishDir = randomMoveDir(ghost);
			return;
		}
		Direction newDir = null;
		double min = Double.MAX_VALUE;
		for (Direction dir : List.of(RIGHT, DOWN, LEFT, UP) /* order matters! */) {
			if (dir.equals(ghost.dir.inverse())) {
				continue;
			}
			V2 neighbor = ghost.tile.sum(dir.vector);
			if (dir.equals(UP) && world.isUpwardsBlocked(neighbor)) {
				continue;
			}
			if (!canAccessTile(ghost, neighbor)) {
				continue;
			}
			double d = distance(neighbor, ghost.targetTile);
			if (d <= min) {
				newDir = dir;
				min = d;
			}
		}
		if (newDir != null) {
			ghost.wishDir = newDir;
//			log("%s's intended direction is %s", ghost.name, ghost.intendedDir);
		}
	}

	private void forceGhostsTurningBack() {
		for (Creature ghost : ghosts) {
			if (ghost.dead) {
				continue;
			}
			ghost.forcedTurningBack = true;
		}
	}

	private void bounce(Creature ghost) {
		if (ghost.stuck) {
			ghost.dir = ghost.wishDir = ghost.wishDir.inverse();
		}
		ghost.speed = levelData().ghostSpeed();
		move(ghost, ghost.wishDir);
	}

	private void move(Creature guy) {
		if (guy.speed == 0) {
			return;
		}
		move(guy, guy.wishDir);
		if (!guy.stuck) {
			guy.dir = guy.wishDir;
		} else {
			move(guy, guy.dir);
		}
	}

	private void move(Creature guy, Direction dir) {

		// portal
		if (guy.tile.equals(World.PORTAL_RIGHT_ENTRY) && dir.equals(RIGHT)) {
			guy.tile = World.PORTAL_LEFT_ENTRY;
			guy.offset = V2.NULL;
			guy.stuck = false;
			return;
		}
		if (guy.tile.equals(World.PORTAL_LEFT_ENTRY) && dir.equals(LEFT)) {
			guy.tile = World.PORTAL_RIGHT_ENTRY;
			guy.offset = V2.NULL;
			guy.stuck = false;
			return;
		}

		// turns
		if (guy.forcedOnTrack && canAccessTile(guy, guy.tile.sum(dir.vector))) {
			if (dir.equals(LEFT) || dir.equals(RIGHT)) {
				if (Math.abs(guy.offset.y) > 1) {
					guy.stuck = true;
					return;
				}
				guy.offset = new V2(guy.offset.x, 0);
			} else if (dir.equals(UP) || dir.equals(DOWN)) {
				if (Math.abs(guy.offset.x) > 1) {
					guy.stuck = true;
					return;
				}
				guy.offset = new V2(0, guy.offset.y);
			}
		}

		V2 velocity = dir.vector.scaled(1.25f * guy.speed); // 100% speed corresponds to 1.25 pixels/tick
		V2 positionAfterMove = world.position(guy).sum(velocity);
		V2 tileAfterMove = world.tile(positionAfterMove);
		V2 offsetAfterMove = world.offset(positionAfterMove, tileAfterMove);

		if (!canAccessTile(guy, tileAfterMove)) {
			guy.stuck = true;
			return;
		}

		// avoid moving partially into inaccessible tile
		if (tileAfterMove.equals(guy.tile)) {
			if (!canAccessTile(guy, guy.tile.sum(dir.vector))) {
				if (dir.equals(RIGHT) && offsetAfterMove.x > 0 || dir.equals(LEFT) && offsetAfterMove.x < 0) {
					guy.offset = new V2(0, guy.offset.y);
					guy.stuck = true;
					return;
				}
				if (dir.equals(DOWN) && offsetAfterMove.y > 0 || dir.equals(UP) && offsetAfterMove.y < 0) {
					guy.offset = new V2(guy.offset.x, 0);
					guy.stuck = true;
					return;
				}
			}
		}
		guy.tileChanged = !guy.tile.equals(tileAfterMove);
		guy.tile = tileAfterMove;
		guy.offset = offsetAfterMove;
		guy.stuck = false;
	}

	private void eatAllFood() {
		for (int x = 0; x < WORLD_WIDTH_TILES; ++x) {
			for (int y = 0; y < WORLD_HEIGHT_TILES; ++y) {
				int index = world.index(x, y);
				if (world.isFoodTile(x, y) && !eatenFood.get(index)) {
					eatenFood.set(index);
					foodRemaining = 0;
				}
			}
		}
	}

	private boolean canAccessTile(Creature guy, V2 tile) {
		int x = tile.x_int(), y = tile.y_int();
		if (x < 0 || x >= WORLD_WIDTH_TILES) {
			return y == 17; // can leave world through horizontal tunnel
		}
		if (y < 0 || y >= WORLD_HEIGHT_TILES) {
			return false;
		}
		if (world.isGhostHouseDoor(tile)) {
			return guy.enteringHouse || guy.leavingHouse;
		}
		return world.map(x, y) != '1';
	}

	private Direction randomMoveDir(Creature guy) {
		List<Direction> dirs = new ArrayList<>(3);
		for (Direction dir : Direction.values()) {
			if (dir.equals(guy.dir.inverse())) {
				continue;
			}
			V2 neighbor = guy.tile.sum(dir.vector);
			if (world.isAccessibleTile(neighbor)) {
				dirs.add(dir);
			}
		}
		return dirs.get(new Random().nextInt(dirs.size()));
	}

	public boolean hasEatenFood(int x, int y) {
		return eatenFood.get(world.index(x, y));
	}
}