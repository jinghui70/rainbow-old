package rdm.editors;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;

import rainbow.db.model.Column;
import rainbow.db.model.Entity;
import rainbow.db.model.Index;
import rainbow.db.model.Model;

public class MvcModel extends AbstractModel {

	private Model model;

	private int col;

	private int row;

	private int inxcol;

	private int inxrow;

	private boolean dirty;
	
	private IStructuredSelection selection;
	
	private Entity curEntity;

	public void initModel(Model model) {
		this.model = model;
		selection = StructuredSelection.EMPTY;
		curEntity = null;
		dirty = false;
		fireChange();
	}

	public Model getModel() {
		return model;
	}
	
	public IStructuredSelection getSelection() {
		return selection;
	}

	public Entity getCurEntity() {
		return curEntity;
	}

	public int getCol() {
		return col;
	}

	public int getRow() {
		return row;
	}

	public int getInxcol() {
		return inxcol;
	}

	public void setInxcol(int inxcol) {
		this.inxcol = inxcol;
	}

	public int getInxrow() {
		return inxrow;
	}

	public void setInxrow(int inxrow) {
		this.inxrow = inxrow;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
		fireDirtyChange();
	}

	void dirty() {
		setDirty(true);
	}

	public void setModelName(String text) {
		model.setName(text);
		dirty();
		fireChange();
	}

	public void setEntityName(TextType type, String text) {
		if (curEntity != null) {
			switch (type) {
			case ENTITY_DBNAME:
				curEntity.setDbName(text);
				break;
			case ENTITY_CNNAME:
				curEntity.setCnName(text);
				break;
			default:
				curEntity.setName(text);
			}
			dirty();
			fireChange();
		}
	}

	public void addEntity() {
		Entity entity = new Entity();
		entity.setName("new");
		entity.setDbName("NEW");
		entity.setCnName("新数据表");
		List<Entity> entities = model.getEntities();
		if (entities == null) {
			entities = new LinkedList<Entity>();
			model.setEntities(entities);
			entities.add(entity);
		} else if (curEntity != null) {
			int index = model.getEntities().indexOf(curEntity);
			model.getEntities().add(index, entity);
		} else
			entities.add(entity);
		dirty();
		selectEntity(new StructuredSelection(entity));
	}

	public void delEntity() {
		if (selection.isEmpty())
			return;
		dirty();
		model.getEntities().removeAll(selection.toList());
		selectEntity(StructuredSelection.EMPTY);
	}

	public void copyEntity() {
		if (curEntity != null) {
			Entity newEntity = curEntity.clone();
			newEntity.setCnName(curEntity.getCnName() + " 副本");
			newEntity.setName(curEntity.getName() + "Copy");
			newEntity.setDbName(curEntity.getDbName() + "_COPY");
			int index = model.getEntities().indexOf(curEntity);
			model.getEntities().add(index + 1, newEntity);
			dirty();
			selectEntity(new StructuredSelection(newEntity));
		}
	}

	public void upEntity() {
		if (curEntity != null) {
			int index = model.getEntities().indexOf(curEntity);
			if (index > 0) {
				Entity e = model.getEntities().get(index - 1);
				model.getEntities().set(index - 1, curEntity);
				model.getEntities().set(index, e);
				dirty();
				fireChange();
			}
		}
	}

