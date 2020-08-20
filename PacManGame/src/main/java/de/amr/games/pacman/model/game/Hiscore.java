package de.amr.games.pacman.model.game;

import static de.amr.easy.game.Application.loginfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Properties;

/**
 * Manages the game's highscore.
 * 
 * @author Armin Reichert
 */
public class Hiscore extends Score {

	private static final File DIR = new File(System.getProperty("user.home"));
	private static final File FILE = new File(DIR, "pacman.hiscore.xml");
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_ZONED_DATE_TIME;

	private final Properties data = new Properties(3);
	private ZonedDateTime time;
	private boolean needsUpdate;

	public Hiscore() {
		time = ZonedDateTime.now();
		needsUpdate = true;
	}

	public void load() {
		loginfo("Loading highscore from file '%s'", FILE);
		try {
			data.loadFromXML(new FileInputStream(FILE));
			points = Integer.valueOf(data.getProperty("score"));
			levelNumber = Integer.valueOf(data.getProperty("level"));
			if (data.getProperty("time") != null) {
				time = ZonedDateTime.parse(data.getProperty("time"), DATE_FORMAT);
			} else {
				time = ZonedDateTime.now();
			}
		} catch (FileNotFoundException e) {
			loginfo("Hiscore file not available, creating new one");
			save();
		} catch (DateTimeParseException e) {
			loginfo("Could not parse time in hiscore file '%s'", FILE);
			e.printStackTrace();
		} catch (Exception e) {
			loginfo("Could not load hiscore file '%s'", FILE);
			e.printStackTrace();
		}
	}

	public void save() {
		if (needsUpdate) {
			data.setProperty("score", Integer.toString(points));
			data.setProperty("level", Integer.toString(levelNumber));
			if (time == null) {
				time = ZonedDateTime.now();
			}
			data.setProperty("time", time.format(DATE_FORMAT));
			try {
				data.storeToXML(new FileOutputStream(FILE), "Pac-Man Highscore");
				needsUpdate = false;
				loginfo("Saved highscore file '%s'", FILE);
			} catch (IOException e) {
				loginfo("Could not save hiscore file '%s'", FILE);
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Checks is the given level number and points mark a new hiscore.
	 * 
	 * @param levelNumber level number
	 * @param points      points
	 */
	public void check(int levelNumber, int points) {
		if (points > this.points) {
			this.points = points;
			this.levelNumber = levelNumber;
			time = ZonedDateTime.now();
			needsUpdate = true;
		}
	}
}