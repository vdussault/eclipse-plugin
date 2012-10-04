package com.vaadin.integration.eclipse;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import com.vaadin.integration.eclipse.util.ErrorUtil;

public class CoreFacetVersionChangeDelegate implements IDelegate {

    public void execute(IProject project, IProjectFacetVersion fv, Object cfg,
            IProgressMonitor monitor) throws CoreException {
        if (monitor != null) {
            monitor.beginTask("", 10);
        }
        try {
            // change facet version - from 0.1 to 1.0, nothing needed
            upgradeFacet(project, fv, cfg, monitor);
            if (monitor != null) {
                monitor.worked(10);
            }
            // } catch (CoreException e) {
            // ErrorUtil.handleBackgroundException(e);
            // ErrorUtil.displayErrorFromBackgroundThread(
            // "Facet version change failed",
            // "Failed to change Vaadin facet version for the project");
        } finally {
            if (monitor != null) {
                monitor.done();
            }
        }
    }

    private static void downgradeToVaadin6(IProject project,
            IProjectFacetVersion fv, Object cfg, IProgressMonitor monitor)
            throws CoreException {
        // TODO implement
        throw ErrorUtil
                .newCoreException("Downgrading the Vaadin facet is not supported");
    }

    private static void upgradeToVaadin7(IProject project,
            IProjectFacetVersion fv, Object cfg, IProgressMonitor monitor)
            throws CoreException {
        // TODO 1.0 to 7.0 => upgrade: add ivy configuration, remove old JARs
        // from classpath
        throw ErrorUtil
                .newCoreException("Upgrading the Vaadin facet is not yet supported");
    }

    /**
     * Upgrade/change the Vaadin facet to the given version. If the project does
     * not have the Vaadin facet, nothing is modified.
     * 
     * @param project
     *            the project to upgrade
     * @param version
     *            the new (exact) Vaadin facet version to use
     */
    public static void upgradeFacet(IProject project,
            IProjectFacetVersion version, Object cfg, IProgressMonitor monitor)
            throws CoreException {
        if (!VaadinFacetUtils.isVaadinProject(project)) {
            return;
        }

        IFacetedProject fproj = ProjectFacetsManager.create(project);
        if (fproj == null
                || !fproj.hasProjectFacet(VaadinFacetUtils.VAADIN_FACET)
                || fproj.hasProjectFacet(version)) {
            return;
        }
        // avoid duplicating code - only does something for ancient projects
        VaadinFacetUtils.fixFacetVersion(project);

        IProjectFacetVersion currentVersion = fproj
                .getInstalledVersion(VaadinFacetUtils.VAADIN_FACET);
        if (VaadinFacetUtils.VAADIN_70.equals(version)
                && VaadinFacetUtils.VAADIN_10.equals(currentVersion)) {
            upgradeToVaadin7(project, version, cfg, monitor);
        } else if (VaadinFacetUtils.VAADIN_10.equals(version)
                && VaadinFacetUtils.VAADIN_70.equals(currentVersion)) {
            downgradeToVaadin6(project, version, cfg, monitor);
        }

        // upgrade facet version
        IFacetedProjectWorkingCopy workingCopy = fproj.createWorkingCopy();
        workingCopy.changeProjectFacetVersion(version);
        workingCopy.commitChanges(null);
    }

}
