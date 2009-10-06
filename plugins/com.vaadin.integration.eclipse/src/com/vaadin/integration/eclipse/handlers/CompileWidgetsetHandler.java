package com.vaadin.integration.eclipse.handlers;

import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.vaadin.integration.eclipse.util.VaadinPluginUtil;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class CompileWidgetsetHandler extends AbstractHandler {
    /**
     * The constructor.
     */
    public CompileWidgetsetHandler() {
    }

    /**
     * the command has been executed, so extract extract the needed information
     * from the application context.
     */
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection currentSelection = HandlerUtil.getCurrentSelection(event);

        final IProject project = VaadinPluginUtil.getProject(currentSelection);

        Job job = new Job("Compiling widgetset...") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask("Compiling wigetset", 100);
                IJavaProject jproject = JavaCore.create(project);
                try {
                    VaadinPluginUtil.compileWidgetset(jproject, monitor);
                } catch (CoreException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                monitor.worked(100);
                monitor.done();
                return Status.OK_STATUS;
            }
        };

        job.setUser(false);
        job.schedule();

        return null;
    }

}
