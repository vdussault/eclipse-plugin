package com.vaadin.integration.eclipse;

import org.eclipse.wst.common.frameworks.datamodel.IDataModelProperties;

/**
 * Property names in the facet data model.
 */
public interface IVaadinFacetInstallDataModelProperties extends
        IDataModelProperties {

    public static final String APPLICATION_NAME = "IVaadinFacetInstallDataModelProperties.APPLICATION_NAME"; //$NON-NLS-1$
    public static final String APPLICATION_PACKAGE = "IVaadinFacetInstallDataModelProperties.APPLICATION_PACKAGE"; //$NON-NLS-1$
    public static final String APPLICATION_CLASS = "IVaadinFacetInstallDataModelProperties.APPLICATION_CLASS"; //$NON-NLS-1$

    public static final String CREATE_ARTIFACTS = "IVaadinFacetInstallDataModelProperties.CREATE_ARTIFACTS"; //$NON-NLS-1$

    public static final String CREATE_PORTLET = "IVaadinFacetInstallDataModelProperties.CREATE_PORTLET"; //$NON-NLS-1$
    public static final String PORTLET_TITLE = "IVaadinFacetInstallDataModelProperties.PORTLET_TITLE"; //$NON-NLS-1$

    public static final String VAADIN_VERSION = "IVaadinFacetInstallDataModelProperties.VAADIN_VERSION"; //$NON-NLS-1$
}
