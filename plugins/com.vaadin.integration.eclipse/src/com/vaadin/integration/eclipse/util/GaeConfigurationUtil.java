package com.vaadin.integration.eclipse.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

public class GaeConfigurationUtil {

    /**
     * Create the Google App Engine configuration file appengine-web.xml from a
     * template if it does not exist. Session support is activated in the
     * template.
     * 
     * Also the logging properties file is created.
     * 
     * @param project
     * @throws CoreException
     */
    public static IFile createAppEngineWebXml(IProject project)
            throws CoreException {
        VaadinPluginUtil.ensureFileFromTemplate(
                ProjectUtil.getWebInfFolder(project).getFile(
                        "logging.properties"), "logging.properties.txt");
        return VaadinPluginUtil.ensureFileFromTemplate(ProjectUtil
                .getWebInfFolder(project).getFile("appengine-web.xml"),
                "appengine-web.txt");
    }

}
