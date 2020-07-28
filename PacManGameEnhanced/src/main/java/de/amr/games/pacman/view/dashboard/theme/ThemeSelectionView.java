package de.amr.games.pacman.view.dashboard.theme;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.util.Optional;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import de.amr.easy.game.controller.Lifecycle;
import de.amr.easy.game.view.View;
import de.amr.games.pacman.controller.creatures.Folks;
import de.amr.games.pacman.controller.creatures.ghost.GhostState;
import de.amr.games.pacman.controller.creatures.pacman.PacManState;
import de.amr.games.pacman.controller.game.GameController;
import de.amr.games.pacman.model.world.api.Direction;
import de.amr.games.pacman.model.world.api.Tile;
import de.amr.games.pacman.view.api.PacManGameView;
import de.amr.games.pacman.view.theme.api.Theme;
import net.miginfocom.swing.MigLayout;

public class ThemeSelectionView extends JPanel implements Lifecycle {

	private boolean initialized = false;
	private Folks folks;

	private GameController gameController;
	private JComboBox<String> comboSelectTheme;
	private JLabel lblPacMan;
	private JLabel lblBlinky;
	private JLabel lblPinky;
	private JLabel lblInky;
	private JLabel lblClyde;

	public void attachTo(GameController gameController) {
		this.gameController = gameController;
		folks = new Folks(gameController.world(), gameController.world().house(0));
	}

