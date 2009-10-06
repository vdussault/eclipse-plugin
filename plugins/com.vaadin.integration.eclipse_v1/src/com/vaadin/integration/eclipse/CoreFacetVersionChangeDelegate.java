package com.vaadin.integration.eclipse;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

public class CoreFacetVersionChangeDelegate implements IDelegate {

    public void execute(IProject project, IProjectFacetVersion fv, Object cfg,
            IProgressMonitor monitor) throws CoreException {
        if (monitor != null) {
            monitor.beginTask("", 1);
        }
        try {
            // change facet version - from 0.1 to 1.0, nothing needed
            if (monitor != null) {
                monitor.worked(1);
            }
        } finally {
            if (monitor != null) {
                monitor.done();
            }
        }
    }

}
