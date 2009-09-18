package com.vaadin.integration.eclipse.properties;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.dialogs.PropertyPage;

import com.vaadin.integration.eclipse.util.VaadinPluginUtil;
import com.vaadin.integration.eclipse.util.DownloadUtils.Version;

public class VaadinVersionPropertyPage extends PropertyPage {

    private VaadinVersionComposite vaadinVersionComposite;
    private Button useVaadinButton;

    @Override
    protected void performDefaults() {
        // revert to the vaadin version currently in the project
        try {
            IProject project = getVaadinProject();
            vaadinVersionComposite.setProject(project);

            useVaadinButton.setSelection(vaadinVersionComposite
                    .getSelectedVersion() != null);
        } catch (CoreException ex) {
            VaadinPluginUtil
                    .handleBackgroundException(
                            IStatus.ERROR,
                            "Failed reverting to the Vaadin version currently used in the project",
                            ex);
            vaadinVersionComposite.setProject(null);
            useVaadinButton.setSelection(false);
        }

        vaadinVersionComposite.enableVaadin(useVaadinButton.getSelection());
    }

    @Override
    public boolean performOk() {
        final IProject project;
        try {
            project = getVaadinProject();
        } catch (CoreException ex) {
            // TODO handle better?
            return false;
        }

        final Version newVaadinVersion = useVaadinButton.getSelection() ? vaadinVersionComposite
                .getSelectedVersion()
                : null;

        // replace the Vaadin JAR in the project if it has changed
        try {
            Version currentVaadinVersion = VaadinPluginUtil
                    .getVaadinLibraryVersion(project);
            if ((newVaadinVersion == null && currentVaadinVersion != null)
                    || (newVaadinVersion != null && !newVaadinVersion
                            .equals(currentVaadinVersion))) {
                // confirm replacement, return false if not confirmed
                String message;
                if (currentVaadinVersion != null && newVaadinVersion == null) {
                    message = "Do you want to remove the Vaadin version "
                            + currentVaadinVersion.getVersionString()
                            + " from the project " + project.getName();
                } else if (currentVaadinVersion == null) {
                    message = "Do you want to add the Vaadin version "
                            + newVaadinVersion.getVersionString()
                            + " to the project " + project.getName();
                } else {
                    message = "Do you want to change the Vaadin version from "
                            + currentVaadinVersion.getVersionString() + " to "
                            + newVaadinVersion.getVersionString()
                            + " in the project " + project.getName();
                }
                if (!MessageDialog.openConfirm(getShell(),
                        "Confirm Vaadin version change", message)) {
                    return false;
                }
            }

            IRunnableWithProgress op = new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor)
                        throws InvocationTargetException {
                    try {
                        VaadinPluginUtil.updateVaadinLibraries(project,
                                newVaadinVersion, monitor);
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        monitor.done();
                    }
                }
            };
            // does not show the progress dialog if in a modal dialog
            // IProgressService service =
            // PlatformUI.getWorkbench().getProgressService();
            // service.busyCursorWhile(op);
            new ProgressMonitorDialog(getShell()).run(true, true, op);

        } catch (CoreException e) {
            VaadinPluginUtil.displayError(
                    "Failed to change Vaadin version in the project", e,
                    getShell());
            VaadinPluginUtil.handleBackgroundException(IStatus.WARNING,
                    "Failed to change Vaadin version in the project", e);
            return false;
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            Throwable realException = e.getTargetException();
            MessageDialog.openError(getShell(), "Error", realException
                    .getMessage());
            VaadinPluginUtil.handleBackgroundException(IStatus.WARNING,
                    "Failed to change Vaadin version in the project", e);
            return false;
        }
        return true;
    }

    /**
     * @see PreferencePage#createContents(Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout(1, false);
        composite.setLayout(layout);

        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        composite.setLayoutData(data);

        // enable/disable Vaadin use
        useVaadinButton = new Button(composite, SWT.CHECK);
        useVaadinButton.setText("Use Vaadin");

        Group group = new Group(composite, SWT.NONE);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setText("Vaadin");
        group.setLayout(new GridLayout(1, false));
        vaadinVersionComposite = new VaadinVersionComposite(group, SWT.NULL);
        vaadinVersionComposite.createContents();

        useVaadinButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                vaadinVersionComposite.enableVaadin(useVaadinButton
                        .getSelection());
            }
        });

        performDefaults();

        return composite;
    }

    private IProject getVaadinProject() throws CoreException {
        IProject project;
        if (getElement() instanceof IJavaProject) {
            project = ((IJavaProject) getElement()).getProject();
        } else if (getElement() instanceof IProject) {
            project = (IProject) getElement();
        } else {
            throw VaadinPluginUtil.newCoreException("Not a Vaadin project",
                    null);
        }
        return project;
    }

}
