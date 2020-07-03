package de.amr.games.pacman.view.play;

import static de.amr.games.pacman.PacManApp.settings;
import static de.amr.games.pacman.controller.actor.GhostState.CHASING;
import static de.amr.games.pacman.controller.actor.GhostState.DEAD;
import static de.amr.games.pacman.controller.actor.GhostState.ENTERING_HOUSE;
import static de.amr.games.pacman.controller.actor.GhostState.FRIGHTENED;
import static de.amr.games.pacman.controller.actor.GhostState.LEAVING_HOUSE;
import static de.amr.games.pacman.controller.actor.GhostState.SCATTERING;
import static de.amr.games.pacman.model.Direction.RIGHT;
import static java.lang.Math.round;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.List;

import de.amr.easy.game.entity.Entity;
import de.amr.easy.game.math.Vector2f;
import de.amr.easy.game.ui.widgets.FrameRateWidget;
import de.amr.easy.game.view.Pen;
import de.amr.games.pacman.controller.GhostCommand;
import de.amr.games.pacman.controller.actor.Creature;
import de.amr.games.pacman.controller.actor.Ghost;
import de.amr.games.pacman.controller.actor.PacMan;
import de.amr.games.pacman.controller.actor.steering.PathProvidingSteering;
import de.amr.games.pacman.controller.ghosthouse.GhostHouseAccessControl;
import de.amr.games.pacman.model.Direction;
import de.amr.games.pacman.model.Game;
import de.amr.games.pacman.model.world.api.World;
import de.amr.games.pacman.model.world.core.Tile;
import de.amr.games.pacman.view.theme.Theme;

/**
 * An extended play view that can visualize actor states, the ghost house pellet counters, ghost
 * routes, the grid background, ghost seats and the current framerate.
 * 
 * @author Armin Reichert
 */
public class PlayView extends SimplePlayView {

	private static final String INFTY = Character.toString('\u221E');
	private static final Font SMALL_FONT = new Font("Arial Narrow", Font.PLAIN, 6);

	public boolean showingFrameRate = false;
	public boolean showingScores = true;
	public boolean showingStates = false;

	/** Optional ghost house control */
	public GhostCommand optionalGhostCommand;

	/** Optional ghost house reference */
	public GhostHouseAccessControl optionalHouseAccessControl;

	private FrameRateWidget frameRateDisplay;

	/** Used in dot counter visualization */
	private final Image inkyImage, clydeImage, pacManImage;

	public PlayView(World world, Theme theme, Game game, int width, int height) {
		super(world, theme, game, width, height);
		inkyImage = theme.spr_ghostColored(Theme.CYAN_GHOST, Direction.RIGHT).frame(0);
		clydeImage = theme.spr_ghostColored(Theme.ORANGE_GHOST, Direction.RIGHT).frame(0);
		pacManImage = theme.spr_pacManWalking(RIGHT).frame(0);
		frameRateDisplay = new FrameRateWidget();
		frameRateDisplay.tf.setPosition(0, 18 * Tile.SIZE);
		frameRateDisplay.font = new Font(Font.MONOSPACED, Font.BOLD, 8);
	}

	@Override
	public void draw(Graphics2D g) {
		drawWorld(g);
		if (showingFrameRate) {
			frameRateDisplay.draw(g);
		}
		drawPlayMode(g);
		drawMessage(g, messageText);
		if (showingScores) {
			drawScores(g);
		}
		drawActors(g);
//		if (showingGrid) {
//			drawActorOffTrack(g);
//		}
		if (showingStates) {
			drawActorStates(g);
			drawGhostHouseState(g);
		}
	}

	private void drawEntityState(Graphics2D g, Entity entity, String text, Color color) {
		try (Pen pen = new Pen(g)) {
			pen.color(color);
			pen.font(SMALL_FONT);
			pen.drawCentered(text, entity.tf.getCenter().x, entity.tf.getCenter().y - 2);
		}
	}

	private void drawPlayMode(Graphics2D g) {
		if (settings.demoMode) {
			try (Pen pen = new Pen(g)) {
				pen.font(theme.fnt_text());
				pen.color(Color.LIGHT_GRAY);
				pen.hcenter("Autopilot", width, 15, Tile.SIZE);
			}
		}
	}

	private void drawActorStates(Graphics2D g) {
		world.population().ghosts().filter(world::included).forEach(ghost -> drawGhostState(g, ghost));
		drawPacManState(g, world.population().pacMan());
	}

	private void drawPacManState(Graphics2D g, PacMan pacMan) {
		if (!pacMan.visible) {
			return;
		}
		String text = pacMan.power > 0 ? String.format("POWER(%d)", pacMan.power) : pacMan.getState().name();
		if (settings.pacManImmortable) {
			text += " immortable";
		}
		drawEntityState(g, pacMan, text, Color.YELLOW);
	}

