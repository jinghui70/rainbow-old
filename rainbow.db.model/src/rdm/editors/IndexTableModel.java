package rdm.editors;

import de.kupzog.ktable.KTableCellEditor;
import de.kupzog.ktable.KTableCellRenderer;
import de.kupzog.ktable.KTableDefaultModel;
import de.kupzog.ktable.editors.KTableCellEditorCheckbox;
import de.kupzog.ktable.editors.KTableCellEditorText;
import de.kupzog.ktable.renderers.CheckableCellRenderer;
import de.kupzog.ktable.renderers.DefaultCellRenderer;
import de.kupzog.ktable.renderers.FixedCellRenderer;
import de.kupzog.ktable.renderers.TextCellRenderer;
import rainbow.db.model.Entity;
import rainbow.db.model.Index;

public class IndexTableModel extends KTableDefaultModel {

    private static final String[] head = { "序号", "索引名", "唯一" };

    public static final int INX_INX = 0;
    public static final int INX_NAME = 1;
    public static final int INX_UNIQUE = 2;

    protected KTableCellRenderer fixCellRenderer = new FixedCellRenderer(DefaultCellRenderer.INDICATION_FOCUS_ROW);
    protected KTableCellRenderer cellRenderer = new TextCellRenderer(DefaultCellRenderer.INDICATION_FOCUS_ROW);
    protected KTableCellRenderer booleanCellRenderer = new CheckableCellRenderer(
            DefaultCellRenderer.INDICATION_FOCUS_ROW);

    protected KTableCellEditor textCellEditor = new KTableCellEditorText(-1);
    protected KTableCellEditor checkboxCellEditor = new KTableCellEditorCheckbox();

    private MvcModel model;

    public IndexTableModel(MvcModel model) {
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
        case INX_INX:
        case INX_UNIQUE:
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
        Index index = model.getCurEntity().getIndexes().get(row - 1);
        switch (col) {
        case INX_NAME:
            return index.getName();
        case INX_UNIQUE:
            return index.isUnique();
        default:
            return null;
        }
    }

    @Override
    public KTableCellEditor doGetCellEditor(int col, int row) {
        if (col < 1 || row < 1)
            return null;
        switch (col) {
        case INX_UNIQUE:
            return checkboxCellEditor;
        default:
            return textCellEditor;
        }
    }

    @Override
    public void doSetContentAt(int col, int row, Object value) {
        boolean changed = true;
        Index index = model.getCurEntity().getIndexes().get(row - 1);
        switch (col) {
        case INX_NAME:
            index.setName((String) value);
            break;
        case INX_UNIQUE:
            boolean unique = (Boolean) value;
            if (unique)
                index.setUnique(true);
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
        case INX_UNIQUE:
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
            return entity.getIndexes().size() + 1;
    }

    @Override
    public int doGetColumnCount() {
        return head.length;
    }

}
