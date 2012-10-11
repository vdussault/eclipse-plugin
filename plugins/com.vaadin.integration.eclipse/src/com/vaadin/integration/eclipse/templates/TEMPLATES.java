package com.vaadin.integration.eclipse.templates;

import com.vaadin.integration.eclipse.templates.v7.ClientRpcTemplate;
import com.vaadin.integration.eclipse.templates.v7.ComponentTemplate;
import com.vaadin.integration.eclipse.templates.v7.ConnectorTemplate;
import com.vaadin.integration.eclipse.templates.v7.ServerRpcTemplate;
import com.vaadin.integration.eclipse.templates.v7.StateTemplate;
import com.vaadin.integration.eclipse.templates.v7.WidgetTemplate;

/**
 * Component template to use.
 * 
 * The titles are shown in combo boxes and are used to identify the templates
 * (together with vaadin62). The first suitable template is selected by default.
 */
public enum TEMPLATES {
    // templates for Vaadin 6.2 and rest of 6 series
    COMMUNICATION_V62(
            "Simple",
            "Simple client-side and server-side component with client-server communication",
            new Class[] {
                    com.vaadin.integration.eclipse.templates.v62.ComponentTemplate.class,
                    com.vaadin.integration.eclipse.templates.v62.VComponentTemplate.class },
            6.2, 7, true, false, false, false), //

    BASIC_V62(
            "Clean",
            "Simple client-side and server-side component",
            new Class[] {
                    com.vaadin.integration.eclipse.templates.v62.CleanComponentTemplate.class,
                    com.vaadin.integration.eclipse.templates.v62.CleanVComponentTemplate.class },
            6.2, 7, true, false, false, false), //
    /*- TODO same as composite
    SERVER_ONLY_V62(
            "Server-side only",
            "Server-side component only, no client-side widget",
            new Class[] { com.vaadin.integration.eclipse.templates.v62.ComponentTemplate.class },
            6.2, 7, false, false, false, false),
    -*/
    // templates for 7
    FULL_FLEDGED(
            "Full fledged",
            "Server-side & client-side with shared state and RPC, including custom widget",
            new Class[] { ComponentTemplate.class, ServerRpcTemplate.class,
                    ClientRpcTemplate.class, StateTemplate.class,
                    ConnectorTemplate.class, WidgetTemplate.class }, 7,
            Double.MAX_VALUE, true, true, true, true), //
    CONNECTOR_ONLY("Connector only",
            "Server-side component and client-side connector only",
            new Class[] { ComponentTemplate.class, ConnectorTemplate.class },
            7, Double.MAX_VALUE, false, false, false, false);

    // title shown in combo box
    private final String title;
    // somewhat longer description
    private final String description;
    // file name of the client-side template or null is no client-side class
    private final Class<Template>[] jetTemplates;
    // minversion inclusive, maxversion exclusive
    private final double minVersion;
    private final double maxVersion;

    // TODO add serverCuTemplate, serverMethodTemplates,
    // serverFieldTemplates, serverImports etc.

    private boolean hasClientRpc = false;
    private boolean hasServerRpc = false;
    private boolean hasState = false;
    private boolean hasWidget = false;

    private TEMPLATES(String title, String description, Class[] templates,
            double minVersion, double maxVersion, boolean hasWidget,
            boolean hasState, boolean hasClientRpc, boolean hasServerRpc) {
        this.title = title;
        this.description = description;
        jetTemplates = templates;
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;

        this.hasClientRpc = hasClientRpc;
        this.hasServerRpc = hasServerRpc;
        this.hasState = hasState;
        this.hasWidget = hasWidget;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Class<Template>[] getClientTemplates() {
        return jetTemplates;
    }

    public boolean hasClientTemplates() {
        return jetTemplates != null;
    }

    public boolean isSuitableFor(double version) {
        return version >= minVersion && version < maxVersion;
    }

    public boolean hasClientRpc() {
        return hasClientRpc;
    }

    public void setHasClientRpc(boolean hasClientRpc) {
        this.hasClientRpc = hasClientRpc;
    }

    public boolean hasServerRpc() {
        return hasServerRpc;
    }

    public void setHasServerRpc(boolean hasServerRpc) {
        this.hasServerRpc = hasServerRpc;
    }

    public boolean hasState() {
        return hasState;
    }

    public void setHasState(boolean hasState) {
        this.hasState = hasState;
    }

    public boolean hasWidget() {
        return hasWidget;
    }

    public void setHasWidget(boolean hasWidget) {
        this.hasWidget = hasWidget;
    }

    public double getMinVersion() {
        return minVersion;
    }

    public double getMaxVersion() {
        return maxVersion;
    }

}