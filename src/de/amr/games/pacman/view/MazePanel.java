package de.amr.games.pacman.view;

import static de.amr.games.pacman.model.Content.EATEN;
import static de.amr.games.pacman.model.Content.ENERGIZER;
import static de.amr.games.pacman.view.PacManGameUI.SPRITES;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.stream.Stream;

import de.amr.easy.game.entity.GameEntity;
import de.amr.easy.game.sprite.Animation;
import de.amr.easy.game.sprite.CyclicAnimation;
import de.amr.easy.game.sprite.Sprite;
import de.amr.games.pacman.actor.game.Cast;
import de.amr.games.pacman.actor.game.GhostState;
import de.amr.games.pacman.model.Game;
import de.amr.games.pacman.model.Maze;

public class MazePanel extends GameEntity {

	private final Maze maze;
	private final Cast actors;
	private final Animation energizerBlinking;
	private final Sprite s_maze_normal;
	private final Sprite s_maze_flashing;
	private boolean flashing;
	private int bonusTimer;

	public MazePanel(Maze maze, Cast actors) {
		this.maze = maze;
		this.actors = actors;
		s_maze_normal = SPRITES.mazeFull();
		s_maze_flashing = SPRITES.mazeFlashing();
		energizerBlinking = new CyclicAnimation(2);
		energizerBlinking.setFrameDuration(500);
	}

	@Override
	public Stream<Sprite> getSprites() {
		return Stream.of(s_maze_normal, s_maze_flashing);
	}

	@Override
	public Sprite currentSprite() {
		return flashing ? s_maze_flashing : s_maze_normal;
	}

	@Override
	public void update() {
		if (bonusTimer > 0) {
			bonusTimer -= 1;
			if (bonusTimer == 0) {
				actors.removeBonus();
			}
		}
		energizerBlinking.update();
	}

	@Override
	public int getWidth() {
		return maze.numCols() * Game.TS;
	}

	@Override
	public int getHeight() {
		return maze.numRows() * Game.TS;
	}

	public void setFlashing(boolean on) {
		flashing = on;
	}

	public void setBonusTimer(int ticks) {
		bonusTimer = ticks;
	}

	@Override
	public void enableAnimation(boolean enable) {
		super.enableAnimation(enable);
		actors.getPacMan().enableAnimation(enable);
		actors.getActiveGhosts().forEach(ghost -> ghost.enableAnimation(enable));
		energizerBlinking.setEnabled(enable);
	}

	@Override
	public void draw(Graphics2D g) {
		g.translate(tf.getX(), tf.getY());
		if (flashing) {
			s_maze_flashing.draw(g);
			actors.getPacMan().draw(g);
		} else {
			s_maze_normal.draw(g);
			drawFood(g);
			drawActors(g);
		}
		g.translate(-tf.getX(), -tf.getY());
	}

	private void drawActors(Graphics2D g) {
		actors.getBonus().ifPresent(bonus -> {
			bonus.placeAt(maze.bonusTile);
			bonus.draw(g);
		});
		actors.getPacMan().draw(g);
		actors.getActiveGhosts().filter(ghost -> ghost.getState() != GhostState.DYING).forEach(ghost -> ghost.draw(g));
		actors.getActiveGhosts().filter(ghost -> ghost.getState() == GhostState.DYING).forEach(ghost -> ghost.draw(g));
	}

	private void drawFood(Graphics2D g) {
		maze.tiles().forEach(tile -> {
			char c = maze.getContent(tile);
			if (c == EATEN || c == ENERGIZER && energizerBlinking.currentFrame() % 2 != 0) {
				g.translate(tile.col * Game.TS, tile.row * Game.TS);
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, Game.TS, Game.TS);
				g.translate(-tile.col * Game.TS, -tile.row * Game.TS);
			}
		});
	}
}