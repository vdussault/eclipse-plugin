package com.vaadin.integration.eclipse.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;

// TODO rename as NewWidgetWizardPage?
public class NewComponentWizardPage extends AbstractVaadinNewTypeWizardPage {

    private Combo extWidgetSetNameText;

    private Button buildClientSideStubButton;

    private Label extWidgetSetNameLabel;

    private boolean buildClientSideStub = true;

    private String widgetsetName;

    /**
     * Constructor for Component wizard page.
     *
     * @param pageName
     */
    public NewComponentWizardPage(IProject project) {
        super("componentwizard", project);
        setTitle("New Component wizard");
        setDescription("This wizard creates a new Vaadin widget.");

        setTypeName("MyComponent", true);
        setSuperClass(VaadinPluginUtil.getVaadinPackagePrefix(project)
                + "ui.AbstractComponent", true);
    }

    // this is called by setPackageFragmentRoot
    @Override
    protected void setProjectInternal(IProject project) {
        super.setProjectInternal(project);

        // clear the widgetset combo in any case
        if (extWidgetSetNameText != null) {
            extWidgetSetNameText.removeAll();
        }

        // show the page even when there is no project - eclipse guidelines
        if (project == null || !VaadinFacetUtils.isVaadinProject(project)) {
            return;
        }

        try {
            // Detect a package where an Application lies in as a default
            // package
            IType[] applications = VaadinPluginUtil.getApplicationClasses(
                    project, null);
            IType projectApplication = null;
            if (applications.length > 0) {
                projectApplication = applications[0];
            } else {
                // if there is no application, reset the fields of the page
                setPackageFragment(null, true);
                return;
            }

            IPackageFragment packageFragment = projectApplication
                    .getPackageFragment();
            setPackageFragment(packageFragment, true);
            setTypeName("MyComponent", true);

            // Detect widgetsets in this project and update the combo
            IType[] wsSubtypes = VaadinPluginUtil.getWidgetSetClasses(project,
                    null);

            if (extWidgetSetNameText != null) {
                for (IType ws : wsSubtypes) {
                    if (project.equals(ws.getResource().getProject())) {
                        extWidgetSetNameText.add(ws.getFullyQualifiedName());
                    }
                }

                // check that there is a widgetset before selecting one
                if (extWidgetSetNameText.getItemCount() > 0) {
                    extWidgetSetNameText.setText(extWidgetSetNameText
                            .getItem(0));
                }
            }

        } catch (CoreException e1) {
            VaadinPluginUtil
                    .handleBackgroundException(
                            IStatus.WARNING,
                            "Failed to select the project in the New Widget wizard",
                            e1);
        }
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

        createContainerControls(composite, nColumns);
        createPackageControls(composite, nColumns);

        // createEnclosingTypeControls(composite, nColumns);

        // createSeparator(composite, nColumns);

        createTypeNameControls(composite, nColumns);
        // createModifieCrControls(composite, nColumns);

        createSuperClassControls(composite, nColumns);

        // createSuperInterfacesControls(composite, nColumns);

        // createCommentControls(composite, nColumns);
        // enableCommentControl(true);

        createClientSideControls(composite, nColumns);

        setControl(composite);

        Dialog.applyDialogFont(composite);

    }

    @SuppressWarnings("restriction")
    private void createClientSideControls(Composite composite, int columns) {

        GridData gd = new GridData(GridData.FILL_HORIZONTAL);

        Label l = new Label(composite, SWT.NULL);
        l.setText("Build client side stub:");
        buildClientSideStubButton = new Button(composite, SWT.CHECK);
        buildClientSideStubButton.setSelection(true);
        buildClientSideStubButton.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
                // NOP
            }

            public void widgetSelected(SelectionEvent e) {
                buildClientSideStub = buildClientSideStubButton.getSelection();
                extWidgetSetNameText.setVisible(buildClientSideStub);
                extWidgetSetNameLabel.setVisible(buildClientSideStub);
            }
        });

        DialogField.createEmptySpace(composite, 2);

        extWidgetSetNameLabel = new Label(composite, SWT.NULL);
        extWidgetSetNameLabel.setText("To widgetset:");

        extWidgetSetNameText = new Combo(composite, SWT.DROP_DOWN
                | SWT.READ_ONLY);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        extWidgetSetNameText.setLayoutData(gd);
        extWidgetSetNameText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                widgetsetName = extWidgetSetNameText.getText();

            }
        });
    }

    @Override
    protected void createTypeMembers(IType type, ImportsManager imports,
            IProgressMonitor monitor) throws CoreException {

        type.createMethod(
                "@Override\n    public String getTag(){\n        return \""
                        + type.getElementName().toLowerCase() + "\" ;\n}\n",
                null, false, monitor);
    }

    public boolean buildClientSideStub() {
        return buildClientSideStub;
    }

    public String getWidgetSetName() {
        return widgetsetName;
    }

}