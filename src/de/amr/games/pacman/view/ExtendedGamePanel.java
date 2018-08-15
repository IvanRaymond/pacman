package de.amr.games.pacman.view;

import static de.amr.easy.game.Application.LOGGER;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Level;

import de.amr.easy.game.input.Keyboard;
import de.amr.easy.game.view.View;
import de.amr.games.pacman.actor.core.MazeMover;
import de.amr.games.pacman.actor.game.Cast;
import de.amr.games.pacman.actor.game.Ghost;
import de.amr.games.pacman.actor.game.PacMan;
import de.amr.games.pacman.controller.event.game.GhostKilledEvent;
import de.amr.games.pacman.model.Content;
import de.amr.games.pacman.model.Game;
import de.amr.games.pacman.model.Tile;
import de.amr.statemachine.StateObject;

/**
 * Game panel subclass displaying internal info (states, routes etc.).
 * 
 * @author Armin Reichert
 */
public class ExtendedGamePanel extends GamePanel {

	private static BufferedImage createGridImage(int numRows, int numCols) {
		GraphicsConfiguration conf = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
				.getDefaultConfiguration();
		BufferedImage image = conf.createCompatibleImage(numCols * Game.TS, numRows * Game.TS + 1,
				Transparency.TRANSLUCENT);
		Graphics2D g = image.createGraphics();
		g.setColor(Color.DARK_GRAY);
		for (int row = 0; row <= numRows; ++row) {
			g.drawLine(0, row * Game.TS, numCols * Game.TS, row * Game.TS);
		}
		for (int col = 1; col < numCols; ++col) {
			g.drawLine(col * Game.TS, 0, col * Game.TS, numRows * Game.TS);
		}
		return image;
	}

	private static final String INFTY = Character.toString('\u221E');

	private final BufferedImage gridImage;
	private boolean showGrid;
	private boolean showRoutes;
	private boolean showStates;

	public ExtendedGamePanel(int width, int height, Game game, Cast actors) {
		super(width, height, game, actors);
		gridImage = createGridImage(game.maze.numRows(), game.maze.numCols());
	}

	@Override
	public View currentView() {
		return this;
	}

	@Override
	public void update() {
		if (Keyboard.keyPressedOnce(KeyEvent.VK_L)) {
			LOGGER.setLevel(LOGGER.getLevel() == Level.OFF ? Level.INFO : Level.OFF);
		}
		if (Keyboard.keyPressedOnce(KeyEvent.VK_G)) {
			showGrid = !showGrid;
		}
		if (Keyboard.keyPressedOnce(KeyEvent.VK_S)) {
			showStates = !showStates;
		}
		if (Keyboard.keyPressedOnce(KeyEvent.VK_R)) {
			showRoutes = !showRoutes;
		}
		if (Keyboard.keyPressedOnce(KeyEvent.VK_K)) {
			killActiveGhosts();
		}
		if (Keyboard.keyPressedOnce(KeyEvent.VK_E)) {
			eatPellets();
		}
		if (Keyboard.keyPressedOnce(KeyEvent.VK_B)) {
			toggleGhostActivity(actors.getBlinky());
		}
		if (Keyboard.keyPressedOnce(KeyEvent.VK_P)) {
			toggleGhostActivity(actors.getPinky());
		}
		if (Keyboard.keyPressedOnce(KeyEvent.VK_I)) {
			toggleGhostActivity(actors.getInky());
		}
		if (Keyboard.keyPressedOnce(KeyEvent.VK_C)) {
			toggleGhostActivity(actors.getClyde());
		}
		super.update();
	}

	private void killActiveGhosts() {
		actors.getActiveGhosts().forEach(ghost -> ghost.processEvent(new GhostKilledEvent(ghost)));
	}

	private void eatPellets() {
		game.maze.tiles().filter(tile -> game.maze.getContent(tile) == Content.PELLET).forEach(tile -> {
			game.maze.setContent(tile, Content.EATEN);
			game.foodEaten += 1;
			game.score.add(game.getFoodValue(Content.PELLET));
		});
	}

	@Override
	public void draw(Graphics2D g) {
		super.draw(g);
		if (showGrid) {
			drawGrid(g);
			drawActorPosition(actors.getPacMan(), g);
			actors.getActiveGhosts().forEach(ghost -> drawActorPosition(ghost, g));
		}
		if (showRoutes) {
			actors.getActiveGhosts().forEach(ghost -> drawRoute(g, ghost));
		}
		if (showStates) {
			drawEntityStates(g);
		}
	}

