package com.vaadin.integration.eclipse.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jst.j2ee.web.componentcore.util.WebArtifactEdit;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;

import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.VaadinPlugin;

public class ProjectUtil {
    /**
     * Find a project that has the Vaadin project facet based on a selection.
     * 
     * If the selection is an element in a suitable project, return that
     * project.
     * 
     * Otherwise, return null.
     * 
     * @param selection
     * @return a Vaadin project
     */
    public static IProject getProject(ISelection selection) {
        IProject project = null;
        if (selection != null && selection.isEmpty() == false
                && selection instanceof IStructuredSelection) {
            IStructuredSelection ssel = (IStructuredSelection) selection;
            if (ssel.size() == 1) {
                Object obj = ssel.getFirstElement();
                if (ssel instanceof TreeSelection) {
                    TreeSelection ts = (TreeSelection) ssel;
                    obj = ts.getPaths()[0].getFirstSegment();
                } else {
                    obj = ssel.getFirstElement();
                }
                if (obj instanceof IJavaProject) {
                    return ((IJavaProject) obj).getProject();
                }
                if (obj instanceof IResource) {
                    project = getProject((IResource) obj);
                } else if (obj instanceof IJavaProject) {
                    project = ((IJavaProject) obj).getProject();
                }
            }
        }
        return project;
    }

    /**
     * Find a project that has the Vaadin project facet based on a resource.
     * 
     * If the resource is an element in a suitable project, return that project.
     * 
     * Otherwise, return null.
     * 
     * @param selection
     * @return a Vaadin project or null
     */
    public static IProject getProject(IResource resource) {
        IContainer container = null;
        IProject project = null;
        if (resource instanceof IContainer) {
            container = (IContainer) resource;
        } else if (resource != null) {
            container = (resource).getParent();
        }
        if (container != null
                && VaadinFacetUtils.isVaadinProject(container.getProject())) {
            project = container.getProject();
        }
        return project;
    }

    public static IFolder getWebInfLibFolder(IProject project)
            throws CoreException {
        IVirtualComponent component = ComponentCore.createComponent(project);
        if (component == null) {
            throw ErrorUtil
                    .newCoreException("Unable to find WEB-INF/lib folder. Ensure the project is a dynamic web project.");
        }
        IVirtualFolder contentFolder = component.getRootFolder();
        return (IFolder) contentFolder.getFolder(WebArtifactEdit.WEBLIB)
                .getUnderlyingFolder();
    }

    public static IFolder getWebInfFolder(IProject project)
            throws CoreException {
        IVirtualComponent component = ComponentCore.createComponent(project);
        if (component == null) {
            throw ErrorUtil
                    .newCoreException("Unable to locate WEB-INF folder. Ensure the project is a dynamic web project.");
        }
        IVirtualFolder contentFolder = component.getRootFolder();
        IContainer underlying = contentFolder
                .getFolder(WebArtifactEdit.WEB_INF).getUnderlyingFolder();
        if (!(underlying instanceof IFolder)) {
            throw ErrorUtil
                    .newCoreException("Unable to locate WEB-INF folder. Ensure the project is a dynamic web project.");
        }
        return (IFolder) underlying;
    }

    public static IFolder getWebContentFolder(IProject project)
            throws CoreException {
        IVirtualComponent component = ComponentCore.createComponent(project);
        if (component == null) {
            throw ErrorUtil
                    .newCoreException("Unable to locate WebContent folder. Ensure the project is a dynamic web project.");
        }
        IVirtualFolder contentFolder = component.getRootFolder();
        IContainer underlying = contentFolder.getUnderlyingFolder();
        if (!(underlying instanceof IFolder)) {
            throw ErrorUtil
                    .newCoreException("Unable to locate WebContent folder. Ensure the project is a dynamic web project.");
        }
        return (IFolder) underlying;
    }

    public static IFolder getSrcFolder(IProject project) throws CoreException {
        try {
            IJavaProject javaProject = JavaCore.create(project);
            for (IClasspathEntry classPathEntry : javaProject.getRawClasspath()) {
                if (classPathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    IPath path = classPathEntry.getPath();
                    return project.getWorkspace().getRoot().getFolder(path);
                    // return project.getFolder(path);
                }
            }

            return null;
        } catch (JavaModelException e) {
            throw ErrorUtil.newCoreException("Unable to locate source folder",
                    e);
        }
    }

    /**
     * Find the Vaadin Application type for a project or null if none found.
     * 
     * @param jproject
     * @return
     * @throws JavaModelException
     */
    public static IType findVaadinApplicationType(IJavaProject jproject)
            throws JavaModelException {
        if (jproject == null) {
            return null;
        }
        return jproject.findType(VaadinPlugin.APPLICATION_CLASS_FULL_NAME);
    }

