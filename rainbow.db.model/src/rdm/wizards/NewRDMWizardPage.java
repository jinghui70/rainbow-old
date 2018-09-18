package rdm.wizards;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

public class NewRDMWizardPage extends WizardNewFileCreationPage {
	
	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public NewRDMWizardPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		setTitle(pageName);
		setDescription("This wizard creates Rainbow Database Modal file with *.rdm.");
		setFileExtension("rdm");
	}

	protected void createLinkTarget() {
	}

	protected IStatus validateLinkedResource() {
		return new Status(IStatus.OK, "rainbow.db.model", IStatus.OK, "", null); //NON-NLS-1 //NON-NLS-2
	}
}