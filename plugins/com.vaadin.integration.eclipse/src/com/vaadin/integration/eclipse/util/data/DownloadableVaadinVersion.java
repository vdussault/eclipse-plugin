package com.vaadin.integration.eclipse.util.data;

import com.vaadin.integration.eclipse.util.files.LocalFileManager.FileType;

public class DownloadableVaadinVersion extends AbstractVaadinVersion {

    private String downloadURL;

    public DownloadableVaadinVersion(String versionNumber, FileType type,
            String url) {
        super(versionNumber, type);
        this.downloadURL = url;
    }

    public String getDownloadURL() {
        return downloadURL;
    }

}
