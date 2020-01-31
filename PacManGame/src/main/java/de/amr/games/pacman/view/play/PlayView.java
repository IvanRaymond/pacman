package de.amr.games.pacman.view.play;

import static de.amr.games.pacman.PacManApp.settings;
import static de.amr.games.pacman.actor.GhostState.CHASING;
import static de.amr.games.pacman.actor.GhostState.DEAD;
import static de.amr.games.pacman.actor.GhostState.ENTERING_HOUSE;
import static de.amr.games.pacman.actor.GhostState.FRIGHTENED;
import static de.amr.games.pacman.actor.GhostState.LEAVING_HOUSE;
import static de.amr.games.pacman.actor.GhostState.SCATTERING;
import static java.lang.Math.PI;
import static java.lang.Math.round;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Transparency;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import de.amr.easy.game.math.Vector2f;
import de.amr.games.pacman.actor.Bonus;
import de.amr.games.pacman.actor.BonusState;
import de.amr.games.pacman.actor.Cast;
import de.amr.games.pacman.actor.Ghost;
import de.amr.games.pacman.actor.GhostState;
import de.amr.games.pacman.actor.PacMan;
import de.amr.games.pacman.actor.core.Actor;
import de.amr.games.pacman.controller.House;
import de.amr.games.pacman.model.Direction;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.Tile;
import de.amr.games.pacman.theme.GhostColor;
import de.amr.games.pacman.view.core.FPSDisplay;
import de.amr.games.pacman.view.core.Pen;
import de.amr.statemachine.core.State;

/**
 * An extended play view.
 * 
 * @author Armin Reichert
 */
public class PlayView extends SimplePlayView {

	private static final String INFTY = Character.toString('\u221E');

