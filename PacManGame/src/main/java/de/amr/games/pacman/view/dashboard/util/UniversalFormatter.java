package de.amr.games.pacman.view.dashboard.util;

import java.awt.Color;
import java.awt.Component;
import java.util.function.Function;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class UniversalFormatter extends DefaultTableCellRenderer {

	public static class Context {
		public JTable table;
		public Object value;
		public boolean isSelected;
		public boolean hasFocus;
		public int row;
		public int column;
	}

	private Context context = new Context();
	public Color hilightColor = new Color(255, 0, 0, 100);
	public Function<Context, Boolean> fnHilightCondition = context -> false;
	public Function<Context, String> fnTextFormat = context -> context.value == null ? "" : String.valueOf(context.value);

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		context.table = table;
		context.value = value;
		context.isSelected = isSelected;
		context.hasFocus = hasFocus;
		context.row = row;
		context.column = column;
		Color bg = isSelected ? table.getSelectionBackground() : table.getBackground();
		setBackground(fnHilightCondition.apply(context) ? hilightColor : bg);
		setText(fnTextFormat.apply(context));
		return this;
	}
}