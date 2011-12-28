package com.vaadin.integration.eclipse.util;

public class PlatformUtil {
    /**
     * Gets the platform specific separator to use between classpath string
     * segments.
     * 
     * @return a colon or a semicolon to use as classpath separator
     */
    public static String getClasspathSeparator() {
        String classpathSeparator;
        if ("windows".equals(getPlatform())) {
            classpathSeparator = ";";
        } else {
            classpathSeparator = ":";
        }
        return classpathSeparator;
    }

    public static String getPlatform() {
        String osname = System.getProperty("os.name");
        if (osname.toLowerCase().startsWith("windows")) {
            return "windows";
        } else if (osname.toLowerCase().contains("linux")) {
            return "linux";
        } else if (osname.toLowerCase().contains("mac")) {
            return "mac";
        } else {
            return "other";
        }

    }
}
