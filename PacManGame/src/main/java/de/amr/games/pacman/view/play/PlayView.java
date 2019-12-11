package de.amr.games.pacman.view.play;

import static de.amr.games.pacman.model.Maze.NESW;
import static java.lang.Math.round;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Transparency;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import de.amr.easy.game.Application;
import de.amr.easy.game.entity.Entity;
import de.amr.easy.game.input.Keyboard;
import de.amr.easy.game.math.Vector2f;
import de.amr.games.pacman.actor.Bonus;
import de.amr.games.pacman.actor.Ghost;
import de.amr.games.pacman.actor.GhostState;
import de.amr.games.pacman.actor.PacMan;
import de.amr.games.pacman.actor.PacManGameCast;
import de.amr.games.pacman.actor.behavior.common.HeadingForTargetTile;
import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.Tile;
import de.amr.statemachine.State;

/**
 * An extended play view.
 * 
 * <p>
 * Features:
 * <ul>
 * <li>Can display grid and alignment of actors (key 'g')
 * <li>Can display actor state (key 's')
 * <li>Can display actor routes (key 'r')
 * <li>Can switch ghosts on and off (keys 'b', 'p', 'i', 'c')
 * </ul>
 * 
 * @author Armin Reichert
 */
public class PlayView extends SimplePlayView {

	private static final String INFTY = Character.toString('\u221E');

	private final BufferedImage gridImage;

	public boolean showGrid = false;
	public boolean showRoutes = false;
	public boolean showStates = false;

	public Supplier<State<GhostState, ?>> fnGhostAttack = () -> null;

	private static BufferedImage createGridImage(int numRows, int numCols) {
		GraphicsConfiguration conf = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
				.getDefaultConfiguration();
		BufferedImage image = conf.createCompatibleImage(numCols * Maze.TS, numRows * Maze.TS + 1,
				Transparency.TRANSLUCENT);
		Graphics2D g = image.createGraphics();
		g.setColor(new Color(0, 60, 0));
		for (int row = 0; row <= numRows; ++row) {
			g.drawLine(0, row * Maze.TS, numCols * Maze.TS, row * Maze.TS);
		}
		for (int col = 1; col < numCols; ++col) {
			g.drawLine(col * Maze.TS, 0, col * Maze.TS, numRows * Maze.TS);
		}
		return image;
	}

	public PlayView(PacManGameCast cast) {
		super(cast);
		gridImage = createGridImage(Maze.NUM_ROWS, Maze.NUM_COLS);
	}

	@Override
	public void init() {
		super.init();
		updateGhostRouteDisplay();
	}

	@Override
	public void update() {
		if (Keyboard.keyPressedOnce(KeyEvent.VK_G)) {
			showGrid = !showGrid;
		}
		if (Keyboard.keyPressedOnce(KeyEvent.VK_S)) {
			showStates = !showStates;
		}
		if (Keyboard.keyPressedOnce(KeyEvent.VK_R)) {
			showRoutes = !showRoutes;
			updateGhostRouteDisplay();
		}
		if (Keyboard.keyPressedOnce(KeyEvent.VK_B)) {
			toggleGhostActivationState(cast.blinky);
		}
		if (Keyboard.keyPressedOnce(KeyEvent.VK_P)) {
			toggleGhostActivationState(cast.pinky);
		}
		if (Keyboard.keyPressedOnce(KeyEvent.VK_I)) {
			toggleGhostActivationState(cast.inky);
		}
		if (Keyboard.keyPressedOnce(KeyEvent.VK_C)) {
			toggleGhostActivationState(cast.clyde);
		}
		super.update();
	}

	private void updateGhostRouteDisplay() {
		// TODO this is ugly
		cast.ghosts().forEach(ghost -> {
			if (ghost.getSteering() instanceof HeadingForTargetTile<?>) {
				HeadingForTargetTile<?> steering = (HeadingForTargetTile<?>) ghost.getSteering();
				steering.fnComputePath = () -> showRoutes;
			}
		});
	}

	private void toggleGhostActivationState(Ghost ghost) {
		if (cast.isActive(ghost)) {
			cast.deactivate(ghost);
		} else {
			cast.activate(ghost);
		}
	}

	@Override
	public void draw(Graphics2D g) {
		drawMaze(g);
		drawScores(g);
		if (showRoutes) {
			cast.activeGhosts().filter(Ghost::visible).forEach(ghost -> drawRoute(g, ghost));
		}
		drawActors(g);
		if (showGrid) {
			g.drawImage(gridImage, 0, 0, null);
			if (cast.isActive(cast.pacMan)) {
				drawGridAlignment(cast.pacMan, g);
			}
			cast.activeGhosts().filter(Ghost::visible).forEach(ghost -> drawGridAlignment(ghost, g));
		}
		if (showStates) {
			drawActorStates(g);
		}
		drawInfoText(g);
	}

	private void drawActorStates(Graphics2D g) {
		if (cast.pacMan.getState() != null && cast.pacMan.visible()) {
			drawText(g, Color.YELLOW, cast.pacMan.tf.getX(), cast.pacMan.tf.getY(), pacManStateText(cast.pacMan));
		}
		cast.activeGhosts().filter(Ghost::visible).forEach(ghost -> {
			drawText(g, color(ghost), ghost.tf.getX(), ghost.tf.getY(), ghostStateText(ghost));
		});
		cast.bonus().ifPresent(bonus -> {
			drawText(g, Color.YELLOW, bonus.tf.getX(), bonus.tf.getY(), bonusStateText(bonus));
		});
	}

