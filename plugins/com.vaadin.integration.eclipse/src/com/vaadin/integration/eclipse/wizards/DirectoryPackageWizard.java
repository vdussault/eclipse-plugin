package com.vaadin.integration.eclipse.wizards;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.jarpackager.JarPackageWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels;
import org.eclipse.jdt.ui.jarpackager.IJarExportRunnable;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;

import com.vaadin.integration.eclipse.util.ErrorUtil;

/**
 * A wizard to edit the metadata of a Vaadin add-on package, store it to the
 * project manifest and export a directory package (internally a JAR).
 * 
 * This is a simplified and customized variant of the standard JAR export
 * wizard. Cannot inherit {@link JarPackageWizard} as it is not designed to be
 * extended.
 * 
 * Some code is duplicated from Eclipse JDT jarpackager as it has limited
 * visibility or is not designed to be extended.
 */
@SuppressWarnings("restriction")
public class DirectoryPackageWizard extends JarPackageWizard {

    public static final String WIZARD_TITLE = "Vaadin Add-on Package Export";

    // standard pattern for saving dialog settings
    private static String DIALOG_SETTINGS_KEY = "VaadinDirectoryPackageWizard"; //$NON-NLS-1$
    private boolean hasNewDialogSettings;

    private DirectoryPackageData directoryPackage;

    private DirectoryPackageWizardPage directoryPackageWizardPage;

    private IStructuredSelection selection;

