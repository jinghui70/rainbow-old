package rdm.editors;

import de.kupzog.ktable.KTableCellEditor;
import de.kupzog.ktable.KTableCellRenderer;
import de.kupzog.ktable.KTableDefaultModel;
import de.kupzog.ktable.editors.KTableCellEditorCheckbox;
import de.kupzog.ktable.editors.KTableCellEditorComboEnum;
import de.kupzog.ktable.editors.KTableCellEditorMultilineText;
import de.kupzog.ktable.editors.KTableCellEditorText;
import de.kupzog.ktable.renderers.CheckableCellRenderer;
import de.kupzog.ktable.renderers.DefaultCellRenderer;
import de.kupzog.ktable.renderers.FixedCellRenderer;
import de.kupzog.ktable.renderers.TextCellRenderer;
import rainbow.db.model.Column;
import rainbow.db.model.ColumnType;
import rainbow.db.model.Entity;

public class ColumnTableModel extends KTableDefaultModel {

	private static final String[] head = { "序号", "字段名", "中文名", "类型", "长度", "精度", "主键", "非空", "属性名", "描述" };

	public static final int COL_INX = 0;
	public static final int COL_DBNAME = 1;
	public static final int COL_CNNAME = 2;
	public static final int COL_TYPE = 3;
	public static final int COL_LENGTH = 4;
	public static final int COL_PRECISION = 5;
	public static final int COL_KEY = 6;
	public static final int COL_MANDATORY = 7;
	public static final int COL_NAME = 8;
	public static final int COL_COMMENT = 9;

	protected KTableCellRenderer fixCellRenderer = new FixedCellRenderer(DefaultCellRenderer.INDICATION_FOCUS_ROW);
	protected KTableCellRenderer cellRenderer = new TextCellRenderer(DefaultCellRenderer.INDICATION_FOCUS_ROW);
	protected KTableCellRenderer booleanCellRenderer = new CheckableCellRenderer(
			DefaultCellRenderer.INDICATION_FOCUS_ROW);

	protected KTableCellEditor textCellEditor = new KTableCellEditorText(-1);
	protected KTableCellEditor typeCellEditor = new KTableCellEditorComboEnum<ColumnType>(ColumnType.class);
	protected KTableCellEditor checkboxCellEditor = new KTableCellEditorCheckbox();
	protected KTableCellEditor multiTextCellEditor = new KTableCellEditorMultilineText();
	
	private MvcModel model;

	public ColumnTableModel(MvcModel model) {
		super();
		this.model = model;
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
		case COL_KEY:
		case COL_MANDATORY:
			return 40;
		case COL_PRECISION:
			return 40;
		case COL_COMMENT:
			return 100;
		default:
			return 80;
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
		Column column = model.getCurEntity().getColumns().get(row - 1);
		switch (col) {
		case COL_DBNAME:
			return column.getDbName();
		case COL_CNNAME:
			return column.getCnName();
		case COL_TYPE:
			return column.getType();
		case COL_LENGTH:
			return column.getLength();
		case COL_PRECISION:
			return column.getPrecision();
		case COL_KEY:
			return column.isKey();
		case COL_MANDATORY:
			return column.isMandatory();
		case COL_NAME:
			return column.getName();
		case COL_COMMENT:
			return column.getComment();
		default:
			return null;
		}
	}

	@Override
	public KTableCellEditor doGetCellEditor(int col, int row) {
		if (col < 1 || row < 1)
			return null;
		switch (col) {
		case COL_TYPE:
			return typeCellEditor;
		case COL_KEY:
		case COL_MANDATORY:
			return checkboxCellEditor;
		case COL_COMMENT:
			return multiTextCellEditor;
		default:
			return textCellEditor;
		}
	}

	@Override
	public void doSetContentAt(int col, int row, Object value) {
		boolean changed = true;
		Column column = model.getCurEntity().getColumns().get(row - 1);
		switch (col) {
		case COL_DBNAME:
			column.setDbName((String) value);
			break;
		case COL_CNNAME:
			column.setCnName((String) value);
			break;
		case COL_TYPE:
			column.setType((ColumnType) value);
			break;
		case COL_LENGTH:
			try {
				column.setLength(Integer.parseInt((String) value));
			} catch (NumberFormatException e) {
				changed = false;
			}
			break;
		case COL_PRECISION:
			try {
				column.setPrecision(Integer.parseInt((String) value));
			} catch (NumberFormatException e) {
				changed = false;
			}
			break;
		case COL_KEY:
			boolean key = (Boolean) value;
			if (key)
				column.setMandatory(true);
			column.setKey(key);
			break;
		case COL_MANDATORY:
			boolean mandatory = (Boolean) value;
			if (column.isKey() && !mandatory)
				changed = false;
			else
				column.setMandatory(mandatory);
			break;
		case COL_NAME:
			String name = (String) value;
			if (name.length() > 0) {
				if (Character.isUpperCase(name.charAt(0))) {
					name = new StringBuilder(name.length()).append(Character.toLowerCase(name.charAt(0)))
							.append(name.substring(1)).toString();
				}
			}
			if (name.equals(column.getName()))
				changed = false;
			else
				column.setName(name);
			break;
		case COL_COMMENT:
			column.setComment((String) value);
			break;
		}
		if (changed)
			model.dirty();
	}

	@Override
	public KTableCellRenderer doGetCellRenderer(int col, int row) {
		if (row == 0 || col == 0)
			return fixCellRenderer;
		switch (col) {
		case COL_KEY:
		case COL_MANDATORY:
			return booleanCellRenderer;
		default:
			return cellRenderer;
		}
	}

	@Override
	public int doGetRowCount() {
		Entity entity = model.getCurEntity();
		if (entity == null)
			return 1;
		else
			return entity.getColumns().size() + 1;
	}

	@Override
	public int doGetColumnCount() {
		return head.length;
	}

}
