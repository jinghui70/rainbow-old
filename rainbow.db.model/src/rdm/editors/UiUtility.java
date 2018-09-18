package rdm.editors;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

/**
 * 界面的常用工具类
 * 
 * @author lijinghui
 * 
 */
public class UiUtility {

    private UiUtility() {
    }

    public static boolean confirm(String question) {
        return confirm(null, question);
    }

    public static boolean confirm(String title, String question) {
        if (title == null)
            title = "请确认";
        return MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), title, question);
    }

    public static void showInfo(String info) {
        MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "Retiasoft", info);
    }

    public static void showInfo(String title, String info) {
        MessageDialog.openInformation(Display.getCurrent().getActiveShell(), title, info);
    }

    public static void showError(String info) {
        MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "错误", info);
    }

    /**
     * 创建一个 Separator
     * 
     * @param container
     * @return
     */
    public static Label createSeparator(Composite container) {
        final Label separator = new Label(container, SWT.HORIZONTAL | SWT.SEPARATOR);
        separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return separator;
    }

    /**
     * 创建一个 Separator
     * 
     * @param container
     * @param horizontalSpan
     * @return
     */
    public static Label createSeparator(Composite container, int horizontalSpan) {
        final Label separator = new Label(container, SWT.HORIZONTAL | SWT.SEPARATOR);
        separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, horizontalSpan, 1));
        return separator;
    }

    /**
     * 创建一个Label
     * 
     * @param container
     * @param title
     * @return
     */
    public static Label createLabel(Composite container, String title) {
        final Label label = new Label(container, SWT.NONE);
        label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        label.setText(title);
        return label;
    }

    /**
     * 创建一个占位的label
     * 
     * @param container
     * @param horizontalSpan
     * @return
     */
    public static Label createPlaceHolderLabel(Composite container, int horizontalSpan) {
        if (horizontalSpan > 0) {
            final Label label = new Label(container, SWT.NONE);
            GridData gridData = new GridData(SWT.FILL, SWT.CENTER, false, false);
            gridData.horizontalSpan = horizontalSpan;
            label.setLayoutData(gridData);
            return label;
        } else
            return null;
    }

    /**
     * 创建一个label和text
     * 
     * @param container
     * @param title
     *            label的文字
     * @param limit
     *            text的最大输入值,0表示不限制
     * @param value
     *            缺省的内容
     * @return
     */
    public static Text createText(Composite container, String title, int limit, String value) {
        return createText(container, title, limit, value, 1);
    }

    /**
     * 创建一个label和text
     * 
     * @param container
     * @param title
     *            label的文字
     * @param limit
     *            text的最大输入值,0表示不限制
     * @param value
     *            缺省的内容
     * @param horizontalSpan
     *            GridData 横向占几个位置
     * @return
     */
    public static Text createText(Composite container, String title, int limit, String value, int horizontalSpan) {
        Label label = createLabel(container, title);
        Text text = new Text(container, SWT.BORDER);
        if (limit > 0)
            text.setTextLimit(limit);
        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, horizontalSpan, 1));
        if (value != null)
            text.setText(value);
        // text.addFocusListener(TextFocusListener.getInstance());
        // text.addTraverseListener(EnterTraverseListenser.getInstance());
        text.setData(label);
        return text;
    }

    /**
     * 设置一个控件的显示状态，如果控件的data存放了其label，也同时设置这个label的显示状态
     * 
     * @param control
     * @param visible
     */
    public static void setVisible(Control control, boolean visible) {
        control.setVisible(visible);
        Object obj = control.getData();
        if (obj != null && obj instanceof Label) {
            Label label = (Label) obj;
            label.setVisible(visible);
        }
    }

    /**
     * 设置一个控件的布局状态，如果控件的data存放了其label，也同时设置这个label的布局状态
     * 
     * @param control
     * @param visible
     */
    public static void setExclude(Control control, boolean exclude) {
        ((GridData) control.getLayoutData()).exclude = exclude;
        control.setVisible(!exclude);
        Object obj = control.getData();
        if (obj != null && obj instanceof Label) {
            Label label = (Label) obj;
            ((GridData) label.getLayoutData()).exclude = exclude;
            label.setVisible(!exclude);
        }
    }

    /**
     * 设置一个控件的Enable状态，如果控件的data存放了其label，也同时设置这个label的Enable状态
     * 
     * @param control
     * @param visible
     */
    public static void setEnabled(Control control, boolean enabled) {
        control.setEnabled(enabled);
        Object obj = control.getData();
        if (obj != null && obj instanceof Label) {
            Label label = (Label) obj;
            label.setEnabled(enabled);
        }
    }

    /**
     * 生成表格的一列
     * 
     * @param table
     * @param name
     * @param width
     * @return
     */
    public static TableColumn createColumn(Table table, String name, int width) {
        return createColumn(table, name, width, SWT.LEFT);
    }

    /**
     * 生成表格的一列
     * 
     * @param table
     * @param name
     * @param width
     * @return
     */
    public static TableColumn createColumn(Table table, String name, int width, int style) {
        TableColumn column = new TableColumn(table, style);
        column.setText(name);
        column.setWidth(width);
        return column;
    }

    /**
     * 生成树的一列
     * 
     * @param tree
     * @param name
     * @param width
     * @return
     */
    public static TreeColumn createColumn(Tree tree, String name, int width) {
        return createColumn(tree, name, width, SWT.LEFT);
    }

    /**
     * 生成树的一列
     * 
     * @param tree
     * @param name
     * @param width
     * @return
     */
    public static TreeColumn createColumn(Tree tree, String name, int width, int style) {
        TreeColumn column = new TreeColumn(tree, style);
        column.setText(name);
        column.setWidth(width);
        return column;
    }

    /**
     * 创建一个label和text
     * 
     * @param container
     * @param title
     *            label的文字
     * @param limit
     *            text的最大输入值,0表示不限制
     * @param value
     *            缺省的内容
     * @param horizontalSpan
     *            GridData 横向占几个位置
     * @return
     */
    public static Button createButton(Composite container, String title, int style, int horizontalSpan) {
        Button button = new Button(container, style);
        button.setText(title);
        button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, horizontalSpan, 1));
        return button;
    }

    public static void setText(Text text, String value) {
        String old = text.getText();
        if (!old.equals(value))
            text.setText(value == null ? "" : value);
    }
}
