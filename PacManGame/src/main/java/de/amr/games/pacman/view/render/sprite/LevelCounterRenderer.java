package de.amr.games.pacman.view.render.sprite;

import java.awt.Graphics2D;
import java.awt.Image;
import java.util.HashMap;
import java.util.Map;

import de.amr.games.pacman.model.Game;
import de.amr.games.pacman.model.world.arcade.Symbol;
import de.amr.games.pacman.model.world.core.Tile;
import de.amr.games.pacman.view.render.IRenderer;

public class LevelCounterRenderer implements IRenderer {

	private final Game game;
	private final Map<Symbol, Image> bonusImages = new HashMap<Symbol, Image>();

	public LevelCounterRenderer(Game game) {
		this.game = game;
		for (Symbol symbol : Symbol.values()) {
			bonusImages.put(symbol, ArcadeSprites.BUNDLE.spr_bonusSymbol(symbol.name()).frame(0));
		}
	}

	@Override
	public void draw(Graphics2D g) {
		int max = 7;
		int first = Math.max(0, game.levelCounter.size() - max);
		int n = Math.min(max, game.levelCounter.size());
		int sz = 2 * Tile.SIZE; // image size
		for (int i = 0, x = -2 * sz; i < n; ++i, x -= sz) {
			Symbol symbol = game.levelCounter.get(first + i);
			g.drawImage(bonusImages.get(symbol), x, -sz, sz, sz, null);
		}
	}
}