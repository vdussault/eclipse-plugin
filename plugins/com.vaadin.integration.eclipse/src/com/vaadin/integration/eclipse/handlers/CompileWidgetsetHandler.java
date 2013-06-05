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
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;

import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.builder.WidgetsetBuildManager;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.WidgetsetUtil;

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

        startCompileWidgetsetJob(currentSelection, activeEditor);

        return null;
    }

    public static void startCompileWidgetsetJob(
            final ISelection currentSelection, final IEditorPart activeEditor) {
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
                            VaadinFacetUtils.fixFacetVersion(project);
                            if (WidgetsetUtil
                                    .isWidgetsetManagedByPlugin(project)) {
                                WidgetsetUtil.ensureWidgetsetNature(project);
                                compiled = compileFile(monitor, file);
                            }
                        }
                        if (!compiled) {
                            IProject project = ProjectUtil
                                    .getProject(currentSelection);
                            if (project == null) {
                                IFile file = getFileForEditor(activeEditor);
                                if (file != null && file.exists()) {
                                    VaadinFacetUtils.fixFacetVersion(file
                                            .getProject());
                                    if (WidgetsetUtil
                                            .isWidgetsetManagedByPlugin(file
                                                    .getProject())) {
                                        WidgetsetUtil
                                                .ensureWidgetsetNature(file
                                                        .getProject());
                                    }
                                }
                                if (WidgetsetUtil
                                        .isWidgetsetManagedByPlugin(file
                                                .getProject())) {
                                    compiled = compileFile(monitor, file);
                                }
                            } else if (VaadinFacetUtils
                                    .isVaadinProject(project)) {
                                VaadinFacetUtils.fixFacetVersion(project);
                                if (WidgetsetUtil
                                        .isWidgetsetManagedByPlugin(project)) {
                                    WidgetsetUtil
                                            .ensureWidgetsetNature(project);
                                    IJavaProject jproject = JavaCore
                                            .create(project);
                                    WidgetsetBuildManager.compileWidgetsets(
                                            jproject, monitor);
                                    compiled = true;
                                }
                            }
                        }
                    } else {
                        IFile file = getFileForEditor(activeEditor);
                        if (file != null
                                && WidgetsetUtil
                                        .isWidgetsetManagedByPlugin(file
                                                .getProject())) {
                            compiled = compileFile(monitor, file);
                        }
                    }

                    if (!compiled) {
                        ErrorUtil
                                .displayErrorFromBackgroundThread(
                                        "Select widgetset",
                                        "Select a widgetset file (..widgetset.gwt.xml) or a Vaadin project to compile.");
                    }
                } catch (OperationCanceledException e) {
                    // Do nothing if user cancels compilation
                } catch (Exception e) {
                    showException(e);
                    // Also log the exception
                    ErrorUtil.handleBackgroundException(IStatus.ERROR,
                            "Widgetset compilation failed", e);
                } finally {
                    monitor.done();
                }
                return Status.OK_STATUS;
            }

        };

        job.setUser(false);
        job.schedule();
    }

    protected static IFile getFileForEditor(IEditorPart editor) {
        IFile file = null;
        if (editor != null
                && editor.getEditorInput() instanceof IFileEditorInput) {
            IFileEditorInput input = (IFileEditorInput) editor.getEditorInput();
            file = input.getFile();
        }
        return file;
    }

    // try to compile a file as a GWT widgetset, or if not one, try to
    // compile widgetsets in the containing project
    protected static boolean compileFile(IProgressMonitor monitor, IFile file)
            throws CoreException, IOException, InterruptedException {
        if (!WidgetsetUtil.isWidgetsetManagedByPlugin(file.getProject())) {
            return false;
        }
        // only one branch is executed so progress is tracked correctly
        boolean compiled = false;
        if (file != null && file.getName().endsWith(".gwt.xml")
                && file.getName().toLowerCase().contains("widgetset")) {
            WidgetsetBuildManager.compileWidgetset(file, monitor);
            compiled = true;
        }
        if (!compiled) {
            IProject project = ProjectUtil.getProject(file);
            if (VaadinFacetUtils.isVaadinProject(project)) {
                IJavaProject jproject = JavaCore.create(project);
                WidgetsetBuildManager.compileWidgetsets(jproject, monitor);
                compiled = true;
            }
        }

        return compiled;
    }

    /**
     * Find a project based on current selection and active editor.
     * 
     * @param currentSelection
     * @param activeEditor
     * @return project or null if no suitable project found based on selection
     *         and active editor
     */
    public static IProject getProject(ISelection currentSelection,
            IEditorPart activeEditor) {
        if (currentSelection instanceof IStructuredSelection
                && ((IStructuredSelection) currentSelection).size() == 1) {
            IStructuredSelection ssel = (IStructuredSelection) currentSelection;
            Object obj = ssel.getFirstElement();
            if (obj instanceof IFile) {
                IFile file = (IFile) obj;
                return file.getProject();
            }
            IProject project = ProjectUtil.getProject(currentSelection);
            if (project == null) {
                IFile file = getFileForEditor(activeEditor);
                if (file != null && file.exists()) {
                    return file.getProject();
                }
            } else {
                return project;
            }
        } else {
            IFile file = getFileForEditor(activeEditor);
            if (file != null) {
                return file.getProject();
            }
        }
        return null;
    }

    protected static void showException(final Exception e) {
        ErrorUtil.displayErrorFromBackgroundThread("Error compiling widgetset",
                "Error compiling widgetset:\n" + e.getClass().getName() + " - "
                        + e.getMessage() + "\n\nSee error log for details.");
    }
}
