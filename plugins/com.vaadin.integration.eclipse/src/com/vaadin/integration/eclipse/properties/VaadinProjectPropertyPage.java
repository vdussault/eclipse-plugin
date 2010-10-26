package com.vaadin.integration.eclipse.properties;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
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

import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.builder.WidgetsetBuildManager;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.PreferenceUtil;
import com.vaadin.integration.eclipse.util.ProjectDependencyManager;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;
import com.vaadin.integration.eclipse.util.WidgetsetUtil;
import com.vaadin.integration.eclipse.util.data.LocalVaadinVersion;

/**
 * Property page grouping Vaadin related project properties.
 * 
 * Vaadin version selection is here, future subpages may contain more settings.
 */
public class VaadinProjectPropertyPage extends PropertyPage {

    private Button useVaadinButton;
    private VaadinVersionComposite vaadinVersionComposite;
    private WidgetsetParametersComposite widgetsetComposite;

    @Override
    protected void performDefaults() {
        // revert to the vaadin version currently in the project
        try {
            IProject project = getVaadinProject();
            vaadinVersionComposite.setProject(project);
            widgetsetComposite.setProject(project);
            useVaadinButton.setEnabled(VaadinPluginUtil
                    .isVaadinJarManagedByPlugin(project));

            useVaadinButton.setSelection(useVaadinButton.isEnabled()
                    && vaadinVersionComposite.getSelectedVersion() != null);
        } catch (CoreException ex) {
            ErrorUtil
                    .handleBackgroundException(
                            IStatus.ERROR,
                            "Failed reverting to the Vaadin version currently used in the project",
                            ex);
            vaadinVersionComposite.setProject(null);
            useVaadinButton.setSelection(false);
        }

        vaadinVersionComposite.enablePluginManagedVaadin(useVaadinButton
                .getSelection());

        // TODO validate liferay path on the fly
    }

