package de.amr.games.pacman.view.intro;

import java.awt.Graphics2D;
import java.util.stream.Stream;

import de.amr.easy.game.entity.GameEntity;
import de.amr.easy.game.sprite.Sprite;
import de.amr.easy.grid.impl.Top4;
import de.amr.games.pacman.theme.GhostColor;
import de.amr.games.pacman.theme.PacManTheme;

public class GhostsChasingPacMan extends GameEntity {

	private final Sprite pacMan;
	private final Sprite ghosts[] = new Sprite[4];

	public GhostsChasingPacMan() {
		pacMan = PacManTheme.ASSETS.pacManWalking(Top4.W);
		ghosts[0] = PacManTheme.ASSETS.ghostColored(GhostColor.RED, Top4.W);
		ghosts[1] = PacManTheme.ASSETS.ghostColored(GhostColor.PINK, Top4.W);
		ghosts[2] = PacManTheme.ASSETS.ghostColored(GhostColor.TURQUOISE, Top4.W);
		ghosts[3] = PacManTheme.ASSETS.ghostColored(GhostColor.ORANGE, Top4.W);
	}

	public void start() {
		init();
		tf.setVelocityX(-1f);
	}

	public void stop() {
		init();
		tf.setVelocityX(0);
	}

	public boolean isComplete() {
		return tf.getX() < -80;
	}

	@Override
	public void draw(Graphics2D g) {
		g.translate(tf.getX(), tf.getY());
		pacMan.draw(g);
		for (int i = 0; i < ghosts.length; ++i) {
			g.translate(16 * (i + 1), 0);
			ghosts[i].draw(g);
			g.translate(-16 * (i + 1), 0);
		}
		g.translate(-tf.getX(), -tf.getY());
	}

	@Override
	public void init() {
		tf.moveTo(288, 100);
	}

	@Override
	public void update() {
		tf.move();
	}

	@Override
	public Sprite currentSprite() {
		return null;
	}

	@Override
	public Stream<Sprite> getSprites() {
		return Stream.empty();
	}
}