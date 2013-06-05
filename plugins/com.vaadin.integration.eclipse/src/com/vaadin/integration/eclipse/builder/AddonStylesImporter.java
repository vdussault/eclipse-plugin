package com.vaadin.integration.eclipse.builder;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.PreferenceUtil;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;
import com.vaadin.integration.eclipse.util.WidgetsetUtil;

public class AddonStylesImporter {

    /**
     * Does the project support Vaadin 7.1 Addon importer
     * 
     * @param jproject
     *            The project
     * @return Returns true if project supports the addon importer
     */
    public static boolean isSupported(IProject project) {
        return VaadinPluginUtil.isVaadinFeatureTypeSupported(
                VaadinPlugin.ADDON_IMPORTER_CLASS, project);
    }

    /**
     * Has the addons scanning explicitly been suspended by the user
     * 
     * @param project
     *            The project
     * @return True if the addon scanning should be suspended
     */
    public static boolean isSuspended(IProject project) {
        return PreferenceUtil.get(project).isAddonThemeScanningSuspended();
    }

    /**
     * Suspends the addon styles scanning
     * 
     * @param project
     * @param suspended
     */
    public static void setSuspended(IProject project, boolean suspended) {
        PreferenceUtil.get(project).setAddonThemeScanningSuspended(suspended);
    }

    public static void run(IProject project, final IProgressMonitor monitor,
            IFolder targetDir) throws CoreException, IOException {

        if (AddonStylesImporter.isSuspended(project)) {
            return;
        }

        IJavaProject jproject = JavaCore.create(project);

        IVMInstall vmInstall = VaadinPluginUtil.getJvmInstall(jproject, true);

        ArrayList<String> commonArgs = WidgetsetUtil.buildCommonArgs(jproject,
                vmInstall);

        ArrayList<String> compilerArgs = new ArrayList<String>(commonArgs);

        compilerArgs.add(VaadinPlugin.ADDON_IMPORTER_CLASS);

        String themePath = targetDir.getLocation().toPortableString();
        compilerArgs.add(themePath);

        final String[] argsStr = new String[compilerArgs.size()];
        compilerArgs.toArray(argsStr);

        ProcessBuilder b = new ProcessBuilder(argsStr);

        IPath projectLocation = project.getLocation();
        b.directory(projectLocation.toFile());

        monitor.beginTask("Creating addons.scss", 1);

        final Process exec = b.start();

        try {
            int result = exec.waitFor();
            monitor.worked(1);
            if (result != 0) {
                throw new Error(
                        "The "
                                + AddonStylesImporter.class.getSimpleName()
                                + " failed to create then addons.scss file with the return code "
                                + result);
            }

        } catch (InterruptedException e) {
            ErrorUtil.logInfo("Update of addons.scss interrupted");
        } finally {
            monitor.done();
        }
    }
}
