package com.vaadin.integration.eclipse.util.files;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.NumberFormatter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.LegacyUtil;
import com.vaadin.integration.eclipse.util.PlatformUtil;
import com.vaadin.integration.eclipse.util.VersionUtil;
import com.vaadin.integration.eclipse.util.data.LocalVaadinVersion;

/**
 * Class with helper methods for handling files stored locally by the plugin.
 * Currently Vaadin JARs (release/nightly/prerelease) and GWT jars are stored
 * locally in the path returned by {@link #getDownloadDirectory()}.
 * 
 * @author Artur Signell
 */
public class LocalFileManager {

    // this is used to extract version number parts for sorting
    // this pattern does not filter out old versions
    private static final String VAADIN_VERSION_PART_PATTERN = "([0-9]*)\\.([0-9])\\.(.+)";

    /**
     * Directory where additional GWT dependencies are stored (as
     * <dir>/<gwtVersion>/<file.jar>
     */
    private static final String GWT_DEPS_DIRECTORY = "gwt-dependencies";

    public enum FileType {
        VAADIN_RELEASE("vaadin", "release", "vaadin-#version#.jar"), //
        VAADIN_PRERELEASE("vaadin-prerelease", "prerelease",
                "vaadin-#version#.jar"), //
        VAADIN_NIGHTLY("vaadin-nightly", "nightly", "vaadin-#version#.jar"), //
        GWT_USER_JAR("gwt-user", null, "gwt-user.jar"), //
        GWT_DEV_JAR("gwt-dev", null, "gwt-dev.jar"), //
        GWT_DEV_JAR_PLATFORM_DEPENDENT("gwt-dev", null,
                "gwt-dev-#platform#.jar");//

        private String localDirectory;
        private String releaseType;
        private String filename;

        private FileType(String localDirectory, String releaseType,
                String filename) {
            this.localDirectory = localDirectory;
            this.releaseType = releaseType;
            this.filename = filename;
        }

        public String getLocalDirectory() {
            return localDirectory;
        }

        public String getReleaseType() {
            return releaseType;
        }

        public String getFilename(String version) {
            return filename.replace("#version#", version).replace("#platform#",
                    PlatformUtil.getPlatform());
        }

        /**
         * Returns the VersionType that corresponds to the given release type.
         * 
         * @param releaseType
         *            The release type to look up
         * @return The VersionType corresponding to releaseType or null if not
         *         found.
         */
        public static FileType getVaadinReleaseType(String releaseType) {
            if (releaseType == null) {
                return null;
            }

            for (FileType t : values()) {
                if (t.getReleaseType().equals(releaseType)) {
                    return t;
                }
            }

            return null;
        }

        public static List<FileType> vaadinReleaseTypes() {
            List<FileType> types = new ArrayList<LocalFileManager.FileType>();
            for (FileType t : values()) {
                if (t.getReleaseType() != null) {
                    types.add(t);
                }
            }

            return types;
        }
    }

