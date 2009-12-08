package com.vaadin.integration.eclipse.wizards;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;

// TODO rename as NewWidgetWizardPage?
public class NewComponentWizardPage extends AbstractVaadinNewTypeWizardPage {

    private Combo extWidgetSetNameText;

    private Label extWidgetSetNameLabel;

    private String widgetsetName;

    private ICompilationUnit createdClientSideClass;

    private boolean is62Project;

    private Combo templateCombo;
    private Label templateDescriptionLabel;

    private TEMPLATE currentTemplate;

    /**
     * Component template to use.
     *
     * The titles are shown in combo boxes and are used to identify the
     * templates (together with vaadin62). The first suitable template is
     * selected by default.
     */
    public enum TEMPLATE {
        // templates for pre-6.2 versions
        BASIC_TK5_V6("Clean", "Simple client-side and server-side component",
                "widget_basic_tk5_v6",
                false), //
        SERVER_ONLY_TK5_V6("Server-side only",
                "Server-side component only, no client-side widget", null,
                false), //
        // templates for Vaadin 6.2 and later
        COMMUNICATION_V62(
                "Simple",
                "Simple client-side and server-side component with client-server communication",
                "widget_communication_v62", true), //
        BASIC_V62("Clean", "Simple client-side and server-side component",
                "widget_basic_v62", true), //
        SERVER_ONLY_V62("Server-side only",
                "Server-side component only, no client-side widget", null, true);

        // title shown in combo box
        private final String title;
        // somewhat longer description
        private final String description;
        // file name of the client-side template or null is no client-side class
        private final String clientTemplate;
        // true for Vaadin 6.2 and later, false for older versions
        private final boolean vaadin62;

        private TEMPLATE(String title, String description, String template,
                boolean v62) {
            this.title = title;
            this.description = description;
            clientTemplate = template;
            vaadin62 = v62;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getClientTemplate() {
            return clientTemplate;
        }

        public boolean hasClientWidget() {
            return clientTemplate != null;
        }

        public boolean isVaadin62() {
            return vaadin62;
        }
    };

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
        setSuperClass(VaadinPluginUtil.getVaadinPackagePrefix(getProject())
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

            is62Project = VaadinPluginUtil.isVaadin62(project);
            if (is62Project) {
                if (isControlCreated()) {
                    extWidgetSetNameText.setVisible(false);
                    extWidgetSetNameLabel.setVisible(false);
                }

            } else {
                if (isControlCreated()) {
                    extWidgetSetNameText.setVisible(true);
                    extWidgetSetNameLabel.setVisible(true);
                }
                // Detect widgetsets in this project and update the combo
                IType[] wsSubtypes = VaadinPluginUtil.getWidgetSetClasses(
                        project, null);

                if (extWidgetSetNameText != null) {
                    for (IType ws : wsSubtypes) {
                        if (project.equals(ws.getResource().getProject())) {
                            extWidgetSetNameText
                                    .add(ws.getFullyQualifiedName());
                        }
                    }

                    // check that there is a widgetset before selecting one
                    if (extWidgetSetNameText.getItemCount() > 0) {
                        extWidgetSetNameText.setText(extWidgetSetNameText
                                .getItem(0));
                    }
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
        l.setText("Component type:");
        templateCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (TEMPLATE template : TEMPLATE.values()) {
            if (is62Project == template.isVaadin62()) {
                templateCombo.add(template.getTitle());
                templateCombo.setData(template.getTitle(), template);
            }
        }
        templateCombo.select(0);
        templateCombo.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
                // NOP
            }

            public void widgetSelected(SelectionEvent e) {
                selectTemplate((TEMPLATE) templateCombo.getData(templateCombo
                        .getText()));
            }
        });

        DialogField.createEmptySpace(composite, columns - 2);

        // TODO show template description somewhere - framed (multiline) label?
        templateDescriptionLabel = new Label(composite, SWT.NULL);
        GridData wideGd = new GridData(SWT.FILL, SWT.BEGINNING, true, false,
                columns, 1);
        templateDescriptionLabel.setLayoutData(wideGd);

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

        selectTemplate((TEMPLATE) templateCombo
                .getData(templateCombo.getText()));
    }

