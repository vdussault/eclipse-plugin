package com.vaadin.integration.eclipse.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jst.j2ee.web.componentcore.util.WebArtifactEdit;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.osgi.framework.Bundle;

import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.builder.WidgetsetBuildManager;
import com.vaadin.integration.eclipse.builder.WidgetsetNature;
import com.vaadin.integration.eclipse.util.DownloadUtils.Version;
import com.vaadin.integration.eclipse.variables.VaadinClasspathVariableInitializer;
import com.vaadin.integration.eclipse.wizards.DirectoryManifestProvider;

public class VaadinPluginUtil {

    public static final String DEFAULT_WIDGET_SET_NAME = "com.vaadin.terminal.gwt.DefaultWidgetSet";

    private static final String WS_COMPILATION_CONSOLE_NAME = "Vaadin Widgetset Compilation";

    /**
     * Handle an exception in a background thread or other non-UI context. The
     * handling primarily consists of tracing the exception.
     *
     * @param t
     */
    public static void handleBackgroundException(Throwable t) {
        handleBackgroundException(t.getMessage(), t);
    }

    /**
     * Handle an exception in a background thread or other non-UI context. The
     * handling primarily consists of tracing the exception.
     *
     * @param message
     * @param t
     */
    public static void handleBackgroundException(String message, Throwable t) {
        handleBackgroundException(IStatus.ERROR, message, t);
    }

    /**
     * Handle an exception in a background thread or other non-UI context. The
     * handling primarily consists of tracing the exception.
     *
     * @param severity
     *            IStatus.OK, IStatus.INFO, IStatus.WARNING or IStatus.ERROR
     * @param message
     * @param t
     */
    public static void handleBackgroundException(int severity, String message,
            Throwable t) {
        // TODO trace the exception and do any other background exception
        // handling
        IStatus status = new Status(severity, VaadinPlugin.PLUGIN_ID, message,
                t);
        VaadinPlugin.getInstance().getLog().log(status);
        // ex.printStackTrace();
    }

    /**
     * Logs an information level event to the Eclipse error log (or other
     * configured destination).
     *
     * @param message
     */
    public static void logInfo(String message) {
        IStatus status = new Status(IStatus.INFO, VaadinPlugin.PLUGIN_ID,
                message);
        VaadinPlugin.getInstance().getLog().log(status);
        // ex.printStackTrace();
    }

    /**
     * Logs a warning not related to an exception to the Eclipse error log (or
     * other configured destination).
     *
     * @param message
     */
    public static void logWarning(String message) {
        IStatus status = new Status(IStatus.WARNING, VaadinPlugin.PLUGIN_ID,
                message);
        VaadinPlugin.getInstance().getLog().log(status);
        // ex.printStackTrace();
    }

    /**
     * Display an error message to the user.
     *
     * @param message
     * @param ex
     */
    public static void displayError(String message, Throwable ex, Shell shell) {
        // TODO trace if needed and report to the user
        MessageDialog.openError(shell, "Error", message);
    }

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

    public static IFolder getWebInfLibFolder(IProject project) {
        IVirtualComponent component = ComponentCore.createComponent(project);
        IVirtualFolder contentFolder = component.getRootFolder();
        return (IFolder) contentFolder.getFolder(WebArtifactEdit.WEBLIB)
                .getUnderlyingFolder();
    }

    public static IFolder getWebInfFolder(IProject project) {
        IVirtualComponent component = ComponentCore.createComponent(project);
        IVirtualFolder contentFolder = component.getRootFolder();
        return (IFolder) contentFolder.getFolder(WebArtifactEdit.WEB_INF)
                .getUnderlyingFolder();
    }

