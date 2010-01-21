package com.vaadin.integration.eclipse.wizards;

import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jdt.ui.jarpackager.IJarBuilder;
import org.eclipse.jdt.ui.jarpackager.IJarBuilderExtension;
import org.eclipse.jdt.ui.jarpackager.IManifestProvider;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;
import org.eclipse.swt.widgets.Shell;

/**
 * Wrapper for IJarBuilder that removes some segments from destination paths
 * selectively.
 * 
 * TODO this could also implement {@link IJarBuilderExtension} to support
 * external class folders
 */
public class DirectoryJarBuilder implements IJarBuilder {

    private final IJarBuilder parent;
    private final IPath stripPath;

    public DirectoryJarBuilder(IJarBuilder builder, IPath stripPath) {
        parent = builder;
        this.stripPath = stripPath;
    }

    public void close() throws CoreException {
        parent.close();
    }

    public String getId() {
        return parent.getId() + "_directory";
    }

    public IManifestProvider getManifestProvider() {
        return parent.getManifestProvider();
    }

    public void open(JarPackageData jarPackage, Shell shell, MultiStatus status)
            throws CoreException {
        parent.open(jarPackage, shell, status);
    }

    public void writeArchive(ZipFile archive, IProgressMonitor monitor) {
        parent.writeArchive(archive, monitor);
    }

    public void writeFile(IFile resource, IPath destinationPath)
            throws CoreException {
        // remove extra directory levels from destination path
        if (stripPath.isPrefixOf(destinationPath)) {
            destinationPath = destinationPath.removeFirstSegments(stripPath
                    .segmentCount());
        }
        parent.writeFile(resource, destinationPath);
    }
}
