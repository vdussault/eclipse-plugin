package com.vaadin.integration.eclipse.builder;

import java.io.IOException;
import java.util.Map;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.util.ProjectUtil;

public class AddonStylesBuilder extends IncrementalProjectBuilder {

    public static final String BUILDER_ID = "com.vaadin.integration.eclipse.addonStylesBuilder";

    @Override
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
            throws CoreException {
        IProject project = getProject();
        if (AddonStylesImporter.supported(project)) {
            String directory = VaadinPlugin.VAADIN_RESOURCE_DIRECTORY;
            IFolder themes = ProjectUtil
                    .getWebContentFolder(getProject().getProject())
                    .getFolder(directory).getFolder("themes");
            
            for (IResource theme : themes.members()) {
                IFolder themeFolder = (IFolder) theme;
                try {
                    AddonStylesImporter.run(project, monitor, themeFolder);
                    themeFolder.refreshLocal(IResource.DEPTH_INFINITE,
                            new SubProgressMonitor(monitor, 1));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static void addBuilder(IProject project) throws CoreException {
        IProjectDescription desc = project.getDescription();
        ICommand[] commands = desc.getBuildSpec();

        for (int i = 0; i < commands.length; ++i) {
            if (commands[i].getBuilderName().equals(
                    AddonStylesBuilder.BUILDER_ID)) {
                return;
            }
        }

        ICommand[] newCommands = new ICommand[commands.length + 1];
        System.arraycopy(commands, 0, newCommands, 0, commands.length);
        ICommand command = desc.newCommand();
        command.setBuilderName(AddonStylesBuilder.BUILDER_ID);
        newCommands[newCommands.length - 1] = command;
        desc.setBuildSpec(newCommands);
        project.setDescription(desc, null);
    }
}
