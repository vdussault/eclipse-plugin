package com.vaadin.integration.eclipse.util.data;

import org.eclipse.core.runtime.IPath;

import com.vaadin.integration.eclipse.util.files.LocalFileManager.FileType;

public class LocalVaadinVersion extends AbstractVaadinVersion {

    private IPath jarFile;

    public LocalVaadinVersion(FileType type, String versionNumber, IPath jarFile) {
        super(versionNumber, type);
        this.jarFile = jarFile;
    }

    /**
     * Returns the path and filename to the local copy of the Vaadin jar.
     * 
     * @return
     */
    public IPath getJarFile() {
        return jarFile;
    }

    /**
     * Returns the filename of the Vaadin jar.
     * 
     * @return
     */
    public String getJarFilename() {
        return jarFile.lastSegment();
    }

}
