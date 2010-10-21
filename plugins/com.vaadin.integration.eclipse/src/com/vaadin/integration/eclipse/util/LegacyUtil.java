package com.vaadin.integration.eclipse.util;


public class LegacyUtil {

    /**
     * Returns true if the gwt version is platform dependent.
     * 
     * @param gwtVersion
     * @return
     */
    public static boolean isPlatformDependentGWT(String gwtVersion) {
        return gwtVersion.startsWith("1");
    }

}
