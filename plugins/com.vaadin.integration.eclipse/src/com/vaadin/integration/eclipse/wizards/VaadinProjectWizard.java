package com.vaadin.integration.eclipse.wizards;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jst.servlet.ui.project.facet.WebProjectWizard;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectTemplate;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import com.vaadin.integration.eclipse.configuration.VaadinProjectCreationDataModelProvider;

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
public class VaadinProjectWizard extends WebProjectWizard {
    public VaadinProjectWizard(IDataModel model) {
        super(model);
        setWindowTitle("New Vaadin Project");
    }

    public VaadinProjectWizard() {
        super();
        setWindowTitle("New Vaadin Project");
    }

    @Override
    protected IDataModel createDataModel() {
        try {
            return DataModelFactory
                    .createDataModel(new VaadinProjectCreationDataModelProvider());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected IFacetedProjectTemplate getTemplate() {
        return ProjectFacetsManager.getTemplate("template.vaadin"); //$NON-NLS-1$
    }

    @Override
    protected IWizardPage createFirstPage() {
        return new VaadinProjectFirstPage(model, "first.page"); //$NON-NLS-1$
    }

}
