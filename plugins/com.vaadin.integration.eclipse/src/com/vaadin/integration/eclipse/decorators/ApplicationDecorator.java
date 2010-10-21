package com.vaadin.integration.eclipse.decorators;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;

public class ApplicationDecorator implements ILightweightLabelDecorator {

    private String iconPath = "icons/application-overlay.png"; // NON-NLS-1

    private static ImageDescriptor applicationImageDescriptor = null;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang
     * .Object, org.eclipse.jface.viewers.IDecoration)
     */
    public void decorate(Object element, IDecoration decoration) {
        boolean application = false;

        // if (element instanceof ICompilationUnit) {
        // ICompilationUnit cu = (ICompilationUnit) element;
        // IType[] types;
        // try {
        // types = cu.getAllTypes();
        // if (types == null) {
        // return;
        // }
        // for (IType type : types) {
        // if (VaadinPluginUtil.typeExtendsClass(type,
        // VaadinPlugin.APPLICATION_CLASS_NAME)) {
        // application = true;
        // break;
        // }
        // }
        // } catch (JavaModelException e) {
        // ErrorUtil.handleBackgroundException(e);
        // }
        // } else
        if (element instanceof IType) {
            IType type = (IType) element;
            try {
                application = VaadinPluginUtil.typeExtendsClass(type,
                        VaadinPlugin.APPLICATION_CLASS_NAME);
            } catch (JavaModelException e) {
                // Can't throw an exception here I think..
                ErrorUtil.handleBackgroundException(e);
            }
        }

        if (application) {
            decoration.addOverlay(getApplicationImageDescriptor(),
                    IDecoration.BOTTOM_LEFT);
        }
    }

    private ImageDescriptor getApplicationImageDescriptor() {

        if (applicationImageDescriptor == null) {
            URL url = FileLocator.find(Platform
                    .getBundle(VaadinPlugin.PLUGIN_ID), new Path(iconPath),
                    null);

            if (url == null) {
                return null;
            }

            applicationImageDescriptor = ImageDescriptor.createFromURL(url);
        }

        return applicationImageDescriptor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.
     * jface.viewers.ILabelProviderListener)
     */
    public void addListener(ILabelProviderListener listener) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
     */
    public void dispose() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang
     * .Object, java.lang.String)
     */
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse
     * .jface.viewers.ILabelProviderListener)
     */
    public void removeListener(ILabelProviderListener listener) {
    }
}