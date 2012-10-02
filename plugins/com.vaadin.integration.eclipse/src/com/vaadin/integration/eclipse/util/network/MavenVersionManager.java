package com.vaadin.integration.eclipse.util.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.data.MavenVaadinVersion;
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
            availableVersions = downloadAvailableVersionsList();
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
     * 
     * @return
     * @throws CoreException
     */
    private static List<MavenVaadinVersion> downloadAvailableVersionsList()
            throws CoreException {
        try {
            String versionData = DownloadManager
                    .downloadURL(AVAILABLE_VAADIN_VERSIONS_7_URL);

            return parseAvailableVersions(versionData);
        } catch (IOException e) {
            throw ErrorUtil.newCoreException(
                    "Failed to download list of available Vaadin versions", e);
        }
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
