package com.vaadin.integration.eclipse.util.data;

import java.io.File;

import com.vaadin.integration.eclipse.util.files.LocalFileManager.FileType;

public class LocalVaadinVersion extends AbstractVaadinVersion {

    private File jarFile;

    public LocalVaadinVersion(FileType type, String versionNumber, File jarFile) {
        super(versionNumber, type);
        this.jarFile = jarFile;
    }

    /**
     * Returns the path and filename to the local copy of the Vaadin jar.
     * 
     * @return
     */
    public String getJarLocation() {
        return jarFile.getAbsolutePath();
    }

    /**
     * Returns the filename of the Vaadin jar.
     * 
     * @return
     */
    public String getJarFilename() {
        return jarFile.getName();
    }

}
