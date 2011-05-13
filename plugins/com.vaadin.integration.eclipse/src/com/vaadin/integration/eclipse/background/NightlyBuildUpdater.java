package com.vaadin.integration.eclipse.background;


import org.eclipse.core.runtime.jobs.ISchedulingRule;


/**
 * Periodic job that checks for updates to nightly builds for projects that have
 * requested to be kept up to date and asks the user about upgrading the nightly
 * builds.
 */
public class NightlyBuildUpdater {

    /**
     * Scheduling rule for performing Vaadin nightly build upgrades.
     */
    public static final ISchedulingRule RULE_NIGHTLY_UPGRADE = new ISchedulingRule() {

        public boolean contains(ISchedulingRule rule) {
            return RULE_NIGHTLY_UPGRADE == rule;
        }

        public boolean isConflicting(ISchedulingRule rule) {
            return RULE_NIGHTLY_UPGRADE == rule;
        }
    };

    /**
     * Scheduling rule for checking for new Vaadin nightly builds.
     */
    public static final ISchedulingRule RULE_NIGHTLY_CHECK = new ISchedulingRule() {

        public boolean contains(ISchedulingRule rule) {
            // can contain perform upgrade and nightly build check
            return RULE_NIGHTLY_UPGRADE == rule || RULE_NIGHTLY_CHECK == rule;
        }

        public boolean isConflicting(ISchedulingRule rule) {
            // conflict with performing upgrade
            // conflict with nightly build check
            return RULE_NIGHTLY_UPGRADE == rule || RULE_NIGHTLY_CHECK == rule;
        }
    };

    /**
     * Scheduling rule for scheduling of checks for new Vaadin nightly builds.
     */
    public static final ISchedulingRule RULE_NIGHTLY_SCHEDULE = new ISchedulingRule() {

        public boolean contains(ISchedulingRule rule) {
            // can contain perform upgrade and nightly build check as well as
            // upgrade check scheduling
            return RULE_NIGHTLY_UPGRADE == rule || RULE_NIGHTLY_CHECK == rule
                    || RULE_NIGHTLY_SCHEDULE == rule;
        }

        public boolean isConflicting(ISchedulingRule rule) {
            // conflict with performing upgrade
            // conflict with nightly build check
            // conflict with another upgrade check scheduling job
            return RULE_NIGHTLY_UPGRADE == rule || RULE_NIGHTLY_CHECK == rule
                    || RULE_NIGHTLY_SCHEDULE == rule;
        }
    };

    // system job that is not visible on the job list
    private NightlyCheckSchedulerJob nightlyCheckSchedulerJob;

    public NightlyBuildUpdater() {
    }

    public void startUpdateJob() {
        // normal (default) job that is visible when active
        NightlyCheckJob nightlyCheckJob = new NightlyCheckJob(
                "Checking for new Vaadin nightly builds");
        nightlyCheckJob.setUser(false);
        // avoid concurrent checks and upgrades
        nightlyCheckJob.setRule(RULE_NIGHTLY_CHECK);

        nightlyCheckSchedulerJob = new NightlyCheckSchedulerJob(
                "Scheduler for checking for new Vaadin nightly builds",
                nightlyCheckJob);
        nightlyCheckSchedulerJob.setSystem(true);
        // avoid concurrent checks and upgrades
        nightlyCheckSchedulerJob.setRule(RULE_NIGHTLY_SCHEDULE);

        // short delay before starting to check for updates
        nightlyCheckSchedulerJob.schedule(30 * 1000l);
    }

    public void stopUpdateJob() {
        if (null != nightlyCheckSchedulerJob) {
            nightlyCheckSchedulerJob.stop();
            nightlyCheckSchedulerJob = null;
        }
    }
}
