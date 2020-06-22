package de.amr.games.pacman.view.dashboard;

import static de.amr.games.pacman.view.dashboard.Formatting.seconds;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumn;

import de.amr.games.pacman.controller.GameController;
import de.amr.games.pacman.controller.GhostCommand;
import de.amr.games.pacman.model.Game;
import de.amr.games.pacman.view.dashboard.GameStateTableModel.Column;
import de.amr.games.pacman.view.dashboard.GameStateTableModel.Row;
import net.miginfocom.swing.MigLayout;
import java.awt.Dimension;

/**
 * Displays information (state, timer values, directions, speed) about the actors and the global
 * game controller.
 * 
 * @author Armin Reichert
 */
public class GameStateView extends JPanel {

	Action actionShowRoutes = new AbstractAction("Show Routes") {

		@Override
		public void actionPerformed(ActionEvent e) {
			JCheckBox cb = (JCheckBox) e.getSource();
			gameController.setShowingActorRoutes(cb.isSelected());
		}
	};

	Action actionShowGrid = new AbstractAction("Show Grid") {

		@Override
		public void actionPerformed(ActionEvent e) {
			JCheckBox cb = (JCheckBox) e.getSource();
			gameController.setShowingGrid(cb.isSelected());
		}
	};

	Action actionShowStates = new AbstractAction("Show States and Counters") {

		@Override
		public void actionPerformed(ActionEvent e) {
			JCheckBox cb = (JCheckBox) e.getSource();
			gameController.setShowingStates(cb.isSelected());
		}
	};

	private GameController gameController;
	private JTable table;
	private JLabel lblGameControllerState;
	private JCheckBox cbShowRoutes;
	private JCheckBox cbShowStates;
	private GhostHouseStateView ghostHouseStateView;
	private JPanel panel;
	private JCheckBox cbShowGrid;
	private JPanel checkBoxesPanel;

	public GameStateView() {
		setLayout(new BorderLayout(0, 0));

		JPanel content = new JPanel();
		add(content, BorderLayout.CENTER);
		content.setLayout(new MigLayout("", "[grow]", "[][][][][grow,top]"));

		lblGameControllerState = new JLabel("Game Controller State");
		lblGameControllerState.setForeground(Color.BLUE);
		lblGameControllerState.setFont(new Font("SansSerif", Font.BOLD, 16));
		content.add(lblGameControllerState, "cell 0 0,alignx center");

		JScrollPane scrollPane = new JScrollPane();
		content.add(scrollPane, "cell 0 1,growx,aligny top");

		table = new JTable();
		table.setPreferredScrollableViewportSize(new Dimension(450, 350));
		scrollPane.setViewportView(table);
		table.setModel(GameStateTableModel.SAMPLE_DATA);

		panel = new JPanel();
		panel.setBorder(
				new TitledBorder(null, "Ghost House", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(59, 59, 59)));
		content.add(panel, "cell 0 2,growx,aligny top");
		panel.setLayout(new MigLayout("", "[grow]", "[grow]"));

		ghostHouseStateView = new GhostHouseStateView();
		panel.add(ghostHouseStateView, "cell 0 0,growx,aligny top");

		checkBoxesPanel = new JPanel();
		content.add(checkBoxesPanel, "cell 0 3,growx");

		cbShowRoutes = new JCheckBox("Show Routes");
		checkBoxesPanel.add(cbShowRoutes);
		cbShowRoutes.setAction(actionShowRoutes);

		cbShowGrid = new JCheckBox("Show Grid");
		checkBoxesPanel.add(cbShowGrid);
		cbShowGrid.setAction(actionShowGrid);

		cbShowStates = new JCheckBox("Show States and Counters");
		checkBoxesPanel.add(cbShowStates);
		cbShowStates.setAction(actionShowStates);
	}

	/**
	 * Attaches this view to the game controller.
	 * 
	 * @param gameController the game controller
	 */
	public void attachTo(GameController gameController) {
		this.gameController = gameController;
		GameStateTableModel tableModel = new GameStateTableModel(gameController);
		tableModel.addTableModelListener(e -> {
			if (e.getColumn() == Column.OnStage.ordinal()) {
				int row = e.getFirstRow();
				if (row != Row.PacMan.ordinal() && row != Row.Bonus.ordinal()) {
					gameController.game.takePart(tableModel.ghostByRow[row], tableModel.data[row].takesPart);
				}
			}
		});
		table.setModel(tableModel);
		column(Column.Tile).setCellRenderer(new TileCellRenderer());
		column(Column.Speed).setCellRenderer(new SpeedCellRenderer());
		column(Column.Remaining).setCellRenderer(new TicksCellRenderer());
		column(Column.Duration).setCellRenderer(new TicksCellRenderer());
		ghostHouseStateView.attachTo(gameController);
		updateViewState();
	}

	private TableColumn column(Column column) {
		return table.getColumnModel().getColumn(column.ordinal());
	}

	public void updateViewState() {
		updateTableData();
		String stateText = gameController.getState().name();
		if (gameController.state().getDuration() != Integer.MAX_VALUE) {
			stateText = String.format("%s (%s sec of %s sec remaining)", gameController.getState(),
					seconds(gameController.state().getTicksRemaining()), seconds(gameController.state().getDuration()));
		}
		lblGameControllerState.setText(stateText);
		cbShowRoutes.setSelected(gameController.isShowingActorRoutes());
		cbShowGrid.setSelected(gameController.isShowingGrid());
		cbShowStates.setSelected(gameController.isShowingStates());
		ghostHouseStateView.updateViewState();
	}

	private void updateTableData() {
		if (table.getModel() instanceof GameStateTableModel) {
			GameStateTableModel model = (GameStateTableModel) table.getModel();
			Game game = gameController.game;
			GhostCommand ghostCommand = gameController.ghostCommand;
			model.data[Row.Blinky.ordinal()] = new ActorData(game, ghostCommand, game.blinky);
			model.data[Row.Pinky.ordinal()] = new ActorData(game, ghostCommand, game.pinky);
			model.data[Row.Inky.ordinal()] = new ActorData(game, ghostCommand, game.inky);
			model.data[Row.Clyde.ordinal()] = new ActorData(game, ghostCommand, game.clyde);
			model.data[Row.PacMan.ordinal()] = new ActorData(game, game.pacMan);
			model.data[Row.Bonus.ordinal()] = new ActorData(game, game.bonus);
			model.fireTableDataChanged();
		}
	}
}