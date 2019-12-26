package de.amr.games.pacman.theme;

import static de.amr.easy.game.Application.LOGGER;
import static de.amr.easy.game.ui.sprites.AnimationType.BACK_AND_FORTH;
import static de.amr.easy.game.ui.sprites.AnimationType.CYCLIC;
import static de.amr.easy.game.ui.sprites.AnimationType.LINEAR;
import static de.amr.graph.grid.impl.Grid4Topology.E;
import static de.amr.graph.grid.impl.Grid4Topology.N;
import static de.amr.graph.grid.impl.Grid4Topology.S;
import static de.amr.graph.grid.impl.Grid4Topology.W;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import de.amr.easy.game.assets.Assets;
import de.amr.easy.game.assets.Sound;
import de.amr.easy.game.ui.sprites.Sprite;
import de.amr.games.pacman.model.BonusSymbol;

/**
 * Theme based on original(?) sprites.
 * 
 * @author Armin Reichert
 */
public class ClassicPacManTheme implements PacManTheme {

	private static Sound sound(String name, String type) {
		return Assets.sound("sfx/" + name + "." + type);
	}

	private static Sound mp3(String name) {
		return sound(name, "mp3");
	}

	private static Sound wav(String name) {
		return sound(name, "wav");
	}

	private static BufferedImage changeColor(BufferedImage src, int from, int to) {
		BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
		Graphics2D g = copy.createGraphics();
		g.drawImage(src, 0, 0, null);
		for (int x = 0; x < copy.getWidth(); ++x) {
			for (int y = 0; y < copy.getHeight(); ++y) {
				if (copy.getRGB(x, y) == from) {
					copy.setRGB(x, y, to);
				}
			}
		}
		g.dispose();
		return copy;
	}

	private final BufferedImage sheet;
	private final BufferedImage mazeEmpty;
	private final BufferedImage mazeFull;
	private final BufferedImage mazeWhite;
	private final BufferedImage pacManFull;
	private final BufferedImage pacManWalking[][];
	private final BufferedImage pacManDying[];
	private final BufferedImage ghostColored[][];
	private final BufferedImage ghostFrightened[];
	private final BufferedImage ghostFlashing[];
	private final BufferedImage ghostEyes[];
	private final BufferedImage greenNumbers[];
	private final BufferedImage pinkNumbers[];
	private final Map<BonusSymbol, BufferedImage> symbolMap = new HashMap<>();

	private BufferedImage $(int x, int y, int w, int h) {
		return sheet.getSubimage(x, y, w, h);
	}

	private BufferedImage $(int x, int y) {
		return $(x, y, 16, 16);
	}

	private BufferedImage[] hstrip(int n, int x, int y) {
		return IntStream.range(0, n).mapToObj(i -> $(x + i * 16, y)).toArray(BufferedImage[]::new);
	}

	public ClassicPacManTheme() {
		Assets.storeTrueTypeFont("font.joystix", "Joystix.ttf", Font.PLAIN, 12);

		sheet = Assets.readImage("pacman_sprites.png");
		mazeFull = Assets.readImage("maze_full.png");
		mazeEmpty = Assets.readImage("maze_empty.png");
		int blue = -14605825; // debugger told me this
		mazeWhite = changeColor(mazeEmpty, blue, Color.WHITE.getRGB());

		// Symbols for bonuses
		BonusSymbol[] symbols = BonusSymbol.values();
		BufferedImage[] symbolImages = hstrip(8, 32, 48);
		for (int i = 0; i < 8; ++i) {
			symbolMap.put(symbols[i], symbolImages[i]);
		}

		// Pac-Man
		pacManFull = $(32, 0);

		// E, W, N, S -> 0(N), 1(E), 2(S), 3(W)
		int reorder[] = { 1, 3, 0, 2 };
		pacManWalking = new BufferedImage[4][];
		for (int dir = 0; dir < 4; ++dir) {
			BufferedImage mouthOpen = $(0, dir * 16), mouthHalfOpen = $(16, dir * 16);
			pacManWalking[reorder[dir]] = new BufferedImage[] { mouthOpen, mouthHalfOpen, pacManFull };
		}

		pacManDying = hstrip(12, 32, 0);

		// Ghosts
		ghostColored = new BufferedImage[4][8];
		for (int color = 0; color < 4; ++color) {
			for (int i = 0; i < 8; ++i) {
				ghostColored[color][i] = $(i * 16, 64 + color * 16);
			}
		}

		ghostFrightened = hstrip(2, 128, 64);
		ghostFlashing = hstrip(4, 128, 64);

		ghostEyes = new BufferedImage[4];
		for (int dir = 0; dir < 4; ++dir) {
			ghostEyes[reorder[dir]] = $(128 + dir * 16, 80);
		}

		// Green numbers (200, 400, 800, 1600)
		greenNumbers = hstrip(4, 0, 128);

		// Pink numbers
		pinkNumbers = new BufferedImage[8];
		// horizontal: 100, 300, 500, 700
		for (int i = 0; i < 4; ++i) {
			pinkNumbers[i] = $(i * 16, 144);
		}
		// 1000
		pinkNumbers[4] = $(64, 144, 19, 16);
		// vertical: 2000, 3000, 5000)
		for (int j = 0; j < 3; ++j) {
			pinkNumbers[5 + j] = $(56, 160 + j * 16, 2 * 16, 16);
		}
		LOGGER.info(String.format("Theme '%s' created.", getClass().getSimpleName()));
	}

