package com.vaadin.integration.eclipse.builder;

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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.vaadin.integration.eclipse.util.VaadinPluginUtil;

public class WidgetsetBuilder extends IncrementalProjectBuilder {

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

            if (VaadinPluginUtil.isWidgetsetPackage(resource.getRawLocation())) {
                switch (delta.getKind()) {
                case IResourceDelta.ADDED:
                    // skip build when the Vaadin JAR is added at project
                    // creation - needed for Ganymede?
                    IProject project = resource.getProject();
                    IJavaProject jproject = JavaCore.create(project);
                    IPath vaadinJarPath = VaadinPluginUtil
                            .findProjectVaadinJarPath(jproject);
                    vaadinJarPath = VaadinPluginUtil.getRawLocation(project,
                            vaadinJarPath);
                    IPath resourcePath = VaadinPluginUtil.getRawLocation(
                            project, resource.getRawLocation());
                    if (resourcePath.equals(vaadinJarPath)) {
                        break;
                    }
                    // fall-through: continue like change or JAR
                case IResourceDelta.CHANGED:
                case IResourceDelta.REPLACED:
                    // compile will clear the dirty flag
                    VaadinPluginUtil.setWidgetsetDirty(getProject(), true);
                    WidgetsetBuildManager.runWidgetSetBuildTool(getProject(),
                            false, monitor);

                    break;
                case IResourceDelta.REMOVED:
                    // we never come here:
                    // cannot reliably detect deletion of a widgetset package as
                    // cannot look inside it one it has been removed; handled
                    // below as any JAR removal instead
                }
            } else if (delta.getKind() == IResourceDelta.REMOVED
                    && isJar(resource)) {
                // when a JAR is removed, we cannot look inside it so assume it
                // might have been a widgetset package
                // TODO #3590 clean GWT module
                VaadinPluginUtil.setWidgetsetDirty(getProject(), true);
                WidgetsetBuildManager.runWidgetSetBuildTool(getProject(),
                        false, monitor);
            } else if (resource.exists()
                    && (isGwtModule(resource)
                            || isComponentWithWidgetAnnotation(resource) || isClientSideJavaClass(resource))) {
                switch (delta.getKind()) {
                case IResourceDelta.ADDED:
                case IResourceDelta.CHANGED:
                    VaadinPluginUtil.setWidgetsetDirty(getProject(), true);

                    break;
                case IResourceDelta.REMOVED:
                    break;
                }
            }

            // return true to continue visiting children.
            return true;
        }

        private boolean isJar(IResource resource) {
            return resource instanceof IFile
                    && resource.getName().endsWith(".jar");
        }

        private boolean isGwtModule(IResource resource) {
            return resource instanceof IFile
                    && resource.getName().endsWith(".gwt.xml");
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
                    VaadinPluginUtil
                            .handleBackgroundException(
                                    IStatus.WARNING,
                                    "Could not check if "
                                            + resource.getName()
                                            + " is a server-side class for a widget",
                                    e);
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

        // detect if widget set compile is needed, then run tool; the
        // "dirty flag" must be saved to file system, so that new build is not
        // needed on startup
        if (VaadinPluginUtil.isWidgetsetDirty(getProject())) {
            WidgetsetBuildManager.runWidgetSetBuildTool(getProject(), false,
                    monitor);
        }
    }

    protected void incrementalBuild(IResourceDelta delta,
            IProgressMonitor monitor) throws CoreException {
        // the visitor does the work.
        delta.accept(new SampleDeltaVisitor(monitor));
    }
}
