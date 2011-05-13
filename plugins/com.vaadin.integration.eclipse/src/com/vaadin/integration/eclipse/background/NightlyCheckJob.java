package com.vaadin.integration.eclipse.background;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.ui.PlatformUI;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.PreferenceUtil;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.data.DownloadableVaadinVersion;
import com.vaadin.integration.eclipse.util.network.DownloadManager;

/**
 * User-visible job that checks for new nightly builds and then re-schedules a
 * new check.
 */
public final class NightlyCheckJob extends Job {
    public NightlyCheckJob(String name) {
        super(name);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Checking for new Vaadin nightly builds", 4);
        try {

            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            // map from project with "use latest nightly" to the current
            // Vaadin version number string in the project
            Map<IProject, String> nightlyProjects = getProjectsUsingLatestNightly();

            monitor.worked(1);

            if (nightlyProjects.isEmpty()) {
                return Status.OK_STATUS;
            } else if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            // update version list
            List<DownloadableVaadinVersion> availableNightlies = DownloadManager
                    .getAvailableNightlyVersions();

            monitor.worked(1);

            final Map<IProject, DownloadableVaadinVersion> possibleUpgrades = new HashMap<IProject, DownloadableVaadinVersion>();

            for (IProject project : nightlyProjects.keySet()) {
                String currentVersion = nightlyProjects.get(project);
                DownloadableVaadinVersion latestNightly = getNightlyToUpgradeTo(
                        currentVersion, availableNightlies);

                if (null != latestNightly
                        && !latestNightly.getVersionNumber().equals(
                                currentVersion)) {
                    possibleUpgrades.put(project, latestNightly);
                }
            }

            monitor.worked(1);

            if (possibleUpgrades.isEmpty()) {
                return Status.OK_STATUS;
            } else if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            // create new task to upgrade Vaadin nightly builds in projects
            NightlyUpgradeJob upgradeJob = new NightlyUpgradeJob(
                    "Upgrade Vaadin nightly builds", possibleUpgrades);
            upgradeJob.setUser(false);
            // avoid concurrent checks and upgrades, "lock" the workspace
            upgradeJob.setRule(MultiRule.combine(
                    NightlyBuildUpdater.RULE_NIGHTLY_UPGRADE, ResourcesPlugin
                            .getWorkspace().getRoot()));
            upgradeJob.schedule();

            monitor.worked(1);

            // "tray notification": the following projects were upgraded to
            // the latest Vaadin nightly builds
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
                public void run() {
                    final UpgradeNotificationPopup popup = new UpgradeNotificationPopup(
                            PlatformUI.getWorkbench().getDisplay(),
                            possibleUpgrades);
                    popup.open();
                }
            });

            return Status.OK_STATUS;
        } catch (CoreException e) {
            ErrorUtil.handleBackgroundException(
                    "Failed to update Vaadin nightly build list", e);
            return new Status(IStatus.WARNING, VaadinPlugin.PLUGIN_ID, 1,
                    "Failed to update Vaadin nightly build list", e);
        } finally {
            monitor.done();
        }
    }

    /**
     * Returns the open projects in the workspace for which the
     * "Use latest nightly" option is selected.
     * 
     * @return
     */
    private Map<IProject, String> getProjectsUsingLatestNightly() {
        Map<IProject, String> projectsWithNightly = new HashMap<IProject, String>();
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IProject[] projects = workspaceRoot.getProjects();
        for (IProject project : projects) {
            try {
                // add if "use latest nightly" is set
                PreferenceUtil preferences = PreferenceUtil.get(project);
                if (preferences.isUsingLatestNightly()) {
                    String versionNumber = ProjectUtil.getVaadinLibraryVersion(
                            project, true);
                    if (null != versionNumber) {
                        projectsWithNightly.put(project, versionNumber);
                    }
                }
            } catch (CoreException e) {
                ErrorUtil.handleBackgroundException(
                        IStatus.WARNING,
                        "Could not check Vaadin version in project "
                                + project.getName(), e);
            }
        }
        return projectsWithNightly;
    }

    /**
     * Returns the latest nightly version for the same branch as the current
     * version.
     * 
     * @param currentVersion
     * @param availableNightlies
     * @return latest nightly for the branch or null if not found
     */
    public static DownloadableVaadinVersion getNightlyToUpgradeTo(
            String currentVersion,
            List<DownloadableVaadinVersion> availableNightlies) {
        String branch = parseBranch(currentVersion);
        if (null == branch) {
            return null;
        }
        for (DownloadableVaadinVersion version : availableNightlies) {
            // sorted with latest first, return first match
            if (branch.equals(parseBranch(version.getVersionNumber()))) {
                return version;
            }
        }
        return null;
    }

    private static String parseBranch(String versionNumber) {
        return versionNumber.substring(0,
                versionNumber.indexOf(".", versionNumber.indexOf(".") + 1));
    }

}