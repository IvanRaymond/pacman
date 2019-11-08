package de.amr.games.pacman.test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import de.amr.games.pacman.model.Maze;
import de.amr.games.pacman.model.Tile;
import de.amr.graph.grid.ui.rendering.ConfigurableGridRenderer;
import de.amr.graph.grid.ui.rendering.GridCanvas;
import de.amr.graph.grid.ui.rendering.GridRenderer;
import de.amr.graph.grid.ui.rendering.WallPassageGridRenderer;

public class BoardPreview extends JFrame {

	static int TS = 16;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(BoardPreview::new);
	}

	private Maze maze;

	public BoardPreview() {
		maze = new Maze();
		setTitle("Pac-Man Maze Preview");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		GridCanvas canvas = new GridCanvas(maze.gridGraph, TS);
		canvas.pushRenderer(createRenderer());
		add(canvas, BorderLayout.CENTER);
		canvas.drawGrid();
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	private Tile tile(int cell) {
		return maze.tileAt(maze.gridGraph.col(cell), maze.gridGraph.row(cell));
	}

	private GridRenderer createRenderer() {
		ConfigurableGridRenderer r = new WallPassageGridRenderer();
		r.fnCellSize = () -> TS;
		r.fnPassageWidth = (u, v) -> TS - 1;
		r.fnPassageColor = (cell, dir) -> Color.WHITE;
		r.fnCellBgColor = cell -> {
			Tile tile = tile(cell);
			if (maze.isWall(tile)) {
				return Color.BLACK;
			}
			if (maze.insideTunnel(tile)) {
				return Color.LIGHT_GRAY;
			}
			if (maze.insideGhostHouse(tile)) {
				return Color.CYAN;
			}
			if (maze.isUnrestrictedIntersection(tile)) {
				return Color.GREEN;
			}
			if (maze.isUpwardsBlockedIntersection(tile)) {
				return Color.YELLOW;
			}
			if (maze.isDoor(tile)) {
				return Color.PINK;
			}
			return Color.WHITE;
		};
		r.fnText = this::text;
		r.fnTextColor = cell -> Color.RED;
		r.fnTextFont = cell -> new Font("Arial Bold", Font.BOLD, TS * 70 / 100);
		return r;
	}

	private String text(int cell) {
		Tile tile = tile(cell);
		if (tile == maze.blinkyHome || tile == maze.blinkyScatter) {
			return "B";
		}
		if (tile == maze.pinkyHome || tile == maze.pinkyScatter) {
			return "P";
		}
		if (tile == maze.inkyHome || tile == maze.inkyScatter) {
			return "I";
		}
		if (tile == maze.clydeHome || tile == maze.clydeScatter) {
			return "C";
		}
		if (tile == maze.pacManHome) {
			return "O";
		}
		if (maze.isWall(tile)) {
			return "";
		}
		if (maze.containsEnergizer(tile)) {
			return "E";
		}
		return String.valueOf(tile.content);
	}
}