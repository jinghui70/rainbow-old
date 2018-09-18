package rdm.editors;

import de.kupzog.ktable.KTableCellEditor;
import de.kupzog.ktable.KTableCellRenderer;
import de.kupzog.ktable.KTableDefaultModel;
import de.kupzog.ktable.editors.KTableCellEditorCheckbox;
import de.kupzog.ktable.editors.KTableCellEditorCombo;
import de.kupzog.ktable.renderers.CheckableCellRenderer;
import de.kupzog.ktable.renderers.DefaultCellRenderer;
import de.kupzog.ktable.renderers.FixedCellRenderer;
import de.kupzog.ktable.renderers.TextCellRenderer;
import rainbow.db.model.Column;
import rainbow.db.model.Entity;
import rainbow.db.model.Index;
import rainbow.db.model.IndexColumn;

public class IndexColumnTableModel extends KTableDefaultModel {

	private static final String[] head = { "序号", "字段名", "升序" };

	public static final int COL_INX = 0;
	public static final int COL_NAME = 1;
	public static final int COL_ASC = 2;

	protected KTableCellRenderer fixCellRenderer = new FixedCellRenderer(DefaultCellRenderer.INDICATION_FOCUS_ROW);
	protected KTableCellRenderer cellRenderer = new TextCellRenderer(DefaultCellRenderer.INDICATION_FOCUS_ROW);
	protected KTableCellRenderer booleanCellRenderer = new CheckableCellRenderer(
			DefaultCellRenderer.INDICATION_FOCUS_ROW);

	protected KTableCellEditorCombo columnCellEditor;
	protected KTableCellEditor checkboxCellEditor = new KTableCellEditorCheckbox();

	private Index index;
	private String[] items;

	public IndexColumnTableModel(Index index, Entity entity) {
		super();
		this.index = index;
		columnCellEditor = new KTableCellEditorCombo();
		items = new String[entity.getColumns().size()];
		int i = 0;
		for (Column column : entity.getColumns()) {
			items[i] = column.getDbName();
			i++;
		}
		columnCellEditor.setItems(items);
	}

	@Override
	public int getFixedHeaderRowCount() {
		return 1;
	}

	@Override
	public int getFixedSelectableRowCount() {
		return 0;
	}

	@Override
	public int getFixedHeaderColumnCount() {
		return 1;
	}

	@Override
	public int getFixedSelectableColumnCount() {
		return 0;
	}

	@Override
	public boolean isColumnResizable(int col) {
		return true;
	}

	@Override
	public boolean isRowResizable(int row) {
		return false;
	}

	@Override
	public int getRowHeightMinimum() {
		return 0;
	}

	@Override
	public int getInitialColumnWidth(int column) {
		switch (column) {
		case COL_INX:
		case COL_ASC:
			return 40;
		default:
			return 100;
		}
	}

	@Override
	public int getInitialRowHeight(int row) {
		return 22;
	}

	@Override
	public Object doGetContentAt(int col, int row, boolean show) {
		if (row == 0)
			return head[col];
		if (col == 0)
			return Integer.toString(row);
		IndexColumn indexColumn = index.getInxColumns().get(row - 1);
		switch (col) {
		case COL_NAME:
			return indexColumn.getName();
		case COL_ASC:
			return indexColumn.isAsc();
		default:
			return null;
		}
	}

	@Override
	public KTableCellEditor doGetCellEditor(int col, int row) {
		if (col < 1 || row < 1)
			return null;
		switch (col) {
		case COL_ASC:
			return checkboxCellEditor;
		default:
			return columnCellEditor;
		}
	}

	@Override
	public void doSetContentAt(int col, int row, Object value) {
		IndexColumn indexColumn = index.getInxColumns().get(row - 1);
		switch (col) {
		case COL_NAME:
			indexColumn.setName(items[(Integer) value]);
			break;
		case COL_ASC:
			indexColumn.setAsc((Boolean) value);
			break;
		}
	}

	@Override
	public KTableCellRenderer doGetCellRenderer(int col, int row) {
		if (row == 0 || col == 0)
			return fixCellRenderer;
		switch (col) {
		case COL_ASC:
			return booleanCellRenderer;
		default:
			return cellRenderer;
		}
	}

	@Override
	public int doGetRowCount() {
		if (index == null)
			return 1;
		else
			return index.getInxColumns().size() + 1;
	}

	@Override
	public int doGetColumnCount() {
		return head.length;
	}

}
