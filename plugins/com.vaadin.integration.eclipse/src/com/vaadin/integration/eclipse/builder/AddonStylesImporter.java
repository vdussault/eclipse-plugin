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
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;
import com.vaadin.integration.eclipse.util.WidgetsetUtil;

public class AddonStylesImporter {

    /**
     * Does the project support Vaadin 7.1 Addon importer
     * 
     * @param jproject
     *            The project
     * @return
     *          Returns true if project supports the addon importer
     */
    public static boolean supported(IProject project) {

        IJavaProject jproject = JavaCore.create(project);

        try {
            IVMInstall vmInstall = VaadinPluginUtil.getJvmInstall(jproject,
                    true);
            ArrayList<String> commonArgs = WidgetsetUtil.buildCommonArgs(
                    jproject, vmInstall);
            ArrayList<String> compilerArgs = new ArrayList<String>(commonArgs);
            compilerArgs.add(VaadinPlugin.ADDON_IMPORTER_CLASS);

            final String[] argsStr = new String[compilerArgs.size()];
            compilerArgs.toArray(argsStr);

            ProcessBuilder b = new ProcessBuilder(argsStr);

            return b.start().waitFor() == 0;

        } catch (Exception e) {
            
        }

        return false;
    }

    public static void run(IProject project,
            final IProgressMonitor monitor, IFolder targetDir)
            throws CoreException, IOException {

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
            if(result != 0){
                throw new Error(
                        "The "
                                + AddonStylesImporter.class.getSimpleName()
                                + " failed to create then addons.scss file with the return code "
                                + result);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            monitor.done();
        }
    }
}
