package com.vaadin.integration.eclipse.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

public class VersionUtil {
    /**
     * Extracts a Vaadin version from a filename, returns null if not a valid
     * Vaadin JAR file name.
     * 
     * @param filename
     *            JAR file name
     * @return Version or null if not a Vaadin JAR
     */
    public static String getVaadinJarVersionFromFilename(String filename) {
        // Vaadin JAR filename is always "vaadin-<version>.jar"

        if (filename.startsWith("vaadin-") && filename.endsWith(".jar")) {
            return filename.replaceAll("^vaadin-", "").replaceAll(".jar$", "");
        } else {
            return null;
        }
    }

    /**
     * Returns the standard filename of the vaadin jar with the given version.
     * 
     * @param vaadinJarVersion
     *            Version string
     * @return The full jar name of the Vaadin jar
     */
    public static String getVaadinJarFilename(String vaadinJarVersion) {
        // Vaadin JAR filename is always "vaadin-<version>.jar"
        return "vaadin-" + vaadinJarVersion + ".jar";
    }

    /**
     * Returns the Vaadin version for the given Vaadin jar
     * 
     * @param resource
     * @return The version string or null if the version could not be
     *         determined.
     */
    public static String getVaadinVersionFromJar(IPath resource) {
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

            // TODO: Try to get version from manifest

            // Try to get version from META-INF/VERSION
            String versionString = null;

            ZipEntry entry = jarFile.getEntry("META-INF/VERSION");
            if (entry != null) {
                InputStream inputStream = jarFile.getInputStream(entry);
                versionString = new BufferedReader(new InputStreamReader(
                        inputStream)).readLine();
                inputStream.close();
            }

            return versionString;
        } catch (Throwable t) {
            ErrorUtil.handleBackgroundException(IStatus.INFO,
                    "Could not access JAR when checking for Vaadin version", t);
        } finally {
            VaadinPluginUtil.closeJarFile(jarFile);
        }
        return null;
    }

    /**
     * Checks if a file with the given name could be a Vaadin Jar. The file does
     * not necessary exist so only a name based check is done.
     * 
     * @param name
     * @return
     */
    public static boolean couldBeVaadinJar(String name) {
        return name.matches("vaadin-([^.]*)\\.jar");
    }
}
