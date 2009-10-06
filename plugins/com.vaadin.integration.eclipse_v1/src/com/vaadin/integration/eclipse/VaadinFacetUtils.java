package com.vaadin.integration.eclipse;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

public class VaadinFacetUtils {
    public static final String VAADIN_FACET_ID = "com.vaadin.integration.eclipse.core";
    public static final String VAADIN_PROJECT_PRESET_ID = "com.vaadin.integration.eclipse.preset";

    public static final IProjectFacet VAADIN_FACET = ProjectFacetsManager
            .getProjectFacet(VAADIN_FACET_ID);
    // public static final IProjectFacetVersion VAADIN_01 = VAADIN_FACET
    // .getVersion("0.1");
    public static final IProjectFacetVersion VAADIN_10 = VAADIN_FACET
            .getVersion("1.0");

    /**
     * Check whether a project has the Vaadin project nature.
     *
     * @param project
     * @return true if the project is an Vaadin project
     */
    public static boolean isVaadinProject(IProject project) {
        if (project == null) {
            return false;
        }

        try {
            IFacetedProject fproj = ProjectFacetsManager.create(project);
            return fproj != null && fproj.hasProjectFacet(VAADIN_FACET);
        } catch (CoreException e) {
            e.printStackTrace();
            return false;
        }
    }

}
