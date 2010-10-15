package com.vaadin.integration.eclipse.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.vaadin.integration.eclipse.VaadinPlugin;

public class LiferayUtil {

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
            throw ErrorUtil.newCoreException(
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
        VaadinPluginUtil.replaceClassPathEntry(entries, vaadinClasspathEntry,
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
                IPath.SEPARATOR + "lib" + IPath.SEPARATOR + "vaadin.jar");
    }

    /**
     * Checks which Vaadin version is present in a Liferay installation.
     * 
     * @param project
     * @return Vaadin version in Liferay, or null if none
     * @throws CoreException
     */
    public static String getVaadinLibraryVersionInLiferay(String liferayPath)
            throws CoreException {
        if (liferayPath == null || "".equals(liferayPath)) {
            return null;
        }

        // get version from META-INF/VERSION in lib/vaadin.jar
        IPath resource = getLiferayVaadinJarPath(liferayPath);
        return VersionUtil.getVaadinVersionFromJar(resource);
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

}
