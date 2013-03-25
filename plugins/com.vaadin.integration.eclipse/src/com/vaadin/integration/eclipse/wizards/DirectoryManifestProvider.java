package com.vaadin.integration.eclipse.wizards;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.ui.jarpackager.ManifestProvider;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;

/**
 * Manifest provider for directory package manifest generation (when none
 * exists) or update.
 * 
 * This class should only be user with a {@link DirectoryPackageData}.
 */
@SuppressWarnings("restriction")
public class DirectoryManifestProvider extends ManifestProvider {

    public static final String MANIFEST_VAADIN_PACKAGE_VERSION = "Vaadin-Package-Version";
    public static final String MANIFEST_IMPLEMENTATION_TITLE = "Implementation-Title";
    public static final String MANIFEST_IMPLEMENTATION_VERSION = "Implementation-Version";

    public static final String MANIFEST_VAADIN_WIDGETSETS = "Vaadin-Widgetsets";
    public static final String MANIFEST_VAADIN_STYLESHEETS = "Vaadin-Stylesheets";

    /**
     * Load and update an existing manifest or generate one if none exists.
     * 
     * Note that this can have a side effect on jarPackage: generateManifest is
     * turned on and back off if it was off and no manifest existed.
     */
    @Override
    public Manifest create(JarPackageData jarPackage) throws CoreException {
        // read the manifest (if generateManifest if off)
        Manifest manifest = super.create(jarPackage);
        if (manifest == null) {
            // we cannot get here if generateManifest was on
            jarPackage.setGenerateManifest(true);
            manifest = super.create(jarPackage);
            jarPackage.setGenerateManifest(false);
        }
        // this could have been in putAdditionalEntries if only generating new
        if (jarPackage instanceof DirectoryPackageData) {
            DirectoryPackageData directoryPackage = (DirectoryPackageData) jarPackage;
            Attributes attributes = manifest.getMainAttributes();
            attributes.putValue(MANIFEST_VAADIN_PACKAGE_VERSION, "1");
            attributes.putValue(MANIFEST_IMPLEMENTATION_TITLE,
                    directoryPackage.getImplementationTitle());
            attributes.putValue(MANIFEST_IMPLEMENTATION_VERSION,
                    directoryPackage.getImplementationVersion());
            if (directoryPackage.getWidgetsets() != null
                    && !"".equals(directoryPackage.getWidgetsets().trim())) {
                attributes.putValue(MANIFEST_VAADIN_WIDGETSETS,
                        directoryPackage.getWidgetsets());
            } else {
                attributes.remove(new Attributes.Name(
                        MANIFEST_VAADIN_WIDGETSETS));
            }

            if (directoryPackage.getStylesheets() != null
                    && !"".equals(directoryPackage.getStylesheets().trim())) {
                attributes.putValue(MANIFEST_VAADIN_STYLESHEETS,
                        directoryPackage.getStylesheets());
            } else {
                attributes.remove(new Attributes.Name(
                        MANIFEST_VAADIN_STYLESHEETS));
            }
        }
        return manifest;
    }

    /**
     * Read the directory-related attributes from the manifest file associated
     * with a directory package into the package object.
     * 
     * @param directoryPackage
     * @throws CoreException
     * @throws IOException
     */
    public static void loadDirectoryAttributesFromManifest(
            DirectoryPackageData directoryPackage) throws CoreException,
            IOException {
        // No need to use buffer here because Manifest(...) does
        InputStream stream = directoryPackage.getManifestFile().getContents(
                false);
        try {
            Manifest manifest = new Manifest(stream);
            Attributes attributes = manifest.getMainAttributes();
            directoryPackage.setImplementationTitle(attributes
                    .getValue(MANIFEST_IMPLEMENTATION_TITLE));
            directoryPackage.setImplementationVersion(attributes
                    .getValue(MANIFEST_IMPLEMENTATION_VERSION));
            directoryPackage.setWidgetsets(attributes
                    .getValue(MANIFEST_VAADIN_WIDGETSETS));
            directoryPackage.setStylesheets(attributes
                    .getValue(MANIFEST_VAADIN_STYLESHEETS));
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

}
