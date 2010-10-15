package com.vaadin.integration.eclipse.util;

import org.eclipse.core.resources.IProject;

import com.vaadin.integration.eclipse.VaadinPlugin;

public class LegacyUtil {
    /**
     * Returns either "com.vaadin." or "com.itmill.toolkit." depending on the
     * Vaadin version in the project. Defaults to "com.vaadin." if neither
     * found.
     * 
     * @param project
     * @return
     * 
     * @deprecated
     */
    @Deprecated
    public static String getVaadinPackagePrefix(IProject project) {
        if (ProjectUtil.isVaadin6(project)) {
            return VaadinPlugin.VAADIN_PACKAGE_PREFIX;
        } else {
            return VaadinPlugin.TOOLKIT_PACKAGE_PREFIX;
        }
    }

    /**
     * Returns either "VAADIN" or "ITMILL" depending on the Vaadin version in
     * the project, returning a default value if neither is found.
     * 
     * @param project
     * @return
     * 
     * @deprecated
     */
    @Deprecated
    public static String getVaadinResourceDirectory(IProject project) {
        if (ProjectUtil.isVaadin6(project)) {
            return "VAADIN";
        } else {
            return "ITMILL";
        }
    }

}
