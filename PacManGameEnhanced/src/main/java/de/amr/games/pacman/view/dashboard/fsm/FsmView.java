package de.amr.games.pacman.view.dashboard.fsm;

import static de.amr.easy.game.Application.loginfo;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeSelectionModel;

import de.amr.easy.game.controller.Lifecycle;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;

public class FsmView extends JPanel implements Lifecycle {

	static final String GRAPHVIZ_ONLINE_URL = "https://dreampuf.github.io/GraphvizOnline";

	private Action actionViewOnline = new AbstractAction("View Online") {

		@Override
		public void actionPerformed(ActionEvent e) {
			tree.getSelectedData().ifPresent(data -> {
				try {
					URI uri = new URI(null, GRAPHVIZ_ONLINE_URL, data.getGraph());
					Desktop.getDesktop().browse(uri);
				} catch (Exception x) {
					x.printStackTrace();
				}
			});
		}
	};

	private Action actionSave = new AbstractAction("Save") {
		@Override
		public void actionPerformed(ActionEvent e) {
			tree.getSelectedData().ifPresent(data -> {
				saveFile(data);
			});
		}
	};

	private Action actionOpenDashboard = new AbstractAction("Open Dashboard") {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (dashboard == null) {
				dashboard = new FsmDashboard(model);
			}
			dashboard.window.setVisible(true);
		};
	};

	private FsmModel model = new FsmModel();

	private FsmDashboard dashboard;
	private FsmTree tree;
	private JTree treeView;
	private JToolBar toolBar;
	private JButton btnViewOnline;
	private JButton btnSave;
	private JButton btnZoomIn;
	private JButton btnZoomOut;
	private JButton btnOpenDashboard;
	private JTabbedPane tabbedPane;
	private FsmTextView fsmEmbeddedTextView;
	private FsmGraphView fsmEmbeddedGraphView;

	public FsmView() {
		setLayout(new BorderLayout(0, 0));

		JSplitPane splitPane = new JSplitPane();
		splitPane.setOneTouchExpandable(true);
		add(splitPane, BorderLayout.CENTER);

		JScrollPane fsmTreeScrollPane = new JScrollPane();
		fsmTreeScrollPane.setMinimumSize(new Dimension(200, 25));
		splitPane.setLeftComponent(fsmTreeScrollPane);

		treeView = new JTree();
		fsmTreeScrollPane.setViewportView(treeView);
		tree = new FsmTree();
		treeView.setModel(tree);
		treeView.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		treeView.addTreeSelectionListener(this::onTreeViewSelectionChange);

		tabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);
		splitPane.setRightComponent(tabbedPane);

		fsmEmbeddedTextView = new FsmTextView();
		fsmEmbeddedGraphView = new FsmGraphView();

		tabbedPane.addTab("Graph", null, fsmEmbeddedGraphView, null);
		tabbedPane.addTab("Source", null, fsmEmbeddedTextView, null);

		toolBar = new JToolBar();
		add(toolBar, BorderLayout.NORTH);

		btnSave = new JButton("New button");
		btnSave.setAction(actionSave);
		toolBar.add(btnSave);

		btnOpenDashboard = new JButton("Open Dashboard");
		btnOpenDashboard.setAction(actionOpenDashboard);
		toolBar.add(btnOpenDashboard);

		btnViewOnline = new JButton("Preview");
		toolBar.add(btnViewOnline);
		btnViewOnline.setAction(actionViewOnline);

		btnZoomIn = new JButton("New button");
		btnZoomIn.setAction(fsmEmbeddedGraphView.actionZoomIn);
		toolBar.add(btnZoomIn);

		btnZoomOut = new JButton("");
		btnZoomOut.setAction(fsmEmbeddedGraphView.actionZoomOut);
		toolBar.add(btnZoomOut);
	}

	private void onTreeViewSelectionChange(TreeSelectionEvent e) {
		tree.setSelectedPath(e.getNewLeadSelectionPath());
		FsmData node = tree.getSelectedData().orElse(null);
		if (node != null) {
			node.updateGraph();
		}
	}

	@Override
	public void init() {
		// not used
	}

	@Override
	public void update() {
		model.checkIfValid();
		if (!model.isValid()) {
			model.rebuild();
			tree.rebuild(model);
			treeView.setSelectionPath(tree.getSelectedPath());
			if (dashboard != null) {
				dashboard.update();
			}
		}
		Optional<FsmData> selection = tree.getSelectedData();
		if (selection.isPresent()) {
			FsmData data = selection.get();
			data.updateGraph();
			fsmEmbeddedGraphView.setData(data);
			fsmEmbeddedTextView.setData(data);
			actions().forEach(action -> action.setEnabled(true));
		} else {
			fsmEmbeddedGraphView.setData(null);
			fsmEmbeddedTextView.setData(null);
			actions().forEach(action -> action.setEnabled(false));
		}
	}

	private Stream<Action> actions() {
		Stream<Action> actions = Stream.of(actionViewOnline, actionSave, fsmEmbeddedGraphView.actionZoomIn,
				fsmEmbeddedGraphView.actionZoomOut);
		return actions;
	}

	private void saveFile(FsmData data) {
		JFileChooser saveDialog = new JFileChooser();
		saveDialog.setDialogTitle("Save state machine as DOT file");
		saveDialog.setSelectedFile(new File(data.getFsm().getDescription()));
		int option = saveDialog.showSaveDialog(this);
		if (option == JFileChooser.APPROVE_OPTION) {
			File file = saveDialog.getSelectedFile();
			if (file.getName().endsWith(".dot")) {
				try (FileWriter w = new FileWriter(saveDialog.getSelectedFile())) {
					w.write(data.getGraph());
				} catch (Exception x) {
					loginfo("DOT file could not be written", file);
				}
			} else if (file.getName().endsWith(".png")) {
				BufferedImage png = Graphviz.fromString(data.getGraph()).render(Format.PNG).toImage();
				try {
					ImageIO.write(png, "png", file);
				} catch (IOException x) {
					loginfo("PNG file could not be written", file);
				}
			}
		}
	}
}