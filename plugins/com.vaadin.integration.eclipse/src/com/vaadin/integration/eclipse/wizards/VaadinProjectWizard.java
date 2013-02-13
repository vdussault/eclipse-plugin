package com.vaadin.integration.eclipse.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jst.servlet.ui.project.facet.WebProjectWizard;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
import org.osgi.service.prefs.BackingStoreException;

import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.configuration.VaadinProjectCreationDataModelProvider;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.PreferenceUtil;

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
        // Ivy resolving might take a while the first time, info about this.
        // Popup can be (globally) disabled by the user.

        // InstanceScope = separate for each workspace.
        // (ConfigurationScope would be shared between workspaces)
        IEclipsePreferences prefs = InstanceScope.INSTANCE
                .getNode(VaadinPlugin.PLUGIN_ID);
        if (!prefs.getBoolean(PreferenceUtil.PREFERENCES_IVYINFO_DISABLED,
                false)) {
            // MDWT should be able to save prefs, but this did not seem to work,
            // so we do it 'manually'.
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
        super.postPerformFinish();
    }
}