	private String bonusStateText(Bonus bonus) {
		return String.format("%s,%d|%d", bonus, bonus.state().getTicksRemaining(), bonus.state().getDuration());
	}

	private String pacManStateText(PacMan pacMan) {
		String text = pacMan.state().getDuration() != State.ENDLESS
				? String.format("(%s,%d|%d)", pacMan.state().id(), pacMan.state().getTicksRemaining(),
						pacMan.state().getDuration())
				: String.format("(%s,%s)", pacMan.state().id(), INFTY);

		if (Application.app().settings.getAsBoolean("pacMan.immortable")) {
			text += "-immortable";
		}
		return text;
	}

	private String ghostStateText(Ghost ghost) {
		String displayName = ghost.getState() == GhostState.DEAD ? ghost.name() : "";
		String nextState = ghost.nextState != ghost.getState() ? String.format("[->%s]", ghost.nextState) : "";
		int duration = ghost.state().getDuration(), remaining = ghost.state().getTicksRemaining();

		if (ghost.getState() == GhostState.FRIGHTENED && cast.pacMan.hasPower()) {
			duration = cast.pacMan.state().getDuration();
			remaining = cast.pacMan.state().getTicksRemaining();
		} else if (ghost.getState() == GhostState.SCATTERING || ghost.getState() == GhostState.CHASING) {
			State<?, ?> attack = fnGhostAttack.get();
			if (attack != null) {
				duration = attack.getDuration();
				remaining = attack.getTicksRemaining();
			}
		}

		return duration != State.ENDLESS
				? String.format("%s(%s,%d|%d)%s", displayName, ghost.getState(), remaining, duration, nextState)
				: String.format("%s(%s,%s)%s", displayName, ghost.getState(), INFTY, nextState);
	}

	private Color color(Ghost ghost) {
		return ghost == cast.blinky ? Color.RED
				: ghost == cast.pinky ? Color.PINK
						: ghost == cast.inky ? Color.CYAN : ghost == cast.clyde ? Color.ORANGE : Color.WHITE;
	}

	private void drawText(Graphics2D g, Color color, float x, float y, String text) {
		g.translate(x, y);
		g.setColor(color);
		g.setFont(new Font("Arial Narrow", Font.PLAIN, 5));
		int width = g.getFontMetrics().stringWidth(text);
		g.drawString(text, -width / 2, -Maze.TS / 2);
		g.translate(-x, -y);
	}

	private void drawGridAlignment(Entity actor, Graphics2D g) {
		g.setColor(Color.GREEN);
		g.translate(actor.tf.getX(), actor.tf.getY());
		int w = actor.tf.getWidth(), h = actor.tf.getHeight();
		if (round(actor.tf.getY()) % Maze.TS == 0) {
			g.drawLine(0, 0, w, 0);
			g.drawLine(0, h, w, h);
		}
		if (round(actor.tf.getX()) % Maze.TS == 0) {
			g.drawLine(0, 0, 0, h);
			g.drawLine(w, 0, w, h);
		}
		g.translate(-actor.tf.getX(), -actor.tf.getY());
	}

	private void drawRoute(Graphics2D g, Ghost ghost) {
		Color ghostColor = color(ghost);
		Stroke solid = g.getStroke();
		if (ghost.targetTile() != null) {
			// draw target tile indicator
			Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 3 }, 0);
			g.setStroke(dashed);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(ghostColor);
			g.drawLine((int) ghost.tf.getCenter().x, (int) ghost.tf.getCenter().y,
					ghost.targetTile().col * Maze.TS + Maze.TS / 2, ghost.targetTile().row * Maze.TS + Maze.TS / 2);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g.setStroke(solid);
			g.translate(ghost.targetTile().col * Maze.TS, ghost.targetTile().row * Maze.TS);
			g.setColor(ghostColor);
			g.fillRect(Maze.TS / 4, Maze.TS / 4, Maze.TS / 2, Maze.TS / 2);
			g.translate(-ghost.targetTile().col * Maze.TS, -ghost.targetTile().row * Maze.TS);
		}
		if (ghost.targetPath().size() > 1) {
			// draw path in ghost's color
			g.setColor(new Color(ghostColor.getRed(), ghostColor.getGreen(), ghostColor.getBlue(), 60));
			for (Tile tile : ghost.targetPath()) {
				g.fillRect(tile.col * Maze.TS, tile.row * Maze.TS, Maze.TS, Maze.TS);
			}
		} else {
			// draw direction indicator
			Vector2f center = ghost.tf.getCenter();
			int dx = NESW.dx(ghost.nextDir()), dy = NESW.dy(ghost.nextDir());
			int r = Maze.TS / 4;
			int lineLen = Maze.TS;
			int indX = (int) (center.x + dx * lineLen);
			int indY = (int) (center.y + dy * lineLen);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(ghostColor);
			g.fillOval(indX - r, indY - r, 2 * r, 2 * r);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		}
		// draw Clyde's chasing zone
		if (ghost == cast.clyde && ghost.getState() == GhostState.CHASING && cast.clyde.targetTile() != null) {
			Vector2f center = cast.clyde.tf.getCenter();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(new Color(ghostColor.getRed(), ghostColor.getGreen(), ghostColor.getBlue(), 100));
			g.drawOval((int) center.x - 8 * Maze.TS, (int) center.y - 8 * Maze.TS, 16 * Maze.TS, 16 * Maze.TS);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		}
	}
}