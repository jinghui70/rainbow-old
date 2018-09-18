package rdm.editors;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import rainbow.db.model.Model;
import rdm.Activator;
import rdm.preferences.PreferenceConstants;

public class PreviewEntityDialog extends Dialog {

	private List<String> templateNames;

	private Map<String, Object> map = new HashMap<String, Object>();

	private byte[] content;

	private StyledText text;

	private String curTemplate = null;

	public PreviewEntityDialog(Shell parentShell, Model model) {
		super(parentShell);
		content = Model.getXmlBinder().marshal(model);

		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		String pathStr = store.getString(PreferenceConstants.P_PATH);
		File templatePath = new File(pathStr);
		if (templatePath.isDirectory() && templatePath.exists()) {
			File[] files = templatePath.listFiles(new FileFilter() {
				@Override
				public boolean accept(File file) {
					return file.getName().endsWith(".xsl");
				}
			});
			templateNames = new ArrayList<String>(files.length + 1);
			templateNames.add("SOURCE");
			for (File file : files) {
				String key = file.getName();
				key = key.substring(0, key.length() - 4);
				templateNames.add(key);
				try {
					Transformer transformer = TransformUtility.getTransformer(file);
					map.put(key, transformer);
				} catch (TransformerConfigurationException e) {
					map.put(key, e.toString());
				}
			}
		}
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 500);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("导出");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		applyDialogFont(composite);

		ListViewer list = new ListViewer(composite, SWT.V_SCROLL | SWT.BORDER);
		list.getList().setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true, 1, 1));
		// list.setLabelProvider(new LabelProvider() {
		// @Override
		// public String getText(Object element) {
		// File file = (File) element;
		// return file.getName();
		// }
		// });
		list.setContentProvider(new ArrayContentProvider());
		list.setInput(templateNames);
		list.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				StructuredSelection s = (StructuredSelection) event.getSelection();
				String key = (String) s.getFirstElement();
				if (key.equals(curTemplate))
					return;
				curTemplate = key;
				Object t = map.get(key);
				if (t == null)
					try {
						text.setText(new String(content, "UTF-8"));
					} catch (UnsupportedEncodingException e1) {
					}
				else if (t instanceof String)
					text.setText((String) t);
				else {
					try {
						StringWriter stringWriter = new StringWriter();
						((Transformer) t).transform(new StreamSource(new ByteArrayInputStream(content)),
								new StreamResult(stringWriter));
						text.setText(stringWriter.toString());
					} catch (TransformerException e) {
						text.setText(e.toString());
					}
				}
			}
		});

		text = new StyledText(composite, SWT.H_SCROLL | SWT.V_SCROLL);
		text.setLayoutData(new GridData(GridData.FILL_BOTH));
		return composite;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CLIENT_ID, "拷贝到剪贴板", false);
		createButton(parent, IDialogConstants.OK_ID, "导出到文件", false);
		createButton(parent, IDialogConstants.CANCEL_ID, "关闭", true);
	}

	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.CLIENT_ID == buttonId) {
			copyPressed();
		} else
			super.buttonPressed(buttonId);
	}

	private void copyPressed() {
		Clipboard clipboard = new Clipboard(getShell().getDisplay());
		TextTransfer textTransfer = TextTransfer.getInstance();
		String displayText = text.getText();
		clipboard.setContents(new String[] { displayText }, new Transfer[] { textTransfer });
		clipboard.dispose();
	}

	@Override
	protected void okPressed() {
		if (curTemplate == null)
			return;
		FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
		String filename = dialog.open();
		if (filename == null)
			return;
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(filename);
			Object t = map.get(curTemplate);
			if (t == null) {
				fos.write(content);
				MessageDialog.openInformation(getShell(), "提示", "导出文件成功");
			} else if (t instanceof Transformer) {
				((Transformer) t).transform(new StreamSource(new ByteArrayInputStream(content)), new StreamResult(fos));
				MessageDialog.openInformation(getShell(), "提示", "导出文件成功");
			} else
				throw new RuntimeException("模版" + curTemplate + "有问题！");
		} catch (Throwable ex) {
			MessageDialog.openError(getShell(), "错误", ex.getMessage());
			return;
		} finally {
			try {
				if (fos != null)
					fos.close();
			} catch (IOException e) {
			}
		}
	}
}