    /**
     * Find the "configuration" path on the local system.
     * 
     * @return
     * @throws CoreException
     */
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
                throw ErrorUtil.newCoreException("getConfigurationPath failed",
                        e);
            }
        }

        if (userLocation != null) {
            try {
                return new Path(FileLocator.toFileURL(userLocation).getPath())
                        .append(IPath.SEPARATOR + VaadinPlugin.PLUGIN_ID);
            } catch (IOException e) {
                throw ErrorUtil.newCoreException("getConfigurationPath failed",
                        e);
            }
        }

        IPath stateLocation = VaadinPlugin.getInstance().getStateLocation();
        if (stateLocation != null) {
            return stateLocation;
        }

        throw ErrorUtil.newCoreException(
                "getConfigurationPath found nowhere to store files", null);
    }

    /**
     * Returns the directory where the plugin should place downloaded files.
     * 
     * @return
     * @throws CoreException
     */
    public static IPath getDownloadDirectory() throws CoreException {
        IPath path = getConfigurationPath()
                .append(IPath.SEPARATOR + "download");

        // Create the directory if it does not exist
        if (!path.toFile().exists()) {
            path.toFile().mkdirs();
        }

        return path;
    }

    public static IPath getDownloadDirectory(FileType type)
            throws CoreException {
        return getDownloadDirectory(type.getLocalDirectory());
    }

    public static IPath getDownloadDirectory(String directoryName)
            throws CoreException {
        IPath path = getDownloadDirectory().append(
                IPath.SEPARATOR + directoryName);

        // Create the directory if it does not exist
        if (!path.toFile().exists()) {
            path.toFile().mkdirs();
        }

        return path;
    }

    /**
     * Returns the directory where the given version of the given type is
     * placed.
     * 
     * @param type
     *            Type of file
     * 
     * @param version
     *            Version string
     * @return
     * @throws CoreException
     */
    public static IPath getVersionedDownloadDirectory(FileType type,
            String version) throws CoreException {
        return getVersionedDownloadDirectory(type.getLocalDirectory(), version);
    }

    public static IPath getVersionedDownloadDirectory(String directoryName,
            String version) throws CoreException {
        IPath path = getDownloadDirectory(directoryName).append(
                IPath.SEPARATOR + version);

        return path;
    }

    /**
     * Returns the version for the newest locally available (cached) Vaadin
     * version. If no local JARs are found in the cache, null is returned.
     * 
     * @return
     * @throws CoreException
     */
    // TODO official versions only?
    public static LocalVaadinVersion getNewestLocalVaadinVersion()
            throws CoreException {
        try {
            List<LocalVaadinVersion> versions = getLocalVaadinVersions(false);
            if (versions.size() > 0) {
                return versions.get(0);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw ErrorUtil.newCoreException(
                    "Failed to get the latest local Vaadin version", e);
        }
    }

    //
    // }

    /**
     * Returns the versions for all the locally cached Vaadin versions, sorted
     * with the latest version first.
     * 
     * If no locally cached version is found, an empty List is returned.
     * 
     * @param includeEarlyVaadin7
     *            if true, single-JAR early Vaadin 7 alphas and betas are also
     *            listed.
     * @return
     * @throws CoreException
     */
    public static List<LocalVaadinVersion> getLocalVaadinVersions(
            boolean includeEarlyVaadin7) throws CoreException {
        NumberFormatter nf = new NumberFormatter(new DecimalFormat("000"));

        // the key is only used internally for sorting
        SortedMap<String, LocalVaadinVersion> versions = new TreeMap<String, LocalVaadinVersion>();

        try {
            for (FileType type : FileType.vaadinReleaseTypes()) {
                File downloadDirectory = getDownloadDirectory(type).toFile();

                if (!downloadDirectory.exists()) {
                    // ignore and continue with next directory
                    continue;
                }

                File[] files = downloadDirectory.listFiles();
                Pattern pattern = Pattern.compile("^"
                        + VAADIN_VERSION_PART_PATTERN + "$");
                for (File jarDirectory : files) {
                    String name = jarDirectory.getName();

                    Matcher m = pattern.matcher(name);
                    if (m.matches()) {
                        try {
                            int major = Integer.parseInt(m.group(1));
                            if (major > 6 && !includeEarlyVaadin7) {
                                continue;
                            }
                            int minor = Integer.parseInt(m.group(2));
                            // the third component may be other than an int
                            String revision = m.group(3);
                            String key = nf.valueToString(major)
                                    + nf.valueToString(minor);
                            // make sure integers are sorted before other
                            // strings
                            if (revision.matches("[0-9]+")) {
                                Integer revInt = Integer.parseInt(revision);
                                key += "2" + nf.valueToString(revInt);
                            } else if (revision.matches("[0-9].*")) {
                                // #3579 this is primarily for 6.2 nightly
                                // builds
                                key += "1" + revision;
                            } else {
                                key += "0" + revision;
                            }
                            LocalVaadinVersion version = new LocalVaadinVersion(
                                    type,
                                    name,
                                    new Path(jarDirectory.getAbsolutePath()
                                            + Path.SEPARATOR
                                            + VersionUtil
                                                    .getVaadinJarFilename(name)));
                            versions.put(key, version);
                        } catch (ParseException pe) {
                            // log and ignore
                            ErrorUtil.handleBackgroundException(IStatus.INFO,
                                    "Failed to parse the Vaadin version number "
                                            + name, pe);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw ErrorUtil.newCoreException(
                    "Failed to list local Vaadin versions", e);
        }

        List<LocalVaadinVersion> versionList = new ArrayList<LocalVaadinVersion>(
                versions.values());

        // return latest version first
        Collections.reverse(versionList);

        return versionList;
    }

    /**
     * Get the local (downloaded) Vaadin JAR version object for a given version
     * number.
     * 
     * @param versionString
     *            Vaadin version number string
     * @return Version object or null if none found
     * @throws CoreException
     */
    public static LocalVaadinVersion getLocalVaadinVersion(String versionString)
            throws CoreException {

        if (versionString == null) {
            // optimization - no need to get the list
            return null;
        }
        List<LocalVaadinVersion> versions = getLocalVaadinVersions(true);
        for (LocalVaadinVersion version : versions) {
            if (version.getVersionNumber().equals(versionString)) {
                return version;
            }
        }

        // not found
        return null;
    }

    /**
     * Returns a reference to the local file with the given type and version.
     * 
     * @param fileType
     * @param versionNumber
     * @return
     * @throws CoreException
     */
    public static IPath getLocalFile(FileType fileType, String versionNumber)
            throws CoreException {
        return LocalFileManager.getVersionedDownloadDirectory(fileType,
                versionNumber).append(
                IPath.SEPARATOR + fileType.getFilename(versionNumber));
    }

    private static IPath getLocalFile(String directoryName,
            String versionNumber, String filename) throws CoreException {
        return LocalFileManager.getVersionedDownloadDirectory(directoryName,
                versionNumber).append(IPath.SEPARATOR + filename);
    }

    public static IPath getLocalGwtUserJar(String gwtVersion)
            throws CoreException {
        return getLocalFile(FileType.GWT_USER_JAR, gwtVersion);
    }

    public static IPath getLocalGWTDependencyJar(String gwtVersion,
            String dependencyJar) throws CoreException {
        return getLocalFile(GWT_DEPS_DIRECTORY, gwtVersion, dependencyJar);
    }

    public static IPath getLocalGwtDevJar(String gwtVersion)
            throws CoreException {
        FileType fileType = FileType.GWT_DEV_JAR;
        if (LegacyUtil.isPlatformDependentGWT(gwtVersion)) {
            fileType = FileType.GWT_DEV_JAR_PLATFORM_DEPENDENT;
        }
        return getLocalFile(fileType, gwtVersion);
    }

    public static boolean isGWTDependency(IPath path) throws CoreException {
        IPath depsPath = getDownloadDirectory(GWT_DEPS_DIRECTORY);
        return (depsPath.isPrefixOf(path.makeAbsolute()));
    }

}
