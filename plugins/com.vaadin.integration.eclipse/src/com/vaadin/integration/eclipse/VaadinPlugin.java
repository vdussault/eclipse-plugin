package com.vaadin.integration.eclipse;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.vaadin.integration.eclipse.background.NightlyBuildUpdater;

public class VaadinPlugin extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "com.vaadin.integration.eclipse";

    public static final String VAADIN_PACKAGE_PREFIX = "com.vaadin.";
    public static final String VAADIN_UI_PACKAGE_PREFIX = VAADIN_PACKAGE_PREFIX
            + "ui.";
    public static final String APPLICATION_CLASS_NAME = "Application";
    public static final String APPLICATION_CLASS_FULL_NAME = VAADIN_PACKAGE_PREFIX
            + APPLICATION_CLASS_NAME;
    public static final String UI_CLASS_NAME = "UI";
    public static final String UI_CLASS_FULL_NAME = VAADIN_UI_PACKAGE_PREFIX
            + UI_CLASS_NAME;

    public static final String ID_COMPILE_WS_APP = "compilews";

    public static final String VAADIN_CLIENT_SIDE_CLASS_PREFIX = "V";

    public static final String VAADIN_RESOURCE_DIRECTORY = "VAADIN";

    public static final String VAADIN_DEFAULT_THEME = "reindeer";

    public static final String GWT_COMPILER_CLASS = "com.vaadin.tools.WidgetsetCompiler";

    public static final String GWT_CODE_SERVER_CLASS = "com.google.gwt.dev.codeserver.CodeServer";

    private static VaadinPlugin instance = null;

    private NightlyBuildUpdater nightlyBuildUpdater;

    public VaadinPlugin() {
        instance = this;
    }

    public static VaadinPlugin getInstance() {
        return instance;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        nightlyBuildUpdater = new NightlyBuildUpdater();
        nightlyBuildUpdater.startUpdateJob();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        nightlyBuildUpdater.stopUpdateJob();
        nightlyBuildUpdater = null;
        super.stop(context);
    }

}
