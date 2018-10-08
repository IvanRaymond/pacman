package de.amr.games.pacman.view.intro;

import static de.amr.easy.game.Application.app;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.BitSet;

import de.amr.easy.game.Application;
import de.amr.easy.game.entity.Entity;
import de.amr.easy.game.ui.sprites.Sprite;
import de.amr.easy.game.view.AnimationController;
import de.amr.easy.game.view.View;
import de.amr.easy.grid.impl.Top4;
import de.amr.games.pacman.theme.PacManTheme;

/**
 * An animation showing Pac-Man and the four ghosts frightened and showing the points scored for the
 * ghosts.
 * 
 * @author Armin Reichert
 */
public class GhostPointsAnimation extends Entity implements AnimationController, View {

	private final Sprite pacMan;
	private final Sprite ghost;
	private final Sprite[] points = new Sprite[4];
	private final BitSet killed = new BitSet(5);
	private int killNext = 0;
	private int ghostTimer;
	private int energizerTimer;
	private boolean energizer;

	public GhostPointsAnimation() {
		pacMan = getTheme().spr_pacManWalking(Top4.E);
		ghost = getTheme().spr_ghostFrightened();
		for (int i = 0; i < 4; ++i) {
			points[i] = getTheme().spr_greenNumber(i);
		}
		ghostTimer = -1;
		tf.setWidth(90);
		tf.setHeight(18);
	}

	private PacManTheme getTheme() {
		return Application.app().settings.get("theme");
	}

	private void resetGhostTimer() {
		ghostTimer = app().clock.sec(1);
	}

	private void resetEnergizerTimer() {
		energizerTimer = app().clock.sec(0.5f);
	}

	@Override
	public void init() {
		killed.clear();
		killNext = 0;
		energizer = true;
	}

	@Override
	public void startAnimation() {
		init();
		resetGhostTimer();
		resetEnergizerTimer();
	}

	@Override
	public void stopAnimation() {
		ghostTimer = -1;
	}

	@Override
	public boolean isAnimationCompleted() {
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
				stopAnimation();
			} else {
				getTheme().snd_eatGhost().play();
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
}