package de.amr.games.pacman.view.intro;

import static de.amr.easy.game.Application.app;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import de.amr.easy.game.Application;
import de.amr.easy.game.entity.Entity;
import de.amr.easy.game.math.Vector2f;
import de.amr.easy.game.ui.sprites.Sprite;
import de.amr.easy.game.view.AnimationController;
import de.amr.easy.game.view.View;
import de.amr.easy.grid.impl.Top4;
import de.amr.games.pacman.theme.GhostColor;
import de.amr.games.pacman.theme.PacManTheme;

public class ChasePacManAnimation extends Entity implements AnimationController, View {

	private final Sprite pacMan;
	private final Sprite ghosts[] = new Sprite[4];
	private int pillTimer;
	private Vector2f startPosition;
	private Vector2f endPosition;
	private boolean pill;

	public ChasePacManAnimation() {
		pacMan = getTheme().spr_pacManWalking(Top4.W);
		ghosts[0] = getTheme().spr_ghostColored(GhostColor.RED, Top4.W);
		ghosts[1] = getTheme().spr_ghostColored(GhostColor.PINK, Top4.W);
		ghosts[2] = getTheme().spr_ghostColored(GhostColor.TURQUOISE, Top4.W);
		ghosts[3] = getTheme().spr_ghostColored(GhostColor.ORANGE, Top4.W);
		pill = true;
		tf.setWidth(88);
		tf.setHeight(16);
	}

	private PacManTheme getTheme() {
		return Application.app().settings.get("theme");
	}

	public void setStartPosition(float x, float y) {
		this.startPosition = Vector2f.of(x, y);
	}

	public void setEndPosition(float x, float y) {
		this.endPosition = Vector2f.of(x, y);
	}

	@Override
	public void init() {
		tf.setPosition(startPosition);
		pillTimer = app().clock.sec(0.5f);
	}

	@Override
	public void update() {
		tf.move();
		if (pillTimer > 0) {
			--pillTimer;
		}
		if (pillTimer == 0) {
			pill = !pill;
			pillTimer = app().clock.sec(0.5f);
		}
	}

	@Override
	public void startAnimation() {
		init();
		tf.setVelocityX(-0.8f);
		getTheme().snd_ghost_chase().loop();
	}

	@Override
	public void stopAnimation() {
		tf.setVelocityX(0);
		getTheme().snd_ghost_chase().stop();
	}

	@Override
	public boolean isAnimationCompleted() {
		return tf.getX() < endPosition.x;
	}

	@Override
	public void draw(Graphics2D g) {
		int x = 0;
		g.translate(tf.getX(), tf.getY());
		g.setColor(Color.PINK);
		if (pill) {
			g.fillRect(6, 6, 2, 2);
		} else {
			g.setFont(new Font("Arial", Font.BOLD, 8));
			g.drawString("10", 0, 10);
		}
		x = 10;
		g.translate(x, 0);
		pacMan.draw(g);
		g.translate(-x, 0);
		for (int i = 0; i < ghosts.length; ++i) {
			x += 16;
			g.translate(x, 0);
			ghosts[i].draw(g);
			g.translate(-x, 0);
		}
		g.translate(-tf.getX(), -tf.getY());
	}
}