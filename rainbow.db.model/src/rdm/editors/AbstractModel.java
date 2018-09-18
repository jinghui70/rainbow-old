package rdm.editors;

import java.util.ArrayList;
import java.util.List;

public class AbstractModel {

    private List<ModelChangeListener> listeners = new ArrayList<ModelChangeListener>(1);

    public void addListener(ModelChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ModelChangeListener listener) {
        listeners.remove(listener);
    }

    protected void fireChange() {
        if (!listeners.isEmpty()) {
            for (ModelChangeListener listener : listeners)
                listener.modelChange();
        }
    }
    
    protected void fireDirtyChange() {
        if (!listeners.isEmpty()) {
            for (ModelChangeListener listener : listeners)
                listener.modelDirty();
        }
    }

}