    /**
     * Creates a wizard for exporting workspace resources to a JAR file.
     */
    public DirectoryPackageWizard() {
        IDialogSettings workbenchSettings = JavaPlugin.getDefault()
                .getDialogSettings();
        IDialogSettings section = workbenchSettings
                .getSection(DIALOG_SETTINGS_KEY);
        if (section == null) {
            hasNewDialogSettings = true;
        } else {
            hasNewDialogSettings = false;
            setDialogSettings(section);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addPages() {
        directoryPackageWizardPage = new DirectoryPackageWizardPage(
                directoryPackage, selection);
        addPage(directoryPackageWizardPage);
        // cannot call super.addPages() here - would add problematic pages
    }

    private void addJavaElement(List<IJavaProject> selectedElements,
            IJavaElement je) {
        int elementType = je.getElementType();
        if (elementType == IJavaElement.COMPILATION_UNIT
                || elementType == IJavaElement.CLASS_FILE
                || elementType == IJavaElement.JAVA_PROJECT) {
            selectedElements.add(je.getJavaProject());
        } else if (elementType == IJavaElement.PACKAGE_FRAGMENT
                || elementType == IJavaElement.PACKAGE_FRAGMENT_ROOT) {
            if (!isInArchiveOrExternal(je)) {
                selectedElements.add(je.getJavaProject());
            }
        } else {
            IOpenable openable = je.getOpenable();
            if (openable instanceof ICompilationUnit) {
                selectedElements.add(((ICompilationUnit) openable).getPrimary()
                        .getJavaProject());
            } else if (openable instanceof IClassFile
                    && !isInArchiveOrExternal(je)) {
                selectedElements.add(((IClassFile) openable).getJavaProject());
            }
        }
    }

    private static boolean isInArchiveOrExternal(IJavaElement element) {
        IPackageFragmentRoot root = (IPackageFragmentRoot) element
                .getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
        return root != null && (root.isArchive() || root.isExternal());
    }

    private void addProject(List<IJavaProject> selectedElements,
            IProject project) {
        try {
            if (project != null && project.hasNature(JavaCore.NATURE_ID)) {
                selectedElements.add(JavaCore.create(project));
            }
        } catch (CoreException ex) {
            // ignore selected element
        }
    }

    /**
     * Gets the current workspace page selection and converts it to a valid
     * selection for this wizard: the (first) Java project containing the
     * selection
     * 
     * @return a valid structured selection based on the current selection
     */
    @SuppressWarnings("unchecked")
    @Override
    protected IStructuredSelection getValidSelection() {
        ISelection currentSelection = JavaPlugin.getActiveWorkbenchWindow()
                .getSelectionService().getSelection();
        if (currentSelection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection) currentSelection;
            List<IJavaProject> selectedElements = new ArrayList<IJavaProject>(
                    structuredSelection.size());
            Iterator<Object> iter = structuredSelection.iterator();
            while (iter.hasNext()) {
                Object selectedElement = iter.next();
                if (selectedElement instanceof IProject) {
                    addProject(selectedElements, (IProject) selectedElement);
                } else if (selectedElement instanceof IResource) {
                    addProject(selectedElements,
                            ((IResource) selectedElement).getProject());
                } else if (selectedElement instanceof IJavaElement) {
                    addJavaElement(selectedElements,
                            (IJavaElement) selectedElement);
                }
            }
            return new StructuredSelection(selectedElements);
        } else {
            return StructuredSelection.EMPTY;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        // ignore the selection argument since the main export wizard changed it
        this.selection = getValidSelection();
        directoryPackage = new DirectoryPackageData();
        // initialize the manifest location etc. in the project
        Object selected = this.selection.getFirstElement();
        if (selected instanceof IJavaProject) {
            try {
                directoryPackage.setupProject((IJavaProject) selected);
            } catch (CoreException e) {
                ErrorUtil.displayError("Could not read project manifest", e,
                        getShell());
            } catch (IOException e) {
                ErrorUtil.displayError("Could not read project manifest", e,
                        getShell());
            }
        }

        setWindowTitle(WIZARD_TITLE);
        // TODO page image
        // setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_JAR_PACKAGER);
        setNeedsProgressMonitor(true);
    }

    @Override
    public IWizardPage getNextPage(IWizardPage page) {
        // need to override to handle before JarPackageWizard
        if (page == directoryPackageWizardPage) {
            return null;
        } else {
            return super.getNextPage(page);
        }
    }

    @Override
    public IWizardPage getPreviousPage(IWizardPage page) {
        // this we do not need to override as long as only one own page
        return super.getPreviousPage(page);
    }

    /**
     * Exports the directory package.
     * 
     * @param op
     *            the export operation to run
     * @return a boolean indicating success or failure
     */
    @Override
    protected boolean executeExportOperation(IJarExportRunnable op) {
        try {
            getContainer().run(true, true, op);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException ex) {
            if (ex.getTargetException() != null) {
                ExceptionHandler
                        .handle(ex, getShell(), WIZARD_TITLE,
                                "Add-on package creation failed. See details for additional information.");
                return false;
            }
        }
        IStatus status = op.getStatus();
        if (!status.isOK()) {
            ErrorDialog.openError(getShell(), WIZARD_TITLE, null, status);
            return !(status.matches(IStatus.ERROR));
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * Overridden to refer to the correct wizard page etc.
     */
    @Override
    public boolean performFinish() {
        // prepare the directory package for export
        directoryPackage.setElements(directoryPackageWizardPage
                .getSelectedElementsWithoutContainedChildren());

        // save the manifest so that it gets updated BEFORE being exported
        // - otherwise the manifest at the root of the JAR would be ok but the
        // one in WebContent/META-INF would only be updated after the export
        try {
            saveManifest();
        } catch (CoreException ex) {
            ErrorUtil.displayError("Updating the manifest failed.", ex,
                    getShell());
            return false;
        } catch (IOException ex) {
            ErrorUtil.displayError("Updating the manifest failed.", ex,
                    getShell());
            return false;
        }

        if (!executeExportOperation(directoryPackage
                .createJarExportRunnable(getShell()))) {
            return false;
        }

        // save the dialog settings
        if (hasNewDialogSettings) {
            IDialogSettings workbenchSettings = JavaPlugin.getDefault()
                    .getDialogSettings();
            IDialogSettings section = workbenchSettings
                    .getSection(DIALOG_SETTINGS_KEY);
            section = workbenchSettings.addNewSection(DIALOG_SETTINGS_KEY);
            setDialogSettings(section);
        }

        // for now, this is the only page
        directoryPackageWizardPage.finish();

        return true;
    }

    private void saveManifest() throws CoreException, IOException {
        ByteArrayOutputStream manifestOutput = new ByteArrayOutputStream();
        Manifest manifest = directoryPackage.getManifestProvider().create(
                directoryPackage);
        manifest.write(manifestOutput);
        ByteArrayInputStream fileInput = new ByteArrayInputStream(
                manifestOutput.toByteArray());
        IFile manifestFile = directoryPackage.getManifestFile();
        if (manifestFile.isAccessible()) {
            if (directoryPackage.allowOverwrite()
                    || queryDialog(
                            "Confirm Update",
                            Messages.format(
                                    "Do you want to update the manifest file ''{0}'' and use it in the package?",
                                    BasicElementLabels.getPathLabel(
                                            manifestFile.getFullPath(), false)))) {
                manifestFile.setContents(fileInput, true, true, null);
            }
        } else {
            manifestFile.create(fileInput, true, null);
        }
    }

    private boolean queryDialog(final String title, final String message) {
        final Shell shell = getShell();
        Display display = shell.getDisplay();
        if (display == null || display.isDisposed()) {
            return false;
        }
        final boolean[] returnValue = new boolean[1];
        Runnable runnable = new Runnable() {
            public void run() {
                returnValue[0] = MessageDialog.openQuestion(shell, title,
                        message);
            }
        };
        display.syncExec(runnable);
        return returnValue[0];
    }

}
