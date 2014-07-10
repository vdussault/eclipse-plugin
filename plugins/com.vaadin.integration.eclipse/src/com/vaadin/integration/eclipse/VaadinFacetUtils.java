package com.vaadin.integration.eclipse;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import com.vaadin.integration.eclipse.util.ErrorUtil;

public class VaadinFacetUtils {
    public static final String VAADIN_FACET_ID = "com.vaadin.integration.eclipse.core";
    public static final String VAADIN6_PROJECT_DEFAULT_PRESET_ID = "com.vaadin.integration.eclipse.presetv6d";
    public static final String VAADIN7_PROJECT_DEFAULT_PRESET_ID = "com.vaadin.integration.eclipse.presetv7d";

    public static final IProjectFacet VAADIN_FACET = ProjectFacetsManager
            .getProjectFacet(VAADIN_FACET_ID);
    // Do not enable this! Strange things will happen if you define a facet
    // version that is not in plugin.xml and we don't want this to be selectable
    // anymore.
    public static final String VAADIN_01_STRING = "0.1";
    // public static final IProjectFacetVersion VAADIN_01 = VAADIN_FACET
    // .getVersion(VAADIN_01_STRING);
    public static final IProjectFacetVersion VAADIN_10 = VAADIN_FACET
            .getVersion("1.0");
    public static final IProjectFacetVersion VAADIN_70 = VAADIN_FACET
            .getVersion("7.0");
    public static final IProjectFacetVersion VAADIN_FACET_CURRENT = VAADIN_70;

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
            ErrorUtil.handleBackgroundException(e);
            return false;
        }
    }

    /**
     * Upgrade the Vaadin facet from version 0.1 to 1.0. If the project does not
     * have the Vaadin facet or has a newer facet version, nothing is modified.
     * 
     * @see CoreFacetVersionChangeDelegate
     * 
     * @param project
     *            the project to upgrade if necessary
     */
    public static void fixFacetVersion(IProject project) {
        if (!isVaadinProject(project)) {
            return;
        }

        try {
            IFacetedProject fproj = ProjectFacetsManager.create(project);
            if (fproj == null
                    || !fproj.hasProjectFacet(VAADIN_FACET)
                    || !VAADIN_01_STRING.equals(fproj.getInstalledVersion(
                            VAADIN_FACET).getVersionString())) {
                return;
            }

            // upgrade facet version
            IFacetedProjectWorkingCopy workingCopy = fproj.createWorkingCopy();
            workingCopy.changeProjectFacetVersion(VAADIN_10);
            workingCopy.commitChanges(null);
        } catch (CoreException e) {
            ErrorUtil.handleBackgroundException(e);
        }
    }

}
