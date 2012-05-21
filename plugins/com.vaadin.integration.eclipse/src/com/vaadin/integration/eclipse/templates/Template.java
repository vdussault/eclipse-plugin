package com.vaadin.integration.eclipse.templates;

public interface Template {

    /**
     * Returns the target for the generated file, i.e the folder where it should
     * be placed. Returns null if it should not be created.
     * 
     * @return
     */
    public String getTarget();

    /**
     * Returns the name of the type created (i.e simpleName)
     * 
     * @return
     */
    public String getTypeName();

    /**
     * Returns the name of the file where the generated content should be put,
     * within the target folder.
     * 
     * @return
     */
    public String getFileName();

    /**
     * Generates content based on the given data.
     * 
     */
    public String generate(String componentName, String componentPackage,
            String componentExtends, String stateExtends,
            String widgetsetPackage, TEMPLATES t);
}
