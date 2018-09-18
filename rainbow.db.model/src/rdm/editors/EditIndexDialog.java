package rdm.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableDefaultModel;
import de.kupzog.ktable.SWTX;
import rainbow.db.model.Entity;
import rainbow.db.model.Index;
import rainbow.db.model.IndexColumn;

public class EditIndexDialog extends Dialog {

	private Index index;
	private Index newIndex;
	private Entity entity;
	private KTableDefaultModel inxcolumnTableModel;
	private KTable inxcolumnTable;
	private int row;

	public EditIndexDialog(Shell parentShell, Index index, Entity entity) {
		super(parentShell);
		this.index = index;
		this.newIndex = index.clone();
		this.entity = entity;
	}

	private Action makeAction(final ActionType actionType) {
		Action action = new Action() {
			@Override
			public void run() {
				runAction(actionType);
			}
		};
		action.setImageDescriptor(actionType.getImageDescriptor());
		actionType.setInstance(action);
		return actionType.instance();
	}

	private void runAction(ActionType actionType) {
		inxcolumnTable.closeEditor();
		if (inxcolumnTable.getRowSelection()==null) {
			if (inxcolumnTable.getCellSelection()!=null)
				row = inxcolumnTable.getCellSelection()[0].y-1;
		} else
			row = inxcolumnTable.getRowSelection()[0]-1;
		switch (actionType) {
		case ACTION_INXCOLUMN_ADD:
			addInxColumn();
			break;
		case ACTION_INXCOLUMN_DEL:
			delInxColumn();
			break;
		case ACTION_INXCOLUMN_UP:
			upInxColumn();
			break;
		case ACTION_INXCOLUMN_DOWN:
			downInxColumn();
			break;
		default:
			break;
		}
	}

	private void addInxColumn() {
		IndexColumn inxcolumn = new IndexColumn();
		inxcolumn.setAsc(true);
		newIndex.getInxColumns().add(inxcolumn);
		row = newIndex.getInxColumns().size();
		inxcolumnTable.setSelection(1, row, true);
		inxcolumnTable.redraw();
	}

	private void delInxColumn() {
		if (row >= 0) {
			newIndex.getInxColumns().remove(row);
			int count = newIndex.getInxColumns().size();
			if (row > count)
				row = count;
			inxcolumnTable.setSelection(1, row, true);
			inxcolumnTable.redraw();
		}

	}

	private void upInxColumn() {
		if (row > 1) {
			IndexColumn temp = newIndex.getInxColumns().get(row - 2);
			newIndex.getInxColumns().set(row - 2, newIndex.getInxColumns().get(row - 1));
			newIndex.getInxColumns().set(row - 1, temp);
			row--;
			inxcolumnTable.setSelection(1, row, true);
			inxcolumnTable.redraw();
		}
	}

	private void downInxColumn() {
		int count = newIndex.getInxColumns().size();
		if (row < count) {
			IndexColumn temp = newIndex.getInxColumns().get(row);
			newIndex.getInxColumns().set(row, newIndex.getInxColumns().get(row - 1));
			newIndex.getInxColumns().set(row - 1, temp);
			row++;
			inxcolumnTable.setSelection(1, row, true);
			inxcolumnTable.redraw();
		}
	}

	@Override
	protected Point getInitialSize() {
		return new Point(300, 400);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(String.format("编辑索引[%s]字段", index.getName()));
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		applyDialogFont(composite);

		ToolBar toolbar = new ToolBar(composite, SWT.HORIZONTAL);
		toolbar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		ToolBarManager tbm = new ToolBarManager(toolbar);
		tbm.add(makeAction(ActionType.ACTION_INXCOLUMN_ADD));
		tbm.add(makeAction(ActionType.ACTION_INXCOLUMN_DEL));
		tbm.add(makeAction(ActionType.ACTION_INXCOLUMN_UP));
		tbm.add(makeAction(ActionType.ACTION_INXCOLUMN_DOWN));
		tbm.update(true);

		inxcolumnTableModel = new IndexColumnTableModel(newIndex, entity);
		inxcolumnTable = new KTable(composite, SWTX.AUTO_SCROLL | SWTX.EDIT_ON_KEY | SWT.BORDER);
		inxcolumnTable.setModel(inxcolumnTableModel);
		inxcolumnTable.setLayoutData(new GridData(GridData.FILL_BOTH));

		return composite;
	}

	@Override
	protected void okPressed() {
		index.setInxColumns(newIndex.getInxColumns());
		super.okPressed();
	}

}