    @Override
    public boolean performOk() {
        final IProject project;
        try {
            project = getVaadinProject();
        } catch (CoreException ex) {
            ErrorUtil.logInfo("Store preferences: not a Vaadin project");
            return true;
        }

        IJavaProject jproject = JavaCore.create(project);

        boolean widgetsetDirty = false;
        Boolean hasWidgetSets = null;

        try {

            PreferenceUtil preferences = PreferenceUtil.get(project);
            // save widgetset compilation parameters

            boolean suspended = widgetsetComposite
                    .areWidgetsetBuildsSuspended();
            WidgetsetBuildManager.setWidgetsetBuildsSuspended(project,
                    suspended);

            boolean verbose = widgetsetComposite.isVerboseOutput();
            boolean changed = preferences
                    .setWidgetsetCompilationVerboseMode(verbose);
            if (changed) {
                widgetsetDirty = true;
            }

            String style = widgetsetComposite.getCompilationStyle();
            String oldStyle = preferences.getWidgetsetCompilationStyle();
            // do not store the default value OBF, but handle it if stored
            if ("OBF".equals(oldStyle)) {
                oldStyle = "";
            }
            if ("OBF".equals(style)) {
                style = "";
            }
            changed = preferences.setWidgetsetCompilationStyle(style);
            if (changed) {
                widgetsetDirty = true;
            }

            String parallelism = widgetsetComposite.getParallelism();
            changed = preferences
                    .setWidgetsetCompilationParallelism(parallelism);
            if (changed) {
                widgetsetDirty = true;
            }

            // if anything changed, mark widgetset as dirty and ask about
            // recompiling it
            if (widgetsetDirty) {
                preferences.persist();

                // will also be saved later, here in case Vaadin version
                // replacement fails
                if (hasWidgetSets == null) {
                    hasWidgetSets = hasWidgetSets(jproject);
                }
                if (hasWidgetSets) {
                    WidgetsetUtil.setWidgetsetDirty(project, true);
                }
            }
        } catch (IOException e) {
            ErrorUtil.displayError(
                    "Failed to save widgetset compilation parameters.", e,
                    getShell());
            ErrorUtil.handleBackgroundException(IStatus.WARNING,
                    "Failed to save widgetset compilation parameters.", e);
            return false;
        }

        if (VaadinPluginUtil.isVaadinJarManagedByPlugin(project)) {
            final LocalVaadinVersion newVaadinVersion = useVaadinButton
                    .getSelection() ? vaadinVersionComposite
                    .getSelectedVersion() : null;

            // replace the Vaadin JAR in the project if it has changed
            // - add new JAR in WEB-INF/lib without modifying the reference to
            // the old one if there is already a Vaadin JAR with a different
            // version elsewhere on the classpath, and do nothing if there is a
            // Vaadin JAR with the correct version on the classpath
            try {
                String currentVaadinVersion = ProjectUtil
                        .getVaadinLibraryVersion(project, true);

                if (useVaadinButton.getSelection()) {
                    VaadinFacetUtils.upgradeFacet(project,
                            VaadinFacetUtils.VAADIN_FACET_CURRENT);
                    if (WidgetsetUtil.isWidgetsetManagedByPlugin(project)) {
                        WidgetsetUtil.ensureWidgetsetNature(project);
                    }
                }

                if ((newVaadinVersion == null && currentVaadinVersion != null)
                        || (newVaadinVersion != null && !newVaadinVersion
                                .getVersionNumber()
                                .equals(currentVaadinVersion))) {
                    // confirm replacement, return false if not confirmed
                    String message;
                    if (currentVaadinVersion != null
                            && newVaadinVersion == null) {
                        message = "Do you want to remove the Vaadin version "
                                + currentVaadinVersion + " from the project "
                                + project.getName() + "?";
                    } else if (currentVaadinVersion == null) {
                        message = "Do you want to add the Vaadin version "
                                + newVaadinVersion.getVersionNumber()
                                + " to the project " + project.getName() + "?";
                    } else {
                        message = "Do you want to change the Vaadin version from "
                                + currentVaadinVersion
                                + " to "
                                + newVaadinVersion.getVersionNumber()
                                + " in the project " + project.getName() + "?";
                    }
                    if (!MessageDialog.openConfirm(getShell(),
                            "Confirm Vaadin version change", message)) {
                        return false;
                    }

                    widgetsetDirty = true;
                }

                IRunnableWithProgress op = new IRunnableWithProgress() {
                    public void run(IProgressMonitor monitor)
                            throws InvocationTargetException {
                        try {
                            ProjectDependencyManager.updateVaadinLibraries(
                                    project, newVaadinVersion, monitor);
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
                ErrorUtil
                        .displayError(
                                "Failed to change Vaadin version in the project. Check that the Vaadin JAR is not in use.",
                                e, getShell());
                ErrorUtil.handleBackgroundException(IStatus.WARNING,
                        "Failed to change Vaadin version in the project", e);
                return false;
            } catch (InterruptedException e) {
                return false;
            } catch (InvocationTargetException e) {
                Throwable realException = e.getTargetException();
                ErrorUtil
                        .displayError(
                                "Failed to change Vaadin version in the project. Check that the Vaadin JAR is not in use.",
                                realException, getShell());
                ErrorUtil.handleBackgroundException(IStatus.WARNING,
                        "Failed to change Vaadin version in the project", e);
                return false;
            }
        }

        // If anything changed, ask about recompiling the widgetset.
        // Mark the widgetset as dirty only if there is a widgetset in the
        // project.
        if (widgetsetDirty) {
            if (hasWidgetSets == null) {
                hasWidgetSets = hasWidgetSets(jproject);
            }
            if (hasWidgetSets) {
                WidgetsetUtil.setWidgetsetDirty(project, true);
            }
        }

        // this may also be true because of hosted mode launch creation or older
        // changes
        if (WidgetsetUtil.isWidgetsetDirty(project)) {
            WidgetsetBuildManager.runWidgetSetBuildTool(project, false,
                    new NullProgressMonitor());
        }

        return true;
    }

    private Boolean hasWidgetSets(IJavaProject jproject) {
        try {
            return WidgetsetUtil.hasWidgetSets(jproject,
                    new NullProgressMonitor());
        } catch (CoreException e) {
            ErrorUtil.handleBackgroundException(IStatus.WARNING,
                    "Could not check whether the project "
                            + jproject.getProject().getName()
                            + " has a widgetset", e);
            return false;
        }
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
                vaadinVersionComposite
                        .enablePluginManagedVaadin(useVaadinButton
                                .getSelection());
            }
        });

        group = new Group(composite, SWT.NONE);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setText("Widgetsets");
        group.setLayout(new GridLayout(1, false));
        widgetsetComposite = new WidgetsetParametersComposite(group, SWT.NULL);
        widgetsetComposite.createContents();

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
            throw ErrorUtil.newCoreException("Not a Vaadin project", null);
        }
        return project;
    }
}