	private static Color dimmed(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	public Supplier<State<GhostState, ?>> fnGhostCommandState = () -> null;
	public House house; // (optional)

	public BooleanSupplier showFPS = () -> false;
	public BooleanSupplier showRoutes = () -> false;
	public BooleanSupplier showGrid = () -> false;
	public BooleanSupplier showStates = () -> false;

	private FPSDisplay fps;
	private final BufferedImage gridImage, inkyImage, clydeImage, pacManImage;
	private final Polygon arrowHead;

	public PlayView(Cast cast) {
		super(cast);
		fps = new FPSDisplay();
		fps.tf.setPosition(0, 18 * Tile.SIZE);
		gridImage = createGridImage(cast.game().maze());
		inkyImage = ghostImage(GhostColor.CYAN);
		clydeImage = ghostImage(GhostColor.ORANGE);
		pacManImage = (BufferedImage) theme().spr_pacManWalking(Direction.RIGHT.ordinal()).frame(0);
		arrowHead = new Polygon(new int[] { -4, 4, 0 }, new int[] { 0, 0, 4 }, 3);
	}

	@Override
	public void draw(Graphics2D g) {
		if (showGrid.getAsBoolean()) {
			g.drawImage(gridImage, 0, 0, null);
		} else {
			fillBackground(g);
		}
		drawMaze(g);
		drawFPS(g);
		drawPlayMode(g);
		drawMessage(g);
		if (showGrid.getAsBoolean()) {
			drawUpwardsBlockedTileMarkers(g);
			drawSeats(g);
		}
		drawScores(g);
		if (showRoutes.getAsBoolean()) {
			drawRoutes(g);
		}
		drawActors(g);
		if (showGrid.getAsBoolean()) {
			drawActorAlignments(g);
		}
		if (showStates.getAsBoolean()) {
			drawActorStates(g);
			drawGhostHouseState(g);
		}
	}

	private BufferedImage createGridImage(Maze maze) {
		GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
				.getDefaultConfiguration();
		BufferedImage img = gc.createCompatibleImage(maze.numCols * Tile.SIZE, maze.numRows * Tile.SIZE + 1,
				Transparency.TRANSLUCENT);
		Graphics2D g = img.createGraphics();
		for (int row = 0; row < maze.numRows; ++row) {
			for (int col = 0; col < maze.numCols; ++col) {
				g.setColor(patternColor(col, row));
				g.fillRect(col * Tile.SIZE, row * Tile.SIZE, Tile.SIZE, Tile.SIZE);
			}
		}
		return img;
	}

	private BufferedImage ghostImage(GhostColor color) {
		return (BufferedImage) theme().spr_ghostColored(color, Direction.RIGHT.ordinal()).frame(0);
	}

	private Color patternColor(int col, int row) {
		return (row + col) % 2 == 0 ? Color.BLACK : new Color(30, 30, 30);
	}

	@Override
	protected Color bgColor(Tile tile) {
		return showGrid.getAsBoolean() ? patternColor(tile.col, tile.row) : super.bgColor(tile);
	}

	private Color color(Ghost ghost) {
		if (ghost == cast().blinky)
			return Color.RED;
		if (ghost == cast().pinky)
			return Color.PINK;
		if (ghost == cast().inky)
			return Color.CYAN;
		if (ghost == cast().clyde)
			return Color.ORANGE;
		throw new IllegalArgumentException("Unknown ghost: " + ghost);
	}

	private void drawSmallText(Graphics2D g, Color color, float x, float y, String text) {
		g.setColor(color);
		g.setFont(new Font("Arial Narrow", Font.PLAIN, 5));
		int sw = g.getFontMetrics().stringWidth(text);
		g.drawString(text, x - sw / 2, y - Tile.SIZE / 2);
	}

	private void drawPlayMode(Graphics2D g) {
		if (settings.demoMode) {
			try (Pen pen = new Pen(g)) {
				pen.font(theme().fnt_text(11));
				pen.color(Color.DARK_GRAY);
				pen.hcenter("Demo Mode", width(), 21);
			}
		}
	}

	private void drawFPS(Graphics2D g) {
		if (showFPS.getAsBoolean()) {
			fps.draw(g);
		}
	}

	private void drawActorStates(Graphics2D g) {
		cast.ghostsOnStage().forEach(ghost -> drawGhostState(g, ghost));
		drawPacManState(g);
		drawBonusState(g);
	}

	private void drawPacManState(Graphics2D g) {
		PacMan pacMan = cast().pacMan;
		if (pacMan.visible()) {
			String text = pacMan.getState().name();
			if (pacMan.powerTicks() > 0) {
				text = String.format("POWER(%d)", pacMan.powerTicks());
			}
			if (settings().pacManImmortable) {
				text += ",lives " + INFTY;
			}
			drawSmallText(g, Color.YELLOW, pacMan.tf.getX(), pacMan.tf.getY(), text);
		}
	}

	private void drawGhostState(Graphics2D g, Ghost ghost) {
		if (!ghost.visible()) {
			return;
		}
		StringBuilder text = new StringBuilder();
		// show ghost name if not obvious
		text.append(ghost.is(DEAD, FRIGHTENED, ENTERING_HOUSE) ? ghost.name() : "");
		// timer values
		int duration = ghost.state().getDuration();
		int remaining = ghost.state().getTicksRemaining();
		// chasing or scattering time
		if (ghost.is(SCATTERING, CHASING)) {
			State<GhostState, ?> attack = fnGhostCommandState.get();
			if (attack != null) {
				duration = attack.getDuration();
				remaining = attack.getTicksRemaining();
			}
		}
		text.append(duration == State.ENDLESS ? String.format("(%s,%s)", ghost.getState(), INFTY)
				: String.format("(%s,%d|%d)", ghost.getState(), remaining, duration));
		if (ghost.is(LEAVING_HOUSE)) {
			text.append(String.format("[->%s]", ghost.followState()));
		}
		drawSmallText(g, color(ghost), ghost.tf.getX(), ghost.tf.getY(), text.toString());
	}

	private void drawBonusState(Graphics2D g) {
		Bonus bonus = cast().bonus;
		String text = "";
		if (bonus.getState() == BonusState.INACTIVE) {
			text = "Bonus inactive";
		} else {
			text = String.format("%s,%d|%d", bonus, bonus.state().getTicksRemaining(), bonus.state().getDuration());
		}
		drawSmallText(g, Color.YELLOW, bonus.tf.getX(), bonus.tf.getY(), text);
	}

	private void drawPacManStarvingTime(Graphics2D g) {
		int col = 1, row = 14;
		int time = house.pacManStarvingTicks();
		g.drawImage(pacManImage, col * Tile.SIZE, row * Tile.SIZE, 10, 10, null);
		try (Pen pen = new Pen(g)) {
			pen.font(new Font(Font.MONOSPACED, Font.BOLD, 8));
			pen.color(Color.WHITE);
			pen.smooth(() -> pen.drawAtTilePosition(col + 2, row, time == -1 ? INFTY : String.format("%d", time)));
		}
	}

	private void drawActorAlignments(Graphics2D g) {
		cast().actorsOnStage().forEach(actor -> drawActorAlignment(actor, g));
	}

	private void drawActorAlignment(Actor<?> actor, Graphics2D g) {
		if (!actor.visible()) {
			return;
		}
		Stroke normal = g.getStroke();
		Stroke fine = new BasicStroke(0.2f);
		g.setStroke(fine);
		g.setColor(Color.GREEN);
		g.translate(actor.tf.getX(), actor.tf.getY());
		int w = actor.tf.getWidth(), h = actor.tf.getHeight();
		if (round(actor.tf.getY()) % Tile.SIZE == 0) {
			g.drawLine(0, 0, w, 0);
			g.drawLine(0, h, w, h);
		}
		if (round(actor.tf.getX()) % Tile.SIZE == 0) {
			g.drawLine(0, 0, 0, h);
			g.drawLine(w, 0, w, h);
		}
		g.translate(-actor.tf.getX(), -actor.tf.getY());
		g.setStroke(normal);
	}

	private void drawUpwardsBlockedTileMarkers(Graphics2D g) {
		g.setColor(dimmed(Color.LIGHT_GRAY, 80));
		for (int row = 0; row < maze().numRows; ++row) {
			for (int col = 0; col < maze().numCols; ++col) {
				Tile tile = maze().tileAt(col, row);
				if (maze().isNoUpIntersection(tile)) {
					Tile above = maze().tileToDir(tile, Direction.UP);
					drawArrowHead(g, Direction.DOWN, above.centerX(), above.y() - 2);
				}
			}
		}
	}

	private void drawSeats(Graphics2D g) {
		Ghost[] ghostsBySeat = { cast.blinky, cast.inky, cast.pinky, cast.clyde };
		IntStream.rangeClosed(0, 3).forEach(seat -> {
			Tile seatTile = maze().ghostHouseSeats[seat];
			g.setColor(color(ghostsBySeat[seat]));
			int x = seatTile.centerX(), y = seatTile.y();
			String text = String.valueOf(seat);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.drawRoundRect(x, y, Tile.SIZE, Tile.SIZE, 2, 2);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 6));
			FontMetrics fm = g.getFontMetrics();
			Rectangle2D r = fm.getStringBounds(text, g);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setColor(Color.WHITE);
			g.drawString(text, x + (Tile.SIZE - Math.round(r.getWidth())) / 2, y + Tile.SIZE - 2);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		});
	}

	private void drawArrowHead(Graphics2D g, Direction dir, int x, int y) {
		double[] angleForDir = { PI, -PI / 2, 0, PI / 2 };
		double angle = angleForDir[dir.ordinal()];
		g.translate(x, y);
		g.rotate(angle);
		g.fillPolygon(arrowHead);
		g.rotate(-angle);
		g.translate(-x, -y);
	}

	private void drawRoutes(Graphics2D g2) {
		boolean show = showRoutes.getAsBoolean();
		// TODO
		cast().ghosts().forEach(ghost -> {
			Arrays.asList(GhostState.values()).forEach(state -> {
				ghost.steering(state).enableTargetPathComputation(show);
			});
		});
		Graphics2D g = (Graphics2D) g2.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		cast().ghostsOnStage().filter(Ghost::visible).forEach(ghost -> drawRoute(g, ghost));
		g.dispose();
	}

	private void drawRoute(Graphics2D g, Ghost ghost) {
		Tile target = ghost.targetTile();
		List<Tile> targetPath = ghost.steering().targetPath();
		int pathLen = targetPath.size();
		Color ghostColor = color(ghost);
		Stroke solid = new BasicStroke(0.5f);
		Stroke dashed = new BasicStroke(0.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 3 },
				0);
		boolean drawRubberBand = target != null && pathLen > 0 && target != targetPath.get(pathLen - 1);
		if (drawRubberBand) {
			// draw rubber band to target tile
			int x1 = ghost.tf.getCenter().roundedX(), y1 = ghost.tf.getCenter().roundedY();
			int x2 = target.centerX(), y2 = target.centerY();
			g.setStroke(dashed);
			g.setColor(dimmed(ghostColor, 200));
			g.drawLine(x1, y1, x2, y2);
			g.translate(target.x(), target.y());
			g.setColor(ghostColor);
			g.setStroke(solid);
			g.fillRect(2, 2, 4, 4);
			g.translate(-target.x(), -target.y());
		}
		if (pathLen > 1) {
			// draw path
			g.setColor(dimmed(ghostColor, 200));
			for (int i = 0; i < targetPath.size() - 1; ++i) {
				Tile from = targetPath.get(i), to = targetPath.get(i + 1);
				g.setColor(ghostColor);
				g.setStroke(solid);
				g.drawLine(from.centerX(), from.centerY(), to.centerX(), to.centerY());
				if (i + 1 == targetPath.size() - 1) {
					drawArrowHead(g, maze().direction(from, to).get(), to.centerX(), to.centerY());
				}
			}
		} else if (ghost.wishDir() != null) {
			// draw direction indicator
			Direction nextDir = ghost.wishDir();
			int x = ghost.tf.getCenter().roundedX(), y = ghost.tf.getCenter().roundedY();
			g.setColor(ghostColor);
			Vector2f dirVector = nextDir.vector();
			drawArrowHead(g, nextDir, x + dirVector.roundedX() * Tile.SIZE, y + dirVector.roundedY() * Tile.SIZE);
		}
		// visualize Inky's chasing (target tile may be null if Blinky is not on stage!)
		if (ghost == cast().inky && ghost.is(CHASING) && ghost.targetTile() != null) {
			{
				int x1 = cast().blinky.tile().centerX(), y1 = cast().blinky.tile().centerY();
				int x2 = ghost.targetTile().centerX(), y2 = ghost.targetTile().centerY();
				g.setColor(Color.GRAY);
				g.drawLine(x1, y1, x2, y2);
			}
			{
				Tile pacManTile = cast().pacMan.tile();
				Direction pacManDir = cast().pacMan.moveDir();
				int s = Tile.SIZE / 2; // size of target square
				g.setColor(Color.GRAY);
				if (settings().overflowBug && pacManDir == Direction.UP) {
					Tile twoAhead = maze().tileToDir(pacManTile, pacManDir, 2);
					Tile twoLeft = maze().tileToDir(twoAhead, Direction.LEFT, 2);
					int x1 = pacManTile.centerX(), y1 = pacManTile.centerY();
					int x2 = twoAhead.centerX(), y2 = twoAhead.centerY();
					int x3 = twoLeft.centerX(), y3 = twoLeft.centerY();
					g.drawLine(x1, y1, x2, y2);
					g.drawLine(x2, y2, x3, y3);
					g.fillRect(x3 - s / 2, y3 - s / 2, s, s);
				} else {
					Tile twoTilesAhead = cast().pacMan.tilesAhead(2);
					int x1 = pacManTile.centerX(), y1 = pacManTile.centerY();
					int x2 = twoTilesAhead.centerX(), y2 = twoTilesAhead.centerY();
					g.drawLine(x1, y1, x2, y2);
					g.fillRect(x2 - s / 2, y2 - s / 2, s, s);
				}
			}
		}
		// draw Clyde's chasing zone
		if (ghost == cast().clyde && ghost.is(CHASING)) {
			int cx = cast().clyde.tile().centerX(), cy = cast().clyde.tile().centerY(), r = 8 * Tile.SIZE;
			g.setColor(new Color(ghostColor.getRed(), ghostColor.getGreen(), ghostColor.getBlue(), 100));
			g.drawOval(cx - r, cy - r, 2 * r, 2 * r);
		}
	}

	private void drawGhostHouseState(Graphics2D g) {
		if (house == null) {
			return; // test scenes may have no ghost house
		}
		drawPacManStarvingTime(g);
		drawDotCounter(g, clydeImage, house.ghostDotCount(cast.clyde), 1, 20,
				!house.isGlobalDotCounterEnabled() && house.isPreferredGhost(cast.clyde));
		drawDotCounter(g, inkyImage, house.ghostDotCount(cast.inky), 24, 20,
				!house.isGlobalDotCounterEnabled() && house.isPreferredGhost(cast.inky));
		drawDotCounter(g, null, house.globalDotCount(), 24, 14, house.isGlobalDotCounterEnabled());
	}

	private void drawDotCounter(Graphics2D g, BufferedImage image, int value, int col, int row,
			boolean emphasized) {
		try (Pen pen = new Pen(g)) {
			if (image != null) {
				g.drawImage(image, col * Tile.SIZE, row * Tile.SIZE, 10, 10, null);
			}
			pen.font(new Font(Font.MONOSPACED, Font.BOLD, 8));
			pen.color(emphasized ? Color.GREEN : Color.WHITE);
			pen.smooth(() -> pen.drawAtTilePosition(col + 2, row, String.format("%d", value)));
		}
	}
}