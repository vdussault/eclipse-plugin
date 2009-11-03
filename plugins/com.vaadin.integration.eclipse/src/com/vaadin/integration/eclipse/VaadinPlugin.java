package com.vaadin.integration.eclipse;

import org.eclipse.ui.plugin.AbstractUIPlugin;

public class VaadinPlugin extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "com.vaadin.integration.eclipse";

    public static final String VAADIN_PACKAGE_PREFIX = "com.vaadin.";
    public static final String TOOLKIT_PACKAGE_PREFIX = "com.itmill.toolkit.";
    public static final String APPLICATION_CLASS_NAME = "Application";

    public static final String ID_COMPILE_WS_APP = "compilews";

    // preference store keys
    public static final String PREFERENCES_WIDGETSET_DIRTY = PLUGIN_ID + "."
            + "widgetsetDirty";

    private static VaadinPlugin instance = null;

    public VaadinPlugin() {
        instance = this;
    }

    public static VaadinPlugin getInstance() {
        return instance;
    }

}
