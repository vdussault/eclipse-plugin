package com.vaadin.integration.eclipse.util.network;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.data.MavenVaadinVersion;
import com.vaadin.integration.eclipse.util.files.LocalFileManager;
import com.vaadin.integration.eclipse.util.files.LocalFileManager.FileType;

public class MavenVersionManager {

    private static final String AVAILABLE_VAADIN_VERSIONS_7_URL = DownloadManager.VAADIN_DOWNLOAD_BASE_URL
            + "VERSIONS_7";

    private static List<MavenVaadinVersion> availableVersions;

    /**
     * Returns a list of what Vaadin versions are available for dependency
     * management systems. The list contains release version and additionally,
     * if onlyRelease is false, nightly and pre-release versions.
     * 
     * It is not guaranteed that the list is fetched from the site every time
     * this is called.
     * 
     * @param onlyRelease
     *            True to include only release builds, false to include others
     *            also (nightly, pre-release)
     * @return A sorted list of available Vaadin versions
     * @throws CoreException
     * 
     */
    public static synchronized List<MavenVaadinVersion> getAvailableVersions(
            boolean onlyRelease) throws CoreException {
        if (availableVersions == null) {
            try {
                availableVersions = downloadAvailableVersionsList();
            } catch (CoreException e) {
                ErrorUtil
                        .handleBackgroundException(
                                "Failed to retrieve available Vaadin 7 version list from server, using cached list",
                                e);
                availableVersions = getCachedAvailableVersionsList();
            }
        }

        List<MavenVaadinVersion> versions;
        if (onlyRelease) {
            // Filter out non-releases
            versions = new ArrayList<MavenVaadinVersion>();
            for (MavenVaadinVersion version : availableVersions) {
                if (version.getType() == FileType.VAADIN_RELEASE) {
                    versions.add(version);
                }
            }
        } else {
            // Return everything
            versions = new ArrayList<MavenVaadinVersion>(availableVersions);
        }
        return versions;
    }

    /**
     * Download and return the list of available Vaadin versions from vaadin.com
     * .
     * 
     * If the download succeeds, also save the list in the cache.
     * 
     * @return
     * @throws CoreException
     */
    private static List<MavenVaadinVersion> downloadAvailableVersionsList()
            throws CoreException {
        try {
            String versionData = DownloadManager
                    .downloadURL(AVAILABLE_VAADIN_VERSIONS_7_URL);

            // store versionData in cache
            try {
                File cacheFile = getCacheFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(
                        cacheFile));
                try {
                    writer.write(versionData);
                } finally {
                    writer.close();
                }
            } catch (CoreException e) {
                // log and ignore - the version list is still valid
                ErrorUtil.handleBackgroundException(
                        "Failed to save Vaadin 7 version list to cache", e);
            } catch (IOException e) {
                // log and ignore - the version list is still valid
                ErrorUtil.handleBackgroundException(
                        "Failed to save Vaadin 7 version list to cache", e);
            }

            return parseAvailableVersions(versionData);
        } catch (IOException e) {
            throw ErrorUtil.newCoreException(
                    "Failed to download list of available Vaadin versions", e);
        }
    }

    /**
     * Return the cached list of available Vaadin versions from last successful
     * request to vaadin.com .
     * 
     * @return
     * @throws CoreException
     */
    private static List<MavenVaadinVersion> getCachedAvailableVersionsList()
            throws CoreException {
        try {
            File cacheFile = getCacheFile();
            InputStream is = new FileInputStream(cacheFile);
            String versionData;
            try {
                versionData = IOUtils.toString(is);
            } finally {
                is.close();
            }

            return parseAvailableVersions(versionData);
        } catch (IOException e) {
            throw ErrorUtil
                    .newCoreException(
                            "Failed to get cached list of available Vaadin versions",
                            e);
        }
    }

    private static File getCacheFile() throws CoreException {
        IPath path = LocalFileManager.getConfigurationPath().append(
                IPath.SEPARATOR + "VERSIONS_7");
        return path.toFile();
    }

    /**
     * Parses the available versions and URLs from comma separated data.
     * 
     * Anything after a comma is ignored, as are comment rows. A row starting
     * with a comma can be used in future file versions for information
     * incompatible with this plug-in version.
     * 
     * @param versionData
     * @return
     */
    private static List<MavenVaadinVersion> parseAvailableVersions(
            String versionData) {
        List<MavenVaadinVersion> availableVersions = new ArrayList<MavenVaadinVersion>();

        String[] rows = versionData.split("(\r|\n)");
        for (String row : rows) {
            String[] data = row.split(",");
            if (data.length == 0 || data[0].startsWith("#")) {
                // Skip unknown data
                continue;
            }

            // in this version, ignore anything after a comma
            String versionNumber = data[0].trim();
            if (!"".equals(versionNumber)) {
                MavenVaadinVersion vaadinVersion = new MavenVaadinVersion(
                        versionNumber);
                availableVersions.add(vaadinVersion);
            }
        }

        return availableVersions;
    }

}
