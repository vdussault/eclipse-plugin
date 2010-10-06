package com.vaadin.integration.eclipse.properties;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
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
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.builder.WidgetsetBuildManager;
import com.vaadin.integration.eclipse.util.DownloadUtils.Version;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;

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
            VaadinPluginUtil
                    .handleBackgroundException(
                            IStatus.ERROR,
                            "Failed reverting to the Vaadin version currently used in the project",
                            ex);
            vaadinVersionComposite.setProject(null);
            useVaadinButton.setSelection(false);
        }

        vaadinVersionComposite.enablePluginManagedVaadin(useVaadinButton.getSelection());

        // TODO validate liferay path on the fly
    }

    @Override
    public boolean performOk() {
        final IProject project;
        try {
            project = getVaadinProject();
        } catch (CoreException ex) {
            VaadinPluginUtil.logInfo("Store preferences: not a Vaadin project");
            return true;
        }

        IJavaProject jproject = JavaCore.create(project);

        boolean widgetsetDirty = false;
        Boolean hasWidgetSets = null;

        try {

            ScopedPreferenceStore prefStore = new ScopedPreferenceStore(
                    new ProjectScope(project), VaadinPlugin.PLUGIN_ID);

            // save Liferay path (if a liferay project)
            if (VaadinPluginUtil.isLiferayProject(project)) {
                VaadinPluginUtil.setLiferayPath(project, vaadinVersionComposite
                        .getLiferayPathField().getText());
            }

            // save widgetset compilation parameters

            boolean suspended = widgetsetComposite
                    .areWidgetsetBuildsSuspended();
            WidgetsetBuildManager.setWidgetsetBuildsSuspended(project,
                    suspended);

            boolean verbose = widgetsetComposite.isVerboseOutput();
            boolean oldVerbose = prefStore
                    .getBoolean(VaadinPlugin.PREFERENCES_WIDGETSET_VERBOSE);
            if (verbose != oldVerbose) {
                prefStore.setValue(VaadinPlugin.PREFERENCES_WIDGETSET_VERBOSE,
                        verbose);
                widgetsetDirty = true;
            }

            String style = widgetsetComposite.getCompilationStyle();
            String oldStyle = prefStore
                    .getString(VaadinPlugin.PREFERENCES_WIDGETSET_STYLE);
            // do not store the default value OBF, but handle it if stored
            if ("OBF".equals(oldStyle)) {
                oldStyle = "";
            }
            if ("OBF".equals(style)) {
                style = "";
            }
            if (!style.equals(oldStyle)) {
                prefStore.setValue(VaadinPlugin.PREFERENCES_WIDGETSET_STYLE,
                        style);
                widgetsetDirty = true;
            }

            String parallelism = widgetsetComposite.getParallelism();
            // empty string if not set
            String oldParallelism = prefStore
                    .getString(VaadinPlugin.PREFERENCES_WIDGETSET_PARALLELISM);
            if (!parallelism.equals(oldParallelism)) {
                prefStore.setValue(
                        VaadinPlugin.PREFERENCES_WIDGETSET_PARALLELISM,
                        parallelism);
                widgetsetDirty = true;
            }

            // if anything changed, mark widgetset as dirty and ask about
            // recompiling it
            if (widgetsetDirty) {
                prefStore.save();

                // will also be saved later, here in case Vaadin version
                // replacement fails
                if (hasWidgetSets == null) {
                    hasWidgetSets = hasWidgetSets(jproject);
                }
                if (hasWidgetSets) {
                    VaadinPluginUtil.setWidgetsetDirty(project, true);
                }
            }
        } catch (IOException e) {
            VaadinPluginUtil.displayError(
                    "Failed to save widgetset compilation parameters.", e,
                    getShell());
            VaadinPluginUtil.handleBackgroundException(IStatus.WARNING,
                    "Failed to save widgetset compilation parameters.", e);
            return false;
        } catch (CoreException e) {
            VaadinPluginUtil.displayError("Failed to save Liferay path.", e,
                    getShell());
            VaadinPluginUtil.handleBackgroundException(IStatus.WARNING,
                    "Failed to save Liferay path.", e);
            return false;
        }

        if (VaadinPluginUtil.isVaadinJarManagedByPlugin(project)) {
            final Version newVaadinVersion = useVaadinButton.getSelection() ? vaadinVersionComposite
                    .getSelectedVersion() : null;

            // replace the Vaadin JAR in the project if it has changed
            // - add new JAR in WEB-INF/lib without modifying the reference to
            // the old one if there is already a Vaadin JAR with a different
            // version elsewhere on the classpath, and do nothing if there is a
            // Vaadin JAR with the correct version on the classpath
            try {
                Version currentVaadinVersion = VaadinPluginUtil
                        .getVaadinLibraryVersion(project, true);

                if (useVaadinButton.getSelection()) {
                    VaadinFacetUtils.upgradeFacet(project,
                            VaadinFacetUtils.VAADIN_FACET_CURRENT);
                    if (VaadinPluginUtil.isWidgetsetManagedByPlugin(project)) {
                        VaadinPluginUtil.ensureWidgetsetNature(project);
                    }
                }

                if ((newVaadinVersion == null && currentVaadinVersion != null)
                        || (newVaadinVersion != null && !newVaadinVersion
                                .equals(currentVaadinVersion))) {
                    // confirm replacement, return false if not confirmed
                    String message;
                    if (currentVaadinVersion != null
                            && newVaadinVersion == null) {
                        message = "Do you want to remove the Vaadin version "
                                + currentVaadinVersion.getVersionString()
                                + " from the project " + project.getName()
                                + "?";
                    } else if (currentVaadinVersion == null) {
                        message = "Do you want to add the Vaadin version "
                                + newVaadinVersion.getVersionString()
                                + " to the project " + project.getName() + "?";
                    } else {
                        message = "Do you want to change the Vaadin version from "
                                + currentVaadinVersion.getVersionString()
                                + " to "
                                + newVaadinVersion.getVersionString()
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
                VaadinPluginUtil
                        .displayError(
                                "Failed to change Vaadin version in the project. Check that the Vaadin JAR is not in use.",
                                e, getShell());
                VaadinPluginUtil.handleBackgroundException(IStatus.WARNING,
                        "Failed to change Vaadin version in the project", e);
                return false;
            } catch (InterruptedException e) {
                return false;
            } catch (InvocationTargetException e) {
                Throwable realException = e.getTargetException();
                VaadinPluginUtil
                        .displayError(
                                "Failed to change Vaadin version in the project. Check that the Vaadin JAR is not in use.",
                                realException, getShell());
                VaadinPluginUtil.handleBackgroundException(IStatus.WARNING,
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
                VaadinPluginUtil.setWidgetsetDirty(project, true);
            }
        }

        // this may also be true because of hosted mode launch creation or older
        // changes
        if (VaadinPluginUtil.isWidgetsetDirty(project)) {
            WidgetsetBuildManager.runWidgetSetBuildTool(project, false,
                    new NullProgressMonitor());
        }

        return true;
    }

    private Boolean hasWidgetSets(IJavaProject jproject) {
        try {
            return VaadinPluginUtil.hasWidgetSets(jproject,
                    new NullProgressMonitor());
        } catch (CoreException e) {
            VaadinPluginUtil.handleBackgroundException(IStatus.WARNING,
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
                vaadinVersionComposite.enablePluginManagedVaadin(useVaadinButton
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
            throw VaadinPluginUtil.newCoreException("Not a Vaadin project",
                    null);
        }
        return project;
    }
}