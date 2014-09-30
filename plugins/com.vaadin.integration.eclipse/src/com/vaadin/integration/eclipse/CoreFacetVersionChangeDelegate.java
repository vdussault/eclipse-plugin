package com.vaadin.integration.eclipse;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import com.vaadin.integration.eclipse.builder.WidgetsetBuildManager;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.ProjectDependencyManager;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.data.MavenVaadinVersion;
import com.vaadin.integration.eclipse.util.network.MavenVersionManager;

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
        // 1.0 to 7.0 upgrade:
        // - remove old JARs from classpath (Vaadin and GWT)
        // - remove Vaadin JAR
        // - add ivy configuration

        try {
            monitor.beginTask("Upgrading Vaadin facet", 4);

            MavenVaadinVersion vaadinVersion = null;
            if (cfg instanceof IDataModel) {
                IDataModel model = (IDataModel) cfg;
                if (model
                        .isPropertySet(CoreFacetInstallDelegate.VAADIN_VERSION)) {
                    // A version was specified on the configuration page - use
                    // that
                    String versionString = model
                            .getStringProperty(CoreFacetInstallDelegate.VAADIN_VERSION);
                    vaadinVersion = new MavenVaadinVersion(versionString);
                }
            }
            if (null == vaadinVersion) {
                List<MavenVaadinVersion> availableVersions = MavenVersionManager
                        .getAvailableVersions(false);
                if (null == availableVersions || availableVersions.size() == 0) {
                    // need a Vaadin version to upgrade to
                    throw ErrorUtil
                            .newCoreException("Upgrading the Vaadin facet failed: no Vaadin version found for Ivy");
                }
                vaadinVersion = availableVersions.get(0);
            }

            IJavaProject jproject = JavaCore.create(project);
            WidgetsetBuildManager.internalSuspendWidgetsetBuilds(project);
            try {
                // remove GWT JARs from the classpath
                ProjectDependencyManager.removeGWTFromClasspath(jproject,
                        new SubProgressMonitor(monitor, 1));

                // remove old Vaadin JAR
                IPath currentJar = ProjectUtil.getVaadinLibraryInProject(
                        project, true);
                // remove the Vaadin JAR and its classpath entry (if any)
                if (currentJar != null) {
                    ProjectDependencyManager.removeVaadinLibrary(jproject,
                            currentJar);
                }
                monitor.worked(1);

                // add Ivy dependency management
                // do not add servlet 3 API dependency automatically on facet
                // upgrade as not creating a UI class and its servlet
                boolean servlet30 = false;
                CoreFacetInstallDelegate.setupIvy(jproject, vaadinVersion,
                        servlet30, false, new SubProgressMonitor(monitor, 2));
            } catch (CoreException e) {
                throw ErrorUtil.newCoreException(
                        "Failed to update Vaadin facet version for project "
                                + project.getName(), e);
            } finally {
                WidgetsetBuildManager.internalResumeWidgetsetBuilds(project);
                // widgetset will not compile here so no point in trying
            }

            // TODO update launches etc?
        } finally {
            monitor.done();
        }
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
    }

}
