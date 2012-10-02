package com.vaadin.integration.eclipse.util.data;

import com.vaadin.integration.eclipse.util.files.LocalFileManager.FileType;

public class MavenVaadinVersion extends AbstractVaadinVersion {
    public MavenVaadinVersion(String versionNumber) {
        super(versionNumber,
                versionNumber.endsWith("-SNAPSHOT") ? FileType.VAADIN_NIGHTLY
                        : FileType.VAADIN_RELEASE);
    }
}