    protected void selectTemplate(TEMPLATE template) {
        currentTemplate = template;

        templateDescriptionLabel.setText(template.getDescription());

        boolean buildClientSideStub = template.hasClientWidget();
        extWidgetSetNameText.setVisible(!template.isVaadin62()
                && buildClientSideStub);
        extWidgetSetNameLabel.setVisible(!template.isVaadin62()
                && buildClientSideStub);

        String prefix = VaadinPluginUtil.getVaadinPackagePrefix(getProject());
        if (template.hasClientWidget()) {
            setSuperClass(prefix + "ui.AbstractComponent", true);
        } else {
            setSuperClass(prefix + "ui.CustomComponent", true);
        }
    }

    @Override
    protected void createTypeMembers(IType type, ImportsManager imports,
            IProgressMonitor monitor) throws CoreException {

        // server-side CustomComponent
        if (!currentTemplate.hasClientWidget()
                && getSuperClass().endsWith("ui.CustomComponent")) {
            // CustomComponent must set composition root
            String prefix = VaadinPluginUtil
                    .getVaadinPackagePrefix(getProject());
            imports.addImport(prefix + "ui.Label");

            type
                    .createMethod(
                            "\n\tpublic "
                                    + type.getElementName()
                                    + "() {\n"
                                    + "\t\tsetCompositionRoot(new Label(\"Custom component\"));\n"
                                    + "\t}\n", null, false, monitor);
        }
        if (!currentTemplate.isVaadin62() && currentTemplate.hasClientWidget()) {
            type
                    .createMethod(
                            "@Override\n\tpublic String getTag(){\n\t\treturn \""
                                    + type.getElementName().toLowerCase()
                                    + "\" ;\n}\n", null, false, monitor);
        }
        if (TEMPLATE.COMMUNICATION_V62 == currentTemplate) {
            try {
                imports.addImport("java.util.Map");
                imports.addImport("com.vaadin.terminal.PaintException");
                imports.addImport("com.vaadin.terminal.PaintTarget");

                // server-side fields
                type.createField(
                        "\tprivate String message = \"Click here.\";\n", null,
                        false, monitor);
                type.createField("\tprivate int clicks = 0;\n", null, false,
                        monitor);

                // server-side methods
                String templateBase = "component/"
                        + currentTemplate.getClientTemplate();

                String paintContentMethod = VaadinPluginUtil
                        .readTextFromTemplate(templateBase
                                + "_server_paintContent.txt");
                type.createMethod(paintContentMethod, null, false, monitor);

                String changeVariablesMethod = VaadinPluginUtil
                        .readTextFromTemplate(templateBase
                                + "_server_changeVariables.txt");
                type.createMethod(changeVariablesMethod, null, false, monitor);
            } catch (IOException e) {
                // handle exception - should not happen as templates are
                // inside the plugin
                VaadinPluginUtil.handleBackgroundException(
                        "Could not find method templates in plugin", e);
            }
        }
    }

    public String getWidgetSetName() {
        return widgetsetName;
    }

    public TEMPLATE getTemplate() {
        return currentTemplate;
    }

    @Override
    protected String constructCUContent(ICompilationUnit cu,
            String typeContent, String lineDelimiter) throws CoreException {
        if (currentTemplate.isVaadin62() && currentTemplate.hasClientWidget()) {
            // add the ClientWidget annotation to the server side class
            String fullyQualifiedName = createdClientSideClass.getTypes()[0]
                    .getFullyQualifiedName();
            typeContent = "@com.vaadin.ui.ClientWidget(" + fullyQualifiedName
                    + ".class)\n" + typeContent;
        }
        return super.constructCUContent(cu, typeContent, lineDelimiter);
    }

    // this must be called before constructCUContent()
    public void setCreatedClientSideClass(ICompilationUnit clientSideClass) {
        createdClientSideClass = clientSideClass;
    }

    @Override
    protected IStatus[] getStatus() {
        IStatus[] status = super.getStatus();
        if (currentTemplate != null && currentTemplate.hasClientWidget()
                && !currentTemplate.isVaadin62()
                && extWidgetSetNameText.getItemCount() == 0) {
            // no widgetset exists
            IStatus[] newStatus = new IStatus[status.length + 1];
            System.arraycopy(status, 0, newStatus, 0, status.length);
            newStatus[newStatus.length - 1] = new Status(IStatus.ERROR,
                    VaadinPlugin.PLUGIN_ID, "No widgetset in project.");
            return newStatus;
        }
        return status;
    }

}