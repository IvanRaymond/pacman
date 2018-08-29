package de.amr.games.pacman.view.play;

import static de.amr.games.pacman.model.Game.TS;
import static de.amr.games.pacman.theme.PacManThemes.THEME;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Optional;

import de.amr.easy.game.entity.GameEntityUsingSprites;
import de.amr.easy.game.sprite.Animation;
import de.amr.easy.game.sprite.CyclicAnimation;
import de.amr.games.pacman.actor.Bonus;
import de.amr.games.pacman.model.Maze;

public class MazeView extends GameEntityUsingSprites {

	private final Maze maze;
	private final Animation energizerBlinking;
	private boolean flashing;
	private Bonus bonus;
	private int bonusTimer;

	public MazeView(Maze maze) {
		this.maze = maze;
		addSprite("s_normal", THEME.mazeFull());
		addSprite("s_flashing", THEME.mazeFlashing());
		energizerBlinking = new CyclicAnimation(2);
		energizerBlinking.setFrameDuration(500);
		energizerBlinking.setEnabled(false);
	}

	@Override
	public void update() {
		if (bonusTimer > 0) {
			bonusTimer -= 1;
			if (bonusTimer == 0) {
				bonus = null;
			}
		}
		energizerBlinking.update();
	}

	public void setFlashing(boolean on) {
		flashing = on;
		setCurrentSprite(flashing ? "s_flashing" : "s_normal");
	}

	public void setBonus(Bonus bonus) {
		this.bonus = bonus;
		bonus.placeAt(maze.getBonusTile(), TS / 2, 0);
	}

	public Optional<Bonus> getBonus() {
		return Optional.ofNullable(bonus);
	}

	public void setBonusTimer(int ticks) {
		bonusTimer = ticks;
	}

	@Override
	public void enableAnimation(boolean enable) {
		super.enableAnimation(enable);
		energizerBlinking.setEnabled(enable);
	}

	@Override
	public void draw(Graphics2D g) {
		g.translate(tf.getX(), tf.getY());
		currentSprite().draw(g);
		g.translate(-tf.getX(), -tf.getY());
		if (!flashing) {
			maze.tiles().forEach(tile -> {
				if (maze.isEatenFood(tile)
						|| maze.isEnergizer(tile) && energizerBlinking.currentFrame() % 2 != 0) {
					g.translate(tile.col * TS, tile.row * TS);
					g.setColor(Color.BLACK);
					g.fillRect(0, 0, TS, TS);
					g.translate(-tile.col * TS, -tile.row * TS);
				}
			});
			if (bonus != null) {
				bonus.draw(g);
			}
		}
	}

	@Override
	public void init() {
		bonus = null;
		setFlashing(false);
	}
}