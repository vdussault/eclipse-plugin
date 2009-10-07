package com.vaadin.integration.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.vaadin.integration.eclipse.VaadinFacetUtils;
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
    public Object execute(final ExecutionEvent event) throws ExecutionException {

        final ISelection currentSelection = HandlerUtil
                .getCurrentSelection(event);

        Job job = new Job("Compiling widgetset...") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    monitor.beginTask("Compiling wigetset", 100);
                    boolean compiled = false;
                    if (currentSelection instanceof IStructuredSelection
                            && ((IStructuredSelection) currentSelection).size() == 1) {
                        IStructuredSelection ssel = (IStructuredSelection) currentSelection;
                        Object obj = ssel.getFirstElement();
                        if (obj instanceof IFile) {
                            IFile file = (IFile) obj;
                            if (file.getName().endsWith(".gwt.xml")
                                    && file.getName().toLowerCase().contains(
                                            "widgetset")) {
                                VaadinPluginUtil
                                        .compileWidgetset(file, monitor);
                                compiled = true;
                            }
                        } else {
                            IProject project = VaadinPluginUtil
                                    .getProject(currentSelection);
                            if (VaadinFacetUtils.isVaadinProject(project)) {
                                IJavaProject jproject = JavaCore
                                        .create(project);
                                VaadinPluginUtil.compileWidgetset(jproject,
                                        null, monitor);
                                compiled = true;
                            }

                        }
                    }

                    if (!compiled) {

                        PlatformUI.getWorkbench().getDisplay().asyncExec(
                                new Runnable() {

                                    public void run() {
                                        Shell shell = PlatformUI.getWorkbench()
                                                .getActiveWorkbenchWindow()
                                                .getShell();
                                        MessageDialog
                                                .openError(shell,
                                                        "Select widgetset",
                                                        "Select a widgetset file (..widgetset.gwt.xml) or a Vaadin project to compile.");

                                    }
                                });

                    }
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                } finally {
                    monitor.worked(100);
                    monitor.done();
                }
                return Status.OK_STATUS;
            }

        };

        job.setUser(false);
        job.schedule();

        return null;
    }
}
