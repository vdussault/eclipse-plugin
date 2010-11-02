package com.vaadin.integration.eclipse.builder;

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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;
import com.vaadin.integration.eclipse.util.VersionUtil;
import com.vaadin.integration.eclipse.util.WidgetsetUtil;

public class WidgetsetBuilder extends IncrementalProjectBuilder {

    class WidgetsetResourceDeltaVisitor implements IResourceDeltaVisitor {
        private IProgressMonitor monitor;

        public WidgetsetResourceDeltaVisitor(IProgressMonitor monitor) {
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

            if (WidgetsetUtil.isWidgetsetPackage(resource.getRawLocation())) {
                switch (delta.getKind()) {
                case IResourceDelta.ADDED:
                case IResourceDelta.CHANGED:
                    // Do not set the widgetset as dirty if there is no
                    // widgetset
                    IProject project = resource.getProject();
                    IJavaProject jproject = JavaCore.create(project);
                    IPath vaadinJarPath = ProjectUtil
                            .findProjectVaadinJarPath(jproject);
                    vaadinJarPath = VaadinPluginUtil.getRawLocation(project,
                            vaadinJarPath);
                    IPath resourcePath = VaadinPluginUtil.getRawLocation(
                            project, resource.getRawLocation());
                    if (resourcePath.equals(vaadinJarPath)) {
                        // #3869/#5214 only mark as dirty if the project has a
                        // custom widgetset
                        if (!WidgetsetUtil.hasWidgetSets(jproject, monitor)) {
                            break;
                        }
                    }

                    // compile will clear the dirty flag
                    WidgetsetUtil.setWidgetsetDirty(getProject(), true);
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
                // When a JAR is removed, we cannot look inside it so assume it
                // might have been a widgetset package.
                // However, if it is a Vaadin JAR, do not mark the widgetset as
                // dirty - that will be done when adding a new Vaadin JAR to
                // the project (#3869).
                // TODO #3590 clean GWT module
                if (!VersionUtil.couldBeOfficialVaadinJar(resource.getName())) {
                    boolean hasWidgetset = false;
                    try {
                        hasWidgetset = WidgetsetUtil.hasWidgetSets(
                                JavaCore.create(getProject()),
                                new NullProgressMonitor());
                    } catch (CoreException e) {
                        ErrorUtil
                                .handleBackgroundException(
                                        IStatus.WARNING,
                                        "Could not check if project has widgetsets, not marking as dirty",
                                        e);
                    }
                    if (hasWidgetset) {
                        WidgetsetUtil.setWidgetsetDirty(getProject(), true);
                        WidgetsetBuildManager.runWidgetSetBuildTool(
                                getProject(), false, monitor);
                    }
                }
            } else if (resource.exists()
                    && (isGwtModule(resource) || isClientSideJavaClass(resource))) {
                switch (delta.getKind()) {
                case IResourceDelta.ADDED:
                case IResourceDelta.CHANGED:
                case IResourceDelta.REMOVED:
                    // removed might not arrive here as resource does not
                    // exist...
                    WidgetsetUtil.setWidgetsetDirty(getProject(), true);

                    break;
                }
            } else if (resource.exists()
                    && isComponentWithWidgetAnnotation(resource)) {
                switch (delta.getKind()) {
                case IResourceDelta.ADDED:
                case IResourceDelta.REMOVED:
                    // removed should never arrive here as does not exist...
                    WidgetsetUtil.setWidgetsetDirty(getProject(), true);
                    break;
                case IResourceDelta.CHANGED:
                    // TODO only if the @ClientWidget annotation changed
                    // VaadinPluginUtil.setWidgetsetDirty(getProject(), true);
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

        /**
         * Check if given resource is java file in gwt module.
         * 
         * Only resources with "client" as a segment of their path are
         * considered. The existence of a GWT module in the parent of that
         * segment is then checked.
         * 
         * @param resource
         * @return true if the resource is a java file in a GWT module
         */
        private boolean isClientSideJavaClass(IResource resource) {
            if (resource instanceof IFile
                    && resource.getName().endsWith(".java")) {
                IPath path = resource.getProjectRelativePath();

                for (int i = 1; i < path.segmentCount(); ++i) {
                    if ("client".equals(path.segment(i))) {
                        // check if the parent has a GWT module
                        IFolder parent = resource.getProject().getFolder(
                                path.uptoSegment(i));
                        try {
                            for (IResource child : parent.members()) {
                                if (child.getName().endsWith(".gwt.xml")) {
                                    return true;
                                }
                            }
                        } catch (CoreException e) {
                            ErrorUtil.handleBackgroundException(
                                    IStatus.WARNING,
                                    "Could not list children of folder "
                                            + parent.getFullPath(), e);
                            // continue search - maybe multiple .client. in path
                        }
                    }
                }
            }
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
                    ErrorUtil
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

    public static final String BUILDER_ID = "com.vaadin.integration.eclipse.widgetsetBuilder";

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
     * java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
     */
    @SuppressWarnings("rawtypes")
    @Override
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
            throws CoreException {
        if (kind == FULL_BUILD) {
            fullBuild(monitor);
        } else {
            IResourceDelta delta = getDelta(getProject());
            if (delta == null) {
                fullBuild(monitor);
            } else {
                incrementalBuild(delta, monitor);
            }
        }
        return null;
    }

    protected void fullBuild(final IProgressMonitor monitor)
            throws CoreException {

        // detect if widget set compile is needed, then run tool; the
        // "dirty flag" must be saved to file system, so that new build is not
        // needed on startup
        if (WidgetsetUtil.isWidgetsetDirty(getProject())) {
            WidgetsetBuildManager.runWidgetSetBuildTool(getProject(), false,
                    monitor);
        }
    }

    protected void incrementalBuild(IResourceDelta delta,
            IProgressMonitor monitor) throws CoreException {
        // the visitor does the work.
        delta.accept(new WidgetsetResourceDeltaVisitor(monitor));
    }
}
