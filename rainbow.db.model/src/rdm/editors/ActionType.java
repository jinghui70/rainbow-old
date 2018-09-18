package rdm.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

import rdm.Activator;

public enum ActionType {

    ACTION_ENTITY_ADD("add"), //
    ACTION_ENTITY_SORT("sort"),
    ACTION_ENTITY_DEL("delete"), //
    ACTION_ENTITY_COPY("copy"),
    ACTION_ENTITY_UP("up"),
    ACTION_ENTITY_DOWN("down"),
    ACTION_ENTITY_PREVIEW("preview"),

    ACTION_COLUMN_ADD("add"),
    ACTION_COLUMN_DEL("delete"),
    ACTION_COLUMN_COPY("copy"),
    ACTION_COLUMN_UP("up"),
    ACTION_COLUMN_DOWN("down"),

    ACTION_INDEX_ADD("add"),
    ACTION_INDEX_DEL("delete"),
    ACTION_INDEX_COPY("copy"),
    ACTION_INDEX_UP("up"),
    ACTION_INDEX_DOWN("down"),
    ACTION_INDEX_EDIT("edit"),
    
    ACTION_INXCOLUMN_ADD("add"),
    ACTION_INXCOLUMN_DEL("delete"),
    ACTION_INXCOLUMN_UP("up"),
    ACTION_INXCOLUMN_DOWN("down");

    String image;

    Action instance;

    ActionType(String image) {
        this.image = image;
    }

    public ImageDescriptor getImageDescriptor() {
        return Activator.getImageDescriptor("/icons/" + image + ".gif");
    }

    public void setInstance(Action action) {
        this.instance = action;
    }

    public Action instance() {
        return instance;
    }

    public void setEnabled(boolean enabled) {
        if (instance != null)
            instance.setEnabled(enabled);
    }
}
