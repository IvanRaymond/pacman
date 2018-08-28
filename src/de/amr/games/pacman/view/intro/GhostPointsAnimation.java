package de.amr.games.pacman.view.intro;

import static de.amr.easy.game.Application.PULSE;
import static de.amr.games.pacman.theme.PacManThemes.THEME;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.BitSet;

import de.amr.easy.game.entity.GameEntity;
import de.amr.easy.game.sprite.Sprite;
import de.amr.easy.grid.impl.Top4;

/**
 * An animation showing Pac-Man and the four ghosts frightened and showing the points scored for the
 * ghosts.
 * 
 * @author Armin Reichert
 */
public class GhostPointsAnimation extends GameEntity {

	private final Sprite pacMan;
	private final Sprite ghost;
	private final Sprite[] points = new Sprite[4];
	private final BitSet killed = new BitSet(5);
	private int killNext = 0;
	private int ghostTimer;
	private int energizerTimer;
	private boolean energizer;

	public GhostPointsAnimation() {
		pacMan = THEME.pacManWalking(Top4.E);
		ghost = THEME.ghostFrightened();
		for (int i = 0; i < 4; ++i) {
			points[i] = THEME.greenNumber(i);
		}
		ghostTimer = -1;
	}

	private void resetGhostTimer() {
		ghostTimer = PULSE.secToTicks(1);
	}

	private void resetEnergizerTimer() {
		energizerTimer = PULSE.secToTicks(0.5f);
		;
	}

	@Override
	public void init() {
		killed.clear();
		killNext = 0;
		energizer = true;
	}

	public void start() {
		init();
		resetGhostTimer();
		resetEnergizerTimer();
	}

	public void stop() {
		ghostTimer = -1;
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
				THEME.soundEatGhost().play();
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

	public boolean isComplete() {
		return false;
	}

	@Override
	public void draw(Graphics2D g) {
		g.translate(tf.getX(), tf.getY());
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
		g.translate(-tf.getX(), -tf.getY());
	}

	@Override
	public int getWidth() {
		return 5 * 18;
	}

	@Override
	public int getHeight() {
		return 18;
	}
}