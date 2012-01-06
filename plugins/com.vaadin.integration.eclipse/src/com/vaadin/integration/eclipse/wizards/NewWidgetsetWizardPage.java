package com.vaadin.integration.eclipse.wizards;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;
import com.vaadin.integration.eclipse.viewers.ApplicationList;

public class NewWidgetsetWizardPage extends AbstractVaadinNewTypeWizardPage {
    private static final String DEFAULT_EXTENDED_WIDGETSET = "terminal.gwt.client.DefaultWidgetSet";

    private ApplicationList applicationList;

    private boolean compileWidgetset = true;

    /**
     * Constructor for NewWidgetsetWizardPage.
     * 
     * @param pageName
     */
    public NewWidgetsetWizardPage(IProject project) {
        super("widgetsetwizard", project);
        setTitle("Vaadin widgetset");
        setDescription("This wizard creates appropriate stub files for a widgetset.");

        setSuperClass(VaadinPlugin.VAADIN_PACKAGE_PREFIX
                + DEFAULT_EXTENDED_WIDGETSET, true);
    }

    /**
     * @see IDialogPage#createControl(Composite)
     */
    public void createControl(Composite parent) {

        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setFont(parent.getFont());

        int nColumns = 4;

        GridLayout layout = new GridLayout();
        layout.numColumns = nColumns;
        composite.setLayout(layout);

        // pick & choose the wanted UI components

        // if this is modified in the dialog, setPackageFragmentRoot() will call
        // setProject()
        // if needed, could override chooseContainer() to filter the dialog
        createContainerControls(composite, nColumns);
        // non-default package is required here
        createPackageControls(composite, nColumns);

        // createEnclosingTypeControls(composite, nColumns);

        // createSeparator(composite, nColumns);

        createTypeNameControls(composite, nColumns);
        // createModifieCrControls(composite, nColumns);

        createSuperClassControls(composite, nColumns);
        // createSuperInterfacesControls(composite, nColumns);

        // createCommentControls(composite, nColumns);
        // enableCommentControl(true);

        Label label = new Label(composite, SWT.NULL);
        label.setText("&Modify applications to use widgetset:");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalAlignment = SWT.TOP;
        label.setLayoutData(gd);

        // applications can be selected from a list
        applicationList = new ApplicationList(composite, SWT.BORDER | SWT.MULTI
                | SWT.V_SCROLL);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalAlignment = SWT.TOP;
        gd.horizontalSpan = nColumns - 1;
        gd.heightHint = 120;
        applicationList.setLayoutData(gd);

        label = new Label(composite, SWT.NULL);
        label.setText("&Compile widgetset");

        final Button b = new Button(composite, SWT.CHECK);
        b.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                compileWidgetset = b.getSelection();
            }

            public void widgetSelected(SelectionEvent e) {
                compileWidgetset = b.getSelection();
            }
        });
        b.setSelection(compileWidgetset);

        setControl(composite);

        Dialog.applyDialogFont(composite);
        // PlatformUI.getWorkbench().getHelpSystem().setHelp(composite,
        // IJavaHelpContextIds.NEW_CLASS_WIZARD_PAGE);
    }

    // this is called by setPackageFragmentRoot
    @Override
    protected void setProjectInternal(IProject project) {
        super.setProjectInternal(project);

        if (project == null || !VaadinFacetUtils.isVaadinProject(project)) {
            applicationList.clear();
            return;
        }

        // update various fields including the application list
        // note that the application list is based on application classes, not
        // web.xml
        if (applicationList != null) {
            applicationList.update(project, false);
            // select all applications by default
            applicationList.selectAll();

            // use the first application in the project to get a name
            if (applicationList.getSelectedApplications().size() > 0) {
                IType projectApplication = applicationList
                        .getSelectedApplications().get(0);
                IPackageFragment packageFragment = projectApplication
                        .getPackageFragment();
                setPackageFragment(packageFragment, true);

                String name = projectApplication.getElementName();
                name.replaceAll("Application", "");
                name = name + "Widgetset";
                setTypeName(name, true);
            }
        }
    }

    @Override
    protected void createTypeMembers(IType type, ImportsManager imports,
            IProgressMonitor monitor) throws CoreException {
        String prefix = VaadinPlugin.VAADIN_PACKAGE_PREFIX;

        imports.addImport(prefix + "terminal.gwt.client.UIDL");
        imports.addImport(prefix + "terminal.gwt.client.Paintable");
        try {
            type.createMethod(
                    VaadinPluginUtil.readTextFromTemplate("widgetsetstub.txt"),
                    null, false, null);
        } catch (IOException e) {
            throw ErrorUtil.newCoreException("createTypeMembers failed", e);
        }
    }

    public boolean compileWidgetset() {
        return compileWidgetset;

    }

    public java.util.List<IType> getApplicationsToModify() {
        return applicationList.getSelectedApplications();
    }

    /*
     * @see NewContainerWizardPage#handleFieldChanged
     */
    @Override
    protected void handleFieldChanged(String fieldName) {
        super.handleFieldChanged(fieldName);

        // highest priority for the Vaadin specific errors
        // all used component status
        IStatus[] status;
        if (project == null || !VaadinFacetUtils.isVaadinProject(project)) {
            status = new Status[] { new Status(IStatus.ERROR,
                    VaadinPlugin.PLUGIN_ID, "No suitable project found.") };
        } else if (getPackageFragment().isDefaultPackage()) {
            status = new Status[] { new Status(IStatus.ERROR,
                    VaadinPlugin.PLUGIN_ID,
                    "Widgetsets cannot be created in the default package.") };
        } else {
            status = new IStatus[] { fContainerStatus, fPackageStatus,
                    fTypeNameStatus };
        }

        // the most severe status will be displayed and the OK button
        // enabled/disabled.
        updateStatus(status);
    }

}