package rdm.editors;

import static rdm.editors.UiUtility.createColumn;
import static rdm.editors.UiUtility.setText;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;

import de.kupzog.ktable.KTable;
import de.kupzog.ktable.KTableDefaultModel;
import de.kupzog.ktable.SWTX;
import rainbow.db.model.Entity;
import rainbow.db.model.Model;

public class ModelEditor extends EditorPart implements ModelChangeListener {

	private MvcModel model;

	private MvcController controller;

	private TableViewer viewer;

	private TabFolder tabFolder;

	private Text textModelName;
	private Text textCnName;
	private Text textDbName;
	private Text textJavaName;
	KTable columnTable;
	KTable indexTable;
	private KTableDefaultModel columnTableModel;
	private KTableDefaultModel indexTableModel;

	public ModelEditor() {
		super();
		model = new MvcModel();
		controller = new MvcController(this, model);
		model.addListener(this);
	}

	public MvcModel getModel() {
		return model;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());
	}

	private FileEditorInput getFileEditorInput() {
		return (FileEditorInput) super.getEditorInput();
	}

	@Override
	public boolean isDirty() {
		return model.isDirty();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	public void setFocus() {
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			InputStream is = getSaveStream();
			getFileEditorInput().getFile().setContents(is, true, true, monitor);
			model.setDirty(false);
		} catch (Exception e) {
			MessageDialog.openError(getSite().getShell(), "SaveAs Failed", e.getMessage());
		}
	}

	@Override
	public void doSaveAs() {
		Shell shell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
		SaveAsDialog dialog = new SaveAsDialog(shell);

		FileEditorInput input = getFileEditorInput();

		IFile original = input.getFile();
		if (original != null)
			dialog.setOriginalFile(original);
		else
			dialog.setOriginalName(input.getName());

		dialog.create();

		if (dialog.open() == Window.CANCEL) {
			return;
		}

		IPath filePath = dialog.getResult();
		if (filePath == null) {
			return;
		}

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IFile file = workspace.getRoot().getFile(filePath);
		FileEditorInput newInput = new FileEditorInput(file);
		boolean success = false;
		try {
			InputStream is = getSaveStream();
			if (file.exists())
				file.setContents(is, true, true, null);
			else
				file.create(is, true, null);
			success = true;
			model.setDirty(false);
		} catch (Exception x) {
			MessageDialog.openError(shell, "SaveAs Failed", x.getMessage());
		} finally {
			if (success)
				setInput(newInput);
		}
	}

	private InputStream getSaveStream() throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		TransformUtility.transform(model.getModel(), os);
		return new ByteArrayInputStream(os.toByteArray());
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		createModelUI(parent);
		SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
		createLeftPart(sashForm);
		createRightPart(sashForm);
		sashForm.setWeights(new int[] { 1, 2 });
		IFile file = getFileEditorInput().getFile();
		if (file.exists()) {
			try {
				file.refreshLocal(IResource.DEPTH_ZERO, null);
			} catch (CoreException e) {
			}
			InputStream is = null;
			try {
				is = file.getContents();
				if (is.available() > 0)
					this.model.initModel(TransformUtility.readModel(is));
				else
					this.model.initModel(new Model());
			} catch(Throwable e) {
				throw new RuntimeException("read file error", e);
			} finally {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		} else {
			this.model.initModel(new Model());
		}
	}

	private void createModelUI(final Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		container.setLayout(new GridLayout(2, false));

		textModelName = TextType.MODEL_NAME.create(container);
		textModelName.addModifyListener(controller);
	}

	private void createLeftPart(Composite parent) {
		Composite container = new Composite(parent, SWT.BORDER);
		GridLayout layout = new GridLayout(1, true);
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		container.setLayout(layout);

		ToolBar toolbar = new ToolBar(container, SWT.HORIZONTAL);
		toolbar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		ToolBarManager tbm = new ToolBarManager(toolbar);
		tbm.add(controller.makeAction(ActionType.ACTION_ENTITY_ADD));
		tbm.add(controller.makeAction(ActionType.ACTION_ENTITY_DEL));
		tbm.add(controller.makeAction(ActionType.ACTION_ENTITY_COPY));
		tbm.add(controller.makeAction(ActionType.ACTION_ENTITY_UP));
		tbm.add(controller.makeAction(ActionType.ACTION_ENTITY_DOWN));
		tbm.add(controller.makeAction(ActionType.ACTION_ENTITY_PREVIEW));
		tbm.add(controller.makeAction(ActionType.ACTION_ENTITY_SORT));
		tbm.update(true);

		viewer = new TableViewer(container, SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new EntityLabelProvider());
		Table table = viewer.getTable();
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		createColumn(table, "表名", 100);
		createColumn(table, "中文名", 100);
		createColumn(table, "对象名", 100);
		viewer.addSelectionChangedListener(controller);
	}

	private void createRightPart(SashForm sashForm) {
		Composite container = new Composite(sashForm, SWT.BORDER);
		container.setLayout(new GridLayout(6, false));
		textDbName = controller.makeText(TextType.ENTITY_DBNAME, container);
		textCnName = controller.makeText(TextType.ENTITY_CNNAME, container);
		textJavaName = controller.makeText(TextType.ENTITY_NAME, container);

		tabFolder = new TabFolder(container, SWT.NONE);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 6, 1));
		TabItem tabItem1 = new TabItem(tabFolder, SWT.NONE);
		tabItem1.setText("字段");
		TabItem tabItem2 = new TabItem(tabFolder, SWT.NONE);
		tabItem2.setText("索引");

		Composite columnPanel = new Composite(tabFolder, SWT.BORDER);
		columnPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout(1, true);
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		columnPanel.setLayout(layout);
		createColumnPanel(columnPanel);
		tabItem1.setControl(columnPanel);

		Composite indexPanel = new Composite(tabFolder, SWT.BORDER);
		indexPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout indexlayout = new GridLayout(1, true);
		indexlayout.verticalSpacing = 0;
		indexlayout.marginHeight = 0;
		indexlayout.marginWidth = 0;
		indexPanel.setLayout(indexlayout);
		createIndexPanel(indexPanel);
		tabItem2.setControl(indexPanel);
	}

	private void createColumnPanel(Composite parent) {
		ToolBar toolbar = new ToolBar(parent, SWT.HORIZONTAL);
		toolbar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		ToolBarManager tbm = new ToolBarManager(toolbar);
		tbm.add(controller.makeAction(ActionType.ACTION_COLUMN_ADD));
		tbm.add(controller.makeAction(ActionType.ACTION_COLUMN_DEL));
		tbm.add(controller.makeAction(ActionType.ACTION_COLUMN_COPY));
		tbm.add(controller.makeAction(ActionType.ACTION_COLUMN_UP));
		tbm.add(controller.makeAction(ActionType.ACTION_COLUMN_DOWN));
		tbm.update(true);

		columnTableModel = new ColumnTableModel(model);
		columnTable = new KTable(parent, SWTX.AUTO_SCROLL | SWTX.EDIT_ON_KEY | SWT.BORDER);
		columnTable.setData(Integer.valueOf(0));
		columnTable.setModel(columnTableModel);
		columnTable.setLayoutData(new GridData(GridData.FILL_BOTH));
		columnTable.addCellSelectionListener(controller);
	}

	private void createIndexPanel(Composite parent) {
		ToolBar toolbar = new ToolBar(parent, SWT.HORIZONTAL);
		toolbar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		ToolBarManager tbm = new ToolBarManager(toolbar);
		tbm.add(controller.makeAction(ActionType.ACTION_INDEX_ADD));
		tbm.add(controller.makeAction(ActionType.ACTION_INDEX_DEL));
		tbm.add(controller.makeAction(ActionType.ACTION_INDEX_COPY));
		tbm.add(controller.makeAction(ActionType.ACTION_INDEX_UP));
		tbm.add(controller.makeAction(ActionType.ACTION_INDEX_DOWN));
		tbm.add(controller.makeAction(ActionType.ACTION_INDEX_EDIT));
		tbm.update(true);

		indexTableModel = new IndexTableModel(model);
		indexTable = new KTable(parent, SWTX.AUTO_SCROLL | SWTX.EDIT_ON_KEY | SWT.BORDER);
		indexTable.setData(Integer.valueOf(1));
		indexTable.setModel(indexTableModel);
		indexTable.setLayoutData(new GridData(GridData.FILL_BOTH));
		indexTable.addCellSelectionListener(controller);
	}

	@Override
	public void modelChange() {
		Model model = this.model.getModel();
		Entity entity = this.model.getCurEntity();
		controller.userInput = false;
		try {
			setText(textModelName, model.getName());
			viewer.setInput(model.getEntities());
			viewer.setSelection(this.model.getSelection());
			if (entity == null) {
				setText(textDbName, null);
				setText(textCnName, null);
				setText(textJavaName, null);
				for (ActionType type : EnumSet.range(ActionType.ACTION_ENTITY_DEL, ActionType.ACTION_INDEX_DOWN))
					type.setEnabled(false);
			} else {
				setText(textDbName, entity.getDbName());
				setText(textCnName, entity.getCnName());
				setText(textJavaName, entity.getName());
				ActionType.ACTION_ENTITY_DEL.setEnabled(true);
				ActionType.ACTION_ENTITY_COPY.setEnabled(true);

				int index = model.getEntities().indexOf(entity);
				ActionType.ACTION_ENTITY_UP.setEnabled(index > 0);
				ActionType.ACTION_ENTITY_DOWN.setEnabled(index < model.getEntities().size() - 1);
				ActionType.ACTION_ENTITY_PREVIEW.setEnabled(true);

				ActionType.ACTION_COLUMN_ADD.setEnabled(true);
				int curRow = this.model.getRow();
				ActionType.ACTION_COLUMN_DEL.setEnabled(curRow > 0);
				ActionType.ACTION_COLUMN_COPY.setEnabled(curRow > 0);
				ActionType.ACTION_COLUMN_UP.setEnabled(curRow > 1);
				ActionType.ACTION_COLUMN_DOWN.setEnabled(curRow > 0 && curRow < entity.getColumns().size());
				ActionType.ACTION_INDEX_ADD.setEnabled(true);
				int curInxRow = this.model.getInxrow();
				ActionType.ACTION_INDEX_DEL.setEnabled(curInxRow > 0);
				ActionType.ACTION_INDEX_EDIT.setEnabled(curInxRow > 0);
				ActionType.ACTION_INDEX_COPY.setEnabled(curInxRow > 0);
				ActionType.ACTION_INDEX_UP.setEnabled(curInxRow > 1);
				ActionType.ACTION_INDEX_DOWN.setEnabled(curInxRow > 0 && curInxRow < entity.getIndexes().size());
			}
			if (this.model.getRow() <= 0 || this.model.getCol() <= 0)
				columnTable.clearSelection();
			else
				columnTable.setSelection(this.model.getCol(), this.model.getRow(), true);
			columnTable.redraw();
			if (this.model.getInxrow() <= 0 || this.model.getInxcol() <= 0)
				indexTable.clearSelection();
			else
				indexTable.setSelection(this.model.getInxcol(), this.model.getInxrow(), true);
			indexTable.redraw();
		} finally {
			controller.userInput = true;
		}
	}

	@Override
	public void modelDirty() {
		firePropertyChange(PROP_DIRTY);
	}
	
}
