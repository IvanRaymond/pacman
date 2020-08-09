package de.amr.games.pacman.view.common;

import java.awt.Graphics2D;

import de.amr.easy.game.entity.Entity;
import de.amr.easy.game.ui.sprites.Sprite;

public interface ISpriteRenderer {

	/**
	 * Draws the selected sprite centered over the collision box of the entity.
	 * 
	 * @param g       graphics context
	 * @param entity  entiy to be drawn
	 * @param sprite  the sprite for the entity in its current state
	 * @param scaling scaling of sprite size against entity size
	 */
	default void drawEntitySprite(Graphics2D g, Entity entity, Sprite sprite, int scaling) {
		if (entity.visible) {
			Graphics2D g2 = (Graphics2D) g.create();
			int w = entity.tf.width, h = entity.tf.height;
			if (sprite.getWidth() != scaling * w || sprite.getHeight() != scaling * h) {
				g2.scale(scaling, scaling);
			}
			float x = entity.tf.x - (sprite.getWidth() - w) / 2, y = entity.tf.y - (sprite.getHeight() - h) / 2;
			sprite.draw(g2, x, y);
			g2.dispose();
		}
	}
}