	@Override
	public Sprite spr_emptyMaze() {
		return Sprite.of(mazeEmpty);
	}

	@Override
	public Sprite spr_fullMaze() {
		return Sprite.of(mazeFull);
	}

	@Override
	public Sprite spr_flashingMaze() {
		return Sprite.of(mazeEmpty, mazeWhite).animate(CYCLIC, MAZE_FLASH_TIME_MILLIS / 2);
	}

	@Override
	public Sprite spr_bonusSymbol(BonusSymbol symbol) {
		return Sprite.of(symbolMap.get(symbol));
	}

	@Override
	public Sprite spr_pacManFull() {
		return Sprite.of(pacManFull);
	}

	@Override
	public Sprite spr_pacManWalking(int dir) {
		return Sprite.of(pacManWalking[dir]).animate(BACK_AND_FORTH, 20);
	}

	@Override
	public Sprite spr_pacManDying() {
		return Sprite.of(pacManDying).animate(LINEAR, 100);
	}

	@Override
	public Sprite spr_ghostColored(GhostColor color, int direction) {
		BufferedImage[] frames;
		switch (direction) {
		case E:
			frames = Arrays.copyOfRange(ghostColored[color.ordinal()], 0, 2);
			break;
		case W:
			frames = Arrays.copyOfRange(ghostColored[color.ordinal()], 2, 4);
			break;
		case N:
			frames = Arrays.copyOfRange(ghostColored[color.ordinal()], 4, 6);
			break;
		case S:
			frames = Arrays.copyOfRange(ghostColored[color.ordinal()], 6, 8);
			break;
		default:
			throw new IllegalArgumentException("Illegal direction: " + direction);
		}
		return Sprite.of(frames).animate(BACK_AND_FORTH, 300);
	}

	@Override
	public Sprite spr_ghostFrightened() {
		return Sprite.of(ghostFrightened).animate(CYCLIC, 300);
	}

	@Override
	public Sprite spr_ghostFlashing() {
		return Sprite.of(ghostFlashing).animate(CYCLIC, 100);
	}

	@Override
	public Sprite spr_ghostEyes(int dir) {
		return Sprite.of(ghostEyes[dir]);
	}

	@Override
	public Sprite spr_greenNumber(int i) {
		return Sprite.of(greenNumbers[i]);
	}

	@Override
	public Sprite spr_pinkNumber(int i) {
		return Sprite.of(pinkNumbers[i]);
	}

	@Override
	public Font fnt_text() {
		return Assets.font("font.joystix");
	}

	@Override
	public Stream<Sound> snd_clips_all() {
		return Stream.of(snd_die(), snd_eatFruit(), snd_eatGhost(), snd_eatPill(), snd_extraLife(), snd_insertCoin(),
				snd_ready(), snd_ghost_chase(), snd_ghost_dead(), snd_waza());
	}

	@Override
	public Sound music_playing() {
		return mp3("bgmusic");
	}

	@Override
	public Sound music_gameover() {
		return mp3("ending");
	}

	@Override
	public void loadMusic() {
	}

	@Override
	public Sound snd_die() {
		return mp3("die");
	}

	@Override
	public Sound snd_eatFruit() {
		return mp3("eat-fruit");
	}

	@Override
	public Sound snd_eatGhost() {
		return mp3("eat-ghost");
	}

	@Override
	public Sound snd_eatPill() {
		return wav("pacman_eat");
	}

	@Override
	public Sound snd_extraLife() {
		return mp3("extra-life");
	}

	@Override
	public Sound snd_insertCoin() {
		return mp3("insert-coin");
	}

	@Override
	public Sound snd_ready() {
		return mp3("ready");
	}

	@Override
	public Sound snd_ghost_dead() {
		return wav("ghost_dead");
	}

	@Override
	public Sound snd_ghost_chase() {
		return wav("ghost_chase");
	}

	@Override
	public Sound snd_waza() {
		return mp3("waza");
	}
}