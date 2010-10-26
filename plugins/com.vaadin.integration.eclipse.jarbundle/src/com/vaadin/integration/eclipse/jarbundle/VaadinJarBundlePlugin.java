package com.vaadin.integration.eclipse.jarbundle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class VaadinJarBundlePlugin extends Plugin implements IStartup {

    /**
     * Directory that contains the same hierarchy as the download directory used
     * by the Vaadin Eclipse Integration Plugin.
     */
    private static final String LOCAL_JAR_FILE_LOCATION = "jarfiles";

    private static VaadinJarBundlePlugin instance;

    private BundleContext bundleContext;

    public VaadinJarBundlePlugin() {
        instance = this;
    }

    public static VaadinJarBundlePlugin getInstance() {
        return instance;
    }

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        super.start(bundleContext);
        this.bundleContext = bundleContext;
        try {
            copyJarFiles();
        } catch (IOException e) {
            showErrorMessage("Copy failed",
                    "Failed to copy bundled Vaadin jar files");

        }
    }

    private void copyJarFiles() throws IOException {
        // Find configuration path
        IPath downloadPath = LocalFileManager.getDownloadDirectory();

        // Local directory
        IPath localPluginDirectory = findBundledJarLocation();

        // Copy locally bundled files if needed
        copyNonexistingFilesRecursively(localPluginDirectory.toFile(),
                downloadPath.toFile());

    }

    private IPath findBundledJarLocation() throws IOException {
        Bundle bundle = bundleContext.getBundle();
        File bundleRoot = FileLocator.getBundleFile(bundle);
        return new Path(bundleRoot.getAbsolutePath() + File.separator
                + LOCAL_JAR_FILE_LOCATION);

    }

    private void copyNonexistingFilesRecursively(File source, File target) {
        // System.out.println("Copy from " + source + " to " + target);

        if (source.isDirectory()) {
            if (!target.exists()) {
                target.mkdirs();
            }

            for (File f : source.listFiles()) {
                copyNonexistingFilesRecursively(f,
                        new File(target, f.getName()));
            }
        } else {
            // Files are copied only if the target does not exist (never
            // overwrite)
            if (!target.exists()) {
                try {
                    copyFile(source, target);
                } catch (Exception e) {
                    showErrorMessage("Copy failed", "Copying file " + source
                            + " to " + target + " failed");
                }
            }
        }

    }

    private void copyFile(File source, File target)
            throws FileNotFoundException, IOException {
        // System.out.println("Copy file: " + source + " -> " + target);
        IOUtils.copyLarge(new FileReader(source), new FileWriter(target));

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

    public void earlyStartup() {
        // Needed for the class to be loaded. The actual initialization is done
        // when the bundle is initialized.
    }
}
