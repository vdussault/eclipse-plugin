package com.vaadin.integration.eclipse.wizards;

public class Vaadin6PresetFactory extends AbstractVaadinPresetFactory {
    private static final String LABEL = "Vaadin 6";
    private static final String DESCRIPTION = "Vaadin 6 project running on Java %s, servlet %s";
    private static final String DEFAULTJAVAVERSION = null;
    private static final String DEFAULTSERVLETVERSION = "2.4";
    private static final String VAADINCOREVERSION = "1.0";

    @Override
    protected String getLabelString() {
        return LABEL;
    }

    @Override
    protected String getDescriptionString() {
        return DESCRIPTION;
    }

    @Override
    protected String getPreferredJavaVersionString() {
        return DEFAULTJAVAVERSION;
    }

    @Override
    protected String getPreferredServletVersionString() {
        return DEFAULTSERVLETVERSION;
    }

    @Override
    protected String getVaadinCoreVersion() {
        return VAADINCOREVERSION;
    }
}
