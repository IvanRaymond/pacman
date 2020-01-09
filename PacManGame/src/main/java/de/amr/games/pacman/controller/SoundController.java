package de.amr.games.pacman.controller;

import java.util.concurrent.CompletableFuture;

import de.amr.easy.game.Application;
import de.amr.easy.game.assets.Sound;
import de.amr.games.pacman.actor.Cast;
import de.amr.games.pacman.actor.GhostState;
import de.amr.games.pacman.theme.Theme;

/**
 * Controls music and sound.
 * 
 * @author Armin Reichert
 */
public class SoundController {

	private Theme theme;
	private CompletableFuture<Void> musicLoading;
	private long lastPelletEatenTimeMillis;

	public SoundController(Theme theme) {
		this.theme = theme;
	}

	public void updatePlayingSounds(Cast cast) {
		if (theme.snd_eatPill().isRunning() && System.currentTimeMillis() - lastPelletEatenTimeMillis > 250) {
			theme.snd_eatPill().stop();
			Application.LOGGER.info("Pellet eaten sound stopped");
		}
		if (cast.ghostsOnStage().anyMatch(ghost -> ghost.is(GhostState.CHASING))) {
			if (!theme.snd_ghost_chase().isRunning()) {
				theme.snd_ghost_chase().loop();
			}
		}
		else {
			theme.snd_ghost_chase().stop();
		}
		if (cast.ghostsOnStage().anyMatch(ghost -> ghost.is(GhostState.DEAD))) {
			if (!theme.snd_ghost_dead().isRunning()) {
				theme.snd_ghost_dead().loop();
			}
		}
		else {
			theme.snd_ghost_dead().stop();
		}
	}

	public void loadMusic() {
		musicLoading = CompletableFuture.runAsync(() -> {
			theme.music_playing();
			theme.music_gameover();
		});
	}

	public boolean isMusicLoadingComplete() {
		return musicLoading.isDone();
	}

	public void muteAll() {
		muteSoundEffects();
		theme.music_playing().stop();
		theme.music_gameover().stop();
	}

	public void muteSoundEffects() {
		theme.snd_clips_all().forEach(Sound::stop);
	}

	public void muteGhostSounds() {
		theme.snd_ghost_chase().stop();
		theme.snd_ghost_dead().stop();
	}

	public void gameStarts() {
		theme.music_playing().volume(.90f);
		theme.music_playing().loop();
	}

	public void gameReady() {
		theme.snd_ready().play();
	}

	public void pelletEaten() {
		if (!theme.snd_eatPill().isRunning()) {
			theme.snd_eatPill().loop();
			Application.LOGGER.info("Pellet eaten sound started");
		}
		lastPelletEatenTimeMillis = System.currentTimeMillis();
	}

	public void ghostEaten() {
		theme.snd_eatGhost().play();
	}

	public void bonusEaten() {
		theme.snd_eatFruit().play();
	}

	public void pacManDied() {
		theme.snd_die().play();
	}

	public void extraLife() {
		theme.snd_extraLife().play();
	}

	public void gameOver() {
		theme.music_gameover().play();
	}

	public boolean isGameOverMusicRunning() {
		return theme.music_gameover().isRunning();
	}
}