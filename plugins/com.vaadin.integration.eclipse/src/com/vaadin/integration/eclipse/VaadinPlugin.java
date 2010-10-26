package com.vaadin.integration.eclipse;

import org.eclipse.ui.plugin.AbstractUIPlugin;

public class VaadinPlugin extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "com.vaadin.integration.eclipse";

    public static final String VAADIN_PACKAGE_PREFIX = "com.vaadin.";
    public static final String APPLICATION_CLASS_NAME = "Application";
    public static final String APPLICATION_CLASS_FULL_NAME = VAADIN_PACKAGE_PREFIX
            + APPLICATION_CLASS_NAME;

    public static final String ID_COMPILE_WS_APP = "compilews";

    public static final String VAADIN_CLIENT_SIDE_CLASS_PREFIX = "V";

    public static final String VAADIN_RESOURCE_DIRECTORY = "VAADIN";

    public static final String VAADIN_DEFAULT_THEME = "reindeer";

    public static final String GWT_COMPILER_CLASS = "com.vaadin.tools.WidgetsetCompiler";

    private static VaadinPlugin instance = null;

    public VaadinPlugin() {
        instance = this;
    }

    public static VaadinPlugin getInstance() {
        return instance;
    }

}
