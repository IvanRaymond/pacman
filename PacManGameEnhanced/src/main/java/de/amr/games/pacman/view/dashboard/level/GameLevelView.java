package de.amr.games.pacman.view.dashboard.level;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import de.amr.easy.game.controller.Lifecycle;
import de.amr.games.pacman.controller.game.GameController;
import de.amr.games.pacman.view.dashboard.util.UniversalFormatter;
import net.miginfocom.swing.MigLayout;

public class GameLevelView extends JPanel implements Lifecycle {

	private GameController controller;
	private JTable table;

	public GameLevelView() {
		setLayout(new BorderLayout(0, 0));

		JPanel content = new JPanel();
		add(content, BorderLayout.CENTER);
		content.setLayout(new MigLayout("", "[grow]", "[grow]"));

		JScrollPane scrollPane = new JScrollPane();
		content.add(scrollPane, "cell 0 0,grow");

		table = new JTable();
		table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		table.setRowHeight(17);
		table.setRowSelectionAllowed(false);
		scrollPane.setViewportView(table);
	}

	public void attachTo(GameController controller) {
		this.controller = controller;
		init();
	}

	@Override
	public void init() {
		if (controller.game.level != null) {
			table.setModel(new GameLevelTableModel(controller.game));
		} else {
			table.setModel(new GameLevelTableModel());
		}
		UniversalFormatter fmt = new UniversalFormatter();
		fmt.fnBoldCondition = c -> c.row < 6;
		table.getColumnModel().getColumns().asIterator().forEachRemaining(column -> column.setCellRenderer(fmt));
		table.getColumnModel().getColumn(0).setMaxWidth(180);
		table.getColumnModel().getColumn(0).setMinWidth(180);
	}

	@Override
	public void update() {
		if (controller.game.level != null) {
			GameLevelTableModel tableModel = (GameLevelTableModel) table.getModel();
			if (!tableModel.hasGame()) {
				init();
			}
			tableModel.fireTableDataChanged();
		}
	}
}