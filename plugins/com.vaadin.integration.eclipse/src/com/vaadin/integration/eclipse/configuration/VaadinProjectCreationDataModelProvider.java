package com.vaadin.integration.eclipse.configuration;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jst.common.project.facet.JavaFacetUtils;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.jst.j2ee.project.facet.J2EEFacetProjectCreationDataModelProvider;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;

import com.vaadin.integration.eclipse.IVaadinFacetInstallDataModelProperties;
import com.vaadin.integration.eclipse.VaadinFacetUtils;

/**
 * Data model provider when creating a Vaadin top-level project.
 * 
 * This sets up the list of required facets and the default project structure.
 * 
 * This class is not used when just adding the Vaadin facet to a Dynamic Web
 * Project.
 */
@SuppressWarnings("deprecation")
public class VaadinProjectCreationDataModelProvider extends
        J2EEFacetProjectCreationDataModelProvider implements
        IVaadinFacetInstallDataModelProperties {

    @Override
    public void init() {
        super.init();

        Collection<IProjectFacet> requiredFacets = new ArrayList<IProjectFacet>();
        requiredFacets.add(JavaFacetUtils.JAVA_FACET);
        requiredFacets.add(IJ2EEFacetConstants.DYNAMIC_WEB_FACET);
        requiredFacets.add(VaadinFacetUtils.VAADIN_FACET);
        setProperty(REQUIRED_FACETS_COLLECTION, requiredFacets);

        FacetDataModelMap map = (FacetDataModelMap) getProperty(FACET_DM_MAP);
        IDataModel webFacet = map
                .getFacetDataModel(IJ2EEFacetConstants.DYNAMIC_WEB);
        IDataModel vaadinFacet = map
                .getFacetDataModel(VaadinFacetUtils.VAADIN_FACET_ID);

        // this is to enable validation of web facet properties on the first
        // page, so that the properties will be found and associated with
        // correct model
        model.addNestedModel(IJ2EEFacetConstants.DYNAMIC_WEB, webFacet);
        model.addNestedModel(VaadinFacetUtils.VAADIN_FACET_ID, vaadinFacet);
    }

}
