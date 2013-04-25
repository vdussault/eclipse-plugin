package com.vaadin.integration.eclipse.wizards;

import java.util.Iterator;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.jarpackager.AbstractJarDestinationWizardPage;
import org.eclipse.jdt.internal.ui.jarpackager.JarPackagerMessages;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.ProblemsLabelDecorator;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;

/**
 * The first (main) page of the Vaadin Directory package export wizard.
 * 
 * This is a simplified and customized variant of the JAR export wizard page
 * that supports editing of the Vaadin manifest fields and pre-selects the most
 * relevant resources to export.
 * 
 * Many messages could otherwise be reused from {@link JarPackagerMessages} but
 * that could lead to a part English, part local language UI.
 * 
 * Some code is duplicated from Eclipse JDT jarpackager as it has limited
 * visibility or is not designed to be extended.
 */
@SuppressWarnings("restriction")
public class DirectoryPackageWizardPage extends
        AbstractJarDestinationWizardPage {

    // ID for stored parameters
    private static final String PAGE_NAME = "DirectoryPackageWizardPage";
    private Button overwriteCheckbox;
    private DirectoryPackageInputGroup inputGroup;
    private Text implementationTitleText;
    private Text implementationVersionText;
    private Text widgetsetsText;
    private Text stylesheetsText;

    private IStructuredSelection initialSelection;

    // this is redundant with a superclass field but provides typing
    private final DirectoryPackageData directoryPackage;

    // borrow selection area size from JAR export wizard for consistency
    private static final int SIZING_SELECTION_WIDGET_WIDTH = 480;
    private static final int SIZING_SELECTION_WIDGET_HEIGHT = 150;

    public DirectoryPackageWizardPage(DirectoryPackageData directoryPackage,
            IStructuredSelection selection) {
        super(PAGE_NAME, selection, directoryPackage);
        this.directoryPackage = directoryPackage;

        setTitle(DirectoryPackageWizard.WIZARD_TITLE);
        setDescription("Define which resources should be exported into the Vaadin add-on package.");

        initialSelection = selection;
    }

    @Override
    public void createControl(final Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL
                | GridData.HORIZONTAL_ALIGN_FILL));

        createPlainLabel(composite, "Select the resources to export.");
        createInputGroup(composite);

        new Label(composite, SWT.NONE); // vertical spacer

        createPlainLabel(composite, "Manifest:");
        createManifestGroup(composite);

        createPlainLabel(composite, "Select the export destination:");
        createDestinationGroup(composite);

        createPlainLabel(composite, "Options:");
        createOptionsGroup(composite);

        restoreWidgetValues();

        if (initialSelection != null) {
            BusyIndicator.showWhile(parent.getDisplay(), new Runnable() {
                public void run() {
                    setupBasedOnInitialSelections();
                }
            });
        }

        setControl(composite);
        update();
        giveFocusToDestination();

        Dialog.applyDialogFont(composite);
    }

    /**
     * Creates the checkbox tree and list for selecting resources.
     * 
     * @param parent
     *            the parent control
     */
    protected void createInputGroup(Composite parent) {
        int labelFlags = JavaElementLabelProvider.SHOW_BASICS
                | JavaElementLabelProvider.SHOW_OVERLAY_ICONS
                | JavaElementLabelProvider.SHOW_SMALL_ICONS;
        ITreeContentProvider treeContentProvider = new StandardJavaElementContentProvider() {
            @Override
            public boolean hasChildren(Object element) {
                // prevent the + from being shown in front of packages
                return !(element instanceof IPackageFragment)
                        && super.hasChildren(element);
            }
        };
        final DecoratingLabelProvider provider = new DecoratingLabelProvider(
                new JavaElementLabelProvider(labelFlags),
                new ProblemsLabelDecorator(null));
        inputGroup = new DirectoryPackageInputGroup(parent,
                JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()),
                treeContentProvider, provider,
                new StandardJavaElementContentProvider(), provider, SWT.NONE,
                SIZING_SELECTION_WIDGET_WIDTH, SIZING_SELECTION_WIDGET_HEIGHT);
        inputGroup.getTree().addListener(SWT.MouseUp, this);
        inputGroup.getTable().addListener(SWT.MouseUp, this);

        ICheckStateListener listener = new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                update();
            }
        };

        inputGroup.addCheckStateListener(listener);
    }

    /**
     * Create the export options widgets.
     * 
     * Use reasonable defaults for most values instead of offering many options.
     * When additional flexibility is needed, the user can directly launch the
     * JAR export wizard.
     * 
     * @param parent
     *            org.eclipse.swt.widgets.Composite
     */
    @Override
    protected void createOptionsGroup(Composite parent) {
        Composite optionsGroup = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        optionsGroup.setLayout(layout);

        overwriteCheckbox = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        overwriteCheckbox.setText("Overwrite existing files without warning");
        overwriteCheckbox.addListener(SWT.Selection, this);
    }

    /**
     * Create the manifest contents widgets.
     * 
     * Only the Vaadin addon package specific fields are presented.
     * 
     * @param parent
     *            org.eclipse.swt.widgets.Composite
     */
    protected void createManifestGroup(Composite parent) {
        Composite manifestGroup = new Composite(parent, SWT.NONE);
        manifestGroup
                .setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        manifestGroup.setLayout(layout);

        Label label;

        label = new Label(manifestGroup, SWT.NONE);
        label.setText("Implementation title:");
        implementationTitleText = new Text(manifestGroup, SWT.BORDER);
        implementationTitleText.setLayoutData(gdhfill());
        implementationTitleText.addListener(SWT.Modify, this);

        label = new Label(manifestGroup, SWT.NONE);
        label.setText("    Name of the add-on. Used in Vaadin Directory.");
        GridData gd = new GridData(SWT.LEFT, SWT.TOP, true, false);
        gd.horizontalSpan = 2;
        label.setLayoutData(gd);

        label = new Label(manifestGroup, SWT.NONE);
        label.setText("Implementation version:");
        implementationVersionText = new Text(manifestGroup, SWT.BORDER);
        implementationVersionText.setLayoutData(gdhfill());
        implementationVersionText.addListener(SWT.Modify, this);

        label = new Label(manifestGroup, SWT.NONE);
        label.setText("    Version of the addon. A \"major.minor.revision\" format is suggested.");
        gd = new GridData(SWT.LEFT, SWT.TOP, true, false);
        gd.horizontalSpan = 2;
        label.setLayoutData(gd);

        label = new Label(manifestGroup, SWT.NONE);
        label.setText("Widgetsets:");
        widgetsetsText = new Text(manifestGroup, SWT.BORDER);
        widgetsetsText.setLayoutData(gdhfill());
        widgetsetsText.addListener(SWT.Modify, this);


        label = new Label(manifestGroup, SWT.NONE);
        label.setText("    Comma separated list of widgetsets included in the add-on. Refers to the GWT xml files (.gwt.xml).");
        gd = new GridData(SWT.LEFT, SWT.TOP, true, false);
        gd.horizontalSpan = 2;
        label.setLayoutData(gd);

        label = new Label(manifestGroup, SWT.NONE);
        label.setText("Stylesheets:");

        stylesheetsText = new Text(manifestGroup, SWT.BORDER);
        stylesheetsText.setLayoutData(gdhfill());
        stylesheetsText.addListener(SWT.Modify, this);

        label = new Label(manifestGroup, SWT.NONE);
        label.setText("    Comma separated list of stylesheets included in the add-on. (e.g VAADIN/addons/myaddon/myaddon.scss)");
        gd = new GridData(SWT.LEFT, SWT.TOP, true, false);
        gd.horizontalSpan = 2;
        label.setLayoutData(gd);

    }

    /**
     * Stores the widget values in the JAR package.
     */
    @Override
    protected void updateModel() {
        if (getControl() == null) {
            return;
        }

        directoryPackage.setImplementationTitle(implementationTitleText
                .getText());
        directoryPackage.setImplementationVersion(implementationVersionText
                .getText());
        directoryPackage.setWidgetsets(widgetsetsText.getText());
        directoryPackage.setStylesheets(stylesheetsText.getText());

        super.updateModel();

        directoryPackage.setOverwrite(overwriteCheckbox.getSelection());
    }

    protected static GridData gdhfill() {
        return new GridData(GridData.FILL_HORIZONTAL);
    }

    @Override
    protected void restoreWidgetValues() {
        super.restoreWidgetValues();

        overwriteCheckbox.setSelection(directoryPackage.allowOverwrite());

        // SWT Text widgets do not accept null values
        String implementationTitle = directoryPackage.getImplementationTitle();
        implementationTitleText
                .setText(implementationTitle != null ? implementationTitle : "");
        String implementationVersion = directoryPackage
                .getImplementationVersion();
        implementationVersionText
                .setText(implementationVersion != null ? implementationVersion
                        : "");
        String widgetsets = directoryPackage.getWidgetsets();
        widgetsetsText.setText(widgetsets != null ? widgetsets : "");

        String stylesheets = directoryPackage.getStylesheets();
        stylesheetsText.setText(stylesheets != null ? stylesheets : "");
    }

    /**
     * {@inheritDoc}
     * 
     * In practice, we should only get {@link IJavaProject} instances in the
     * selection here.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void setupBasedOnInitialSelections() {
        Iterator<Object> iterator = initialSelection.iterator();
        while (iterator.hasNext()) {
            Object selectedElement = iterator.next();

            if (selectedElement instanceof IJavaElement
                    && !((IJavaElement) selectedElement).exists()) {
                continue;
            }

            if (selectedElement instanceof IJavaProject) {
                try {
                    inputGroup.selectProject((IJavaProject) selectedElement);
                } finally {
                    inputGroup.setInitiallySelecting(false);
                    // only use the first project found
                }
                break;
            }
        }

        // select the first checked item in the tree
        TreeItem[] items = inputGroup.getTree().getItems();
        int i = 0;
        while (i < items.length && !items[i].getChecked()) {
            i++;
        }
        if (i < items.length) {
            inputGroup.getTree().setSelection(new TreeItem[] { items[i] });
            inputGroup.getTree().showSelection();
            inputGroup.populateListViewer(items[i].getData());
        }
    }

    /**
     * Return the elements which will be exported.
     * 
     * {@see
     * org.eclipse.jdt.ui.jarpackager.JarPackageData#setElements(Object[])}
     * 
     * {@see
     * DirectoryPackageInputGroup#getSelectedElementsWithoutContainedChildren()}
     * 
     * @return Object[] elements to export
     */
    Object[] getSelectedElementsWithoutContainedChildren() {
        return inputGroup.getSelectedElementsWithoutContainedChildren();
    }
}
