package rdm.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public enum TextType {

    MODEL_NAME("数据源"), //
    ENTITY_DBNAME("表名"), //
    ENTITY_CNNAME("中文名"), //
    ENTITY_NAME("对象名");

    String label;

    TextType(String label) {
        this.label = label;
    }

    public Text create(Composite parent) {
        final Label label = new Label(parent, SWT.NONE);
        label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        label.setText(this.label);
        Text text = new Text(parent, SWT.BORDER);
        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        text.setData(this);
        return text;
    }

}