	private void drawGrid(Graphics2D g) {
		g.translate(mazePanel.tf.getX(), mazePanel.tf.getY());
		g.drawImage(gridImage, 0, 0, null);
		g.translate(-mazePanel.tf.getX(), -mazePanel.tf.getY());
	}

	private void drawEntityStates(Graphics2D g) {
		g.translate(mazePanel.tf.getX(), mazePanel.tf.getY());
		PacMan pacMan = actors.getPacMan();
		drawText(g, Color.YELLOW, pacMan.tf.getX(), pacMan.tf.getY(), pacManState(pacMan));
		actors.getActiveGhosts().filter(Ghost::isVisible).forEach(ghost -> {
			drawText(g, ghostColor(ghost), ghost.tf.getX() - Game.TS, ghost.tf.getY(), ghostState(ghost));
		});
		g.translate(-mazePanel.tf.getX(), -mazePanel.tf.getY());
	}

	private String pacManState(PacMan pacMan) {
		StateObject<?, ?> state = pacMan.getStateMachine().currentStateObject();
		return state.getDuration() != StateObject.ENDLESS
				? String.format("(%s,%d|%d)", state.id(), state.getRemaining(), state.getDuration())
				: String.format("(%s,%s)", state.id(), INFTY);
	}

	private String ghostState(Ghost ghost) {
		StateObject<?, ?> state = ghost.getStateMachine().currentStateObject();
		return state.getDuration() != StateObject.ENDLESS
				? String.format("%s(%s,%d|%d)", ghost.getName(), state.id(), state.getRemaining(), state.getDuration())
				: String.format("%s(%s,%s)", ghost.getName(), state.id(), INFTY);
	}

	private void toggleGhostActivity(Ghost ghost) {
		actors.setActive(ghost, !actors.isActive(ghost));
	}

	private static Color ghostColor(Ghost ghost) {
		switch (ghost.getName()) {
		case Blinky:
			return Color.RED;
		case Pinky:
			return Color.PINK;
		case Inky:
			return new Color(64, 224, 208);
		case Clyde:
			return Color.ORANGE;
		default:
			throw new IllegalArgumentException();
		}
	}

	private void drawText(Graphics2D g, Color color, float x, float y, String text) {
		g.translate(x, y);
		g.setColor(color);
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Game.TS / 2));
		g.drawString(text, 0, -Game.TS / 2);
		g.translate(-x, -y);
	}

	private void drawActorPosition(MazeMover actor, Graphics2D g) {
		g.setColor(Color.GREEN);
		g.translate(mazePanel.tf.getX(), mazePanel.tf.getY());
		g.translate(actor.tf.getX(), actor.tf.getY());
		int w = actor.getWidth(), h = actor.getHeight();
		if (actor.isAlignedY()) {
			g.drawLine(0, 0, w, 0);
			g.drawLine(0, h, w, h);
		}
		if (actor.isAlignedX()) {
			g.drawLine(0, 0, 0, h);
			g.drawLine(w, 0, w, h);
		}
		g.translate(-actor.tf.getX(), -actor.tf.getY());
		g.translate(-mazePanel.tf.getX(), -mazePanel.tf.getY());
	}

	private void drawRoute(Graphics2D g, Ghost ghost) {
		List<Tile> path = ghost.getNavigation().computeRoute(ghost).path;
		if (path.size() > 1) {
			g.setColor(ghostColor(ghost));
			g.translate(mazePanel.tf.getX(), mazePanel.tf.getY());
			for (int i = 0; i < path.size() - 1; ++i) {
				Tile u = path.get(i), v = path.get(i + 1);
				int u1 = u.col * Game.TS + Game.TS / 2;
				int u2 = u.row * Game.TS + Game.TS / 2;
				int v1 = v.col * Game.TS + Game.TS / 2;
				int v2 = v.row * Game.TS + Game.TS / 2;
				g.drawLine(u1, u2, v1, v2);
			}
			// Target tile
			Tile tile = path.get(path.size() - 1);
			g.translate(tile.col * Game.TS, tile.row * Game.TS);
			g.fillRect(Game.TS / 4, Game.TS / 4, Game.TS / 2, Game.TS / 2);
			g.translate(-tile.col * Game.TS, -tile.row * Game.TS);
			g.translate(-mazePanel.tf.getX(), -mazePanel.tf.getY());
		}
	}
}