	private void drawGhostState(Graphics2D g, Ghost ghost) {
		if (!ghost.visible) {
			return;
		}
		if (ghost.getState() == null) {
			return; // may happen in test applications where not all ghosts are used
		}
		StringBuilder text = new StringBuilder();
		// show ghost name if not obvious
		text.append(ghost.is(DEAD, FRIGHTENED, ENTERING_HOUSE) ? ghost.name : "");
		// timer values
		int duration = ghost.state().getDuration();
		int remaining = ghost.state().getTicksRemaining();
		// chasing or scattering time
		if (optionalGhostCommand != null && ghost.is(SCATTERING, CHASING)) {
			if (optionalGhostCommand.state() != null) {
				duration = optionalGhostCommand.state().getDuration();
				remaining = optionalGhostCommand.state().getTicksRemaining();
			}
		}
		if (duration != Integer.MAX_VALUE) {
			text.append(String.format("(%s,%d|%d)", ghost.getState(), remaining, duration));
		} else {
			text.append(String.format("(%s,%s)", ghost.getState(), INFTY));
		}
		if (ghost.is(LEAVING_HOUSE)) {
			text.append(String.format("[->%s]", ghost.subsequentState));
		}
		drawEntityState(g, ghost, text.toString(), worldRenderer.ghostColor(ghost));
	}

	private void drawPacManStarvingTime(Graphics2D g) {
		int col = 1, row = 14;
		int time = optionalHouseAccessControl.pacManStarvingTicks();
		g.drawImage(pacManImage, col * Tile.SIZE, row * Tile.SIZE, 10, 10, null);
		try (Pen pen = new Pen(g)) {
			pen.font(new Font(Font.MONOSPACED, Font.BOLD, 8));
			pen.color(Color.WHITE);
			pen.smooth(() -> pen.drawAtGridPosition(time == -1 ? INFTY : String.format("%d", time), col + 2, row, Tile.SIZE));
		}
	}

	private void drawActorOffTrack(Graphics2D g) {
		world.population().creatures().filter(world::included).forEach(actor -> drawActorOffTrack(actor, g));
	}

	private void drawActorOffTrack(Creature<?> actor, Graphics2D g) {
		if (!actor.visible) {
			return;
		}
		Stroke normal = g.getStroke();
		Stroke fine = new BasicStroke(0.2f);
		g.setStroke(fine);
		g.setColor(Color.RED);
		g.translate(actor.tf.x, actor.tf.y);
		int w = actor.tf.width, h = actor.tf.height;
		Direction moveDir = actor.moveDir();
		if ((moveDir == Direction.LEFT || moveDir == Direction.RIGHT) && round(actor.tf.y) % Tile.SIZE != 0) {
			g.drawLine(0, 0, w, 0);
			g.drawLine(0, h, w, h);
		}
		if ((moveDir == Direction.UP || moveDir == Direction.DOWN) && round(actor.tf.x) % Tile.SIZE != 0) {
			g.drawLine(0, 0, 0, h);
			g.drawLine(w, 0, w, h);
		}
		g.translate(-actor.tf.x, -actor.tf.y);
		g.setStroke(normal);
	}


	private void drawGhostHouseState(Graphics2D g) {
		if (optionalHouseAccessControl == null) {
			return; // test scenes may have no ghost house
		}
		drawPacManStarvingTime(g);
		drawDotCounter(g, clydeImage, optionalHouseAccessControl.ghostDotCount(world.population().clyde()), 1, 20,
				!optionalHouseAccessControl.isGlobalDotCounterEnabled()
						&& optionalHouseAccessControl.isPreferredGhost(world.population().clyde()));
		drawDotCounter(g, inkyImage, optionalHouseAccessControl.ghostDotCount(world.population().inky()), 24, 20,
				!optionalHouseAccessControl.isGlobalDotCounterEnabled()
						&& optionalHouseAccessControl.isPreferredGhost(world.population().inky()));
		drawDotCounter(g, null, optionalHouseAccessControl.globalDotCount(), 24, 14,
				optionalHouseAccessControl.isGlobalDotCounterEnabled());
	}

	private void drawDotCounter(Graphics2D g, Image image, int value, int col, int row, boolean emphasized) {
		try (Pen pen = new Pen(g)) {
			if (image != null) {
				g.drawImage(image, col * Tile.SIZE, row * Tile.SIZE, 10, 10, null);
			}
			pen.font(new Font(Font.MONOSPACED, Font.BOLD, 8));
			pen.color(emphasized ? Color.GREEN : Color.WHITE);
			pen.smooth(() -> pen.drawAtGridPosition(String.format("%d", value), col + 2, row, Tile.SIZE));
		}
	}
}