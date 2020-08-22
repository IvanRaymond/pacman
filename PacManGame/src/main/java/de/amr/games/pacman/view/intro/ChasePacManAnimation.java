package de.amr.games.pacman.view.intro;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import de.amr.easy.game.entity.GameObject;
import de.amr.games.pacman.controller.creatures.Folks;
import de.amr.games.pacman.controller.creatures.SmartGuy;
import de.amr.games.pacman.controller.creatures.ghost.Ghost;
import de.amr.games.pacman.controller.creatures.ghost.GhostState;
import de.amr.games.pacman.controller.creatures.pacman.PacManState;
import de.amr.games.pacman.controller.game.Timing;
import de.amr.games.pacman.model.world.api.Direction;
import de.amr.games.pacman.model.world.arcade.ArcadeWorld;
import de.amr.games.pacman.model.world.components.Tile;
import de.amr.games.pacman.view.api.Theme;

public class ChasePacManAnimation extends GameObject {

	enum PelletDisplay {
		SIMPLE, TEN, ENERGIZER, FIFTY
	}

	private final ArcadeWorld world;
	private final Folks folks;
	private Theme theme;
	private long pelletTimer;
	private PelletDisplay pelletDisplay;

	public ChasePacManAnimation(Theme theme, ArcadeWorld world) {
		this.world = world;
		folks = new Folks(world, world.house(0));
		setTheme(theme);
	}

	public Folks getFolks() {
		return folks;
	}

	public void setTheme(Theme theme) {
		this.theme = theme;
	}

	@Override
	public void init() {
		pelletTimer = Timing.sec(6 * 0.5f);
		pelletDisplay = PelletDisplay.SIMPLE;

		folks.guys().forEach(SmartGuy::init);

		folks.pacMan.body.tf.vx = -0.55f;
		folks.pacMan.body.moveDir = Direction.LEFT;
		folks.pacMan.ai.setState(PacManState.AWAKE);

		folks.ghosts().forEach(ghost -> {
			ghost.body.moveDir = Direction.LEFT;
			ghost.body.tf.setVelocity(-0.55f, 0);
			ghost.ai.setState(GhostState.CHASING);
			ghost.ai.state(GhostState.CHASING).removeTimer();
		});

		initPositions(world.width() * Tile.SIZE);
	}

	public void initPositions(int rightBorder) {
		int size = 2 * Tile.SIZE;
		int x = rightBorder;
		Ghost[] ghosts = folks.ghosts().toArray(Ghost[]::new);
		for (int i = 0; i < ghosts.length; ++i) {
			ghosts[i].body.tf.setPosition(x, tf.y);
			x -= size;
		}
		folks.pacMan.body.tf.setPosition(x, tf.y);
	}

	@Override
	public void update() {
		folks.guys().forEach(c -> c.body.tf.move());
		if (pelletTimer > 0) {
			if (pelletTimer % Timing.sec(0.5f) == 0)
				if (pelletDisplay == PelletDisplay.FIFTY) {
					pelletDisplay = PelletDisplay.SIMPLE;
				} else {
					pelletDisplay = PelletDisplay.values()[pelletDisplay.ordinal() + 1]; // succ
				}
			pelletTimer--;
		} else {
			pelletTimer = Timing.sec(6 * 0.5f);
		}
	}

	@Override
	public void start() {
		init();
		theme.sounds().clipGhostChase().loop();
		if (!theme.sounds().clipCrunching().isRunning()) {
			theme.sounds().clipCrunching().loop();
		}
	}

	@Override
	public void stop() {
		theme.sounds().clipGhostChase().stop();
		theme.sounds().clipCrunching().stop();
		folks.guys().forEach(creature -> creature.body.tf.vx = 0);
	}

	@Override
	public boolean isComplete() {
		return folks.guys().map(creature -> creature.body.tf.x / Tile.SIZE).allMatch(x -> x > world.width() || x < -2);
	}

	@Override
	public void draw(Graphics2D g) {
		theme.pacManRenderer(folks.pacMan).render(g, folks.pacMan);
		folks.ghosts().forEach(ghost -> {
			theme.ghostRenderer(ghost).render(g, ghost);
		});
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int x = (int) folks.pacMan.body.tf.x - Tile.SIZE;
		int y = (int) folks.pacMan.body.tf.y;
		switch (pelletDisplay) {
		case SIMPLE:
			g.setColor(Color.PINK);
			g.fillRect(x, y + 4, 2, 2);
			break;
		case TEN:
			g.setFont(new Font("Arial", Font.BOLD, 8));
			g.setColor(Color.PINK);
			g.drawString("10", x - 6, y + 6);
			break;
		case ENERGIZER:
			g.setColor(Color.PINK);
			g.fillOval(x - 4, y, 8, 8);
			break;
		case FIFTY:
			g.setFont(new Font("Arial", Font.BOLD, 8));
			g.setColor(Color.PINK);
			g.drawString("50", x - 6, y + 6);
			break;
		default:
		}
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
	}
}