	public ThemeSelectionView() {
		setLayout(new MigLayout("", "[][grow]", "[][grow]"));

		JLabel lblSelectTheme = new JLabel("Select Theme");
		add(lblSelectTheme, "cell 0 0,alignx trailing");

		comboSelectTheme = new JComboBox<>();
		comboSelectTheme.setModel(new DefaultComboBoxModel<String>(new String[] { "ARCADE", "LETTERS", "BLOCKS" }));
		add(comboSelectTheme, "cell 1 0,alignx left");

		JPanel panelPreview = new JPanel();
		panelPreview.setBackground(Color.BLACK);
		add(panelPreview, "cell 1 1,alignx left,aligny top");
		panelPreview.setLayout(new MigLayout("", "[grow][grow][grow][grow][grow]", "[]"));

		lblPacMan = new JLabel("Pac-Man");
		lblPacMan.setVerticalAlignment(SwingConstants.BOTTOM);
		lblPacMan.setForeground(Color.WHITE);
		lblPacMan.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblPacMan.setHorizontalAlignment(SwingConstants.CENTER);
		lblPacMan.setHorizontalTextPosition(SwingConstants.CENTER);
		lblPacMan.setVerticalTextPosition(SwingConstants.BOTTOM);
		panelPreview.add(lblPacMan, "cell 0 0,grow");

		lblBlinky = new JLabel("Blinky");
		lblBlinky.setVerticalAlignment(SwingConstants.BOTTOM);
		lblBlinky.setPreferredSize(new Dimension(40, 60));
		lblBlinky.setForeground(Color.WHITE);
		lblBlinky.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblBlinky.setHorizontalAlignment(SwingConstants.CENTER);
		lblBlinky.setHorizontalTextPosition(SwingConstants.CENTER);
		lblBlinky.setVerticalTextPosition(SwingConstants.BOTTOM);
		panelPreview.add(lblBlinky, "cell 1 0,grow");

		lblPinky = new JLabel("Pinky");
		lblPinky.setVerticalAlignment(SwingConstants.BOTTOM);
		lblPinky.setPreferredSize(new Dimension(40, 60));
		lblPinky.setForeground(Color.WHITE);
		lblPinky.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblPinky.setHorizontalAlignment(SwingConstants.CENTER);
		lblPinky.setHorizontalTextPosition(SwingConstants.CENTER);
		lblPinky.setVerticalTextPosition(SwingConstants.BOTTOM);
		panelPreview.add(lblPinky, "cell 2 0,grow");

		lblInky = new JLabel("Inky");
		lblInky.setVerticalAlignment(SwingConstants.BOTTOM);
		lblInky.setPreferredSize(new Dimension(40, 60));
		lblInky.setForeground(Color.WHITE);
		lblInky.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblInky.setHorizontalAlignment(SwingConstants.CENTER);
		lblInky.setHorizontalTextPosition(SwingConstants.CENTER);
		lblInky.setVerticalTextPosition(SwingConstants.BOTTOM);
		panelPreview.add(lblInky, "cell 3 0,grow");

		lblClyde = new JLabel("Clyde");
		lblClyde.setVerticalAlignment(SwingConstants.BOTTOM);
		lblClyde.setPreferredSize(new Dimension(40, 60));
		lblClyde.setForeground(Color.WHITE);
		lblClyde.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblClyde.setHorizontalAlignment(SwingConstants.CENTER);
		lblClyde.setHorizontalTextPosition(SwingConstants.CENTER);
		lblClyde.setVerticalTextPosition(SwingConstants.BOTTOM);
		panelPreview.add(lblClyde, "cell 4 0,grow");
		comboSelectTheme.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				String themeName = comboSelectTheme.getModel().getElementAt(comboSelectTheme.getSelectedIndex());
				gameController.selectTheme(themeName);
				updatePreviewLabels();
			}
		});
	}

	private Optional<View> currentView() {
		return Optional.ofNullable(gameController).flatMap(GameController::currentView);
	}

	private void updateSelectionFromTheme() {
		currentView().ifPresent(view -> {
			PacManGameView gameView = (PacManGameView) view;
			if (!comboSelectTheme.getSelectedItem().equals(gameController.getTheme().name())) {
				comboSelectTheme.setSelectedItem(gameView.getTheme().name().toUpperCase());
				updatePreviewLabels();
			}
		});
	}

	private void updatePreviewLabels() {
		Theme theme = gameController.getTheme();
		int size = 32;
		setPacManLabel(folks, theme, size);
		setGhostLabels(folks, theme, size);
	}

	private static BufferedImage scaled(BufferedImage source, int scaledWidth, int scaledHeight) {
		BufferedImage result = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = result.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.drawImage(source, 0, 0, scaledWidth, scaledHeight, null);
		return result;
	}

	private void setPacManLabel(Folks folks, Theme theme, int size) {
		folks.pacMan.setState(PacManState.AWAKE);
		folks.pacMan.setMoveDir(Direction.RIGHT);
		folks.pacMan.entity.tf.setPosition(Tile.SIZE / 2, Tile.SIZE / 2);
		BufferedImage small = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		theme.pacManRenderer(folks.pacMan).render(small.createGraphics());
		lblPacMan.setIcon(new ImageIcon(scaled(small, size, size)));
	}

	private void setGhostLabels(Folks folks, Theme theme, int size) {
		folks.ghosts().forEach(ghost -> {
			ghost.setState(GhostState.CHASING);
			ghost.setMoveDir(Direction.RIGHT);
			ghost.entity.tf.setPosition(Tile.SIZE / 2, Tile.SIZE / 2);
			BufferedImage small = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			theme.ghostRenderer(ghost).render(small.createGraphics());
			ImageIcon icon = new ImageIcon(scaled(small, size, size));
			switch (ghost.name) {
			case "Blinky":
				lblBlinky.setIcon(icon);
				break;
			case "Pinky":
				lblPinky.setIcon(icon);
				break;
			case "Inky":
				lblInky.setIcon(icon);
				break;
			case "Clyde":
				lblClyde.setIcon(icon);
				break;
			default:
				break;
			}
		});
	}

	@Override
	public void init() {
		// not called!
	}

	@Override
	public void update() {
		if (!initialized) {
			updatePreviewLabels();
			initialized = true;
		}
		updateSelectionFromTheme();
	}
}