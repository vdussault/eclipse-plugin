package com.vaadin.integration.eclipse.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import com.vaadin.integration.eclipse.util.data.AbstractVaadinVersion;

public class VersionUtil {

    private static final String GWT_VERSION_DEPENDENCIES_ATTRIBUTE = "GWT-Version-Dependencies";
    private static final String VAADIN_VERSION_PATTERN = "([0-9]*)\\.([0-9])\\.(.+)";
    public static final String VAADIN_JAR_REGEXP = "^vaadin-(server-)?"
            + VAADIN_VERSION_PATTERN + "\\.jar$";

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

            // Try to get version from manifest (in Vaadin 6.4.6 and newer)
            String versionString = getManifestVaadinVersion(jarFile);
            if (versionString != null) {
                return versionString;
            }

            // Try to get version from META-INF/VERSION
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
     * Returns the Vaadin JAR version, as specified in the manifest.
     * 
     * @param jarFile
     *            A JarFile reference to a vaadin jar. Not closed by the method,
     *            needs to be closed afterwards. Must not be null.
     * @return The Vaadin version stated in the manifest or null if not found.
     * @throws IOException
     */
    private static String getManifestVaadinVersion(JarFile jarFile)
            throws IOException {
        return getManifestVersion(jarFile, "Bundle-Version");
    }

    /**
     * Returns the GWT version required by the Vaadin JAR, as specified in the
     * manifest.
     * 
     * @param jarFile
     *            A JarFile reference to a vaadin jar. Not closed by the method,
     *            needs to be closed afterwards. Must not be null.
     * @return The Vaadin version stated in the manifest or null if not found.
     * @throws IOException
     */
    private static String getManifestGWTVersion(JarFile jarFile)
            throws IOException {
        return getManifestVersion(jarFile, "GWT-Version");
    }

    private static String getManifestVersion(JarFile jarFile,
            String versionAttribute) throws IOException {
        Manifest manifest = jarFile.getManifest();
        if (manifest == null) {
            return null;
        }
        Attributes attr = manifest.getMainAttributes();
        String bundleName = attr.getValue("Bundle-Name");
        if (bundleName != null
                && (bundleName.equals("Vaadin") || bundleName
                        .startsWith("vaadin-"))) {
            return attr.getValue(versionAttribute);
        }

        return null;
    }

    private static List<String> getManifestArrayAttribute(JarFile jarFile,
            String attributeName) throws IOException {
        Manifest manifest = jarFile.getManifest();
        if (manifest == null) {
            return null;
        }
        Attributes attr = manifest.getMainAttributes();
        String commaSeparatedValue = attr.getValue(attributeName);
        ArrayList<String> result = new ArrayList<String>();
        if (commaSeparatedValue != null) {
            for (String value : commaSeparatedValue.split(",")) {
                result.add(value.trim());
            }
        }

        return result;
    }

    /**
     * Checks if a file with the given name could be a Vaadin Jar. The file does
     * not necessary exist so only a name based check is done.
     * 
     * @param name
     * @return
     */
    public static boolean couldBeOfficialVaadinJar(String name) {
        // Official Vaadin jars are named vaadin-<version>.jar
        // <version> should always start with a number. Failing to check this
        // will return true for e.g. vaadin-treetable-1.0.0.jar

        return name.matches(VAADIN_JAR_REGEXP);
    }

    /**
     * Returns the GWT version required by the given vaadin jar.
     * 
     * @param vaadinJarPath
     *            The path of Vaadin jar, must not be null.
     * @return The required GWT version or null if it could not be determined
     * @throws IOException
     */
    public static String getRequiredGWTVersionForVaadinJar(IPath vaadinJarPath)
            throws IOException {

        File vaadinJarFile = vaadinJarPath.toFile();
        if (vaadinJarFile == null || !vaadinJarFile.exists()) {
            return null;
        }

        // Check gwt version from included Vaadin jar
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(vaadinJarFile);
            // Check GWT version from manifest
            String manifestGWTVersion = getManifestGWTVersion(jarFile);
            if (manifestGWTVersion != null) {
                return manifestGWTVersion;
            }

            ZipEntry entry = jarFile.getEntry("META-INF/GWT-VERSION");
            if (entry == null) {
                // found JAR but not GWT version information in it, use
                // default
                return null;
            }

            // extract GWT version from the JAR
            InputStream gwtVersionStream = jarFile.getInputStream(entry);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    gwtVersionStream));

            String gwtVersion = reader.readLine();
            return gwtVersion;
        } finally {
            if (jarFile != null) {
                VaadinPluginUtil.closeJarFile(jarFile);
            }
        }
    }

    public static List<String> getRequiredGWTDependenciesForVaadinJar(
            IPath vaadinJarPath) throws IOException {
        File vaadinJarFile = vaadinJarPath.toFile();
        if (vaadinJarFile == null || !vaadinJarFile.exists()) {
            return null;
        }

        // Check gwt version from included Vaadin jar
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(vaadinJarFile);
            // Check GWT version from manifest
            return getManifestArrayAttribute(jarFile,
                    GWT_VERSION_DEPENDENCIES_ATTRIBUTE);
        } finally {
            if (jarFile != null) {
                VaadinPluginUtil.closeJarFile(jarFile);
            }
        }
    }

    /**
     * Checks if the Vaadin version is 7 or higher.
     * 
     * If major version cannot be determined, false is returned.
     * 
     * @param vaadinVersion
     * @return
     */
    public static boolean isVaadin7(AbstractVaadinVersion vaadinVersion) {
        return isVaadin7VersionString(vaadinVersion.getVersionNumber());
    }

    /**
     * Checks if the Vaadin version is 7.1 or higher.
     * 
     * If major or minor version cannot be determined, false is returned.
     * 
     * @param vaadinVersion
     * @return
     */
    public static boolean isVaadin71(AbstractVaadinVersion vaadinVersion) {
        return isVaadin71VersionString(vaadinVersion.getVersionNumber());
    }

    /**
     * Checks if the Vaadin version is 7.3 or higher.
     * 
     * If major or minor version cannot be determined, false is returned.
     * 
     * @param vaadinVersion
     * @return
     */
    public static boolean isVaadin73(AbstractVaadinVersion vaadinVersion) {
        return isVaadin73VersionString(vaadinVersion.getVersionNumber());
    }

    /**
     * Checks if the Vaadin version is 7 or higher.
     * 
     * If major version cannot be determined, false is returned.
     * 
     * @param vaadinVersion
     * @return
     */
    public static boolean isVaadin7VersionString(String vaadinVersion) {
        return isAtLeastVersionString(vaadinVersion, 7, 0);
    }

    public static boolean isVaadin71VersionString(String vaadinVersion) {
        return isAtLeastVersionString(vaadinVersion, 7, 1);
    }

    public static boolean isVaadin73VersionString(String vaadinVersion) {
        return isAtLeastVersionString(vaadinVersion, 7, 3);
    }

    private static boolean isAtLeastVersionString(String vaadinVersion,
            int majorVersion, int minorVersion) {
        if (null == vaadinVersion) {
            return false;
        }
        String[] versionStrings = vaadinVersion.split("[.-]");
        if (versionStrings.length < 2) {
            return false;
        }
        try {
            int major = Integer.parseInt(versionStrings[0]);
            int minor = Integer.parseInt(versionStrings[1]);
            return major > majorVersion
                    || (major == majorVersion && minor >= minorVersion);
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
