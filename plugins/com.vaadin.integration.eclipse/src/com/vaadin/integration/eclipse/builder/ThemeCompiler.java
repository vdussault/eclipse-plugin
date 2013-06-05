package com.vaadin.integration.eclipse.builder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.ui.console.MessageConsoleStream;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.consoles.CompileThemeConsole;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.PreferenceUtil;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;
import com.vaadin.integration.eclipse.util.WidgetsetUtil;

public class ThemeCompiler {

    /**
     * Does the project support Vaadin 7 theme compilation
     * 
     * @param jproject
     *            The project
     * @return Returns true if project supports SCSS theme compilation
     */
    public static boolean isSupported(IProject project) {
        return VaadinPluginUtil.isVaadinFeatureTypeSupported(
                VaadinPlugin.THEME_COMPILER_CLASS, project);
    }

    /**
     * Has automatic theme compilation explicitly been suspended by the user
     * 
     * @param project
     *            The project
     * @return True if theme compilation should be suspended
     */
    public static boolean isSuspended(IProject project) {
        return PreferenceUtil.get(project).isThemeCompilationSuspended();
    }

    /**
     * Suspends automatic theme compilation
     * 
     * @param project
     * @param suspended
     */
    public static void setSuspended(IProject project, boolean suspended) {
        PreferenceUtil.get(project).setThemeCompilationSuspended(suspended);
    }

    public static void run(IProject project, final IProgressMonitor monitor,
            IFolder themeDir) throws CoreException, IOException {

        // TODO should be done by builder, not here
        // if (ThemeCompiler.isSuspended(project)) {
        // return;
        // }

        final long start = new Date().getTime();
        CompileThemeConsole console = CompileThemeConsole.get();

        IJavaProject jproject = JavaCore.create(project);

        IVMInstall vmInstall = VaadinPluginUtil.getJvmInstall(jproject, true);

        ArrayList<String> commonArgs = WidgetsetUtil.buildCommonArgs(jproject,
                vmInstall);

        ArrayList<String> compilerArgs = new ArrayList<String>(commonArgs);

        compilerArgs.add(VaadinPlugin.THEME_COMPILER_CLASS);

        // .scss file and .css file paths
        IFile scssFile = themeDir.getFile("styles.scss");
        IFile cssFile = themeDir.getFile("styles.css");
        compilerArgs.add(scssFile.getLocation().toPortableString());
        compilerArgs.add(cssFile.getLocation().toPortableString());

        final String[] argsStr = new String[compilerArgs.size()];
        compilerArgs.toArray(argsStr);

        ProcessBuilder b = new ProcessBuilder(argsStr);

        IPath projectLocation = project.getLocation();
        b.directory(projectLocation.toFile());

        final Process exec = b.start();

        console.setCompilationProcess(exec);

        MessageConsoleStream newMessageStream = console.newMessageStream();

        console.activate();
        newMessageStream.println("Compiling theme " + themeDir.getName());
        newMessageStream.println();

        InputStream inputStream = exec.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(inputStream));
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            newMessageStream.println(line);
        }

        monitor.beginTask("Compiling theme", 1);
        try {
            int result = exec.waitFor();
            monitor.worked(1);
            if (result != 0) {
                newMessageStream.println("Compiling theme "
                        + themeDir.getName() + " failed after "
                        + (System.currentTimeMillis() - start) + " ms");
            } else {
                newMessageStream.println("Compilation of theme "
                        + themeDir.getName() + " done in "
                        + (System.currentTimeMillis() - start) + " ms");
            }
            newMessageStream.println();

            themeDir.refreshLocal(IResource.DEPTH_INFINITE,
                    new SubProgressMonitor(monitor, 1));
        } catch (InterruptedException e) {
            newMessageStream.println("Compiling theme " + themeDir.getName()
                    + " interrupted");
            ErrorUtil.logInfo("Theme compilation interrupted");
        } finally {
            monitor.done();
            console.setCompilationProcess(null);
        }
    }
}
