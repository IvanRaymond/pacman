package de.amr.games.pacman.view.dashboard.fsm;

import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import de.amr.easy.game.Application;
import de.amr.easy.game.controller.Lifecycle;
import de.amr.statemachine.core.StateMachine;

public class FsmDashboard implements Lifecycle {

	static int WIDTH = 1024, HEIGHT = 700;

	private class FsmFrame extends JInternalFrame {
		private FsmGraphView graphView;
		private FsmData data;

		public FsmFrame(FsmData data) {
			this.data = data;
			setClosable(false);
			setResizable(true);
			setIconifiable(true);
			setMaximizable(true);
			setTitle(data.getFsm().getDescription());
			graphView = new FsmGraphView();
			graphView.setData(data);
			getContentPane().add(graphView);
		}
	}

	private final FsmModel model;
	public final JFrame window;
	private final JDesktopPane desktop;
	private final Map<StateMachine<?, ?>, FsmFrame> fsmFrameMap = new HashMap<>();
	private JMenu framesMenu = new JMenu("State Machines");

	public FsmDashboard(FsmModel model) {
		this.model = model;
		desktop = new JDesktopPane();
		desktop.setSize(WIDTH, HEIGHT);
		desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
		window = new JFrame("Pac-Man State Machines Dashboard");
		window.setContentPane(desktop);
		window.setSize(WIDTH + 5, HEIGHT + 5);
		JMenuBar menuBar = new JMenuBar();
		window.setJMenuBar(menuBar);
		menuBar.add(framesMenu);
	}

	@Override
	public void init() {
	}

	@Override
	public void update() {
		desktop.removeAll();
		fsmFrameMap.clear();
		framesMenu.removeAll();
		model.data().forEach(data -> {
			FsmFrame frame = new FsmFrame(data);
			fsmFrameMap.put(data.getFsm(), frame);
			desktop.add(frame);
			frame.setSize(desktop.getWidth() * 90 / 100, 250);
			frame.setLocation(0, 25 * fsmFrameMap.size());
			frame.setVisible(true);
			try {
				frame.setMaximum(false);
			} catch (PropertyVetoException x) {
				x.printStackTrace();
			}
			JCheckBoxMenuItem item = new JCheckBoxMenuItem(frame.data.getFsm().getDescription());
			item.setSelected(frame.isMaximum());
			framesMenu.add(item);
			item.addActionListener(e -> {
				try {
					frame.setMaximum(item.isSelected());
				} catch (PropertyVetoException x) {
					x.printStackTrace();
				}
			});
			frame.addInternalFrameListener(new InternalFrameAdapter() {
				@Override
				public void internalFrameIconified(InternalFrameEvent e) {
					item.setSelected(true);
				}

				@Override
				public void internalFrameDeiconified(InternalFrameEvent e) {
					item.setSelected(false);
				}
			});
			frame.graphView.update();
		});
		Application.loginfo("Menu updated, %d entries", framesMenu.getItemCount());
	}
}