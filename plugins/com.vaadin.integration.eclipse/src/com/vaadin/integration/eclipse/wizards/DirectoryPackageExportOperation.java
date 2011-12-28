package com.vaadin.integration.eclipse.wizards;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jdt.internal.ui.jarpackager.JarFileExportOperation;
import org.eclipse.jdt.internal.ui.jarpackager.JarPackagerMessages;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;
import org.eclipse.swt.widgets.Shell;

import com.vaadin.integration.eclipse.VaadinPlugin;

/**
 * Export operation for a Vaadin directory package JAR.
 * 
 * @see JarFileExportOperation
 */
public class DirectoryPackageExportOperation extends JarFileExportOperation {

    /**
     * MultiStatus that allows changing the message, copied from
     * {@link JarFileExportOperation}.MessageMultiStatus .
     */
    private static class ModifiableMessageMultiStatus extends MultiStatus {
        ModifiableMessageMultiStatus(String pluginId, int code, String message,
                Throwable exception) {
            super(pluginId, code, message, exception);
        }

        /*
         * allows to change the message
         */
        @Override
        protected void setMessage(String message) {
            super.setMessage(message);
        }
    }

    public DirectoryPackageExportOperation(JarPackageData jarPackage,
            Shell parent) {
        super(jarPackage, parent);
    }

    public DirectoryPackageExportOperation(JarPackageData[] jarPackages,
            Shell parent) {
        super(jarPackages, parent);
    }

    @Override
    public IStatus getStatus() {
        IStatus originalStatus = super.getStatus();
        // #3960 filter out "Exported with compile warnings" messages
        if (originalStatus.isMultiStatus()) {
            ModifiableMessageMultiStatus newStatus = new ModifiableMessageMultiStatus(
                    VaadinPlugin.PLUGIN_ID, IStatus.OK, "", null);
            String message = null;

            // readd all status elements except "exported with compile warnings"
            for (IStatus status : originalStatus.getChildren()) {
                if (!status
                        .getMessage()
                        .contains(
                                JarPackagerMessages.JarFileExportOperation_exportedWithCompileWarnings
                                        .replace("{0}", ""))) {
                    newStatus.add(status);
                }
            }

            switch (newStatus.getSeverity()) {
            case IStatus.OK:
                message = ""; //$NON-NLS-1$
                break;
            case IStatus.INFO:
                message = JarPackagerMessages.JarFileExportOperation_exportFinishedWithInfo;
                break;
            case IStatus.WARNING:
                message = JarPackagerMessages.JarFileExportOperation_exportFinishedWithWarnings;
                break;
            case IStatus.ERROR:
                message = JarPackagerMessages.JarFileExportOperation_jarCreationFailed;
                break;
            default:
                // defensive code in case new severity is defined
                message = ""; //$NON-NLS-1$
                break;
            }
            newStatus.setMessage(message);

            return newStatus;
        }
        return originalStatus;
    }
}
