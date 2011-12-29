package com.vaadin.integration.eclipse.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.vaadin.integration.eclipse.VaadinPlugin;

public class ErrorUtil {
    /**
     * Handle an exception in a background thread or other non-UI context. The
     * handling primarily consists of tracing the exception.
     * 
     * @param t
     */
    public static void handleBackgroundException(Throwable t) {
        handleBackgroundException(t.getMessage(), t);
    }

    /**
     * Handle an exception in a background thread or other non-UI context. The
     * handling primarily consists of tracing the exception.
     * 
     * @param message
     * @param t
     */
    public static void handleBackgroundException(String message, Throwable t) {
        handleBackgroundException(IStatus.ERROR, message, t);
    }

    /**
     * Handle an error in a background thread or other non-UI context. The
     * handling primarily consists of logging the error.
     * 
     * @param message
     */
    public static void handleBackgroundError(String message) {
        handleBackgroundException(message, null);
    }

    /**
     * Handle an exception in a background thread or other non-UI context. The
     * handling primarily consists of tracing the exception.
     * 
     * @param severity
     *            IStatus.OK, IStatus.INFO, IStatus.WARNING or IStatus.ERROR
     * @param message
     * @param t
     */
    public static void handleBackgroundException(int severity, String message,
            Throwable t) {
        // TODO trace the exception and do any other background exception
        // handling
        IStatus status;
        if (t == null) {
            status = new Status(severity, VaadinPlugin.PLUGIN_ID, message);
        } else {
            status = new Status(severity, VaadinPlugin.PLUGIN_ID, message, t);
        }
        VaadinPlugin.getInstance().getLog().log(status);
        // ex.printStackTrace();
    }

    /**
     * Logs an information level event to the Eclipse error log (or other
     * configured destination).
     * 
     * @param message
     */
    public static void logInfo(String message) {
        IStatus status = new Status(IStatus.INFO, VaadinPlugin.PLUGIN_ID,
                message);
        VaadinPlugin.getInstance().getLog().log(status);
        // ex.printStackTrace();
    }

    /**
     * Logs a warning not related to an exception to the Eclipse error log (or
     * other configured destination).
     * 
     * @param message
     */
    public static void logWarning(String message) {
        IStatus status = new Status(IStatus.WARNING, VaadinPlugin.PLUGIN_ID,
                message);
        VaadinPlugin.getInstance().getLog().log(status);
        // ex.printStackTrace();
    }

    /**
     * Display an error message to the user.
     * 
     * @param message
     * @param ex
     */
    public static void displayError(String message, Throwable ex, Shell shell) {
        // TODO trace if needed and report to the user
        MessageDialog.openError(shell, "Error", message);
    }

    public static void displayErrorFromBackgroundThread(final String title,
            final String message) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            public void run() {
                Shell shell = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell();
                MessageDialog.openError(shell, title, message);
            }
        });
    }

    /**
     * Display a warning message to the user.
     * 
     * @param message
     */
    public static void displayWarning(String message, Shell shell) {
        MessageDialog.openWarning(shell, "Warning", message);
    }

    public static void displayWarningFromBackgroundThread(final String title,
            final String message) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            public void run() {
                Shell shell = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell();
                MessageDialog.openWarning(shell, title, message);
            }
        });
    }

    public static CoreException newCoreException(String message, Throwable e) {
        return new CoreException(new Status(Status.ERROR,
                VaadinPlugin.PLUGIN_ID, message, e));
    }

    public static CoreException newCoreException(String message) {
        return new CoreException(new Status(Status.ERROR,
                VaadinPlugin.PLUGIN_ID, message));
    }

}