    public static IFolder getWebContentFolder(IProject project) {
        IVirtualComponent component = ComponentCore.createComponent(project);
        IVirtualFolder contentFolder = component.getRootFolder();
        return (IFolder) contentFolder.getUnderlyingFolder();
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
            throw newCoreException("Unable to locate source folder", e);
        }
    }

    /**
     * Find the Vaadin Application type for a project or null if none found.
     *
     * @param jproject
     * @return
     * @throws JavaModelException
     */
    private static IType findVaadinApplicationType(IJavaProject jproject)
            throws JavaModelException {
        if (jproject == null) {
            return null;
        }
        IType type = jproject.findType(VaadinPlugin.VAADIN_PACKAGE_PREFIX
                + VaadinPlugin.APPLICATION_CLASS_NAME);
        if (type == null) {
            type = jproject.findType(VaadinPlugin.TOOLKIT_PACKAGE_PREFIX
                    + VaadinPlugin.APPLICATION_CLASS_NAME);
        }
        return type;
    }

    /**
     * Returns either "com.vaadin." or "com.itmill.toolkit." depending on the
     * Vaadin version in the project. Defaults to "com.vaadin." if neither
     * found.
     *
     * @param project
     * @return
     */
    public static String getVaadinPackagePrefix(IProject project) {
        if (isVaadin6(project)) {
            return VaadinPlugin.VAADIN_PACKAGE_PREFIX;
        } else {
            return VaadinPlugin.TOOLKIT_PACKAGE_PREFIX;
        }
    }

    /**
     * Returns either "VAADIN" or "ITMILL" depending on the Vaadin version in
     * the project, returning a default value if neither is found.
     *
     * @param project
     * @return
     */
    public static String getVaadinResourceDirectory(IProject project) {
        if (isVaadin6(project)) {
            return "VAADIN";
        } else {
            return "ITMILL";
        }
    }

    /**
     * Checks the Vaadin version, returns true for Vaadin 6.0+, false for IT
     * Mill Toolkit.
     *
     * @param project
     * @return true if a Vaadin project (or unknown), false for IT Mill Toolkit
     *         project
     */
    public static boolean isVaadin6(IProject project) {
        IJavaProject jproject = JavaCore.create(project);
        if (jproject != null) {
            try {
                if (jproject.findType(VaadinPlugin.VAADIN_PACKAGE_PREFIX
                        + VaadinPlugin.APPLICATION_CLASS_NAME) != null) {
                    return true;
                } else if (jproject
                        .findType(VaadinPlugin.TOOLKIT_PACKAGE_PREFIX
                                + VaadinPlugin.APPLICATION_CLASS_NAME) != null) {
                    return false;
                }
            } catch (JavaModelException e) {
                handleBackgroundException(
                        IStatus.WARNING,
                        "Failed to check the Vaadin version used in the project, assuming 6.0+",
                        e);
                return true;
            }
        }
        // default value
        return true;
    }

    /**
     * Check is a project uses Vaadin 6.2 or later.
     *
     * @param project
     * @return
     */
    public static boolean isVaadin62(IProject project) {
        if (!isVaadin6(project)) {
            return false;
        }

        IPath findProjectVaadinJarPath;
        try {
            findProjectVaadinJarPath = findProjectVaadinJarPath(JavaCore
                    .create(project));
        } catch (CoreException e) {
            return false;
        }

        return findProjectVaadinJarPath != null
                && VaadinPluginUtil
                        .isWidgetsetPackage(findProjectVaadinJarPath);

    }

    public static boolean isGwt20(IProject project) {
        IJavaProject jproject = JavaCore.create(project);
        if (jproject != null) {
            try {
                if (jproject.findType("com.google.gwt.dev.DevMode") != null) {
                    return true;
                }
            } catch (JavaModelException e) {
                handleBackgroundException(
                        IStatus.WARNING,
                        "Failed to check the GWT version used in the project, assuming 1.x",
                        e);
            }
        }
        // default value
        return false;
    }

    private static IPackageFragmentRoot getJavaFragmentRoot(IProject project)
            throws CoreException {
        try {
            IJavaProject javaProject = JavaCore.create(project);
            for (IClasspathEntry classPathEntry : javaProject.getRawClasspath()) {
                if (classPathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    IPackageFragmentRoot fragmentRoot = javaProject
                            .findPackageFragmentRoot(classPathEntry.getPath());
                    return fragmentRoot;
                }
            }

            return null;
        } catch (JavaModelException e) {
            throw newCoreException("Unable to locate source folder", e);
        }
    }

    /**
     * Create a configuration file from a template if it does not exist.
     *
     * @param file
     *            the file to create from template
     * @param template
     * @throws CoreException
     */
    @SuppressWarnings("unchecked")
    public static IFile ensureFileFromTemplate(IFile file, String template)
            throws CoreException {

        try {
            if (file.exists()) {
                return file;
            }

            String stub = VaadinPluginUtil.readTextFromTemplate(template);

            ByteArrayInputStream stubstream = new ByteArrayInputStream(stub
                    .getBytes());

            file.create(stubstream, true, null);

            return file;

        } catch (JavaModelException e) {
            throw VaadinPluginUtil.newCoreException("Failed to create "
                    + file.getName() + " file", e);
        } catch (IOException e) {
            throw VaadinPluginUtil.newCoreException("Failed to create "
                    + file.getName() + " file", e);
        }
    }

    public static void copyPluginFileToProject(IPath src, IFile dest)
            throws CoreException {
        try {
            // Bundle bundle = VaadinPlugin.getInstance().getBundle();
            // InputStream input = FileLocator.openStream(bundle, src, false);

            File file = src.toFile();
            FileInputStream input = new FileInputStream(file);
            dest.create(input, true, null);
            input.close();
        } catch (Exception e) {
            throw newCoreException("Failed to copy file to project", e);
        }
    }

    public static void copySourceFileToProject(IProject project,
            Path sourceFile, String destinationPackage, String destinationFile)
            throws CoreException {
        try {
            // JavaCore.createCompilationUnitFrom();
            Bundle bundle = VaadinPlugin.getInstance().getBundle();
            InputStream input = FileLocator.openStream(bundle, sourceFile,
                    false);
            StringWriter writer = new StringWriter();
            IOUtils.copy(input, writer);
            input.close();

            String contents = writer.toString();

            IPackageFragmentRoot fragmentRoot = VaadinPluginUtil
                    .getJavaFragmentRoot(project);
            IPackageFragment packageFragment = fragmentRoot
                    .createPackageFragment(destinationPackage, true, null);
            // ICompilationUnit compilationUnit =
            packageFragment.createCompilationUnit(destinationFile, contents,
                    true, null);

            // JavaCore.create(project.getWorkspace().getRoot().getFolder(sourceFile))
        } catch (Exception e) {
            throw newCoreException("Failed to copy source file to project", e);
        }

    }

    public static String createApplicationClassSource(String packageName,
            String applicationName, String applicationClass,
            String vaadinPackagePrefix) {
        String template = "package " + packageName + ";\n\n" + "import "
                + vaadinPackagePrefix + "Application;\n" + "import "
                + vaadinPackagePrefix + "ui.*;\n\n" + "public class "
                + applicationClass + " extends Application {\n"
                + "\t@Override\n" + "\tpublic void init() {\n"
                + "\t\tWindow mainWindow = new Window(\"" + applicationName
                + "\");\n"
                + "\t\tLabel label = new Label(\"Hello Vaadin user\");\n"
                + "\t\tmainWindow.addComponent(label);\n"
                + "\t\tsetMainWindow(mainWindow);\n" + "\t}\n" + "\n" + "}\n";

        return template;

    }

    /**
     * Ensure that some Vaadin jar file can be found in the project. If none can
     * be found, adds the specified version from the local repository.
     *
     * No launch configurations are updated. Use updateVaadinLibraries if such
     * updates are needed.
     *
     * Requests to the user for widgetset builds in the project are suspended
     * for the duration of this operation and resumed after completion. At the
     * end, the user is asked about compiling the widgetset if it is dirty.
     *
     * @param project
     * @param vaadinJarVersion
     * @param monitor
     * @throws CoreException
     */
    public static void ensureVaadinLibraries(IProject project,
            Version vaadinJarVersion, IProgressMonitor monitor)
            throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask(
                    "Ensuring the project includes the Vaadin library", 1);

            IJavaProject jproject = JavaCore.create(project);
            try {
                IType findType = findVaadinApplicationType(jproject);
                if (findType == null) {
                    WidgetsetBuildManager
                            .internalSuspendWidgetsetBuilds(project);
                    try {
                        addVaadinLibrary(jproject, vaadinJarVersion,
                                new SubProgressMonitor(monitor, 1));

                        // refresh library folder to recompile parts of project
                        IFolder lib = getWebInfLibFolder(project);
                        if (lib.exists()) {
                            // should exist after adding the library
                            lib.refreshLocal(IResource.DEPTH_ONE, null);
                        }
                    } finally {
                        WidgetsetBuildManager
                                .internalResumeWidgetsetBuilds(project);
                        if (VaadinPluginUtil.isWidgetsetDirty(project)) {
                            WidgetsetBuildManager.runWidgetSetBuildTool(
                                    project, false, new NullProgressMonitor());
                        }
                    }
                }
            } catch (JavaModelException e) {
                throw newCoreException(
                        "Failed to ensure that a Vaadin jar is included in project",
                        e);
            }
        } finally {
            monitor.done();
        }
    }

    /**
     * Ensure that an Vaadin jar file can be found in the project and is of the
     * correct version. If none can be found or the version does not match,
     * replaces any old Vaadin JAR with the specified version from the local
     * repository.
     *
     * Update widgetset compilation launch configurations in the project to
     * refer to the new Vaadin and GWT versions (only when changing Vaadin
     * version, not when adding the JAR).
     *
     * Requests to the user for widgetset builds in the project are suspended
     * for the duration of this operation and resumed after completion. At the
     * end, the user is asked about compiling the widgetset if it is dirty.
     *
     * @param project
     * @param vaadinJarVersion
     *            or null to remove current Vaadin library
     * @throws CoreException
     */
    public static void updateVaadinLibraries(IProject project,
            Version vaadinJarVersion, IProgressMonitor monitor)
            throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask("Updating Vaadin libraries in the project", 12);

            // do nothing if correct version is already in the project
            Version currentVersion = getVaadinLibraryVersion(project, true);
            if ((vaadinJarVersion == currentVersion)
                    || (vaadinJarVersion != null && vaadinJarVersion
                            .equals(currentVersion))) {
                return;
            }
            IJavaProject jproject = JavaCore.create(project);
            WidgetsetBuildManager.internalSuspendWidgetsetBuilds(project);
            try {
                // replace the Vaadin JAR (currentVersion) with the new one
                if (currentVersion != null) {
                    removeVaadinLibrary(jproject, currentVersion);
                }
                monitor.worked(1);
                if (vaadinJarVersion != null) {
                    addVaadinLibrary(jproject, vaadinJarVersion,
                            new SubProgressMonitor(monitor, 9));
                }
                // refresh library folder to recompile parts of project
                IFolder lib = getWebInfLibFolder(project);
                if (lib.exists()) {
                    // should exist at least if added to the project
                    lib.refreshLocal(IResource.DEPTH_ONE, null);
                }

                // TODO also handle adding Vaadin JAR to a project if the user
                // has removed it and adds a different version?
                if (currentVersion != null && vaadinJarVersion != null) {
                    // update launches
                    String oldVaadinJarName = currentVersion.getJarFileName();
                    String newVaadinJarName = vaadinJarVersion.getJarFileName();
                    // this is safer than findProjectVaadinJarPath() as we may
                    // be in the process of changing the classpath
                    IPath vaadinJarPath = getWebInfLibFolder(project).getFile(
                            newVaadinJarName).getFullPath();
                    updateLaunchClassPath(project,
                            new String[] { oldVaadinJarName },
                            vaadinJarPath);
                    monitor.worked(1);

                    // check if Vaadin JAR is explicitly on classpath instead of
                    // through the "magic" classpath container for WEB-INF/lib
                    // => update classpath reference
                    IClasspathEntry[] rawClasspath = jproject.getRawClasspath();
                    List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
                    for (IClasspathEntry entry : rawClasspath) {
                        entries.add(entry);
                    }

                    IClasspathEntry vaadinEntry = JavaCore.newLibraryEntry(
                            vaadinJarPath, null, null);

                    // replace explicit reference to the Vaadin JAR if found,
                    // otherwise do nothing
                    replaceClassPathEntry(entries, vaadinEntry,
                            new String[] { oldVaadinJarName }, false);

                    IClasspathEntry[] entryArray = entries
                            .toArray(new IClasspathEntry[entries.size()]);
                    jproject.setRawClasspath(entryArray, null);

                    monitor.worked(1);
                }
            } catch (JavaModelException e) {
                throw newCoreException(
                        "Failed to update Vaadin jar in project", e);
            } finally {
                WidgetsetBuildManager.internalResumeWidgetsetBuilds(project);
                if (VaadinPluginUtil.isWidgetsetDirty(project)) {
                    WidgetsetBuildManager.runWidgetSetBuildTool(project, false,
                            new NullProgressMonitor());
                }
            }
        } finally {
            monitor.done();
        }
    }

    /**
     * Is the project type Vaadin Liferay project, i.e. does the plugin use
     * Vaadin from a Liferay directory instead of managing it itself.
     * 
     * @param project
     * @return
     */
    public static boolean isLiferayProject(IProject project) {
        if (project == null) {
            return false;
        }
        ScopedPreferenceStore prefStore = new ScopedPreferenceStore(
                new ProjectScope(project), VaadinPlugin.PLUGIN_ID);

        return prefStore
                .getBoolean(VaadinPlugin.PREFERENCES_PROJECT_TYPE_LIFERAY);
    }

    /**
     * Returns the Liferay WEB-INF path of a Liferay project, or null if not a
     * Liferay project.
     * 
     * @param project
     * @return
     */
    public static String getLiferayWebInfPath(IProject project) {
        if (project == null) {
            return null;
        }
        ScopedPreferenceStore prefStore = new ScopedPreferenceStore(
                new ProjectScope(project), VaadinPlugin.PLUGIN_ID);

        boolean liferayProject = prefStore
                .getBoolean(VaadinPlugin.PREFERENCES_PROJECT_TYPE_LIFERAY);
        return liferayProject ? prefStore
                .getString(VaadinPlugin.PREFERENCES_LIFERAY_PATH) : null;
    }

    /**
     * Returns true if the widgetset should be managed (created, compiled etc.)
     * by the plugin.
     * 
     * @param project
     * @return true if the widgetset should be automatically managed
     */
    public static boolean isWidgetsetManagedByPlugin(IProject project) {
        // TODO allow widgetset compilation in a Liferay project?
        return project != null && !isLiferayProject(project);
    }

    /**
     * Returns true if the Vaadin JAR should be managed (upgraded etc.) by the
     * plugin. If the project has a Vaadin JAR on the classpath outside of the
     * standard location (WEB-INF/lib), or if the project is a Liferay project,
     * the Vaadin JAR is not be managed by the plugin.
     * 
     * @param project
     * @return true if the plugin should upgrade Vaadin in the project etc.
     */
    public static boolean isVaadinJarManagedByPlugin(IProject project) {
        if (project == null) {
            return false;
        }
        IFolder lib = getWebInfLibFolder(project);
        if (!lib.exists()) {
            return false;
        }

        if (isLiferayProject(project)) {
            return false;
        }

        // TODO check if there is a Vaadin JAR on the classpath outside of the
        // WEB-INF/lib folder => return false

        return true;

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
     * @return
     * @throws CoreException
     */
    public static Version getVaadinLibraryVersion(IProject project,
            boolean useClasspath)
            throws CoreException {
        IFolder lib = getWebInfLibFolder(project);
        if (lib.exists()) {
            IResource[] files = lib.members();
            for (IResource resource : files) {
                // is it a Vaadin JAR?
                if (resource instanceof IFile) {
                    Version version = DownloadUtils
                            .getVaadinJarVersion(resource.getName());
                    if (version != null) {
                        return version;
                    }
                }
            }
        }

        if (useClasspath) {
            IJavaProject jproject = JavaCore.create(project);
            IPath resource = findProjectVaadinJarPath(jproject);
            return getVaadinVersionFromJar(resource);
        }

        return null;
    }

    private static Version getVaadinVersionFromJar(IPath resource) {
        if (resource == null || !resource.toPortableString().endsWith(".jar")
                || !resource.toFile().exists()) {
            return null;
        }
        JarFile jarFile = null;
        try {
            URL url = new URL("file:" + resource.toPortableString());
            url = new URL("jar:" + url.toExternalForm() + "!/");
            JarURLConnection conn = (JarURLConnection) url.openConnection();
            jarFile = conn.getJarFile();
            String versionString = null;
            ZipEntry entry = jarFile.getEntry("META-INF/VERSION");
            if (entry != null) {
                InputStream inputStream = jarFile.getInputStream(entry);
                versionString = new BufferedReader(new InputStreamReader(
                        inputStream)).readLine();
                inputStream.close();
            }
            closeJarFile(jarFile);
            jarFile = null;
            if (versionString != null) {
                // TODO assuming official version
                return DownloadUtils.getVaadinJarVersion("vaadin-"
                        + versionString + ".jar");
            }
        } catch (Throwable t) {
            handleBackgroundException(IStatus.INFO,
                    "Could not access JAR when checking for Vaadin version", t);
        } finally {
            closeJarFile(jarFile);
        }
        return null;
    }

    /**
     * Checks which Vaadin version is present in a Liferay installation.
     * 
     * @param project
     * @return Vaadin version in Liferay, or null if none
     * @throws CoreException
     */
    public static Version getVaadinLibraryVersionInLiferay(String liferayPath)
            throws CoreException {
        if (liferayPath == null || "".equals(liferayPath)) {
            return null;
        }

        // get version from META-INF/VERSION in lib/vaadin.jar
        IPath resource = getLiferayVaadinJarPath(liferayPath);
        return getVaadinVersionFromJar(resource);
    }

    public static CoreException newCoreException(String message, Throwable e) {
        return new CoreException(new Status(Status.ERROR,
                VaadinPlugin.PLUGIN_ID, message, e));
    }

    /**
     * Adds the specified Vaadin jar version from the local store to the
     * project. The specified version must be found from the local store or an
     * exception is thrown.
     *
     * @param jproject
     * @param vaadinJarVersion
     * @param monitor
     * @throws CoreException
     */
    private static void addVaadinLibrary(IJavaProject jproject,
            Version vaadinJarVersion, IProgressMonitor monitor)
            throws CoreException {

        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask(
                    "Adding Vaadin required libraries to the project", 5);

            IProject project = jproject.getProject();
            IFolder lib = getWebInfLibFolder(project);
            if (!lib.exists()) {
                createFolders(lib, monitor);
            }
            IFile targetFile = lib.getFile(vaadinJarVersion.getJarFileName());
            DownloadUtils.ensureVaadinJarExists(vaadinJarVersion,
                    new SubProgressMonitor(monitor, 1));
            IPath sourceFile = DownloadUtils
                    .getLocalVaadinJar(vaadinJarVersion);

            VaadinPluginUtil.copyPluginFileToProject(sourceFile, targetFile);

            // refresh project
            lib.refreshLocal(IResource.DEPTH_ONE, null);

            // make sure the GWT library versions match the Vaadin JAR
            // requirements
            updateGWTLibraries(jproject, new SubProgressMonitor(monitor, 4));
        } catch (Exception e) {
            throw newCoreException("Failed to add Vaadin jar to project", e);
        } finally {
            monitor.done();
        }
    }

    /**
     * Removes the specified Vaadin jar version from the project (if it exists).
     *
     * @param jproject
     * @param vaadinJarVersion
     * @throws CoreException
     */
    private static void removeVaadinLibrary(IJavaProject jproject,
            Version vaadinJarVersion) throws CoreException {
        try {
            IProject project = jproject.getProject();
            IFolder lib = getWebInfLibFolder(project);
            // if not in WEB-INF/lib, don't try to make any changes
            if (lib.exists()) {
                IFile targetFile = lib.getFile(vaadinJarVersion
                        .getJarFileName());
                targetFile.delete(true, null);

                // refresh project
                lib.refreshLocal(IResource.DEPTH_ONE, null);
            }
        } catch (Exception e) {
            throw newCoreException("Failed to remove Vaadin jar from project",
                    e);
        }
    }

    /**
     * Ensure that the project classpath contains the GWT libraries, adding them
     * if necessary.
     *
     * Also update widgetset compilation launch configuration paths as needed.
     *
     * Requests to the user for widgetset builds in the project are suspended
     * for the duration of this operation and resumed after completion. At the
     * end, the user is asked about compiling the widgetset if it is dirty.
     *
     * @param project
     * @param monitor
     * @throws CoreException
     */
    public static void ensureGWTLibraries(IProject project,
            IProgressMonitor monitor) throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor
                    .beginTask(
                            "Ensuring that the project classpath contains GWT libraries",
                            1);

            IJavaProject jproject = JavaCore.create(project);
            try {
                IType findType = jproject
                        .findType("com.google.gwt.core.client.EntryPoint");

                if (findType == null) {
                    WidgetsetBuildManager
                            .internalSuspendWidgetsetBuilds(project);
                    try {
                        updateGWTLibraries(jproject, new SubProgressMonitor(
                                monitor, 1));
                    } finally {
                        WidgetsetBuildManager
                                .internalResumeWidgetsetBuilds(project);
                        if (VaadinPluginUtil.isWidgetsetDirty(project)) {
                            WidgetsetBuildManager.runWidgetSetBuildTool(
                                    project, false, new NullProgressMonitor());
                        }
                    }
                }
            } catch (JavaModelException e) {
                throw newCoreException(
                        "Failed to ensure GWT libraries are present in the project",
                        e);
            }
        } finally {
            monitor.done();
        }
    }

    /**
     * Download and add or update GWT libraries in a project based on the Vaadin
     * version in the project (if any).
     *
     * The project build path and any external launches (including the widgetset
     * compilation launch for Vaadin 6.1 or earlier) are also updated.
     *
     * If the project build path contains user-defined GWT JARs, neither the
     * build path nor the launches are modified.
     *
     * @param jproject
     * @param monitor
     * @throws CoreException
     */
    private static void updateGWTLibraries(IJavaProject jproject,
            IProgressMonitor monitor) throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask("Updating GWT libraries", 14);

            String gwtVersion = getRequiredGWTVersionForProject(jproject);
            monitor.worked(1);

            // do not replace the GWT JARs directly on the build path and in
            // launches if they are user-defined
            // this ignores JARs in WEB-INF/lib, which could be temporary for
            // OOPHM, and does modify the top-level classpath entries - it is up
            // to the user to ensure the correct classpath order in such cases
            if (isUsingUserDefinedGwt(jproject, false)) {
                return;
            }

            try {
                DownloadUtils.ensureGwtUserJarExists(gwtVersion,
                        new SubProgressMonitor(monitor, 5));
                DownloadUtils.ensureGwtDevJarExists(gwtVersion,
                        new SubProgressMonitor(monitor, 5));

                IClasspathEntry[] rawClasspath = jproject.getRawClasspath();
                List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
                for (IClasspathEntry entry : rawClasspath) {
                    entries.add(entry);
                }

                // use the VAADIN_DOWNLOAD_VARIABLE variable and variable
                // classpath entries where feasible

                IPath devJarPath = DownloadUtils.getLocalGwtDevJar(gwtVersion);
                IClasspathEntry gwtDev = makeVariableClasspathEntry(
                        VaadinClasspathVariableInitializer.VAADIN_DOWNLOAD_VARIABLE,
                        devJarPath);

                IPath userJarPath = DownloadUtils
                        .getLocalGwtUserJar(gwtVersion);
                IClasspathEntry gwtUser = makeVariableClasspathEntry(
                        VaadinClasspathVariableInitializer.VAADIN_DOWNLOAD_VARIABLE,
                        userJarPath);

                // replace gwt-dev-[platform].jar and/or gwt-dev.jar if found,
                // otherwise append new entry
                String devJarName = "gwt-dev-" + getPlatform() + ".jar";
                replaceClassPathEntry(entries, gwtDev, new String[] {
                        "gwt-dev.jar", devJarName }, true);

                // replace gwt-user.jar if found, otherwise append new entry
                replaceClassPathEntry(entries, gwtUser,
                        new String[] { "gwt-user.jar" }, true);

                IClasspathEntry[] entryArray = entries
                        .toArray(new IClasspathEntry[entries.size()]);
                jproject.setRawClasspath(entryArray, null);

                monitor.worked(1);

                IProject project = jproject.getProject();

                // update classpaths also in launches
                if (!isUsingUserDefinedGwt(jproject, true)) {
                    updateLaunchClassPath(project, new String[] {
                            "gwt-dev.jar", devJarName }, devJarPath);
                    monitor.worked(1);

                    updateLaunchClassPath(project,
                            new String[] { "gwt-user.jar" }, userJarPath);
                    monitor.worked(1);
                } else {
                    monitor.worked(2);
                }
            } catch (JavaModelException e) {
                throw newCoreException("addGWTLibraries failed", e);
            }
        } finally {
            monitor.done();
        }

    }

    /**
     * Create a variable-based classpath entry if the given path is under the
     * target of the variable, an absolute one otherwise.
     *
     * @param variableName
     * @param jarPath
     * @return
     */
    private static IClasspathEntry makeVariableClasspathEntry(
            String variableName, IPath jarPath) {
        IPath variablePath = JavaCore.getClasspathVariable(variableName);
        if (variablePath.isPrefixOf(jarPath)) {
            // path starting with the variable name => relative to its content
            IPath jarVariablePath = new Path(variableName).append(jarPath
                    .removeFirstSegments(variablePath.segmentCount()));
            return JavaCore.newVariableEntry(jarVariablePath, null, null);
        } else {
            return JavaCore.newLibraryEntry(jarPath, null, null);
        }
    }

    /**
     * Replace an existing class path entry (identified by last segment name)
     * with a new one or optionally append the new entry if not found.
     *
     * The position of the replaced element on the class path is kept unchanged.
     * If a new entry is added, it is inserted at the beginning of the class
     * path.
     *
     * @param entries
     *            list of class path entries to modify
     * @param newEntry
     *            new entry
     * @param entryNames
     *            the first entry whose last path segment matches an element on
     *            this list is replaced
     * @param addIfMissing
     *            true to add the entry if no entry matching entryNames was
     *            found
     */
    private static void replaceClassPathEntry(List<IClasspathEntry> entries,
            IClasspathEntry newEntry, String[] entryNames, boolean addIfMissing) {
        boolean found = false;
        for (int i = 0; i < entries.size(); ++i) {
            for (String entryName : entryNames) {
                if (entryName.equals(entries.get(i).getPath().lastSegment())) {
                    entries.set(i, newEntry);
                    found = true;
                    break;
                }
            }
        }
        if (addIfMissing && !found && !entries.contains(newEntry)) {
            entries.add(0, newEntry);
        }
    }

    /**
     * Update the class path for program execution launch configurations
     * referring to any of the the given JAR file names in their arguments (not
     * the class path of the launch itself!) or in the class path of a Java
     * launch. This is called when a JAR is replaced by a different version
     * which may have a different name or location.
     *
     * The old JAR is identified by its file name without path. For external
     * launches, the JAR path is extracted by back-tracking from the JAR file
     * name to the previous path separator and that full path is replaced with
     * the given new path to a JAR file.
     *
     * This is primarily meant for updating the generated widgetset compilation
     * and hosted mode launches, but will also modify certain other kinds of
     * launches.
     *
     * @throws CoreException
     */
    private static void updateLaunchClassPath(IProject project,
            String[] jarNames, IPath jarPath) throws CoreException {
        // list all launches
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType typeExternal = manager
                .getLaunchConfigurationType(IExternalToolConstants.ID_PROGRAM_LAUNCH_CONFIGURATION_TYPE);
        ILaunchConfigurationType typeJava = manager
                .getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
        // it seems it is not possible to get the project for an external launch
        // with the official APIs
        // ILaunchConfiguration[] launchConfigurations = manager
        // .getLaunchConfigurations(type);

        // limit to launches that are top-level resources in the project of
        // interest
        for (IResource resource : project.members()) {
            // identify the external launches referring to the JAR
            if (resource instanceof IFile
                    && "launch".equals(resource.getFileExtension())) {
                ILaunchConfiguration launchConfiguration = manager
                        .getLaunchConfiguration((IFile) resource);
                if (launchConfiguration != null && launchConfiguration.exists()) {
                    if (typeExternal.equals(launchConfiguration.getType())) {
                        String arguments = launchConfiguration.getAttribute(
                                IExternalToolConstants.ATTR_TOOL_ARGUMENTS, "");
                        for (String jarName : jarNames) {
                            if (arguments.contains(jarName)) {
                                // update the classpath of a single launch
                                updateLaunchClassPath(launchConfiguration, jarName,
                                        jarPath);
                            }
                        }
                    } else if (typeJava.equals(launchConfiguration.getType())) {
                        IJavaProject jproject = JavaCore.create(project);
                        updateJavaLaunchClassPath(jproject,
                                launchConfiguration, jarNames, jarPath);
                    }
                }
            }
        }
    }

    private static void updateLaunchClassPath(
            ILaunchConfiguration launchConfiguration, String jarName,
            IPath jarPath) throws CoreException {
        // update a launch - careful about separators etc.
        ILaunchConfigurationWorkingCopy workingCopy = launchConfiguration
                .getWorkingCopy();

        String arguments = launchConfiguration.getAttribute(
                IExternalToolConstants.ATTR_TOOL_ARGUMENTS, "");

        // find the JAR reference (from previous separator to the next) and
        // replace it with the new path
        // on all platforms, also need to handle &quot;
        String separators;
        if ("windows".equals(VaadinPluginUtil.getPlatform())) {
            separators = ";&\"";
        } else {
            separators = ":&;\"";
        }

        // look for the JAR name potentially preceded with a path etc.
        Pattern pattern = Pattern.compile("[" + separators + "]([^"
                + separators + "]*" + jarName + ")[" + separators + "]");
        Matcher matcher = pattern.matcher(arguments);

        String newPath = JavaRuntime.newArchiveRuntimeClasspathEntry(jarPath)
                .getLocation();

        // replace path from previous separator to the next
        String result = arguments;
        matcher.find();
        for (int group = 1; group <= matcher.groupCount(); ++group) {
            result = result.replace(matcher.group(group), newPath);
            matcher.find();
        }

        workingCopy.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS,
                result);

        // save the launch
        workingCopy.doSave();
    }

    private static void updateJavaLaunchClassPath(IJavaProject jproject,
            ILaunchConfiguration launchConfiguration, String[] jarNames,
            IPath jarPath) throws CoreException {
        // update a launch
        ILaunchConfigurationWorkingCopy workingCopy = launchConfiguration
                .getWorkingCopy();

        boolean modified = false;

        List<String> classPath = workingCopy.getAttribute(
                IJavaLaunchConfigurationConstants.ATTR_CLASSPATH,
                new ArrayList<String>());
        List<String> newClassPath = new ArrayList<String>();

        // any of the JAR file names
        StringBuilder jarNamesRegexp = new StringBuilder();
        for (String jarName : jarNames) {
            if (jarNamesRegexp.length() > 0) {
                jarNamesRegexp.append("|");
            }
            jarNamesRegexp.append(jarName);
        }
        Pattern pattern = Pattern.compile("externalArchive=\".*[/\\\\]("
                + jarNamesRegexp.toString() + ")\".*");
        Matcher matcher = pattern.matcher("");
        for (String cpMemento : classPath) {
            matcher.reset(cpMemento);
            if (matcher.find()) {
                // new memento from path
                String newMemento = JavaRuntime
                        .newArchiveRuntimeClasspathEntry(jarPath).getMemento();
                newClassPath.add(newMemento);
                modified = true;
            } else {
                newClassPath.add(cpMemento);
            }
        }

        if (modified) {
            workingCopy.setAttribute(
                    IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, newClassPath);
        }

        // save the launch
        workingCopy.doSave();
    }

    /**
     * Checks if the project is using a custom (user-defined) GWT version
     * directly on the build path.
     *
     * @param jproject
     * @param useContainers
     *            also look into classpath containers such as WEB-INF/lib
     * @return true if the classpath contains GWT JARs other than those managed
     *         by the plugin
     */
    private static boolean isUsingUserDefinedGwt(IJavaProject jproject,
            boolean useContainers) {
        try {
            // make sure both kinds of paths are handled
            String gwtDownloadPath1 = getDownloadDirectory().toPortableString();
            String gwtDownloadPath2 = getDownloadDirectory().toOSString();

            IClasspathEntry[] rawClasspath = jproject.getRawClasspath();
            for (int i = 0; i < rawClasspath.length; i++) {
                IClasspathEntry cp = rawClasspath[i];
                if (cp.toString().contains("gwt-dev")
                        || cp.toString().contains("gwt-user")) {
                    if (!cp.toString().startsWith("VAADIN_DOWNLOAD")
                            && !cp.toString().startsWith(gwtDownloadPath1)
                            && !cp.toString().startsWith(gwtDownloadPath2)) {
                        return true;
                    }
                } else if (useContainers
                        && cp.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                    // primarily WEB-INF/lib
                    IClasspathContainer container = JavaCore
                            .getClasspathContainer(cp.getPath(), jproject);
                    IClasspathEntry[] containerEntries = container
                            .getClasspathEntries();
                    for (IClasspathEntry ccp : containerEntries) {
                        if (ccp.toString().contains("gwt-dev")
                                || ccp.toString().contains("gwt-user")) {
                            return true;
                        }
                    }
                }
            }
        } catch (CoreException e) {
            handleBackgroundException(
                    IStatus.WARNING,
                    "Could not determine whether the project uses user-defined GWT JARs. Assuming GWT JARs are managed by the plugin.",
                    e);
        }
        return false;
    }

    /**
     * Returns the first gwt dev jar defined in project classpath.
     *
     * If not set, a gwt jar file provided by plugin is returned.
     */
    public static IPath getGWTDevJarPath(IJavaProject jproject)
            throws CoreException {
        // check first for explicitly set gwt-dev jar file
        IClasspathEntry[] rawClasspath = jproject.getRawClasspath();
        for (IClasspathEntry cp : rawClasspath) {
            if (cp.toString().contains("gwt-dev")) {
                // User has explicitly defined GWT version to use directly on
                // the classpath, or classpath entry created by the plugin
                IClasspathEntry resolvedClasspathEntry = JavaCore
                        .getResolvedClasspathEntry(cp);
                return resolvedClasspathEntry.getPath();
            } else if (cp.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                // primarily WEB-INF/lib
                IClasspathContainer container = JavaCore.getClasspathContainer(
                        cp.getPath(), jproject);
                if (container != null) {
                    IClasspathEntry[] containerEntries = container
                            .getClasspathEntries();
                    for (IClasspathEntry ccp : containerEntries) {
                        if (ccp.toString().contains("gwt-dev")) {
                            // User has explicitly defined GWT version to use
                            IClasspathEntry resolvedClasspathEntry = JavaCore
                                    .getResolvedClasspathEntry(ccp);
                            return resolvedClasspathEntry.getPath();
                        }
                    }
                }
            }
        }

        String gwtVersion = getRequiredGWTVersionForProject(jproject);
        return DownloadUtils.getLocalGwtDevJar(gwtVersion);
    }

    public static String getPlatform() {
        String osname = System.getProperty("os.name");
        if (osname.toLowerCase().contains("mac")) {
            return "mac";
        } else if (osname.toLowerCase().contains("linux")) {
            return "linux";
        } else {
            return "windows";
        }

    }

    /**
     * Returns the first gwt user jar defined in projects classpath.
     *
     * If not set, a gwt jar file provided by plugin is returned.
     */
    public static IPath getGWTUserJarPath(IJavaProject jproject)
            throws CoreException {
        // check first for explicitly set gwt-user jar file
        IClasspathEntry[] rawClasspath = jproject.getRawClasspath();
        for (IClasspathEntry cp : rawClasspath) {
            if (cp.toString().contains("gwt-user")) {
                // User has explicitly defined GWT version to use directly on
                // the classpath, or classpath entry created by the plugin
                IClasspathEntry resolvedClasspathEntry = JavaCore
                        .getResolvedClasspathEntry(cp);
                return resolvedClasspathEntry.getPath();
            } else if (cp.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                // primarily WEB-INF/lib
                IClasspathContainer container = JavaCore.getClasspathContainer(
                        cp.getPath(), jproject);
                IClasspathEntry[] containerEntries = container
                        .getClasspathEntries();
                for (IClasspathEntry ccp : containerEntries) {
                    if (ccp.toString().contains("gwt-user")) {
                        // User has explicitly defined GWT version to use
                        IClasspathEntry resolvedClasspathEntry = JavaCore
                                .getResolvedClasspathEntry(ccp);
                        return resolvedClasspathEntry.getPath();
                    }
                }
            }
        }

        String gwtVersion = getRequiredGWTVersionForProject(jproject);
        return DownloadUtils.getLocalGwtUserJar(gwtVersion);
    }

    public static Path getPathToTemplateFile(String path) throws IOException {
        Bundle bundle = VaadinPlugin.getInstance().getBundle();
        URL fileURL = FileLocator.toFileURL(bundle.getResource("template/"
                + path));
        return new Path(fileURL.getPath());
    }

    public static String readTextFromStream(InputStream resourceAsStream) {
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(resourceAsStream, writer);
            resourceAsStream.close();
        } catch (IOException e) {
            // TODO this error message might not be ideal
            handleBackgroundException(IStatus.ERROR,
                    "Failed to read template file from the Vaadin plugin", e);
        }

        return writer.toString();

    }

    public static IPath getConfigurationPath() throws CoreException {
        URL userLocation = Platform.getUserLocation().getURL();
        URL configurationLocation = Platform.getConfigurationLocation()
                .getURL();

        if (configurationLocation != null) {
            try {
                return new Path(FileLocator.toFileURL(configurationLocation)
                        .getPath()).append(IPath.SEPARATOR
                        + VaadinPlugin.PLUGIN_ID);
            } catch (IOException e) {
                throw newCoreException("getConfigurationPath failed", e);
            }
        }

        if (userLocation != null) {
            try {
                return new Path(FileLocator.toFileURL(userLocation).getPath())
                        .append(IPath.SEPARATOR + VaadinPlugin.PLUGIN_ID);
            } catch (IOException e) {
                throw newCoreException("getConfigurationPath failed", e);
            }
        }

        IPath stateLocation = VaadinPlugin.getInstance().getStateLocation();
        if (stateLocation != null) {
            return stateLocation;
        }

        throw newCoreException(
                "getConfigurationPath found nowhere to store files", null);
    }

    public static IPath getDownloadDirectory() throws CoreException {
        IPath path = getConfigurationPath()
                .append(IPath.SEPARATOR + "download");

        // Create the directory if it does not exist
        if (!path.toFile().exists()) {
            path.toFile().mkdirs();
        }

        return path;
    }

    public static IPath getDownloadDirectory(String identifier)
            throws CoreException {
        IPath path = getDownloadDirectory()
                .append(IPath.SEPARATOR + identifier);

        // Create the directory if it does not exist
        if (!path.toFile().exists()) {
            path.toFile().mkdirs();
        }

        return path;
    }

    public static IPath getVersionedDownloadDirectory(String identifier,
            String version) throws CoreException {
        IPath path = getDownloadDirectory(identifier).append(
                IPath.SEPARATOR + version);

        return path;
    }

    public static String getRequiredGWTVersionForProject(IJavaProject jproject) {
        // if no information exists, default to 1.5.3
        String gwtVersion = "1.5.3";

        try {
            // TODO find Vaadin JAR also elsewhere on the classpath
            IFolder lib = getWebInfLibFolder(jproject.getProject());
            if (!lib.exists()) {
                return gwtVersion;
            }
            IResource[] files = lib.members();

            // Check gwt version from included Vaadin jar
            for (IResource resource : files) {
                // is it a Vaadin JAR?
                if (resource instanceof IFile) {
                    Version version = DownloadUtils
                            .getVaadinJarVersion(resource.getName());
                    if (version == null) {
                        continue;
                    }

                    IPath jarLocation = resource.getLocation();
                    if (jarLocation == null) {
                        throw newCoreException("Could not access Vaadin JAR",
                                null);
                    }

                    JarFile jarFile = null;
                    try {
                        jarFile = new JarFile(jarLocation.toFile()
                                .getAbsolutePath());
                        ZipEntry entry = jarFile
                                .getEntry("META-INF/GWT-VERSION");
                        if (entry == null) {
                            // found JAR but not GWT version information in it,
                            // use default
                            break;
                        }

                        // extract GWT version from the JAR
                        InputStream gwtVersionStream = jarFile
                                .getInputStream(entry);
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(gwtVersionStream));

                        gwtVersion = reader.readLine();
                    } finally {
                        if (jarFile != null) {
                            closeJarFile(jarFile);
                        }
                    }

                    // do not continue with other JARs
                    break;
                }
            }
        } catch (IOException ex) {
            handleBackgroundException(IStatus.WARNING,
                    "Failed to determine the GWT library version to use, defaulting to "
                            + gwtVersion, ex);
        } catch (CoreException ex) {
            handleBackgroundException(IStatus.WARNING,
                    "Failed to determine the GWT library version to use, defaulting to "
                            + gwtVersion, ex);
        }

        return gwtVersion;
    }

    public static IPath findProjectVaadinJarPath(IJavaProject javaProject)
            throws CoreException {
        IJavaElement type;
        type = findVaadinApplicationType(javaProject);
        if (type == null) {
            return null;
        }

        while (type.getParent() != null) {
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

        // For some reason we were not able to locate the jar which contains
        // Vaadin jar
        return null;
    }

    /**
     * Create the folder if it does not exist. If the parent folder does not
     * exist, it is created first.
     *
     * @param folder
     * @param monitor
     * @throws CoreException
     */
    public static void createFolders(IFolder folder, IProgressMonitor monitor)
            throws CoreException {
        if (folder.exists()) {
            return;
        }
        if (!folder.getParent().exists()) {
            createFolders((IFolder) folder.getParent(), monitor);
        }

        folder.create(true, false, monitor);

    }

    public static boolean typeExtendsClass(IType type, String className)
            throws JavaModelException {
        if (type.exists() && type.isStructureKnown() && type.isClass()) {
            ITypeHierarchy h = type.newSupertypeHierarchy(null);
            IType spr = h.getSuperclass(type);
            while (spr != null) {
                if (!spr.isClass()) {
                    break;
                }
                if (spr.getFullyQualifiedName().equals(className)) {

                    return true;
                }
                spr = h.getSuperclass(spr);
            }
        }
        return false;
    }

    private static InputStream openPluginFileAsStream(String templateName)
            throws IOException {
        Bundle bundle = VaadinPlugin.getInstance().getBundle();
        InputStream input = FileLocator.openStream(bundle, new Path(
                templateName), false);
        return input;
    }

    public static String readTextFromTemplate(String templateName)
            throws IOException {
        return readTextFromStream(openPluginFileAsStream("template/"
                + templateName));
    }

    public static IType[] getSubClasses(IProject project, String superClass,
            boolean allSubTypes, IProgressMonitor monitor)
            throws JavaModelException {
        // find all non-binary subclasses of a named class in the project
        IJavaProject javaProject = JavaCore.create(project);
        if (javaProject == null) {
            return new IType[0];
        }
        IType superType = javaProject.findType(superClass);
        ITypeHierarchy newTypeHierarchy = superType.newTypeHierarchy(monitor);

        IType[] subTypes;
        if (allSubTypes) {
            subTypes = newTypeHierarchy.getAllSubtypes(superType);
        } else {
            subTypes = newTypeHierarchy.getSubclasses(superType);
        }
        List<IType> types = new ArrayList<IType>();
        for (IType subclass : subTypes) {
            // #3441 for some reason, getAllSubtypes fetches types that are in
            // the project AND those open in editors => filter for project
            if (subclass.isResolved() && !subclass.isBinary()
                    && javaProject.equals(subclass.getJavaProject())) {
                types.add(subclass);
            }
        }
        return types.toArray(new IType[0]);
    }

    public static IType[] getApplicationClasses(IProject project,
            IProgressMonitor monitor) throws JavaModelException {
        // find all non-binary subclasses of Application in the project
        return getSubClasses(project, getVaadinPackagePrefix(project)
                + VaadinPlugin.APPLICATION_CLASS_NAME, true, monitor);
    }

    public static IType[] getWidgetSetClasses(IProject project,
            IProgressMonitor monitor) throws JavaModelException {
        // find all non-binary subclasses of WidgetSet in the project
        return getSubClasses(project, getVaadinPackagePrefix(project)
                + "terminal.gwt.client.WidgetSet", true, monitor);
    }

    /**
     * Find the (first) widgetset in a project, or indicate where the widgetset
     * should be created. If <code>create</code> is true, create the widgetset
     * if it did not exist.
     *
     * Unless explicitly given, the default location for a new widgetset is
     * based on the location of the Application class with the shortest package
     * path. A "widgetset" package is created under that package.
     *
     * A widgetset file should be named *widgetset*.gwt.xml - the ".gwt.xml" is
     * not a part of the module name.
     *
     * @param project
     * @param create
     *            create widgetset if it does not exist
     * @param root
     *            package fragment root under which the widgetset should be
     *            created, null for default/automatic
     * @param defaultPackage
     *            package name under which to create the widgetset, null to
     *            deduce from application locations - default (empty) package is
     *            not allowed
     * @param monitor
     * @return widgetset module name (with package path using dots), null if no
     *         suitable location found
     * @throws CoreException
     */
    public static String getWidgetSet(IJavaProject project, boolean create,
            IPackageFragmentRoot root, String defaultPackage,
            IProgressMonitor monitor)
            throws CoreException {
        IPackageFragmentRoot[] packageFragmentRoots = project
                .getPackageFragmentRoots();

        // this duplicates some code with findWidgetSets with a few
        // modifications for efficiency - stop at first match and never continue
        // after that
        final StringBuilder wsPathBuilder = new StringBuilder();
        IResourceVisitor visitor = new IResourceVisitor() {
            boolean continueSearch = true;

            public boolean visit(IResource arg0) throws CoreException {
                if (arg0 instanceof IFile) {
                    IFile f = (IFile) arg0;
                    String name = f.getName();
                    if (name.endsWith(".gwt.xml")
                            && name.toLowerCase().contains("widgetset")) {
                        wsPathBuilder.append(f.getFullPath());
                        continueSearch = false;
                    }
                }
                return continueSearch;
            }
        };

        for (int i = 0; i < packageFragmentRoots.length; i++) {
            if (!packageFragmentRoots[i].isArchive()) {
                IResource underlyingResource = packageFragmentRoots[i]
                        .getUnderlyingResource();
                underlyingResource.accept(visitor);
                if (!wsPathBuilder.toString().equals("")) {
                    String wspath = wsPathBuilder.toString();
                    IPath fullPath = underlyingResource.getFullPath();
                    wspath = wspath.replace(fullPath.toString() + "/", "");
                    wspath = wspath.replaceAll("/", ".").replaceAll(".gwt.xml",
                            "");
                    return wspath;
                }
                // keep track of the first suitable package fragment root to use
                // as the default
                if (root == null) {
                    root = packageFragmentRoots[i];
                }
            }
        }

        /*
         * Project don't have custom widget set yet Come up with a default name
         * ( tool will create it later ). Find application classes and use the
         * one that has shortest package name. Add "WidgetSet" to that.
         */

        IType[] applicationClasses = getApplicationClasses(
                project.getProject(), monitor);
        if (defaultPackage == null) {
            String shortestPackagename = null;
            IType appWithShortestPackageName = null;
            for (int i = 0; i < applicationClasses.length; i++) {
                IType appclass = applicationClasses[i];
                String packagename = appclass.getPackageFragment().toString();
                if (shortestPackagename == null
                        || packagename.length() < shortestPackagename.length()) {
                    shortestPackagename = packagename;
                    appWithShortestPackageName = appclass;
                }
            }
            if (appWithShortestPackageName != null) {
                defaultPackage = appWithShortestPackageName
                        .getPackageFragment().getElementName()
                        + ".widgetset";
                // find the package fragment root in which the application is
                // located to create the widgetset in the same source tree
                IPath path = appWithShortestPackageName.getPath();
                for (IPackageFragmentRoot newRoot = null; path.segmentCount() > 0; path = path
                        .removeLastSegments(1)) {
                    newRoot = project.findPackageFragmentRoot(path);
                    if (newRoot != null) {
                        root = newRoot;
                        break;
                    }
                }
            }
        }
        if (defaultPackage != null) {
            // Use project name for the widgetset by default
            String wsName = project.getProject().getName();

            // normalize in case not a valid Java identifier
            if (!Character.isJavaIdentifierStart(wsName.charAt(0))) {
                // add X to the beginning of the name
                wsName = "X" + wsName;
            } else {
                // uppercase first character
                wsName = wsName.substring(0, 1).toUpperCase()
                        + wsName.substring(1).toLowerCase();
            }
            // normalize a little
            wsName = wsName.replaceAll("[^A-Za-z_0-9]", "_");

            wsName += "Widgetset";
            String fullyQualifiedName = defaultPackage + "." + wsName;

            VaadinPluginUtil.logInfo("No widget set found, "
                    + fullyQualifiedName + " will be created...");

            /* Update web.xml */

            WebArtifactEdit artifact = WebArtifactEdit
                    .getWebArtifactEditForWrite(project.getProject());
            if (artifact == null) {
                handleBackgroundException("Couldn't open web.xml for edit.",
                        null);
            } else {
                try {
                    WebXmlUtil.setWidgetSet(artifact, fullyQualifiedName,
                            Arrays.asList(applicationClasses));
                    artifact.saveIfNecessary(null);
                } finally {
                    artifact.dispose();
                }
            }

            if (create) {
                if (root != null) {
                    // TODO monitor usage; test this
                    IPackageFragment fragment = root.createPackageFragment(
                            defaultPackage, true, monitor);
                    if (fragment.getResource() instanceof IFolder) {
                        IFolder wsFolder = (IFolder) fragment.getResource();
                        IFile file = wsFolder.getFile(wsName + ".gwt.xml");
                        ensureFileFromTemplate(file, "widgetsetxmlstub2.txt");

                        // mark the created widgetset as dirty
                        setWidgetsetDirty(project.getProject(), true);
                    }
                }
            }

            return fullyQualifiedName;
        }

        return null;
    }

    /**
     * Find the (first) widgetset used in the project based on web.xml . If none
     * is mentioned there, return {@link #DEFAULT_WIDGET_SET_NAME}.
     *
     * @param project
     * @return first widgetset GWT module name used in web.xml or default
     *         widgetset
     */
    public static String getConfiguredWidgetSet(IJavaProject project) {
        WebArtifactEdit artifact = WebArtifactEdit
                .getWebArtifactEditForRead(project.getProject());
        if (artifact == null) {
            handleBackgroundException("Couldn't open web.xml for reading.",
                    null);
        } else {
            try {
                Map<String, String> widgetsets = WebXmlUtil
                        .getWidgetSets(artifact);
                for (String widgetset : widgetsets.values()) {
                    if (widgetset != null) {
                        return widgetset;
                    }
                }
            } finally {
                artifact.dispose();
            }
        }

        return DEFAULT_WIDGET_SET_NAME;
    }

    /**
     * Find the list of widgetsets in the project.
     *
     * Only GWT modules (.gwt.xml files) with "widgetset" in the file name are
     * returned.
     *
     * @param project
     * @param monitor
     * @return list of widgetset module names in the project
     * @throws CoreException
     */
    public static List<String> findWidgetSets(IJavaProject project,
            IProgressMonitor monitor) throws CoreException {
        IPackageFragmentRoot[] packageFragmentRoots = project
                .getPackageFragmentRoots();
        final List<IPath> paths = new ArrayList<IPath>();
        IResourceVisitor visitor = new IResourceVisitor() {
            public boolean visit(IResource arg0) throws CoreException {
                if (arg0 instanceof IFile) {
                    IFile f = (IFile) arg0;
                    String name = f.getName();
                    if (name.endsWith(".gwt.xml")
                            && name.toLowerCase().contains("widgetset")) {
                        paths.add(f.getFullPath());
                        return false;
                    }
                }
                return true;
            }
        };

        List<String> widgetsets = new ArrayList<String>();
        for (int i = 0; i < packageFragmentRoots.length; i++) {
            if (!packageFragmentRoots[i].isArchive()) {
                IResource underlyingResource = packageFragmentRoots[i]
                        .getUnderlyingResource();
                if (underlyingResource != null) {
                    underlyingResource.accept(visitor);

                    for (IPath path : paths) {
                        String wspath = path.toString();
                        IPath fullPath = underlyingResource.getFullPath();
                        wspath = wspath.replace(fullPath.toString() + "/", "");
                        wspath = wspath.replaceAll("/", ".").replaceAll(
                                ".gwt.xml", "");
                        widgetsets.add(wspath);
                    }

                    paths.clear();
                }
            }
        }

        return widgetsets;
    }

    /**
     * Check if a project contains one or more widgetsets. This method is more
     * efficient than {@link #findWidgetSets(IJavaProject, IProgressMonitor)} as
     * the evaluation stops upon the first match.
     *
     * Only GWT modules (.gwt.xml files) with "widgetset" in the file name are
     * taken into account.
     *
     * @param project
     * @param monitor
     * @return true if the project directly contains at least one widgetset
     * @throws CoreException
     */
    public static boolean hasWidgetSets(IJavaProject project,
            IProgressMonitor monitor) throws CoreException {
        final boolean[] found = new boolean[] { false };
        IResourceVisitor visitor = new IResourceVisitor() {
            public boolean visit(IResource arg0) throws CoreException {
                if (found[0]) {
                    return false;
                }
                if (arg0 instanceof IFile) {
                    IFile f = (IFile) arg0;
                    String name = f.getName();
                    if (name.endsWith(".gwt.xml")
                            && name.toLowerCase().contains("widgetset")) {
                        found[0] = true;
                        return false;
                    }
                }
                return true;
            }
        };

        IPackageFragmentRoot[] packageFragmentRoots = project
                .getPackageFragmentRoots();
        for (int i = 0; i < packageFragmentRoots.length; i++) {
            if (!packageFragmentRoots[i].isArchive()) {
                IResource underlyingResource = packageFragmentRoots[i]
                        .getUnderlyingResource();
                underlyingResource.accept(visitor);

                if (found[0]) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Find the list of widgetsets in the project in a format suitable for a
     * Vaadin addon manifest file.
     *
     * @param project
     * @param monitor
     * @return String comma-separated list of widgetset module names in the
     *         project
     * @throws CoreException
     */
    public static String findWidgetSetsString(IJavaProject project,
            IProgressMonitor monitor) throws CoreException {
        List<String> widgetsets = findWidgetSets(project, monitor);
        StringBuilder result = new StringBuilder();
        Iterator<String> it = widgetsets.iterator();
        while (it.hasNext()) {
            result.append(it.next());
            if (it.hasNext()) {
                result.append(",");
            }
        }
        return result.toString();
    }

    public static boolean isWidgetsetPackage(IPath resource) {
        if (resource != null && resource.toPortableString().endsWith(".jar")) {
            JarFile jarFile = null;
            try {
                URL url = new URL("file:" + resource.toPortableString());
                url = new URL("jar:" + url.toExternalForm() + "!/");
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                jarFile = conn.getJarFile();
                Manifest manifest = jarFile.getManifest();
                closeJarFile(jarFile);
                jarFile = null;
                Attributes mainAttributes = manifest.getMainAttributes();
                if (mainAttributes.getValue("Vaadin-Widgetsets") != null) {
                    return true;
                }
            } catch (Throwable t) {
                handleBackgroundException(IStatus.INFO,
                        "Could not access JAR when checking for widgetsets", t);
            } finally {
                closeJarFile(jarFile);
            }
        }
        return false;
    }

    private static boolean isNeededForWidgetsetCompilation(IResource resource) {
        if (resource instanceof IFile && resource.getName().endsWith(".jar")) {
            JarFile jarFile = null;
            try {
                URL url = new URL(resource.getLocationURI().toString());
                url = new URL("jar:" + url.toExternalForm() + "!/");
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                jarFile = conn.getJarFile();
                Manifest manifest = jarFile.getManifest();
                if (manifest == null) {
                    return false;
                }
                Attributes mainAttributes = manifest.getMainAttributes();
                if (mainAttributes
                        .getValue(DirectoryManifestProvider.MANIFEST_VAADIN_WIDGETSETS) != null) {
                    return true;
                } else {
                    // not a vaadin widget package, but it still may be
                    // needed for referenced gwt modules (cant know for
                    // sure)

                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry nextElement = entries.nextElement();
                        if (nextElement.getName().endsWith(".gwt.xml")) {
                            return true;
                        }
                    }
                }
            } catch (MalformedURLException e) {
                String message = (jarFile == null) ? "Could not access JAR when searching for widgetsets"
                        : "Could not access JAR when searching for widgetsets: "
                                + jarFile.getName();
                handleBackgroundException(IStatus.WARNING, message, e);
            } catch (IOException e) {
                String message = (jarFile == null) ? "Could not access JAR when searching for widgetsets"
                        : "Could not access JAR when searching for widgetsets: "
                                + jarFile.getName();
                handleBackgroundException(IStatus.WARNING, message, e);
            } finally {
                closeJarFile(jarFile);
            }
        } else {
            // detect if is jar and if in widgetset
        }
        return false;
    }

    private static void closeJarFile(JarFile jarFile) {
        // TODO make better jar handling. Windows locks files without
        // this, mac fails to rebuild widgetset with
        if (getPlatform().equals("windows")) {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                    handleBackgroundException(IStatus.WARNING,
                            "Closing JAR file failed", e);
                }
            }
        }
    }

    /**
     * Returns jar files which contain widgetset for given project.
     * <p>
     * Method will iterate files in WEB-INF/lib and check each jar file there.
     *
     * @param project
     * @return
     * @throws CoreException
     */
    public static Collection<IPath> getAvailableVaadinWidgetsetPackages(
            IJavaProject project) throws CoreException {
        final Collection<IPath> vaadinpackages = new HashSet<IPath>();

        IFolder webInfLibFolder = getWebInfLibFolder(project.getProject());
        if (!webInfLibFolder.exists()) {
            return vaadinpackages;
        }
        webInfLibFolder.accept(new IResourceVisitor() {
            public boolean visit(IResource resource) throws CoreException {
                if (isNeededForWidgetsetCompilation(resource)) {
                    vaadinpackages.add(resource.getRawLocation());
                    return true;
                }
                return true;
            }
        });

        // TODO should iterate project classpath too. Referenced gwt modules
        // (without any server side code like google-maps.jar) should not need
        // to be in web-inf/lib, but just manually added for project classpath

        return vaadinpackages;
    }

    /**
     * Add widgetset nature to a project if not already there. Only modifies
     * Vaadin projects.
     * 
     * @param project
     */
    public static void ensureWidgetsetNature(final IProject project) {
        if (!VaadinFacetUtils.isVaadinProject(project)) {
            return;
        }
        try {
            // Add nature if not there (to enable WidgetSet builder).
            // Nice when upgrading projects.
            IProjectNature nature = project
                    .getNature(WidgetsetNature.NATURE_ID);
            if (nature == null) {
                WidgetsetNature.addWidgetsetNature(project);
            }
        } catch (Exception e) {
            handleBackgroundException(IStatus.WARNING,
                    "Adding widgetset nature to the project failed.", e);
        }
    }

    /**
     * Checks if the widgetset in a project is marked as dirty. If the project
     * is not a Vaadin project or does not have widgetsets, returns
     * <code>false</code>.
     *
     * If the flag is not present in project preferences, test whether there are
     * widgetsets and as a side effect mark dirty (if any exist) / clean (no
     * widgetset) based on that.
     *
     * @param project
     * @return true if the project has widgetset(s) that have not been compiled
     *         since the last relevant modification
     */
    public static boolean isWidgetsetDirty(IProject project) {
        // default to clean until some widgetset found
        boolean result = false;
        try {
            IProgressMonitor monitor = new NullProgressMonitor();
            if (!isVaadin62(project)) {
                return false;
            }

            // from this point on, default to dirty
            result = true;

            ScopedPreferenceStore prefStore = new ScopedPreferenceStore(
                    new ProjectScope(project), VaadinPlugin.PLUGIN_ID);
            if (prefStore.contains(VaadinPlugin.PREFERENCES_WIDGETSET_DIRTY)) {
                return prefStore
                        .getBoolean(VaadinPlugin.PREFERENCES_WIDGETSET_DIRTY);
            } else {
                result = hasWidgetSets(JavaCore.create(project), monitor);
                setWidgetsetDirty(project, result);
                return result;
            }
        } catch (CoreException e) {
            return result;
        }
    }

    /**
     * Mark the widgetset(s) in a project as clean (compiled) or dirty (modified
     * since the last compilation).
     *
     * TODO note: keeping track of this in preferences might be an issue with
     * version control etc. if versioning preferences
     *
     * @param project
     * @param dirty
     */
    public static void setWidgetsetDirty(IProject project, boolean dirty) {
        ScopedPreferenceStore prefStore = new ScopedPreferenceStore(
                new ProjectScope(project), VaadinPlugin.PLUGIN_ID);

        // save as string so that the value false does not result in the entry
        // being removed - we use three states: true, false and absent
        prefStore.setValue(VaadinPlugin.PREFERENCES_WIDGETSET_DIRTY, Boolean
                .toString(dirty));
        try {
            prefStore.save();
        } catch (IOException e) {
            handleBackgroundException(IStatus.WARNING,
                    "Could not save widgetset compilation state for project "
                            + project.getName(), e);
        }
    }

    /**
     * Helper method to compile a single widgetset.
     *
     * Instead the "old method" of using launch configurations (.launch) running
     * compilation via {@link ProcessBuilder}. Also notifies eclipse of possibly
     * changed files in widgetset directory.
     *
     * Note, this only works for projects with vaadin 6.2 and later.
     *
     * Normally this method should be called by {@link WidgetsetBuildManager} to
     * ensure that multiple builds of the same widgetset are not run
     * concurrently.
     *
     * @param jproject
     * @param moduleName
     *            explicit widgetset module name - not null
     * @param monitor
     * @throws CoreException
     * @throws IOException
     * @throws InterruptedException
     */
    public static void compileWidgetset(IJavaProject jproject,
            String moduleName, final IProgressMonitor monitor)
            throws CoreException, IOException, InterruptedException {

        IProject project = jproject.getProject();

        if (!isWidgetsetManagedByPlugin(project)) {
            return;
        }

        final long start = new Date().getTime();

        try {
            ScopedPreferenceStore prefStore = new ScopedPreferenceStore(
                    new ProjectScope(project), VaadinPlugin.PLUGIN_ID);

            boolean verbose = prefStore
                    .contains(VaadinPlugin.PREFERENCES_WIDGETSET_VERBOSE) ? prefStore
                    .getBoolean(VaadinPlugin.PREFERENCES_WIDGETSET_VERBOSE)
                    : false;

            final Long estimatedCompilationTime;
            if (prefStore
                    .contains(VaadinPlugin.PREFERENCES_WIDGETSET_COMPILATION_ETA)) {
                estimatedCompilationTime = prefStore
                        .getLong(VaadinPlugin.PREFERENCES_WIDGETSET_COMPILATION_ETA);
            } else {
                /**
                 * An estimation of widgetset compilation time in millis. Will
                 * be updated after each compilation.
                 */
                estimatedCompilationTime = 120 * 1000l;
            }

            // TODO should report progress more correctly - unknown?
            monitor.beginTask("Compiling widgetset " + moduleName
                    + " in project " + project.getName(), 100);
            monitor.subTask("Preparing to compile widgetset " + moduleName
                    + " in project " + project.getName());

            ArrayList<String> args = new ArrayList<String>();

            moduleName = moduleName.replace(".client.", ".");

            String vmName = getJvmExecutablePath(jproject);
            args.add(vmName);

            // refresh only the WebContent/VAADIN/widgetsets
            String resourceDirectory = getVaadinResourceDirectory(project);
            final IFolder wsDir = getWebContentFolder(project).getFolder(
                    resourceDirectory).getFolder("widgetsets");

            // refresh this requires that the directory exists
            createFolders(wsDir, new SubProgressMonitor(monitor, 1));

            // construct the class path, including GWT JARs and project sources
            String classPath = getProjectBaseClasspath(jproject, true);

            String classpathSeparator = getClasspathSeparator();

            // add widgetset JARs
            Collection<IPath> widgetpackagets = getAvailableVaadinWidgetsetPackages(jproject);
            IPath vaadinJarPath = findProjectVaadinJarPath(jproject);
            for (IPath file2 : widgetpackagets) {
                if (!file2.equals(vaadinJarPath)) {
                    classPath = classPath + classpathSeparator
                            + file2.toString();
                }
            }

            // construct rest of the arguments for the launch

            args.add("-Djava.awt.headless=true");
            args.add("-Xss8M");
            args.add("-Xmx512M");
            args.add("-XX:MaxPermSize=512M");

            if (getPlatform().equals("mac")) {
                args.add("-XstartOnFirstThread");
            }

            args.add("-classpath");
            // args.add(classPath.replaceAll(" ", "\\ "));
            args.add(classPath);

            String compilerClass = "com.vaadin.tools.WidgetsetCompiler";
            args.add(compilerClass);

            args.add("-out");
            IPath projectRelativePath = wsDir.getProjectRelativePath();
            args.add(projectRelativePath.toString());

            String style = prefStore
                    .getString(VaadinPlugin.PREFERENCES_WIDGETSET_STYLE);
            if ("DRAFT".equals(style)) {
                args.add("-style");
                args.add("PRETTY");
                args.add("-draftCompile");
            } else if (!"".equals(style)) {
                args.add("-style");
                args.add(style);
            }

            String parallelism = prefStore
                    .getString(VaadinPlugin.PREFERENCES_WIDGETSET_PARALLELISM);
            if ("".equals(parallelism)) {
                args.add("-localWorkers");
                args.add("" + Runtime.getRuntime().availableProcessors());
            } else {
                args.add("-localWorkers");
                args.add(parallelism);
            }

            if (verbose) {
                args.add("-logLevel");
                args.add("INFO");
            } else {
                args.add("-logLevel");
                args.add("WARN");
            }

            args.add(moduleName);

            final String[] argsStr = new String[args.size()];
            args.toArray(argsStr);

            ProcessBuilder b = new ProcessBuilder(argsStr);

            IPath projectLocation = project.getLocation();
            b.directory(projectLocation.toFile());

            b.redirectErrorStream(true);

            monitor.subTask("Compiling widgetset " + moduleName
                    + " in project " + project.getName());

            final Process exec = b.start();

            // compilation now on

            Thread t = new Thread() {
                @Override
                public synchronized void run() {
                    int i = 0;
                    int estimatedProgress = 0;

                    while (true) {
                        if (monitor.isCanceled()) {
                            exec.destroy();
                            break;
                        } else {
                            try {
                                i++;
                                // give user a feeling that something is
                                // happening

                                int currentProgress = (int) (100 * (new Date()
                                        .getTime() - start) / estimatedCompilationTime);
                                if (currentProgress > 100) {
                                    currentProgress = 100;
                                }
                                int delta = currentProgress - estimatedProgress;

                                monitor.worked(delta);
                                estimatedProgress += delta;
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                // STOP executing monitoring cancelled state,
                                // compilation finished
                                break;
                            }
                        }
                    }
                }
            };

            t.start();

            ConsolePlugin plugin = ConsolePlugin.getDefault();
            IConsoleManager conMan = plugin.getConsoleManager();
            IConsole[] existing = conMan.getConsoles();
            MessageConsole myConsole = null;
            for (int i = 0; i < existing.length; i++) {
                if (WS_COMPILATION_CONSOLE_NAME.equals(existing[i].getName())) {
                    myConsole = (MessageConsole) existing[i];
                }
            }
            // no console found, so create a new one
            if (myConsole == null) {
                myConsole = new MessageConsole(WS_COMPILATION_CONSOLE_NAME,
                        null);
                conMan.addConsoles(new IConsole[] { myConsole });
            }

            myConsole.clearConsole();

            MessageConsoleStream newMessageStream = myConsole
                    .newMessageStream();

            if (verbose) {
                myConsole.activate();

                newMessageStream.println();
                newMessageStream.println("Executing compiler with parameters "
                        + args);
            } else {
                myConsole.activate();

                newMessageStream.println();
                newMessageStream.println("Compiling widgetset " + moduleName);
            }

            InputStream inputStream = exec.getInputStream();
            BufferedReader bufferedReader2 = new BufferedReader(
                    new InputStreamReader(inputStream));
            String line = null;
            while ((line = bufferedReader2.readLine()) != null) {
                newMessageStream.println(line);
            }

            int waitFor = exec.waitFor();

            // end thread (possibly still) polling for cancelled status
            t.interrupt();

            if (waitFor == 0) {
                wsDir.refreshLocal(IResource.DEPTH_INFINITE,
                        new SubProgressMonitor(monitor, 1));
                setWidgetsetDirty(project, false);
                prefStore.setValue(
                        VaadinPlugin.PREFERENCES_WIDGETSET_COMPILATION_ETA,
                        new Date().getTime() - start);
                prefStore.save();

                if (!verbose) {
                    // if verbose, the output of the compiler is sufficient
                    newMessageStream.println("Widgetset compilation completed");
                }
            } else {
                // cancelled or failed
                setWidgetsetDirty(project, true);

                if (monitor.isCanceled()) {
                    newMessageStream.println("Widgetset compilation canceled");
                } else {
                    newMessageStream.println("Widgetset compilation failed");
                }
            }
        } finally {
            monitor.done();
        }
    }

    /**
     * Returns the project classpath as a string, in a format that can be used
     * when launching external programs on the same platform where Eclipse is
     * running.
     *
     * For a Vaadin 6.2+ project, output locations should be on the classpath of
     * the widgetset compiler (but after all source directories) to enable
     * accessing the server side annotations.
     *
     * @param jproject
     * @param includeOutputDirectories
     *            true to also include output (class file) locations on the
     *            classpath
     * @return
     * @throws CoreException
     * @throws JavaModelException
     */
    public static String getProjectBaseClasspath(IJavaProject jproject,
            boolean includeOutputDirectories) throws CoreException,
            JavaModelException {
        String classpathSeparator = getClasspathSeparator();
        IProject project = jproject.getProject();

        // use LinkedHashSet that preserves order but eliminates duplicates
        Set<IPath> sourceLocations = new LinkedHashSet<IPath>();
        Set<IPath> outputLocations = new LinkedHashSet<IPath>();
        Set<IPath> otherLocations = new LinkedHashSet<IPath>();

        // ensure the default output location is on the classpath
        outputLocations.add(getRawLocation(project, jproject
                .getOutputLocation()));

        // key libraries
        IRuntimeClasspathEntry gwtdev = JavaRuntime
                .newArchiveRuntimeClasspathEntry(getGWTDevJarPath(jproject));
        otherLocations.add(getRawLocation(project, gwtdev.getPath()));

        IRuntimeClasspathEntry gwtuser = JavaRuntime
                .newArchiveRuntimeClasspathEntry(getGWTUserJarPath(jproject));
        otherLocations.add(getRawLocation(project, gwtuser.getPath()));

        IRuntimeClasspathEntry vaadinJar = JavaRuntime
                .newArchiveRuntimeClasspathEntry(findProjectVaadinJarPath(jproject));
        otherLocations.add(getRawLocation(project, vaadinJar.getPath()));

        // iterate over build path and classify its components
        // only source locations and their output directories (if any) are used
        for (IClasspathEntry classPathEntry : jproject
                .getResolvedClasspath(true)) {
            if (classPathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                // gwt compiler also needs javafiles for classpath
                IPath path = classPathEntry.getPath();
                sourceLocations.add(getRawLocation(project, path));

                // source entry has custom output location?
                IPath outputLocation = classPathEntry.getOutputLocation();
                if (outputLocation != null) {
                    outputLocations
                            .add(getRawLocation(project, outputLocation));
                }
                // } else {
                // IPath path = classPathEntry.getPath();
                // IPath rawLocation = getRawLocation(project, path);
                // otherLocations.add(rawLocation);
            }
        }

        // source directories must come before output locations
        Set<IPath> locations = new LinkedHashSet<IPath>();

        locations.addAll(sourceLocations);
        if (includeOutputDirectories) {
            locations.addAll(outputLocations);
        }
        locations.addAll(otherLocations);

        // safeguard
        locations.remove(null);

        // construct classpath string
        IRuntimeClasspathEntry systemLibsEntry = JavaRuntime
                .newVariableRuntimeClasspathEntry(new Path(
                        JavaRuntime.JRELIB_VARIABLE));

        String classPath = systemLibsEntry.getLocation();
        for (IPath path : locations) {
            if ("".equals(classPath)) {
                classPath = path.toPortableString();
            } else {
                classPath = classPath + classpathSeparator
                        + path.toPortableString();
            }
        }

        return classPath;
    }

    /**
     * Gets the platform specific separator to use between classpath string
     * segments.
     *
     * @return a colon or a semicolon to use as classpath separator
     */
    private static String getClasspathSeparator() {
        String classpathSeparator;
        if ("windows".equals(getPlatform())) {
            classpathSeparator = ";";
        } else {
            classpathSeparator = ":";
        }
        return classpathSeparator;
    }

    /**
     * Returns the full path to the Java executable. The project JVM is used if
     * available, the workspace default VM if none is specified for the project.
     *
     * @param jproject
     * @return JVM executable path in platform specific format
     * @throws CoreException
     */
    public static String getJvmExecutablePath(IJavaProject jproject)
            throws CoreException {
        String vmName;
        IVMInstall vmInstall = JavaRuntime.getVMInstall(jproject);
        // this might be unnecessary
        if (vmInstall == null) {
            vmInstall = JavaRuntime.getDefaultVMInstall();
        }
        File vmBinDir = new File(vmInstall.getInstallLocation(), "bin");
        // windows hack, as Eclipse can run the JVM but does not give its
        // executable name through public APIs
        if ("windows".equals(getPlatform())) {
            vmName = new File(vmBinDir, "java.exe").getAbsolutePath();
        } else {
            vmName = new File(vmBinDir, "java").getAbsolutePath();
        }
        return vmName;
    }

    /**
     * Convert a path to a raw filesystem location - also works when the project
     * is outside the workspace
     *
     * @param project
     * @param path
     * @return
     */
    public static IPath getRawLocation(IProject project, IPath path) {
        // constructing the handles is inexpensive
        IFolder folder = project.getWorkspace().getRoot().getFolder(path);
        IFile file = project.getWorkspace().getRoot().getFile(path);
        if (folder.exists()) {
            return folder.getRawLocation();
        } else if (file.exists()) {
            return file.getRawLocation();
        } else {
            // assumed to be complete path if not in the workspace
            return path;
        }
    }

    /**
     * Find Java launch configuration for GWT hosted mode, create it if missing.
     *
     * @param project
     * @return the {@link ILaunchConfiguration} created/found launch
     *         configuration or null if none
     */
    public static ILaunchConfiguration createHostedModeLaunch(IProject project) {
        if (project == null) {
            return null;
        }

        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

        ILaunchConfigurationType type = manager
                .getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);

        try {
            IJavaProject jproject = JavaCore.create(project);

            // check GWT version
            boolean isGwt20 = isGwt20(project);

            String launchName = "GWT " + (isGwt20 ? "development" : "hosted")
                    + " mode for " + project.getName();

            // find and return existing launch, if any
            ILaunchConfiguration[] launchConfigurations = manager
                    .getLaunchConfigurations();
            for (ILaunchConfiguration launchConfiguration : launchConfigurations) {
                if (launchName.equals(launchConfiguration.getName())) {
                    // is the launch in the same project?
                    String launchProject = launchConfiguration
                            .getAttribute(
                                    IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                                    "");
                    if (project.getName().equals(launchProject)) {
                        logInfo("GWT development mode launch already exists for the project");
                        return launchConfiguration;
                    }
                }
            }

            // create a new launch configuration

            ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(
                    project, launchName);

            String mainClass = "com.google.gwt.dev.GWTShell";
            if (isGwt20) {
                mainClass = "com.google.gwt.dev.DevMode";
            }
            workingCopy.setAttribute(
                    IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
                    mainClass);

            workingCopy.setAttribute(
                    IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                    project.getName());

            IPath location = project.getLocation();
            workingCopy.setAttribute(
                    IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY,
                    location.toOSString());

            String resourceDirectory = getVaadinResourceDirectory(project);
            IFolder wsDir = getWebContentFolder(project).getFolder(
                    resourceDirectory).getFolder("widgetsets");
            String wsDirString = wsDir.getLocation().toPortableString();
            if (wsDirString.startsWith(location.toPortableString())) {
                wsDirString = wsDirString.replaceFirst(location
                        .toPortableString()
                        + "/", "");
            }
            String arguments;
            if (isGwt20) {
                arguments = "-noserver -war " + wsDirString + " "
                        + getConfiguredWidgetSet(jproject);
            } else {
                arguments = "-noserver -out " + wsDirString;
            }

            workingCopy.setAttribute(
                    IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS,
                    arguments);

            String vmargs = "-Xmx512M -XX:MaxPermSize=256M";
            if (getPlatform().equals("mac") && !isGwt20) {
                vmargs = vmargs + " -XstartOnFirstThread";
            }
            workingCopy
                    .setAttribute(
                            IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS,
                            vmargs);

            // construct the launch classpath

            List<String> classPath = new ArrayList<String>();

            // GWT libraries should come first
            classPath.add(JavaRuntime.newArchiveRuntimeClasspathEntry(
                    getGWTDevJarPath(jproject)).getMemento());
            classPath.add(JavaRuntime.newArchiveRuntimeClasspathEntry(
                    getGWTUserJarPath(jproject)).getMemento());

            // default classpath reference, instead of "exploding"
            // JavaRuntime.computeUnresolvedRuntimeClasspath()
            classPath.add(JavaRuntime.newDefaultProjectClasspathEntry(jproject)
                    .getMemento());

            // add source paths on the classpath
            for (IClasspathEntry entry : jproject.getRawClasspath()) {
                if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    IRuntimeClasspathEntry source = JavaRuntime
                            .newArchiveRuntimeClasspathEntry(entry.getPath());
                    classPath.add(source.getMemento());
                }
            }

            workingCopy
                    .setAttribute(
                            IJavaLaunchConfigurationConstants.ATTR_CLASSPATH,
                            classPath);
            workingCopy.setAttribute(
                    IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH,
                    false);

            return workingCopy.doSave();

        } catch (CoreException e) {
            handleBackgroundException(
                    "Failed to find or create development mode launch for project "
                            + project.getName(), e);
            return null;
        }
    }

    /**
     * Check that a Liferay path is valid: WEB-INF directory exists and contains
     * lib/vaadin.jar .
     * 
     * @param liferayPath
     *            path to validate as a string
     * @return true if the Liferay WEB-INF directory exists and has
     *         lib/vaadin.jar
     */
    public static boolean validateLiferayPath(String liferayPath) {
        IPath jarPath = getLiferayVaadinJarPath(liferayPath);
        return jarPath.toFile().exists();
    }

    public static void setLiferayPath(IProject project, String liferayPath)
            throws CoreException {

        // assuming the path has already been validated

        // save path

        ScopedPreferenceStore prefStore = new ScopedPreferenceStore(
                new ProjectScope(project), VaadinPlugin.PLUGIN_ID);

        prefStore.setValue(VaadinPlugin.PREFERENCES_LIFERAY_PATH, liferayPath);
        
        try {
            prefStore.save();
        } catch (IOException e) {
            throw VaadinPluginUtil.newCoreException(
                    "Failed to save project preferences", e);
        }
        
        // update classpath
        
        IJavaProject jproject = JavaCore.create(project);

        IClasspathEntry[] rawClasspath = jproject.getRawClasspath();
        List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
        for (IClasspathEntry entry : rawClasspath) {
            entries.add(entry);
        }

        IPath vaadinJarPath = getLiferayVaadinJarPath(liferayPath);
        IClasspathEntry vaadinClasspathEntry = JavaCore.newLibraryEntry(
                vaadinJarPath, null, null);

        // replace vaadin.jar if found, otherwise append new entry
        replaceClassPathEntry(entries, vaadinClasspathEntry,
                new String[] { "vaadin.jar" }, true);

        IClasspathEntry[] entryArray = entries
                .toArray(new IClasspathEntry[entries.size()]);
        jproject.setRawClasspath(entryArray, null);
    }

    public static IPath getLiferayWebInfPath(String liferayPath) {
        // TODO make this work with other application servers
        if (!liferayPath.contains("WEB-INF")
                && !(new File(liferayPath + "/lib/vaadin.jar").exists())) {
            // assuming Tomcat with default Liferay directory structure 
            liferayPath = liferayPath + "/webapps/ROOT/WEB-INF";
        }
        return new Path(liferayPath);
    }

    public static IPath getLiferayVaadinJarPath(String liferayPath) {
        return getLiferayWebInfPath(liferayPath).append(
                IPath.SEPARATOR + "lib"
                + IPath.SEPARATOR + "vaadin.jar");
    }
}
