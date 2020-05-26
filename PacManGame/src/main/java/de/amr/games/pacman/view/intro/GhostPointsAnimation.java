package de.amr.games.pacman.view.intro;

import static de.amr.games.pacman.model.Game.sec;
import static de.amr.graph.grid.impl.Grid4Topology.E;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.BitSet;

import de.amr.easy.game.entity.GameObject;
import de.amr.easy.game.ui.sprites.Sprite;
import de.amr.games.pacman.theme.Theme;

/**
 * An animation showing Pac-Man and the four ghosts frightened and showing the points scored for the
 * ghosts.
 * 
 * @author Armin Reichert
 */
public class GhostPointsAnimation extends GameObject {

	private final Theme theme;
	private final Sprite pacMan;
	private final Sprite ghost;
	private final Sprite[] points = new Sprite[4];
	private final BitSet killed = new BitSet(5);
	private int killNext = 0;
	private int ghostTimer;
	private int energizerTimer;
	private boolean energizer;

	public GhostPointsAnimation(Theme theme) {
		this.theme = theme;
		pacMan = theme.spr_pacManWalking(E);
		ghost = theme.spr_ghostFrightened();
		int i = 0;
		for (int number : new int[] { 200, 400, 800, 1600 }) {
			points[i++] = theme.spr_number(number);
		}
		ghostTimer = -1;
		tf.width = (90);
		tf.height = (18);
	}

	private void resetGhostTimer() {
		ghostTimer = sec(1);
	}

	private void resetEnergizerTimer() {
		energizerTimer = sec(0.5f);
	}

	@Override
	public void init() {
		killed.clear();
		killNext = 0;
		energizer = true;
	}

	@Override
	public void start() {
		init();
		resetGhostTimer();
		resetEnergizerTimer();
	}

	@Override
	public void stop() {
		ghostTimer = -1;
	}

	@Override
	public boolean isComplete() {
		return false;
	}

	@Override
	public void update() {
		if (ghostTimer > 0) {
			ghostTimer -= 1;
		}
		if (ghostTimer == 0) {
			killed.set(killNext);
			killNext = killNext + 1;
			if (killed.cardinality() == 5) {
				stop();
			} else {
				theme.snd_eatGhost().play();
				resetGhostTimer();
			}
		}
		if (energizerTimer > 0) {
			energizerTimer -= 1;
		}
		if (energizerTimer == 0) {
			energizer = !energizer;
			resetEnergizerTimer();
		}
	}

	@Override
	public void draw(Graphics2D g) {
		g.translate(tf.x, tf.y);
		int x = 0;
		pacMan.draw(g);
		x += 12;
		g.translate(x, 0);
		if (energizer) {
			g.setColor(Color.PINK);
			g.fillOval(4, 4, 8, 8);
		} else {
			g.setColor(Color.PINK);
			g.setFont(new Font("Arial", Font.BOLD, 8));
			g.drawString("50", 4, 12);
		}
		g.translate(-x, 0);
		for (int i = 0; i < 4; ++i) {
			x += 18;
			g.translate(x, 0);
			if (killed.get(i)) {
				points[i].draw(g);
			} else {
				ghost.draw(g);
			}
			g.translate(-x, 0);
		}
		g.translate(-tf.x, -tf.y);
	}
}