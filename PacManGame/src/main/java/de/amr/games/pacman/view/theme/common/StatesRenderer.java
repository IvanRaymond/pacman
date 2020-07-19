package de.amr.games.pacman.view.theme.common;

import static de.amr.games.pacman.PacManApp.settings;
import static de.amr.games.pacman.controller.creatures.ghost.GhostState.CHASING;
import static de.amr.games.pacman.controller.creatures.ghost.GhostState.DEAD;
import static de.amr.games.pacman.controller.creatures.ghost.GhostState.ENTERING_HOUSE;
import static de.amr.games.pacman.controller.creatures.ghost.GhostState.FRIGHTENED;
import static de.amr.games.pacman.controller.creatures.ghost.GhostState.SCATTERING;
import static de.amr.games.pacman.view.theme.common.Rendering.ghostColor;
import static java.lang.Math.round;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;

import de.amr.easy.game.entity.Entity;
import de.amr.easy.game.view.Pen;
import de.amr.games.pacman.controller.creatures.api.Creature;
import de.amr.games.pacman.controller.creatures.ghost.Ghost;
import de.amr.games.pacman.controller.creatures.pacman.PacMan;
import de.amr.games.pacman.controller.game.GhostCommand;
import de.amr.games.pacman.controller.world.arcade.Folks;
import de.amr.games.pacman.model.world.api.Direction;
import de.amr.games.pacman.model.world.api.Tile;
import de.amr.games.pacman.model.world.api.World;
import de.amr.games.pacman.view.theme.api.IRenderer;

public class StatesRenderer implements IRenderer {

	private static final Font SMALL_FONT = new Font("Arial", Font.PLAIN, 6);

	private final World world;
	private final Folks folks;
	private final GhostCommand ghostCommand;

	public StatesRenderer(World world, Folks folks, GhostCommand ghostCommand) {
		this.world = world;
		this.folks = folks;
		this.ghostCommand = ghostCommand;
	}

	@Override
	public void render(Graphics2D g) {
		drawActorStates(g);
		drawActorsOffTrack(g);
	}

	private void drawActorStates(Graphics2D g) {
		folks.ghosts().filter(world::contains).forEach(ghost -> drawGhostState(g, ghost, ghostCommand));
		drawPacManState(g, folks.pacMan);
	}

	private void drawPacManState(Graphics2D g, PacMan pacMan) {
		if (!pacMan.isVisible() || pacMan.getState() == null) {
			return;
		}
		String text = pacMan.getPower() > 0 ? String.format("POWER(%d)", pacMan.getPower()) : pacMan.getState().name();
		if (settings.pacManImmortable) {
			text += " immortable";
		}
		drawEntityState(g, pacMan.entity, text, Color.YELLOW);
	}

	private void drawEntityState(Graphics2D g, Entity entity, String text, Color color) {
		try (Pen pen = new Pen(g)) {
			pen.color(color);
			pen.font(SMALL_FONT);
			pen.drawCentered(text, entity.tf.getCenter().x, entity.tf.getCenter().y - 2);
		}
	}

	private void drawGhostState(Graphics2D g, Ghost ghost, GhostCommand ghostCommand) {
		if (!ghost.isVisible()) {
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
		if (ghostCommand != null && ghost.is(SCATTERING, CHASING)) {
			if (ghostCommand.state() != null) {
				duration = ghostCommand.state().getDuration();
				remaining = ghostCommand.state().getTicksRemaining();
			}
		}
		if (duration != Integer.MAX_VALUE) {
			text.append(String.format("(%s,%d|%d)", ghost.getState(), remaining, duration));
		} else {
			text.append(String.format("(%s,%s)", ghost.getState(), Rendering.INFTY));
		}
		drawEntityState(g, ghost.entity, text.toString(), ghostColor(ghost));
	}

	private void drawActorsOffTrack(Graphics2D g) {
		drawActorOffTrack(g, folks.pacMan);
		folks.ghosts().forEach(ghost -> drawActorOffTrack(g, ghost));
	}

	private void drawActorOffTrack(Graphics2D g, Creature<?> creature) {
		if (!creature.isVisible()) {
			return;
		}
		Stroke normal = g.getStroke();
		Stroke fine = new BasicStroke(0.2f);
		g.setStroke(fine);
		g.setColor(Color.RED);
		g.translate(creature.entity.tf.x, creature.entity.tf.y);
		int w = creature.entity.tf.width, h = creature.entity.tf.height;
		Direction moveDir = creature.moveDir();
		if ((moveDir == Direction.LEFT || moveDir == Direction.RIGHT) && round(creature.entity.tf.y) % Tile.SIZE != 0) {
			g.drawLine(0, 0, w, 0);
			g.drawLine(0, h, w, h);
		}
		if ((moveDir == Direction.UP || moveDir == Direction.DOWN) && round(creature.entity.tf.x) % Tile.SIZE != 0) {
			g.drawLine(0, 0, 0, h);
			g.drawLine(w, 0, w, h);
		}
		g.translate(-creature.entity.tf.x, -creature.entity.tf.y);
		g.setStroke(normal);
	}
}