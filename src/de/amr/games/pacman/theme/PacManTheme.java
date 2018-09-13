package de.amr.games.pacman.theme;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.stream.Stream;

import de.amr.easy.game.assets.Sound;
import de.amr.easy.game.sprite.Sprite;
import de.amr.games.pacman.model.BonusSymbol;

public interface PacManTheme {

	Sprite spr_emptyMaze();

	Sprite spr_fullMaze();

	Sprite spr_flashingMaze();

	Sprite spr_bonusSymbol(BonusSymbol symbol);

	BufferedImage img_bonusSymbol(BonusSymbol symbol);

	Sprite spr_pacManFull();

	Sprite spr_pacManWalking(int dir);

	Sprite spr_pacManDying();

	Sprite spr_ghostColored(GhostColor color, int direction);

	Sprite spr_ghostFrightened();

	Sprite spr_ghostFlashing();

	Sprite spr_ghostEyes(int dir);

	Sprite spr_greenNumber(int i);

	Sprite spr_pinkNumber(int i);

	Font fnt_text();

	default Font fnt_text(int size) {
		return fnt_text().deriveFont((float) size);
	}

	Sound snd_music_play();
	
	Sound snd_music_gameover();
	
	Sound snd_die();

	Sound snd_eatFruit();

	Sound snd_eatGhost();

	Sound snd_eatPill();

	Sound snd_eating();

	Sound snd_extraLife();

	Sound snd_insertCoin();

	Sound snd_ready();

	Sound snd_siren();

	Sound snd_waza();

	Stream<Sound> snd_clips_all();
	
	Stream<Sound> snd_music_all();
	
}
