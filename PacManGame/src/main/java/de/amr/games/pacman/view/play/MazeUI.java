package de.amr.games.pacman.view.play;

import static de.amr.games.pacman.model.PacManGame.TS;

import java.awt.Graphics2D;

import de.amr.easy.game.entity.SpriteEntity;
import de.amr.easy.game.ui.sprites.Animation;
import de.amr.easy.game.ui.sprites.CyclicAnimation;
import de.amr.easy.game.ui.sprites.Sprite;
import de.amr.games.pacman.actor.Bonus;
import de.amr.games.pacman.model.PacManGame;
import de.amr.games.pacman.model.Tile;

/**
 * Displays the maze, bonus symbol and handles animations like blinking.
 * 
 * @author Armin Reichert
 */
public class MazeUI extends SpriteEntity {

	private final PacManGame game;
	private final Animation blinkingEnergizer;
	private boolean flashing;
	private int bonusTimer;

	public MazeUI(PacManGame game) {
		this.game = game;
		sprites.set("normal", game.theme.spr_fullMaze());
		sprites.set("flashing", game.theme.spr_flashingMaze());
		sprites.select("normal");
		blinkingEnergizer = new CyclicAnimation(2);
		blinkingEnergizer.setFrameDuration(500);
		blinkingEnergizer.setEnabled(false);
	}

	@Override
	public void init() {
		game.removeBonus();
		bonusTimer = 0;
		setFlashing(false);
	}

	@Override
	public void update() {
		if (bonusTimer > 0) {
			bonusTimer -= 1;
			if (bonusTimer == 0) {
				game.removeBonus();
			}
		}
		blinkingEnergizer.update();
	}

	public void setFlashing(boolean state) {
		flashing = state;
		sprites.select(flashing ? "flashing" : "normal");
	}

	public void setBonus(Bonus bonus) {
		game.setBonus(bonus);
		Tile tile = game.maze.getBonusTile();
		bonus.tf.setPosition(tile.col * TS + TS / 2, tile.row * TS);
	}

	public void removeBonus() {
		game.removeBonus();
	}

	public void setBonusTimer(int ticks) {
		bonusTimer = ticks;
	}

	public void enableSprites(boolean enable) {
		sprites.enableAnimation(enable);
		blinkingEnergizer.setEnabled(enable);
	}

	@Override
	public void draw(Graphics2D g) {
		Sprite mazeSprite = sprites.current().get();
		g.translate(tf.getX(), tf.getY());
		g.setColor(game.theme.color_mazeBackground());
		g.fillRect(0, 0, mazeSprite.getWidth(), mazeSprite.getHeight());
		mazeSprite.draw(g);
		g.translate(-tf.getX(), -tf.getY());
		if (!flashing) {
			// hide eaten pellets and let energizers blink
			game.maze.tiles().forEach(tile -> {
				if (game.maze.containsEatenFood(tile) || game.maze.containsEnergizer(tile)
						&& blinkingEnergizer.currentFrame() != 0) {
					g.translate(tile.col * TS, tile.row * TS);
					g.setColor(game.theme.color_mazeBackground());
					g.fillRect(0, 0, TS, TS);
					g.translate(-tile.col * TS, -tile.row * TS);
				}
			});
			game.getBonus().ifPresent(bonus -> bonus.draw(g));
		}
	}
}