package com.vaadin.integration.eclipse.builder;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.vaadin.integration.eclipse.util.VaadinPluginUtil;

public class WidgetsetBuilder extends IncrementalProjectBuilder {

    boolean widgetsetBuildPending = false;

    class SampleDeltaVisitor implements IResourceDeltaVisitor {
        private IProgressMonitor monitor;

        public SampleDeltaVisitor(IProgressMonitor monitor) {
            this.monitor = monitor;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse
         * .core.resources.IResourceDelta)
         */
        public boolean visit(IResourceDelta delta) throws CoreException {
            IResource resource = delta.getResource();
            if (resource instanceof IFolder) {
                IFolder f = (IFolder) resource;
                if (f.getName().equals("build")) {
                    return false;
                }
            }

            // TODO never gets to the REMOVED branch as cannot open the JAR
            if (VaadinPluginUtil.isWidgetsetPackage(resource.getRawLocation())) {
                switch (delta.getKind()) {
                case IResourceDelta.ADDED:
                    runWidgetSetBuildTool(monitor);
                    break;
                case IResourceDelta.REMOVED:
                    runWidgetSetBuildTool(monitor);
                    break;
                case IResourceDelta.CHANGED:
                    runWidgetSetBuildTool(monitor);
                    break;
                }
            } else if (isComponentWithWidgetAnnotation(resource)
                    || isClientSideJavaClass(resource)) {
                switch (delta.getKind()) {
                case IResourceDelta.ADDED:
                case IResourceDelta.CHANGED:
                    /*
                     * TODO this should just turn on some sort of indicator that
                     * widgetset compilation is pending, instead of aggressively
                     * start compilation. "Dirty widgetset" flag to either file
                     * or widgetsets gwt module file. Users dont want to waste
                     * their time or cpu cycles here.
                     */
                    boolean weWantToGetRidOfPluginUsersTotally = false;
                    if (weWantToGetRidOfPluginUsersTotally) {
                        runWidgetSetBuildTool(monitor);
                    }
                    break;
                case IResourceDelta.REMOVED:
                    break;
                }
            }

            // return true to continue visiting children.
            return true;
        }

        private boolean isClientSideJavaClass(IResource resource) {
            // TODO Check if given resource is java file in gwt module
            return false;
        }

        private boolean isComponentWithWidgetAnnotation(IResource resource) {
            if (resource instanceof IFile
                    && resource.getName().endsWith(".java")) {
                IFile file = (IFile) resource;

                ICompilationUnit createCompilationUnitFrom = JavaCore
                        .createCompilationUnitFrom(file);
                try {
                    IType[] types = createCompilationUnitFrom.getTypes();
                    for (int i = 0; i < types.length; i++) {
                        IType t = types[i];
                        IAnnotation[] annotations = t.getAnnotations();
                        for (int j = 0; j < annotations.length; j++) {
                            if (annotations[j].getElementName().contains(
                                    "ClientWidget")) {
                                return true;
                            }
                        }
                    }
                } catch (JavaModelException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return false;
        }

    }

    private Date start;

    public static final String BUILDER_ID = "com.vaadin.integration.eclipse.widgetsetBuilder";

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
     * java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
            throws CoreException {
        if (kind == FULL_BUILD) {
            fullBuild(monitor);
        } else {
            start = new Date();
            IResourceDelta delta = getDelta(getProject());
            if (delta == null) {
                fullBuild(monitor);
            } else {
                incrementalBuild(delta, monitor);
            }
            System.out.println("Vaadin WS: Incr build took: "
                    + (new Date().getTime() - start.getTime()));
        }
        return null;
    }

    protected void fullBuild(final IProgressMonitor monitor)
            throws CoreException {

        /*
         * TODO detect if widget set is needed, then run tool the "dirty flag"
         * must be saved to file system, so that new build is not needed on
         * startup
         */
        if (false) {
            runWidgetSetBuildTool(monitor);
        }
    }

    private void runWidgetSetBuildTool(final IProgressMonitor monitor)
            throws CoreException {

        if (!widgetsetBuildPending) {
            widgetsetBuildPending = true;
            final IJavaProject p = JavaCore.create(getProject());

            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

                public void run() {
                    final Shell shell = PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getShell();

                    boolean openQuestion = MessageDialog
                            .openQuestion(shell, "Compile widgetset",
                                    "Your client side code might need a recompilation. Compile widgetset now?");
                    if (openQuestion) {

                        Job job = new Job("Compiling widgetset...") {
                            @Override
                            protected IStatus run(IProgressMonitor monitor) {
                                widgetsetBuildPending = false;
                                monitor.beginTask("Compiling wigetset", 100);
                                try {
                                    VaadinPluginUtil.compileWidgetsets(shell,
                                            p, monitor);
                                } catch (CoreException e) {
                                    VaadinPluginUtil.handleBackgroundException(
                                            IStatus.ERROR,
                                            "Widgetset compilation failed", e);
                                } catch (IOException e) {
                                    VaadinPluginUtil.handleBackgroundException(
                                            IStatus.ERROR,
                                            "Widgetset compilation failed", e);
                                } catch (InterruptedException e) {
                                    VaadinPluginUtil.handleBackgroundException(
                                            IStatus.ERROR,
                                            "Widgetset compilation failed", e);
                                }
                                monitor.worked(100);
                                monitor.done();
                                return Status.OK_STATUS;
                            }
                        };

                        job.setUser(false);
                        // lazily run job to let possible other changes modify
                        // project state, like dragging multiple jar files to
                        // classpath
                        job.schedule(500);
                    } else {
                        widgetsetBuildPending = false;
                    }
                }
            });

        }

    }

    protected void incrementalBuild(IResourceDelta delta,
            IProgressMonitor monitor) throws CoreException {
        // the visitor does the work.
        delta.accept(new SampleDeltaVisitor(monitor));
    }
}
