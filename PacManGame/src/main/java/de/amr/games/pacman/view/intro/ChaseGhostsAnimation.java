package de.amr.games.pacman.view.intro;

import static de.amr.games.pacman.model.Direction.RIGHT;

import java.awt.Graphics2D;

import de.amr.easy.game.entity.GameObject;
import de.amr.easy.game.math.Vector2f;
import de.amr.easy.game.ui.sprites.Sprite;
import de.amr.games.pacman.controller.sound.PacManSoundManager;
import de.amr.games.pacman.view.render.sprite.ArcadeSprites;

/**
 * An animation showing Pac-Man chasing the ghosts and scoring points for each killed ghost.
 * 
 * @author Armin Reichert
 */
public class ChaseGhostsAnimation extends GameObject {

	private final PacManSoundManager soundManager;
	private final Sprite pacMan;
	private final Sprite ghost;
	private final Sprite[] points = new Sprite[4];
	private Vector2f startPosition;
	private Vector2f endPosition;
	private final boolean[] killed = new boolean[4];
	private float pacManX;
	private int ghostsKilled;

	public ChaseGhostsAnimation(ArcadeSprites theme, PacManSoundManager soundManager) {
		this.soundManager = soundManager;
		pacMan = theme.spr_pacManWalking(RIGHT);
		ghost = theme.spr_ghostFrightened();
		int i = 0;
		for (int number : new int[] { 200, 400, 800, 1600 }) {
			points[i++] = theme.spr_number(number);
		}
		tf.width = (5 * 18);
		tf.height = (18);
	}

	public void setStartPosition(float x, float y) {
		this.startPosition = Vector2f.of(x, y);
	}

	public void setEndPosition(float x, float y) {
		this.endPosition = Vector2f.of(x, y);
	}

	@Override
	public void init() {
		for (int i = 0; i < 4; i++) {
			killed[i] = false;
		}
		pacManX = 0;
		ghostsKilled = 0;
		tf.setPosition(startPosition);
	}

	@Override
	public void start() {
		init();
		tf.vx = .8f;
		soundManager.snd_eatPill().loop();
	}

	@Override
	public void stop() {
		tf.vx = 0;
		soundManager.snd_eatPill().stop();
	}

	@Override
	public boolean isComplete() {
		return tf.x > endPosition.x;
	}

	@Override
	public void update() {
		if (tf.vx > 0) {
			tf.move();
			if (tf.x + tf.width < 0) {
				return;
			}
			pacManX += 0.3f;
			if (ghostsKilled < 4) {
				int x = (int) pacManX + 4;
				if (x % 16 == 0) {
					pacManX += 1;
					killed[ghostsKilled] = true;
					++ghostsKilled;
				}
			}
		}
	}

	@Override
	public void draw(Graphics2D g) {
		g = (Graphics2D) g.create();
		g.translate(tf.x, tf.y);
		for (int i = 0; i < 4; ++i) {
			g.translate(18 * (i + 1), 0);
			if (killed[i]) {
				points[i].draw(g);
			} else {
				ghost.draw(g);
			}
			g.translate(-18 * (i + 1), 0);
		}
		g.translate(pacManX, 0);
		pacMan.draw(g);
		g.translate(-pacManX, 0);
		g.translate(-tf.x, -tf.y);
		g.dispose();
	}
}