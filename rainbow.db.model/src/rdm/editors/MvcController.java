package rdm.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableCellSelectionListener;

public class MvcController implements ModifyListener, ISelectionChangedListener, KTableCellSelectionListener {

	private MvcModel model;

	private ModelEditor view;

	boolean userInput = true;

	public MvcController(ModelEditor view, MvcModel model) {
		this.view = view;
		this.model = model;
	}

	public Text makeText(TextType textType, Composite parent) {
		Text text = textType.create(parent);
		text.addModifyListener(this);
		return text;
	}

	@Override
	public void modifyText(ModifyEvent e) {
		if (userInput) {
			Text text = (Text) e.widget;
			TextType type = (TextType) (text.getData());
			if (type == TextType.MODEL_NAME)
				model.setModelName(text.getText());
			else
				model.setEntityName(type, text.getText());
		}
	}

	public Action makeAction(final ActionType actionType) {
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
		view.columnTable.closeEditor();
		view.indexTable.closeEditor();
		switch (actionType) {
		case ACTION_ENTITY_ADD:
			model.addEntity();
			break;
		case ACTION_ENTITY_DEL:
			model.delEntity();
			break;
		case ACTION_ENTITY_COPY:
			model.copyEntity();
			break;
		case ACTION_ENTITY_UP:
			model.upEntity();
			break;
		case ACTION_ENTITY_DOWN:
			model.downEntity();
			break;
		case ACTION_ENTITY_PREVIEW:
			model.previewEntity();
			break;
		case ACTION_ENTITY_SORT:
			model.sortEntity();
			break;
		case ACTION_COLUMN_ADD:
			model.addColumn();
			break;
		case ACTION_COLUMN_DEL:
			model.delColumn();
			break;
		case ACTION_COLUMN_COPY:
			model.copyColumn();
			break;
		case ACTION_COLUMN_UP:
			model.upColumn();
			break;
		case ACTION_COLUMN_DOWN:
			model.downColumn();
			break;
		case ACTION_INDEX_ADD:
			model.addIndex();
			break;
		case ACTION_INDEX_DEL:
			model.delIndex();
			break;
		case ACTION_INDEX_COPY:
			model.copyIndex();
			break;
		case ACTION_INDEX_UP:
			model.upIndex();
			break;
		case ACTION_INDEX_DOWN:
			model.downIndex();
			break;
		case ACTION_INDEX_EDIT:
			model.editIndex();
			break;
		default:
			break;
		}
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		if (userInput) {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			model.selectEntity(selection);
		}
	}

	@Override
	public void cellSelected(KTable table, int col, int row, int statemask) {
		if (userInput) {
			if (Integer.valueOf(0).equals(table.getData()))
				model.selectCell(col, row);
			else
				model.selectIndex(col, row);
		}
	}

	@Override
	public void fixedCellSelected(KTable table, int col, int row, int statemask) {
	}
}
