package com.vaadin.integration.eclipse.wizards;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;

import com.vaadin.integration.eclipse.util.VaadinPluginUtil;

/**
 * Vaadin Directory package model for export.
 *
 * This provides some defaults and a somewhat more convenient API than using
 * {@link JarPackageData} directly.
 */
public class DirectoryPackageData extends JarPackageData {

    private String implementationTitle;
    private String implementationVersion;
    private String implementationVendor;
    private String licenseTitle;

    // TODO Vaadin widgetsets for the addon

    /**
     * Constructor with default manifest provider.
     */
    public DirectoryPackageData() {
        setManifestProvider(new DirectoryManifestProvider());
    }

    /**
     * Prepare the directory package description for a project.
     *
     * Setup the manifest location and load the manifest, as well as set up
     * generation of the manifest. The manifest is assumed to be located in the
     * webcontent directory.
     *
     * @param jproject
     * @throws IOException
     *             if loading of a pre-existing manifest fails
     * @throws CoreException
     *             if loading of a pre-existing manifest fails
     */
    public void setupProject(IJavaProject jproject) throws CoreException,
            IOException {
        if (jproject == null) {
            return;
        }

        // find manifest from WebContent
        // IFile rootManifest =
        // jproject.getProject().getFile("META-INF/MANIFEST.MF");
        IFile manifestFile = VaadinPluginUtil.getWebContentFolder(
                jproject.getProject()).getFile("META-INF/MANIFEST.MF");
        boolean manifestExists = manifestFile.exists();

        setManifestLocation(manifestFile.getFullPath());
        // DirectoryManifestProvider uses this in a somewhat non-standard
        // manner.
        // We need to save the manifest separately (rather than letting
        // JarFileExportOperation save it) to make sure it is included in the
        // WebContent/META-INF of the generated JAR - otherwise, it would be
        // updated too late.
        setGenerateManifest(false);
        setReuseManifest(true);
        setSaveManifest(true);

        if (manifestExists) {
            // read relevant values from the old manifest
            DirectoryManifestProvider.loadDirectoryAttributesFromManifest(this);
        }
        // if nothing loaded, set default values
        // TODO defaults?
        if (getImplementationTitle() == null) {
            setImplementationTitle(jproject.getProject().getName());
        }
        if (getImplementationVersion() == null) {
            setImplementationVersion("1.0");
        }
        if (getImplementationVendor() == null) {
            setImplementationVendor("-");
        }
        if (getLicenseTitle() == null) {
            setLicenseTitle("proprietary");
        }
    }

    public String getImplementationTitle() {
        return implementationTitle;
    }

    public void setImplementationTitle(String implementationTitle) {
        this.implementationTitle = implementationTitle;
    }

    public String getImplementationVersion() {
        return implementationVersion;
    }

    public void setImplementationVersion(String implementationVersion) {
        this.implementationVersion = implementationVersion;
    }

    public String getImplementationVendor() {
        return implementationVendor;
    }

    public void setImplementationVendor(String implementationVendor) {
        this.implementationVendor = implementationVendor;
    }

    public String getLicenseTitle() {
        return licenseTitle;
    }

    public void setLicenseTitle(String licenseTitle) {
        this.licenseTitle = licenseTitle;
    }

}