	public void downEntity() {
		if (curEntity != null) {
			int count = model.getEntities().size();
			int index = model.getEntities().indexOf(curEntity);
			if (index < count - 1) {
				Entity e = model.getEntities().get(index + 1);
				model.getEntities().set(index + 1, curEntity);
				model.getEntities().set(index, e);
				dirty();
				fireChange();
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void previewEntity() {
		Model tempModel = new Model();
		tempModel.setName(model.getName());
		tempModel.setEntities(selection.toList());
		PreviewEntityDialog dialog = new PreviewEntityDialog(null, tempModel);
		dialog.open();
	}

	public void sortEntity() {
		List<Entity> entities = model.getEntities();
		if (entities == null || entities.isEmpty())
			return;
		Collections.sort(entities, new Comparator<Entity>() {
			@Override
			public int compare(Entity first, Entity second) {
				return first.getDbName().compareToIgnoreCase(second.getDbName());
			}
		});
		dirty();
		fireChange();
	}

	public void selectEntity(IStructuredSelection selection) {
		this.selection = selection;
		Entity entity = null;
		if (!selection.isEmpty())
			entity = (Entity) selection.getFirstElement();
		if (entity != curEntity) {
			curEntity = entity;
			col = 1;
			row = curEntity == null ? 0 : curEntity.getColumns().size();
			inxcol = 1;
			inxrow = curEntity == null ? 0 : curEntity.getIndexes().size();
			fireChange();
		}
	}

	public void selectIndex(int col, int row) {
		this.inxcol = col;
		if (this.inxrow != row) {
			this.inxrow = row;
			fireChange();
		}
	}

	public void selectCell(int col, int row) {
		this.col = col;
		if (this.row != row) {
			this.row = row;
			fireChange();
		}
	}

	public void addColumn() {
		if (curEntity != null) {
			Column column = new Column();
			column.setDbName("NEW");
			curEntity.getColumns().add(row++, column);
			col = 1;
			dirty();
			fireChange();
		}
	}

	public void delColumn() {
		if (curEntity != null && row > 0) {
			curEntity.getColumns().remove(row - 1);
			int count = curEntity.getColumns().size();
			if (row > count)
				row = count;
			dirty();
			fireChange();
		}
	}

	public void copyColumn() {
		if (curEntity != null && row > 0) {
			Column column = curEntity.getColumns().get(row - 1);
			Column newColumn = column.clone();
			curEntity.getColumns().add(row++, newColumn);
			dirty();
			fireChange();
		}
	}

	public void upColumn() {
		if (curEntity != null && row > 1) {
			Column temp = curEntity.getColumns().get(row - 2);
			curEntity.getColumns().set(row - 2, curEntity.getColumns().get(row - 1));
			curEntity.getColumns().set(row - 1, temp);
			row--;
			dirty();
			fireChange();
		}
	}

	public void downColumn() {
		if (curEntity != null) {
			int count = curEntity.getColumns().size();
			if (row < count) {
				Column temp = curEntity.getColumns().get(row);
				curEntity.getColumns().set(row, curEntity.getColumns().get(row - 1));
				curEntity.getColumns().set(row - 1, temp);
				row++;
				dirty();
				fireChange();
			}
		}
	}

	public void addIndex() {
		if (curEntity != null) {
			Index index = new Index();
			index.setName("NEW");
			curEntity.getIndexes().add(index);
			inxrow = curEntity.getIndexes().size();
			inxcol = 1;
			dirty();
			fireChange();
		}
	}

	public void delIndex() {
		if (curEntity != null && inxrow > 0) {
			curEntity.getIndexes().remove(inxrow - 1);
			int count = curEntity.getIndexes().size();
			if (inxrow > count)
				inxrow = count;
			dirty();
			fireChange();
		}
	}

	public void copyIndex() {
		if (curEntity != null && inxrow > 0) {
			Index index = curEntity.getIndexes().get(inxrow - 1);
			Index newIndex = index.clone();
			newIndex.setName(newIndex.getName() + "_COPY");
			curEntity.getIndexes().add(newIndex);
			inxrow = curEntity.getIndexes().size();
			dirty();
			fireChange();
		}
	}

	public void upIndex() {
		if (curEntity != null && inxrow > 1) {
			Index temp = curEntity.getIndexes().get(inxrow - 2);
			curEntity.getIndexes().set(inxrow - 2, curEntity.getIndexes().get(inxrow - 1));
			curEntity.getIndexes().set(inxrow - 1, temp);
			inxrow--;
			dirty();
			fireChange();
		}
	}

	public void downIndex() {
		if (curEntity != null) {
			int count = curEntity.getIndexes().size();
			if (inxrow < count) {
				Index temp = curEntity.getIndexes().get(inxrow);
				curEntity.getIndexes().set(inxrow, curEntity.getIndexes().get(inxrow - 1));
				curEntity.getIndexes().set(inxrow - 1, temp);
				inxrow++;
				dirty();
				fireChange();
			}
		}
	}

	public void editIndex() {
		Index index = curEntity.getIndexes().get(inxrow - 1);
		EditIndexDialog dialog = new EditIndexDialog(null, index, curEntity);
		if (dialog.open() == Window.OK) {
			dirty();
		}
	}

}