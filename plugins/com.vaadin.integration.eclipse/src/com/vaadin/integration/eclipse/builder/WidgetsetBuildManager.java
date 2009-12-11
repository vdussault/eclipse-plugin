package com.vaadin.integration.eclipse.builder;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;

/**
 * Manager for asking the user about triggering widgetset builds, triggering the
 * builds for a project and ensuring multiple builds are not run concurrently.
 */
public class WidgetsetBuildManager {

    /**
     * Confirmation dialog that offers an option to turn off automatic widgetset
     * recompilation.
     */
    private static class ConfirmationDialog extends MessageDialog {

        private Button checkBox;
        private boolean suspended = false;

        public ConfirmationDialog(Shell parentShell, String dialogTitle,
                Image dialogTitleImage, String dialogMessage,
                int dialogImageType, String[] dialogButtonLabels,
                int defaultIndex) {
            super(parentShell, dialogTitle, dialogTitleImage, dialogMessage,
                    dialogImageType, dialogButtonLabels, defaultIndex);
        }

        @Override
        protected Control createCustomArea(Composite parent) {
            checkBox = new Button(parent, SWT.CHECK);
            checkBox.setText("Suspend automatic recompilation");
            checkBox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    suspended = checkBox.getSelection();
                }
            });
            return checkBox;
        }

        public static boolean openQuestion(Shell parent, String title,
                String message, IProject project) {
            String[] buttonLabels = new String[] { IDialogConstants.YES_LABEL,
                    IDialogConstants.NO_LABEL };
            ConfirmationDialog dialog = new ConfirmationDialog(parent, title,
                    null, message, QUESTION, buttonLabels, 0);
            boolean result = dialog.open() == 0;

            // save user decision on suspending automatic builds
            ScopedPreferenceStore prefStore = new ScopedPreferenceStore(
                    new ProjectScope(project), VaadinPlugin.PLUGIN_ID);
            prefStore.setValue(VaadinPlugin.PREFERENCES_WIDGETSET_SUSPENDED,
                    dialog.suspended);

            if (dialog.suspended) {
                MessageDialog
                        .openInformation(
                                parent,
                                "Widgetset compilation suspended",
                                "Automatic widgetset recompilation for the project "
                                        + project.getName()
                                        + " is now suspended."
                                        + "A widgetset can be recompiled with the \"Compile widgetset\" button."
                                        + "Automatic recompilation can be re-enabled in the project properties.");
            }

            return result;
        }

    }

    private static final class CompileWidgetsetJob extends Job {
        private final IProject project;

        private CompileWidgetsetJob(String name, IProject project) {
            super(name);
            this.project = project;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            projectWidgetsetBuildPending.remove(project);
            internalCompile(project, monitor);
            return Status.OK_STATUS;
        }
    }

    /**
     * Which modules have a widgetset build running.
     * 
     * This is a set of project name + {@link IPath.SEPARATOR} + module name
     */
    private static Set<String> widgetsetBuildRunning = new HashSet<String>();

    /**
     * Which projects have a queued/pending widgetset build. This is set at
     * request time so that additional requests are ignored while waiting for
     * user confirmation.
     */
    private static Set<IProject> projectWidgetsetBuildPending = new HashSet<IProject>();

    /**
     * Which projects are being modified internally by the plugin and should not
     * be considered candidates for widgetset building.
     */
    private static Set<IProject> projectWidgetsetBuildSuspended = new HashSet<IProject>();

    /**
     * Ask the user whether he wants the widgetset(s) to be compiled and trigger
     * a build if necessary.
     * 
     * This method can be called in a background thread, and returns immediately
     * unless <code>synchronous</code> is <code>true</code>.
     * 
     * Multiple concurrent builds for a project are not allowed, but different
     * projects can be built concurrently. All widgetset builds for the project
     * are blocked until the user answers the question.
     * 
     * @param project
     *            the project whose widgetset(s) to compile
     * @param synchronous
     *            if true, do not return until the widgetset has been compiled
     *            or the user has chosen not to compile it
     * @param monitor
     *            progress monitor for synchronous execution, currently not used
     *            for asynchronous builds that run as separate jobs
     * @throws CoreException
     */
    public static void runWidgetSetBuildTool(final IProject project,
            final boolean synchronous, final IProgressMonitor monitor) {

        if (isBuildRunning(project) || isWidgetsetBuildingSuspended(project)) {
            // no message, ignore request
        } else if (!projectWidgetsetBuildPending.contains(project)) {
            projectWidgetsetBuildPending.add(project);

            // value modifiable by the nested anonymous class
            final boolean[] openQuestion = new boolean[] { false };

            Runnable runnable = new Runnable() {

                public void run() {
                    Shell shell = PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getShell();

                    // provides an option to disable further recompilation
                    // requests for the project, and tells how to re-enable
                    openQuestion[0] = ConfirmationDialog
                            .openQuestion(
                                    shell,
                                    "Compile widgetset",
                                    "Your client side code in the project "
                                            + project.getName()
                                            + " might need a recompilation. Compile widgetset now?",
                                    project);
                    if (!synchronous) {
                        if (openQuestion[0]) {
                            CompileWidgetsetJob job = new CompileWidgetsetJob(
                                    "Compiling wigetset for project "
                                            + project.getName() + "...",
                                    project);

                            job.setUser(false);

                            // lazily run job to let possible other changes
                            // modify project state, like dragging multiple jar
                            // files to classpath
                            job.schedule(500);
                        } else {
                            projectWidgetsetBuildPending.remove(project);
                        }
                    }
                }
            };

            if (synchronous) {
                PlatformUI.getWorkbench().getDisplay().syncExec(runnable);

                // do this in the caller thread synchronously to avoid blocking
                // the UI thread - otherwise Job.join() would be ok
                projectWidgetsetBuildPending.remove(project);
                if (openQuestion[0]) {
                    internalCompile(project, monitor);
                }
            } else {
                PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
            }
        }
    }

    private static boolean isBuildRunning(IProject project) {
        for (String running : widgetsetBuildRunning) {
            if (running.startsWith(project.getName() + IPath.SEPARATOR)) {
                return true;
            }
        }
        return false;
    }

    private static void internalCompile(IProject project,
            IProgressMonitor monitor) {
        try {
            final IJavaProject jproject = JavaCore.create(project);
            compileWidgetsets(jproject, monitor);
        } catch (CoreException e) {
            VaadinPluginUtil.handleBackgroundException(IStatus.ERROR,
                    "Widgetset compilation failed", e);
        } catch (IOException e) {
            VaadinPluginUtil.handleBackgroundException(IStatus.ERROR,
                    "Widgetset compilation failed", e);
        } catch (InterruptedException e) {
            VaadinPluginUtil.handleBackgroundException(IStatus.ERROR,
                    "Widgetset compilation failed", e);
        }
    }

    /**
     * Helper method to compile one or more widgetsets for given project.
     * 
     * {@see #compileWidgetset(IJavaProject, String, IProgressMonitor)}
     * 
     * If the project has multiple widgetsets and the user has not specified
     * which one to compile, notify the user.
     * 
     * Widgetset rebuild questions are not shown if a widgetset build request is
     * pending for the project, if the requested widgetset is being built, if
     * there is an internal operation in progress that has suspended widgetset
     * builds or if the user has explicitly suspended automatic widgetset
     * builds.
     * 
     * Note, this only works for projects with vaadin 6.2 and later.
     * 
     * @param shell
     *            the shell to enable asking the user which widgetset(s) to
     *            compile (if needed)
     * @param jproject
     * @param monitor
     * @throws CoreException
     * @throws IOException
     * @throws InterruptedException
     */
    public static void compileWidgetsets(IJavaProject jproject,
            final IProgressMonitor monitor) throws CoreException, IOException,
            InterruptedException {
        // if no more than one widgetset in the project, compile it (or
        // create a new one)
        IProject project = jproject.getProject();
        try {
            monitor.beginTask("Compiling widgetsets in project "
                    + project.getName(), 30);
            List<String> widgetsets = VaadinPluginUtil.findWidgetSets(jproject,
                    monitor);
            if (widgetsets.size() <= 1) {
                String widgetset = VaadinPluginUtil.getWidgetSet(jproject,
                        true, monitor);
                if (widgetset == null) {
                    showErrorMessage("No suitable widgetset location found",
                            "No suitable location found for the widgetset. This should not happen.");
                    return;
                }
                widgetset = widgetset.replace(".client.", ".");
                compileWidgetsetIfNotRunning(jproject, widgetset,
                        new SubProgressMonitor(monitor, 27));
                if (widgetsets.size() == 0) {
                    // refresh the created widgetset - need to find it first
                    String pathStr = widgetset.replace(".", "/") + ".gwt.xml";
                    IPackageFragmentRoot[] packageFragmentRoots = jproject
                            .getPackageFragmentRoots();
                    for (IPackageFragmentRoot root : packageFragmentRoots) {
                        if (!(root instanceof JarPackageFragmentRoot)) {
                            IResource underlyingResource = root
                                    .getUnderlyingResource();

                            if (underlyingResource instanceof IFolder) {
                                IFolder folder = (IFolder) underlyingResource;
                                IContainer parent = folder.getFile(pathStr)
                                        .getParent();
                                if (parent.exists()) {
                                    parent.refreshLocal(IResource.DEPTH_ONE,
                                            monitor);
                                }
                            }
                        }
                    }
                }
            } else {
                // TODO ask the user, compile all the selected ones
                // TODO queue immediately as separate tasks
                showErrorMessage(
                        "Select widgetset",
                        "Multiple widgetsets in project "
                                + project.getName()
                                + ". Select a widgetset file (..widgetset.gwt.xml) to compile.");
                // for (String widgetset : widgetsets) {
                // compileWidgetset(project, widgetset, monitor);
                // }
            }
        } finally {
            monitor.done();
        }
    }

    /**
     * Compile a widgetset if it is not already being compiled.
     * 
     * Show an error message if a build is already running.
     * 
     * @param jproject
     * @param widgetset
     *            widgetset GWT module name
     * @param monitor
     * 
     * @throws CoreException
     * @throws IOException
     * @throws InterruptedException
     */
    private static void compileWidgetsetIfNotRunning(IJavaProject jproject,
            String widgetset, IProgressMonitor monitor) throws CoreException,
            IOException, InterruptedException {
        IProject project = jproject.getProject();
        String key = project.getName() + IPath.SEPARATOR + widgetset;
        if (!widgetsetBuildRunning.contains(key)) {
            widgetsetBuildRunning.add(key);
            try {
                VaadinPluginUtil.compileWidgetset(jproject, widgetset, monitor);

                // could create a hosted mode launch here if it does not exist -
                // instead, do it on demand from the project properties as it
                // requires downloading GWT etc.

                // VaadinPluginUtil.createHostedModeLaunch(project);
            } finally {
                widgetsetBuildRunning.remove(key);
            }
        } else {
            showErrorMessage("Widgetset build running",
                    "A build for the widgetset " + widgetset
                            + " in the project " + project.getName()
                            + " is already running.");
        }
    }

    private static void showErrorMessage(final String title,
            final String message) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            public void run() {
                Shell shell = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell();
                MessageDialog.openError(shell, title, message);
            }
        });
    }

    /**
     * Extracts fully qualified widgetset name and project from given file
     * (expected to be *widgetset*.gwt.xml file) and compiles that widgetset.
     * 
     * @param file
     * @param monitor
     * @throws CoreException
     * @throws IOException
     * @throws InterruptedException
     */
    public static void compileWidgetset(IFile file, IProgressMonitor monitor)
            throws CoreException, IOException, InterruptedException {
        IProject project = file.getProject();
        IJavaProject jproject = JavaCore.create(project);

        IPackageFragmentRoot[] allPackageFragmentRoots = jproject
                .getAllPackageFragmentRoots();

        IPath rootPath = null;
        IPath location = file.getFullPath();
        for (int i = 0; rootPath == null && i < allPackageFragmentRoots.length; i++) {
            IPackageFragmentRoot root = allPackageFragmentRoots[i];
            IPath fullPath = root.getPath();
            if (location.toString().startsWith(fullPath.toString() + "/")) {
                rootPath = fullPath;
            }
        }
        String name = location.toString()
                .replace(rootPath.toString() + "/", "");

        String fqname = name.replace(".gwt.xml", "");
        fqname = fqname.replaceAll("/", ".");

        compileWidgetsetIfNotRunning(jproject, fqname, monitor);
    }

    /**
     * Checks whether widgetset building for a project is suspended, either
     * because of an internal operation in progress or because the user
     * explicitly suspended widgetset builds.
     * 
     * @param project
     * @return
     */
    private static boolean isWidgetsetBuildingSuspended(final IProject project) {
        return projectWidgetsetBuildSuspended.contains(project)
                || isWidgetsetBuildsSuspended(project);
    }

    /**
     * Checks whether widgetset building for a project has been suspended
     * explicitly by the user.
     * 
     * @param project
     * @return
     */
    public static boolean isWidgetsetBuildsSuspended(final IProject project) {
        ScopedPreferenceStore prefStore = new ScopedPreferenceStore(
                new ProjectScope(project), VaadinPlugin.PLUGIN_ID);

        return prefStore
                .getBoolean(VaadinPlugin.PREFERENCES_WIDGETSET_SUSPENDED);
    }

    /**
     * Persistently suspend widgetset build questions on user demand for a
     * project.
     * 
     * As a side effect, remove the project from the pending list.
     * 
     * @see #runWidgetSetBuildTool(IProject, boolean, IProgressMonitor)
     * 
     * @param project
     * @param suspend
     */
    public static void setWidgetsetBuildsSuspended(IProject project,
            boolean suspend) {
        ScopedPreferenceStore prefStore = new ScopedPreferenceStore(
                new ProjectScope(project), VaadinPlugin.PLUGIN_ID);

        prefStore.setValue(VaadinPlugin.PREFERENCES_WIDGETSET_SUSPENDED,
                suspend);

        // make sure we don't get stuck in an inconsistent state
        projectWidgetsetBuildPending.remove(project);
    }

    /**
     * Suspend widgetset build requests for a project while internal
     * modifications to the project are being performed by the plugin.
     * 
     * @see #internalResumeWidgetsetBuilds(IProject)
     * 
     * @param project
     */
    public static void internalSuspendWidgetsetBuilds(IProject project) {
        projectWidgetsetBuildSuspended.add(project);
    }

    /**
     * Resume making suspended widgetset build requests for a project.
     * 
     * No requests are queued for the project while widgetset builds are
     * suspended, and the caller should call {@link #runWidgetSetBuildTool()}
     * after this call if necessary.
     * 
     * This method must be called after
     * {@link #internalSuspendWidgetsetBuilds(IProject)}, a try-finally pattern
     * is strongly recommended.
     * 
     * @param project
     */
    public static void internalResumeWidgetsetBuilds(IProject project) {
        projectWidgetsetBuildSuspended.remove(project);
    }

}
