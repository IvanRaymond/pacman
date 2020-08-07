package de.amr.games.pacman.view.theme.arcade;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import de.amr.easy.game.assets.Assets;
import de.amr.games.pacman.controller.creatures.ghost.Ghost;
import de.amr.games.pacman.controller.creatures.pacman.PacMan;
import de.amr.games.pacman.model.world.api.World;
import de.amr.games.pacman.view.common.MessagesRenderer;
import de.amr.games.pacman.view.common.ScoreRenderer;
import de.amr.games.pacman.view.theme.api.IGameRenderer;
import de.amr.games.pacman.view.theme.api.IGhostRenderer;
import de.amr.games.pacman.view.theme.api.IPacManRenderer;
import de.amr.games.pacman.view.theme.api.IWorldRenderer;
import de.amr.games.pacman.view.theme.api.PacManSounds;
import de.amr.games.pacman.view.theme.arcade.sounds.ArcadeSounds;
import de.amr.games.pacman.view.theme.core.AbstractTheme;

public class ArcadeTheme extends AbstractTheme {

	public static final ArcadeTheme THEME = new ArcadeTheme();

	private Map<World, WorldRenderer> worldRenderers = new HashMap<>();
	private Map<PacMan, PacManRenderer> pacManRenderers = new HashMap<>();
	private Map<Ghost, GhostRenderer> ghostRenderers = new HashMap<>();
	private MessagesRenderer messagesRenderer;

	private ArcadeTheme() {
		super("ARCADE");
		put("font", Assets.storeTrueTypeFont("PressStart2P", "themes/arcade/PressStart2P-Regular.ttf", Font.PLAIN, 8));
		put("maze-flash-sec", 0.4f);
		put("sprites", new ArcadeThemeSprites());
		put("sounds", ArcadeSounds.SOUNDS);
	}

	@Override
	public IWorldRenderer worldRenderer(World world) {
		WorldRenderer renderer = worldRenderers.get(world);
		if (renderer == null) {
			renderer = new WorldRenderer();
			worldRenderers.put(world, renderer);
		}
		return renderer;
	}

	@Override
	public IPacManRenderer pacManRenderer(PacMan pacMan) {
		PacManRenderer renderer = pacManRenderers.get(pacMan);
		if (renderer == null) {
			renderer = new PacManRenderer();
			pacManRenderers.put(pacMan, renderer);
		}
		return renderer;
	}

	@Override
	public IGhostRenderer ghostRenderer(Ghost ghost) {
		GhostRenderer renderer = ghostRenderers.get(ghost);
		if (renderer == null) {
			renderer = new GhostRenderer();
			ghostRenderers.put(ghost, renderer);
		}
		return renderer;
	}

	@Override
	public MessagesRenderer messagesRenderer() {
		if (messagesRenderer == null) {
			messagesRenderer = new MessagesRenderer();
			messagesRenderer.setFont($font("font"));
		}
		return messagesRenderer;
	}

	@Override
	public IGameRenderer levelCounterRenderer() {
		return new LevelCounterRenderer();
	}

	@Override
	public IGameRenderer livesCounterRenderer() {
		return new LivesCounterRenderer();
	}

	@Override
	public IGameRenderer scoreRenderer() {
		ScoreRenderer renderer = new ScoreRenderer();
		renderer.setFont($font("font"));
		return renderer;
	}

	@Override
	public PacManSounds sounds() {
		return $value("sounds");
	}
}