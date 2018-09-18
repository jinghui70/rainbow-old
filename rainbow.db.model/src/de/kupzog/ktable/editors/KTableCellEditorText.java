/*
 * Copyright (C) 2004 by Friederich Kupzog Elektronik & Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
    
    Author: Friederich Kupzog  
    fkmk@kupzog.de
    www.kupzog.de/fkmk
 */

package de.kupzog.ktable.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableCellEditor;

/**
 * A simple cell editor that simply creates a text widget that allows the user
 * to type in one line of text.
 * <p>
 * This class is very similar to <code>KTableCellEditorText</code>, but allows
 * the navigation within the text widget using ARROW_LEFT and ARROW_RIGHT keys.
 * 
 * @see de.kupzog.ktable.editors.KTableCellEditorText
 * @author Lorenz Maierhofer <lorenz.maierhofer@logicmindguide.com>
 */
public class KTableCellEditorText extends KTableCellEditor {
	protected Text m_Text;

	private int limit;

	private VerifyListener verifyListener;

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

	public KTableCellEditorText(int limit) {
		this.limit = limit;
	}

	public KTableCellEditorText(VerifyListener verifyListener) {
		this.verifyListener = verifyListener;
	}

	public KTableCellEditorText(int limit, VerifyListener verifyListener) {
		this.limit = limit;
		this.verifyListener = verifyListener;
	}

	@Override
	public void open(KTable table, int col, int row, Rectangle rect) {
		super.open(table, col, row, rect);
		Object object = m_Model.getContentAt(m_Col, m_Row, false);
		m_Text.setText(object == null ? "" : object.toString());
		m_Text.selectAll();
		m_Text.setVisible(true);
		m_Text.setFocus();
	}

	@Override
	public void close(boolean save) {
		if (save)
			m_Model.setContentAt(m_Col, m_Row, m_Text.getText());
		m_Text.removeKeyListener(keyListener);
		m_Text.removeTraverseListener(travListener);
		if (verifyListener != null)
			m_Text.removeVerifyListener(verifyListener);
		super.close(save);
		m_Text = null;
	}

	@Override
	protected Control createControl() {
		m_Text = new Text(m_Table, SWT.NONE);
		if (limit > 0)
			m_Text.setTextLimit(limit);
		m_Text.addKeyListener(keyListener);
		m_Text.addTraverseListener(travListener);
		if (verifyListener != null)
			m_Text.addVerifyListener(verifyListener);
		return m_Text;
	}

	/**
	 * Implement In-Textfield navigation with the keys...
	 * 
	 * @see de.kupzog.ktable.KTableCellEditor#onTraverse(org.eclipse.swt.events.TraverseEvent)
	 */
	@Override
	protected void onTraverse(TraverseEvent e) {
		if (e.keyCode == SWT.ARROW_LEFT || e.keyCode == SWT.ARROW_RIGHT) {
			e.doit = false;
		} else
			super.onTraverse(e);
	}

	/*
	 * overridden from superclass
	 */
	@Override
	public void setBounds(Rectangle rect) {
		super.setBounds(new Rectangle(rect.x, rect.y + (rect.height - 15) / 2 + 1, rect.width, 15));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.kupzog.ktable.KTableCellEditor#setContent(java.lang.Object)
	 */
	@Override
	public void setContent(Object content) {
		m_Text.setText(content.toString());
		m_Text.setSelection(content.toString().length());
	}

}