package com.vaadin.integration.eclipse.jarbundle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleContext;

public class VaadinJarBundlePlugin extends Plugin implements IStartup {

    /**
     * Directory that contains the same hierarchy as the download directory used
     * by the Vaadin Eclipse Integration Plugin.
     */
    private static final String LOCAL_JAR_FILE_LOCATION = "jarfiles";

    private static final String PLUGIN_ID = "com.vaadin.integration.eclipse.jarbundle";

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
        getLog().log(
                new Status(Status.INFO, PLUGIN_ID,
                        "Copying bundled Vaadin files to integration plugin if needed"));
        // Find configuration path
        IPath downloadPath = LocalFileManager.getDownloadDirectory();

        // Copy locally bundled files if needed
        copyNonexistingFilesRecursively(LOCAL_JAR_FILE_LOCATION,
                downloadPath.toFile());

    }

    private void copyNonexistingFilesRecursively(String bundleEntry, File target)
            throws IOException {
        if (bundleEntry.contains(".svn")) {
            return;
        }

        @SuppressWarnings("unchecked")
        Enumeration<String> paths = getBundle().getEntryPaths(bundleEntry);

        if (paths != null) {
            // Directory
            target.mkdirs();

            while (paths.hasMoreElements()) {
                String path = paths.nextElement();
                copyNonexistingFilesRecursively(path,
                        new File(target, path.substring(bundleEntry.length())));
            }
        } else {

            // Files are copied only if the target does not exist (never
            // overwrite)
            if (!target.exists()) {
                URL file = getBundle().getEntry(bundleEntry);
                try {
                    copyFile(file, target);
                } catch (Exception e) {
                    String message = "Copying file " + file.toString() + " to "
                            + target + " failed";
                    getLog().log(new Status(Status.ERROR, PLUGIN_ID, message));

                }
            }
        }
    }

    private void copyFile(URL source, File target)
            throws FileNotFoundException, IOException {
        // System.out.println("Copy file: " + source + " -> " + target);
        IOUtils.copyLarge(source.openStream(), new FileOutputStream(target));

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
