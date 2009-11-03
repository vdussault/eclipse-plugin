package com.vaadin.integration.eclipse.handlers;

import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.builder.WidgetsetBuildManager;
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
        final IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
        final Shell shell = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getShell();

        Job job = new Job("Compiling widgetset...") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    monitor.beginTask("Compiling wigetset", 1);

                    boolean compiled = false;
                    if (currentSelection instanceof IStructuredSelection
                            && ((IStructuredSelection) currentSelection).size() == 1) {
                        IStructuredSelection ssel = (IStructuredSelection) currentSelection;
                        Object obj = ssel.getFirstElement();
                        if (obj instanceof IFile) {
                            IFile file = (IFile) obj;
                            IProject project = file.getProject();
                            VaadinFacetUtils.upgradeFacet(project,
                                    VaadinFacetUtils.VAADIN_FACET_CURRENT);
                            VaadinPluginUtil.ensureWidgetsetNature(project);
                            compiled = compileFile(monitor, file);
                        }
                        if (!compiled) {
                            IProject project = VaadinPluginUtil
                                    .getProject(currentSelection);
                            if (project == null) {
                                IFile file = getFileForEditor(activeEditor);
                                if (file != null && file.exists()) {
                                    VaadinFacetUtils.upgradeFacet(file
                                            .getProject(),
                                                    VaadinFacetUtils.VAADIN_FACET_CURRENT);
                                    VaadinPluginUtil.ensureWidgetsetNature(file
                                            .getProject());
                                }
                                compiled = compileFile(monitor, file);
                            } else if (VaadinFacetUtils
                                    .isVaadinProject(project)) {
                                VaadinFacetUtils.upgradeFacet(project,
                                        VaadinFacetUtils.VAADIN_FACET_CURRENT);
                                VaadinPluginUtil.ensureWidgetsetNature(project);
                                IJavaProject jproject = JavaCore
                                        .create(project);
                                WidgetsetBuildManager.compileWidgetsets(shell,
                                        jproject, monitor);
                                compiled = true;
                            }
                        }
                    } else {
                        IFile file = getFileForEditor(activeEditor);
                        compiled = compileFile(monitor, file);
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
                    VaadinPluginUtil.handleBackgroundException(e);
                } finally {
                    monitor.done();
                }
                return Status.OK_STATUS;
            }

            private IFile getFileForEditor(IEditorPart editor) {
                IFile file = null;
                if (editor != null
                        && editor.getEditorInput() instanceof IFileEditorInput) {
                    IFileEditorInput input = (IFileEditorInput) activeEditor
                            .getEditorInput();
                    file = input.getFile();
                }
                return file;
            }

            // try to compile a file as a GWT widgetset, or if not one, try to
            // compile widgetsets in the containing project
            private boolean compileFile(IProgressMonitor monitor, IFile file)
                    throws CoreException, IOException, InterruptedException {
                // only one branch is executed so progress is tracked correctly
                boolean compiled = false;
                if (file != null && file.getName().endsWith(".gwt.xml")
                        && file.getName().toLowerCase().contains("widgetset")) {
                    WidgetsetBuildManager.compileWidgetset(file, monitor);
                    compiled = true;
                }
                if (!compiled) {
                    IProject project = VaadinPluginUtil.getProject(file);
                    if (VaadinFacetUtils.isVaadinProject(project)) {
                        IJavaProject jproject = JavaCore.create(project);
                        WidgetsetBuildManager.compileWidgetsets(shell,
                                jproject,
                                monitor);
                        compiled = true;
                    }
                }

                return compiled;
            }

        };

        job.setUser(false);
        job.schedule();

        return null;
    }
}
