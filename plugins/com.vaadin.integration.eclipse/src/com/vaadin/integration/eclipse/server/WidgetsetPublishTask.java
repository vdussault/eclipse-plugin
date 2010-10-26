package com.vaadin.integration.eclipse.server;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.model.PublishOperation;

import com.vaadin.integration.eclipse.builder.WidgetsetBuildManager;
import com.vaadin.integration.eclipse.util.WidgetsetUtil;

/**
 * Task to compile widgetset(s) in a project, to be executed when publishing to
 * a server.
 */
public class WidgetsetPublishTask extends PublishOperation {

    private final IProject project;

    public WidgetsetPublishTask(IProject project) {
        super("Compile widgetset", "Compile widgetset(s) for project "
                + project.getName());
        this.project = project;
    }

    @Override
    public void execute(IProgressMonitor monitor, IAdaptable info)
            throws CoreException {
        if (WidgetsetUtil.isWidgetsetDirty(project)) {
            WidgetsetBuildManager.runWidgetSetBuildTool(project, true, monitor);
        }
    }

    @Override
    public int getOrder() {
        // TODO not really documented what values to use
        return 1;
    }

    @Override
    public int getKind() {
        return PREFERRED;
    }

}
