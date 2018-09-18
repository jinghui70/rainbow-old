package rdm.editors;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import rainbow.db.model.Entity;

public class EntityLabelProvider extends LabelProvider implements ITableLabelProvider {

    @Override
    public Image getColumnImage(Object element, int columnIndex) {
        return null;
    }

    @Override
    public String getColumnText(Object element, int columnIndex) {
        Entity entity = (Entity) element;
        switch (columnIndex) {
        case 0:
            return entity.getDbName();
        case 1:
            return entity.getCnName();
        default:
            return entity.getName();
        }
    }
}
