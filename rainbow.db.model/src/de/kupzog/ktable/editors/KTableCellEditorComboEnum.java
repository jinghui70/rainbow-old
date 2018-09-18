package de.kupzog.ktable.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableCellEditor;

/**
 * A combobox cell editor.
 * 
 */
public class KTableCellEditorComboEnum<E extends Enum<E>> extends KTableCellEditor {
    private CCombo m_Combo;
    private Cursor m_ArrowCursor;

    private Class<E> enumType;

    public KTableCellEditorComboEnum(Class<E> enumType) {
        super();
        this.enumType = enumType;
    }

    private KeyAdapter keyListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            try {
                onKeyPressed(e);
            } catch (Exception ex) {
            }
        }
    };

    private TraverseListener travListener = new TraverseListener() {
        public void keyTraversed(TraverseEvent e) {
            onTraverse(e);
        }
    };

    @Override
    public void open(KTable table, int row, int col, Rectangle rect) {
        m_ArrowCursor = new Cursor(Display.getDefault(), SWT.CURSOR_ARROW);
        super.open(table, row, col, rect);
        setContent(m_Model.getContentAt(m_Col, m_Row, false));
        m_Combo.setListVisible(true);
    }

    @Override
    public void close(boolean save) {
        if (save)
            m_Model.setContentAt(m_Col, m_Row, getContent());
        m_Combo.removeKeyListener(keyListener);
        m_Combo.removeTraverseListener(travListener);
        super.close(save);
        m_Combo = null;
        m_ArrowCursor.dispose();
    }

    @Override
    protected Control createControl() {
        m_Combo = new CCombo(m_Table, SWT.READ_ONLY);
        m_Combo.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

        E[] all = enumType.getEnumConstants();
        String[] strings = new String[all.length];
        for (int i = 0; i < all.length; i++)
            strings[i] = all[i].toString();
        m_Combo.setItems(strings);

        m_Combo.addKeyListener(keyListener);
        m_Combo.addTraverseListener(travListener);

        m_Combo.setCursor(m_ArrowCursor);
        return m_Combo;
    }

    /**
     * Overwrite the onTraverse method to ignore arrowup and arrowdown events so
     * that they get interpreted by the editor control.
     * <p>
     * Comment that out if you want the up and down keys move the editor.<br>
     * Hint by David Sciamma.
     */
    @Override
    protected void onTraverse(TraverseEvent e) {
        switch (e.keyCode) {
        case SWT.ARROW_UP:
        case SWT.ARROW_DOWN: {
            break;
        }
        default:
            super.onTraverse(e);
        }
    }

    @Override
    public void setBounds(Rectangle rect) {
        super.setBounds(new Rectangle(rect.x, rect.y + 1, rect.width, rect.height - 2));
    }

    public E getContent() {
        int index = m_Combo.getSelectionIndex();
        if (index == -1)
            return null;
        return enumType.getEnumConstants()[index];
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setContent(Object content) {
        if (content == null)
            m_Combo.select(-1);
        else if (content.getClass() == enumType) {
            m_Combo.select(((E) content).ordinal());
        }
    }

}
