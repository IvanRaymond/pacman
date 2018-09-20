package de.amr.games.pacman.test.navigation;

import java.util.concurrent.Executors;

import de.amr.games.pacman.PacManApp;
import de.amr.games.pacman.theme.ClassicPacManTheme;

public class FollowFixedPathTestApp extends PacManApp {

	public static void main(String[] args) {
		try {
			THEME = ClassicPacManTheme.class.newInstance();
			LOGGER.info(String.format("Theme '%s' created.", THEME.getClass().getSimpleName()));
			Executors.newSingleThreadExecutor().submit((() -> THEME.snd_music_all()));
			launch(new FollowFixedPathTestApp());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public FollowFixedPathTestApp() {
		settings.title = "Follow Fixed Path";
	}

	@Override
	public void init() {
		setController(new FollowFixedPathTestController());
	}
}