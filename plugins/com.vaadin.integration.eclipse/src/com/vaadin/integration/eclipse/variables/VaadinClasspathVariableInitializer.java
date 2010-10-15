package com.vaadin.integration.eclipse.variables;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ClasspathVariableInitializer;
import org.eclipse.jdt.core.JavaCore;

import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;

/**
 * Define a classpath variable for the download directory to enable simple
 * handling of GWT JARs in shared projects.
 */
public class VaadinClasspathVariableInitializer extends
        ClasspathVariableInitializer {

    public static final String VAADIN_DOWNLOAD_VARIABLE = "VAADIN_DOWNLOAD";

    private IProgressMonitor fMonitor;

    // implicit 0-argument constructor as required

    @Override
    public void initialize(String variable) {
        IPath newPath = null;
        try {
            if (variable.equals(VAADIN_DOWNLOAD_VARIABLE)) {
                newPath = VaadinPluginUtil.getDownloadDirectory();
            }
            if (newPath != null) {
                JavaCore.setClasspathVariable(variable, newPath, getMonitor());
            }
        } catch (CoreException ex) {
            // this should not happen
            ErrorUtil.handleBackgroundException(IStatus.ERROR,
                    "Could not resolve classpath variable "
                            + VAADIN_DOWNLOAD_VARIABLE, ex);
        }
    }

    protected IProgressMonitor getMonitor() {
        if (fMonitor == null) {
            return new NullProgressMonitor();
        }
        return fMonitor;
    }

}
