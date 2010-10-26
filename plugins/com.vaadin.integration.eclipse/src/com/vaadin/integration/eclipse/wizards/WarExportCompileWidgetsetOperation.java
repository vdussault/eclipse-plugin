package com.vaadin.integration.eclipse.wizards;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jst.j2ee.datamodel.properties.IJ2EEComponentExportDataModelProperties;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;

import com.vaadin.integration.eclipse.builder.WidgetsetBuildManager;
import com.vaadin.integration.eclipse.util.WidgetsetUtil;

public class WarExportCompileWidgetsetOperation extends
        AbstractDataModelOperation {

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info)
            throws ExecutionException {
        IStatus stat = OK_STATUS;

        // ask about compiling widgetset if widgetset builds are not suspended
        Object component = getDataModel().getProperty(
                IJ2EEComponentExportDataModelProperties.COMPONENT);
        if (component instanceof IVirtualComponent) {
            IProject project = ((IVirtualComponent) component).getProject();
            if (WidgetsetUtil.isWidgetsetDirty(project)) {
                WidgetsetBuildManager.runWidgetSetBuildTool(project, true,
                        monitor);
            }
        }

        return stat;

    }

}
