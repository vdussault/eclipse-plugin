package com.vaadin.eclipse.integration.jarbundle;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

/**
 * Method copied from Vaadin Eclipse Integration Plugin. Must match for this
 * plugin to work.
 * 
 * @author Artur Signell
 */
public class LocalFileManager {

    public static final String VAADIN_INTEGRATION_PLUGIN_ID = "com.vaadin.integration.eclipse";

    /**
     * Find the "configuration" path on the local system.
     * 
     * @return
     * @throws CoreException
     * @throws IOException
     */
    private static IPath getConfigurationPath() throws IOException {
        URL userLocation = Platform.getUserLocation().getURL();
        URL configurationLocation = Platform.getConfigurationLocation()
                .getURL();

        if (configurationLocation != null) {
            return new Path(FileLocator.toFileURL(configurationLocation)
                    .getPath()).append(IPath.SEPARATOR
                    + VAADIN_INTEGRATION_PLUGIN_ID);
        }

        if (userLocation != null) {
            return new Path(FileLocator.toFileURL(userLocation).getPath())
                    .append(IPath.SEPARATOR + VAADIN_INTEGRATION_PLUGIN_ID);
        }

        IPath stateLocation = VaadinJarBundlePlugin.getInstance()
                .getStateLocation();
        if (stateLocation != null) {
            return stateLocation;
        }

        throw new IOException(
                "getConfigurationPath found nowhere to store files");
    }

    /**
     * Returns the directory where the plugin should place downloaded files.
     * 
     * @return
     * @throws CoreException
     * @throws IOException
     */
    public static IPath getDownloadDirectory() throws IOException {
        IPath path = getConfigurationPath()
                .append(IPath.SEPARATOR + "download");

        // Create the directory if it does not exist
        if (!path.toFile().exists()) {
            path.toFile().mkdirs();
        }

        return path;
    }

}
