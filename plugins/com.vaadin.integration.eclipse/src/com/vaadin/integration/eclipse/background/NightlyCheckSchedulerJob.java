package com.vaadin.integration.eclipse.background;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

/**
 * Background scheduler (not visible in the task list) that triggers nightly
 * build check job.
 */
public final class NightlyCheckSchedulerJob extends Job {
    private final Job nightlyCheckJob;

    public NightlyCheckSchedulerJob(String name, Job nightlyCheckJob) {
        super(name);
        this.nightlyCheckJob = nightlyCheckJob;

        nightlyCheckJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                // reschedule: check every 24h
                schedule(24 * 60 * 60 * 1000l);
            }
        });
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }

        if (null != nightlyCheckJob) {
            nightlyCheckJob.schedule();
        }
        return Status.OK_STATUS;
    }

    public void stop() {
        nightlyCheckJob.cancel();
        cancel();
    }

}