    public static String getRequiredGWTVersionForProject(IJavaProject jproject) {
        // if no information exists, default to 2.0.4
        String gwtVersion = "2.0.4";

        try {
            // find Vaadin JAR on the classpath
            IPath vaadinJarPath = ProjectUtil
                    .findProjectVaadinJarPath(jproject);
            if (vaadinJarPath == null) {
                throw ErrorUtil.newCoreException("Could not access Vaadin JAR",
                        null);
            }
            File vaadinJarFile = vaadinJarPath.toFile();
            if (!vaadinJarFile.exists()) {
                return gwtVersion;
            }

            // Check gwt version from included Vaadin jar
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(vaadinJarFile.getAbsolutePath());
                ZipEntry entry = jarFile.getEntry("META-INF/GWT-VERSION");
                if (entry == null) {
                    // found JAR but not GWT version information in it, use
                    // default
                    return gwtVersion;
                }

                // extract GWT version from the JAR
                InputStream gwtVersionStream = jarFile.getInputStream(entry);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(gwtVersionStream));

                gwtVersion = reader.readLine();
            } finally {
                if (jarFile != null) {
                    VaadinPluginUtil.closeJarFile(jarFile);
                }
            }
        } catch (IOException ex) {
            ErrorUtil.handleBackgroundException(IStatus.WARNING,
                    "Failed to determine the GWT library version to use, defaulting to "
                            + gwtVersion, ex);
        } catch (CoreException ex) {
            ErrorUtil.handleBackgroundException(IStatus.WARNING,
                    "Failed to determine the GWT library version to use, defaulting to "
                            + gwtVersion, ex);
        }

        return gwtVersion;
    }

    public static boolean isGwt20(IProject project) {
        IJavaProject jproject = JavaCore.create(project);
        if (jproject != null) {
            try {
                if (jproject.findType("com.google.gwt.dev.DevMode") != null) {
                    return true;
                }
            } catch (JavaModelException e) {
                ErrorUtil
                        .handleBackgroundException(
                                IStatus.WARNING,
                                "Failed to check the GWT version used in the project, assuming 1.x",
                                e);
            }
        }
        // default value
        return false;
    }

    /**
     * Checks which Vaadin version is in use in the project. This optionally
     * also checks the classpath for Vaadin JARs and deduces the version from
     * the meta-data inside the JAR if no official Vaadin JAR is found in
     * WEB-INF/lib.
     * 
     * @param project
     * @param useClasspath
     *            true to also search elsewhere on the classpath if no Vaadin
     *            JAR is found in WEB-INF/lib
     * @return The version of the Vaadin JAR currently in the project. Returns
     *         null if no Vaadin JAR was found or if the version number could
     *         not be determined.
     * @throws CoreException
     */
    public static String getVaadinLibraryVersion(IProject project,
            boolean useClasspath) throws CoreException {
        IFolder lib = ProjectUtil.getWebInfLibFolder(project);
        if (lib.exists()) {
            IResource[] files = lib.members();
            for (IResource resource : files) {
                // is it a Vaadin JAR?
                if (resource instanceof IFile) {
                    if (VersionUtil.couldBeVaadinJar(resource.getName())) {
                        // Name matches vaadin jar, still check for version from
                        // the jar itself

                        String version = VersionUtil
                                .getVaadinVersionFromJar(resource.getFullPath());

                        if (version != null) {
                            return version;
                        }
                    }
                }
            }
        }

        if (useClasspath) {
            IJavaProject jproject = JavaCore.create(project);
            IPath resource = ProjectUtil.findProjectVaadinJarPath(jproject);
            return VersionUtil.getVaadinVersionFromJar(resource);
        }

        return null;
    }

    public static IPath findProjectVaadinJarPath(IJavaProject javaProject)
            throws CoreException {
        IJavaElement type;
        type = ProjectUtil.findVaadinApplicationType(javaProject);

        while (type != null && type.getParent() != null) {
            if (type instanceof IPackageFragmentRoot) {
                IPackageFragmentRoot jar = (IPackageFragmentRoot) type;
                IResource resource = jar.getResource();
                if (resource == null) {
                    // Galileo
                    return jar.getPath();
                } else {
                    // Ganymede
                    IPath rawLocation = resource.getRawLocation();
                    return rawLocation;
                }
            }
            type = type.getParent();
        }

        // at project creation, maybe not yet compiled => search for JARs on
        // the classpath
        IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
        for (IClasspathEntry cp : rawClasspath) {
            if (cp.toString().contains(".jar")) {
                // User has explicitly defined GWT version to use directly on
                // the classpath, or classpath entry created by the plugin
                IClasspathEntry resolvedClasspathEntry = JavaCore
                        .getResolvedClasspathEntry(cp);
                IPath path = resolvedClasspathEntry.getPath();
                if (VaadinPluginUtil.isVaadinJar(path)) {
                    return path;
                }
            } else if (cp.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                // primarily WEB-INF/lib, but possibly also Liferay etc.
                IClasspathContainer container = JavaCore.getClasspathContainer(
                        cp.getPath(), javaProject);
                IClasspathEntry[] containerEntries = container
                        .getClasspathEntries();
                for (IClasspathEntry ccp : containerEntries) {
                    if (ccp.toString().contains(".jar")) {
                        // User has explicitly defined GWT version to use
                        IClasspathEntry resolvedClasspathEntry = JavaCore
                                .getResolvedClasspathEntry(ccp);
                        IPath path = resolvedClasspathEntry.getPath();
                        if (VaadinPluginUtil.isVaadinJar(path)) {
                            return path;
                        }
                    }
                }
            }
        }

        // still no luck? check WEB-INF/lib
        IFolder lib = ProjectUtil.getWebInfLibFolder(javaProject.getProject());
        if (!lib.exists()) {
            return null;
        }
        for (IResource resource : lib.members()) {
            // is it a Vaadin JAR?
            if (resource instanceof IFile
                    && VaadinPluginUtil.isVaadinJar(resource.getLocation())) {
                return resource.getLocation();
            }
        }

        // For some reason we were not able to locate the Vaadin JAR
        return null;
    }

    /**
     * Check is a project uses Vaadin 6.2 or later.
     * 
     * @param project
     * @return
     */
    public static boolean isVaadin62(IProject project) {
        IPath findProjectVaadinJarPath;
        try {
            findProjectVaadinJarPath = ProjectUtil
                    .findProjectVaadinJarPath(JavaCore.create(project));
        } catch (CoreException e) {
            return false;
        }

        return findProjectVaadinJarPath != null
                && VaadinPluginUtil
                        .isWidgetsetPackage(findProjectVaadinJarPath);

    }

}
