package com.vaadin.integration.eclipse.util.data;

import com.vaadin.integration.eclipse.util.files.LocalFileManager.FileType;

public class MavenVaadinVersion extends AbstractVaadinVersion {
    // TODO update to beta2
    public static final String VAADIN_7_BETA_VERSION_STRING = "7.0.0.beta1";
    public static final String VAADIN_7_SNAPSHOT_VERSION_STRING = "7.0-SNAPSHOT";

    public static final MavenVaadinVersion VAADIN_7_BETA_VERSION = new MavenVaadinVersion(
            VAADIN_7_BETA_VERSION_STRING);
    public static final MavenVaadinVersion VAADIN_7_SNAPSHOT_VERSION = new MavenVaadinVersion(
            VAADIN_7_SNAPSHOT_VERSION_STRING);

    public MavenVaadinVersion(String versionNumber) {
        super(versionNumber,
                versionNumber.endsWith("-SNAPSHOT") ? FileType.VAADIN_NIGHTLY
                        : FileType.VAADIN_RELEASE);
    }
}
