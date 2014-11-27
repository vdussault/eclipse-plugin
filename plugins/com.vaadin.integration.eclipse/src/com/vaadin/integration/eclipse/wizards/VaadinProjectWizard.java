package com.vaadin.integration.eclipse.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jst.servlet.ui.project.facet.WebProjectWizard;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.prefs.BackingStoreException;

import com.vaadin.integration.eclipse.IVaadinFacetInstallDataModelProperties;
import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.configuration.VaadinProjectCreationDataModelProvider;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.PreferenceUtil;
import com.vaadin.integration.eclipse.util.VersionUtil;

/**
 * Vaadin top level project creation wizard.
 * 
 * Note that Vaadin projects can also be created through the normal
 * "Dynamic Web Project" wizard by adding the Vaadin facet there or by adding
 * the Vaadin facet to an already existing project.
 * 
 * This is really just a customized and pre-configured version of the standard
 * dynamic web project creation wizard.
 */
public abstract class VaadinProjectWizard extends WebProjectWizard {
    public VaadinProjectWizard(IDataModel model) {
        super(model);
        setWindowTitle(getProjectTypeTitle());
    }

    public VaadinProjectWizard() {
        super();
        setWindowTitle(getProjectTypeTitle());
    }

    protected abstract String getProjectTypeTitle();

    @Override
    protected IDataModel createDataModel() {
        try {
            ProjectFacetsManager
                    .getProjectFacet(VaadinFacetUtils.VAADIN_FACET_ID);
            return DataModelFactory
                    .createDataModel(new VaadinProjectCreationDataModelProvider());
        } catch (Exception e) {
            ErrorUtil.handleBackgroundException(e);
            return null;
        }
    }

    @Override
    protected IWizardPage createFirstPage() {
        return new VaadinProjectFirstPage(model, "first.page"); //$NON-NLS-1$
    }

    @Override
    protected void setRuntimeAndDefaultFacets(IRuntime runtime) {
        super.setRuntimeAndDefaultFacets(runtime);

        // select the Vaadin preset configuration by default
        final IFacetedProjectWorkingCopy dm = getFacetedProjectWorkingCopy();
        dm.setSelectedPreset(getDefaultPreset());
    }

    protected abstract String getDefaultPreset();

    @Override
    protected void postPerformFinish() throws InvocationTargetException {
        IDataModel model = getDataModel();
        String vaadinVersion = model
                .getStringProperty(IVaadinFacetInstallDataModelProperties.VAADIN_VERSION);
        boolean vaadin7 = VersionUtil.isVaadin7VersionString(vaadinVersion);
        if (vaadin7) {
            // Ivy resolving might take a while the first time, info about this.
            // Popup can be (globally) disabled by the user.

            // InstanceScope = separate for each workspace.
            // (ConfigurationScope would be shared between workspaces)
            IEclipsePreferences prefs = new InstanceScope()
                    .getNode(VaadinPlugin.PLUGIN_ID);

            maybeShowIvyIsDownloadingMessage(prefs);
            maybeShowLunaSR1BugWarning(prefs);
        }

        super.postPerformFinish();
    }

    private void maybeShowIvyIsDownloadingMessage(IEclipsePreferences prefs) {
        if (!prefs.getBoolean(PreferenceUtil.PREFERENCES_IVYINFO_DISABLED,
                false)) {
            // MDWT should be able to save prefs, but this did not seem to
            // work, so we do it 'manually'.
            MessageDialogWithToggle d = MessageDialogWithToggle
                    .openInformation(
                            getShell(),
                            "Resolving dependencies",
                            "Vaadin jars and dependencies are automatically resolved and downloaded."
                                    + "\n\nIf the selected version is not already on your system, "
                                    + "this process might take several minutes."
                                    + " During this time your project will not compile."
                                    + "\n\nYou can follow the progress in the status bar (IvyDE resolve).",
                            "Don't show this message again", false, null, null);

            if (d.getToggleState()) {
                prefs.putBoolean(PreferenceUtil.PREFERENCES_IVYINFO_DISABLED,
                        true);
                try {
                    prefs.flush();
                } catch (BackingStoreException e) {
                    ErrorUtil.handleBackgroundException(e);
                }
            }
        }

    }

    private void maybeShowLunaSR1BugWarning(IEclipsePreferences prefs) {
        if (!isBrokenLunaSR1())
            return;

        if (!prefs.getBoolean(
                PreferenceUtil.PREFERENCES_ECLIPSE_LUNA_SR1_BUG_INFO, false)) {
            // MDWT should be able to save prefs, but this did not seem to
            // work, so we do it 'manually'.
            String title = "Eclipse hotfix needed";
            final String UPDATE_URL = "http://download.eclipse.org/eclipse/updates/4.4/";
            String message = "You need to install the hotfix named 'E4 RCP patch (bugzillas 445122) available from "
                    + UPDATE_URL
                    + " (in the group Eclipse 4.4.1 Patches for bug 445122)"
                    + "\n\n"
                    + "Eclipse Luna SR1 contains a serious bug which prevents Vaadin projects from working properly.";
            String dontshowagain = "Don't show this message again";
            final Shell shell = getShell();
            MessageDialogWithToggle d = new MessageDialogWithToggle(shell,
                    title, null, message, 0, new String[] { "Ok",
                            "Copy URL to clipboard" }, 0, dontshowagain, false);

            d.open();
            int buttonId = d.getReturnCode() - IDialogConstants.INTERNAL_ID;
            if (buttonId == 1) {
                // Copy URL
                TextTransfer textTransfer = TextTransfer.getInstance();
                Display display = shell.getDisplay();
                Clipboard clipboard = new Clipboard(display);

                clipboard.setContents(new Object[] { UPDATE_URL },
                        new Transfer[] { textTransfer });

            }

            if (d.getToggleState()) {
                prefs.putBoolean(
                        PreferenceUtil.PREFERENCES_ECLIPSE_LUNA_SR1_BUG_INFO,
                        true);
                try {
                    prefs.flush();
                } catch (BackingStoreException e) {
                    ErrorUtil.handleBackgroundException(e);
                }
            }
        }

    }

    private boolean isEclipseLunaSR1() {
        try {
            Bundle bundle = Platform.getBundle("org.eclipse.epp.package.jee");
            Version version = bundle.getVersion();
            if (version.getMajor() == 4 && version.getMinor() == 4
                    && version.getMicro() == 1) {
                // We are only interested in Luna SR1
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    private boolean isBrokenLunaSR1() {
        if (!isEclipseLunaSR1())
            return false;

        try {
            // Luna SR1 ships with org.eclipse.osgi_3.10.1.v20140909-1633.jar
            // The "E4 RCP patch (bugzillas 445122)" fix updates this to
            // 3.10.2.v20141020-1740

            // The original Luna release ships with 3.10.0.v20140606-1445.jar
            Bundle bundle = Platform.getBundle("org.eclipse.osgi");
            if (bundle == null)
                return false; // This should not really happen, stay quiet if it
                              // does
            Version v = bundle.getVersion();
            if (v.getMajor() == 3 && v.getMinor() == 10 && v.getMicro() == 1) {
                // Version shipped with Luna SR1
                return true;
            }

            return false;
        } catch (Exception e) {
            // Assume everything is ok and don't bother the user in vain
            return false;
        }

    }
}
