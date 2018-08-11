package de.amr.games.pacman.model;

public interface Content {

	// Structure
	static final char WALL = '#';
	static final char DOOR = 'D';
	static final char TUNNEL = 'T';

	// Position markers
	static final char POS_BONUS = '$';
	static final char POS_PACMAN = 'O';
	static final char POS_BLINKY = 'B';
	static final char POS_INKY = 'I';
	static final char POS_PINKY = 'P';
	static final char POS_CLYDE = 'C';

	// Food
	static final char PELLET = '.';
	static final char ENERGIZER = '*';
	static final char EATEN = ':';
}