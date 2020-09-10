package de.amr.games.pacman.view.theme.arcade;

import java.awt.Graphics2D;

import de.amr.games.pacman.model.game.PacManGame;
import de.amr.games.pacman.model.world.components.Tile;
import de.amr.games.pacman.view.api.IGameRenderer;

class LivesCounterRenderer implements IGameRenderer {

	@Override
	public void render(Graphics2D g, PacManGame game) {
		ArcadeSpritesheet arcadeSprites = ArcadeTheme.THEME.$value("sprites");
		for (int i = 0, x = Tile.SIZE; i < game.lives; ++i, x += 2 * Tile.SIZE) {
			g.drawImage(arcadeSprites.imageLivesCounter(), x, 0, null);
		}
	}
}