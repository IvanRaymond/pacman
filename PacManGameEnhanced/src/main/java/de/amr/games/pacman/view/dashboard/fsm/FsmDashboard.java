package de.amr.games.pacman.view.dashboard.fsm;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

import de.amr.easy.game.ui.widgets.MultiPanel;
import de.amr.games.pacman.model.fsm.FsmData;
import de.amr.games.pacman.model.fsm.FsmModel;

/**
 * Shows up to 4 state machines in a window.
 * 
 * @author Armin Reichert
 */
public class FsmDashboard extends JFrame {

	static class FsmSelectionModel extends DefaultComboBoxModel<String> {

		public FsmSelectionModel(FsmModel model) {
			model.data().sorted().forEach(data -> {
				addElement(data.getFsm().getDescription());
			});
		}
	}

	private Action actionZoomAllIn = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			for (FsmGraphView view : views) {
				view.actionZoomIn.actionPerformed(e);
			}
		}
	};

	private Action actionZoomAllOut = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			for (FsmGraphView view : views) {
				view.actionZoomOut.actionPerformed(e);
			}
		}
	};

	private final FsmModel model;
	private MultiPanel multiPanel;
	private FsmGraphView[] views = new FsmGraphView[4];

	public FsmDashboard(FsmModel model) {
		this.model = model;
		setTitle("State Machines Dashboard");
		multiPanel = new MultiPanel();
		multiPanel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		getContentPane().add(multiPanel);
		for (int i = 0; i < 4; ++i) {
			multiPanel.getComboBox(i).setModel(new FsmSelectionModel(model));
			multiPanel.getComboBox(i).addItemListener(this::onFsmSelection);
			views[i] = new FsmGraphView();
			multiPanel.getPanel(i).add(views[i], BorderLayout.CENTER);
			multiPanel.getToolBar(i).add(views[i].actionZoomIn);
			multiPanel.getToolBar(i).add(views[i].actionZoomOut);
		}
		multiPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('+'), actionZoomAllIn);
		multiPanel.getActionMap().put(actionZoomAllIn, actionZoomAllIn);
		multiPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('-'), actionZoomAllOut);
		multiPanel.getActionMap().put(actionZoomAllOut, actionZoomAllOut);
	}

	@SuppressWarnings("unchecked")
	private void onFsmSelection(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			JComboBox<String> combo = (JComboBox<String>) e.getSource();
			int index = comboIndex(combo);
			List<FsmData> dataList = model.data().sorted().collect(Collectors.toList());
			views[index].setData(dataList.get(combo.getSelectedIndex()));
		}
	}

	private int comboIndex(JComboBox<String> combo) {
		if (combo == multiPanel.getComboBox(0)) {
			return 0;
		}
		if (combo == multiPanel.getComboBox(1)) {
			return 1;
		}
		if (combo == multiPanel.getComboBox(2)) {
			return 2;
		}
		if (combo == multiPanel.getComboBox(3)) {
			return 3;
		}
		throw new IllegalArgumentException();
	}

	public void rebuild() {
		List<FsmData> dataList = model.data().sorted().collect(Collectors.toList());
		for (int i = 0; i < 4; ++i) {
			multiPanel.getComboBox(i).setModel(new FsmSelectionModel(model));
			if (i < dataList.size()) {
				multiPanel.getComboBox(i).setSelectedIndex(i);
				views[i].setData(dataList.get(i));
			} else {
				multiPanel.getComboBox(i).setSelectedIndex(-1);
				views[i].setData(null);
			}
		}
	}

	public void update() {
		for (int i = 0; i < 4; ++i) {
			if (views[i].getData() != null) {
				views[i].getData().updateGraphVizText();
			}
			views[i].update();
		}
	}
}