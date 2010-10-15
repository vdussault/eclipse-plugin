package com.vaadin.integration.eclipse.util.data;

import com.vaadin.integration.eclipse.util.files.LocalFileManager.FileType;

public abstract class AbstractVaadinVersion {

    private String versionNumber;
    private FileType type;

    public AbstractVaadinVersion(String versionNumber, FileType type) {
        this.versionNumber = versionNumber;
        this.type = type;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public FileType getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return versionNumber.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof AbstractVaadinVersion)) {
            return false;
        }

        AbstractVaadinVersion other = (AbstractVaadinVersion) obj;

        return other.versionNumber.equals(versionNumber);
    }

    @Override
    public String toString() {
        // Required for download popup / version list to work the way it is
        // implemented
        return getVersionNumber();
    }
}
