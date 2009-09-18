package com.vaadin.integration.eclipse.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jst.j2ee.internal.common.J2EEVersionUtil;
import org.eclipse.jst.j2ee.internal.plugin.J2EEPlugin;
import org.eclipse.jst.j2ee.internal.plugin.J2EEPreferences;
import org.eclipse.jst.j2ee.project.facet.J2EEModuleFacetInstallDataModelProvider;
import org.eclipse.wst.common.frameworks.datamodel.DataModelPropertyDescriptor;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

import com.vaadin.integration.eclipse.IVaadinFacetInstallDataModelProperties;
import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.util.DownloadUtils;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;
import com.vaadin.integration.eclipse.util.DownloadUtils.Version;

/**
 * This data model provider is used whenever installing the Vaadin facet to a
 * project, whether at project creation or for an existing project.
 */
public class VaadinFacetInstallDataModelProvider extends
        J2EEModuleFacetInstallDataModelProvider implements
        IVaadinFacetInstallDataModelProperties {

    private static final String BASE_PACKAGE_NAME = "com.example";

    public static final String DEFAULT_APPLICATION_NAME = "Vaadin Application";
    public static final String DEFAULT_APPLICATION_PACKAGE = BASE_PACKAGE_NAME;
    public static final String DEFAULT_APPLICATION_CLASS = "VaadinApplication";

    private List<String> vaadinVersions;

    @Override
    public Set getPropertyNames() {
        Set names = super.getPropertyNames();
        names.add(APPLICATION_NAME);
        names.add(APPLICATION_PACKAGE);
        names.add(APPLICATION_CLASS);
        names.add(CREATE_ARTIFACTS);
        names.add(CREATE_PORTLET);
        names.add(PORTLET_TITLE);
        names.add(VAADIN_VERSION);
        return names;
    }

    private String getProjectName() {
        Object projectNameObject = getProperty(FACET_PROJECT_NAME);
        if (projectNameObject == null) {
            return null;
        }
        String projectName = projectNameObject.toString();
        if (!Character.isJavaIdentifierStart(projectName.charAt(0))) {
            // uh-oh, replace first character
            projectName = "X" + projectName.substring(1).toLowerCase();
        } else {
            // uppercase first character
            projectName = projectName.substring(0, 1).toUpperCase()
                    + projectName.substring(1).toLowerCase();
        }
        // let's normalize a little
        projectName = projectName.replaceAll("[^A-Za-z_0-9]", "_");
        return projectName;
    }

    @Override
    public Object getDefaultProperty(String propertyName) {
        if (propertyName.equals(CONFIG_FOLDER)) {
            // this is required when adding the facet to an existing project, as
            // not inheriting WebFacetInstallDataModelProvider
            return J2EEPlugin.getDefault().getJ2EEPreferences().getString(
                    J2EEPreferences.Keys.WEB_CONTENT_FOLDER);
        } else if (propertyName.equals(APPLICATION_NAME)) {
            String projectName = getProjectName();
            if (projectName == null) {
                return DEFAULT_APPLICATION_NAME;
            } else {
                return projectName + " Application";
            }
        } else if (propertyName.equals(APPLICATION_PACKAGE)) {
            String projectName = getProjectName();
            if (projectName == null) {
                return DEFAULT_APPLICATION_PACKAGE;
            } else {
                return BASE_PACKAGE_NAME + "." + getProjectName().toLowerCase();
            }
        } else if (propertyName.equals(APPLICATION_CLASS)) {
            String projectName = getProjectName();
            if (projectName == null) {
                return DEFAULT_APPLICATION_CLASS;
            } else {
                return projectName + "Application";
            }
        } else if (propertyName.equals(CREATE_ARTIFACTS)) {
            // by default, do not create artifacts if the configuration page is
            // not shown (e.g. when importing a project from version control or
            // adding the facet to an existing project)
            return Boolean.FALSE;
        } else if (propertyName.equals(CREATE_PORTLET)) {
            return Boolean.FALSE;
        } else if (propertyName.equals(PORTLET_TITLE)) {
            return "Portlet Title";
        } else if (propertyName.equals(VAADIN_VERSION)) {
            try {
                Version latestLocal = DownloadUtils
                        .getLatestLocalVaadinJarVersion();
                return (latestLocal != null) ? latestLocal.getVersionString()
                        : null;
            } catch (CoreException ex) {
                VaadinPluginUtil
                        .handleBackgroundException(
                                IStatus.WARNING,
                                "Checking the latest locally cached Vaadin version failed",
                                ex);
                return null;
            }
        } else if (propertyName.equals(FACET_ID)) {
            return VaadinFacetUtils.VAADIN_FACET_ID;
        }
        return super.getDefaultProperty(propertyName);
    }

    @Override
    public boolean propertySet(String propertyName, Object propertyValue) {
        if (FACET_PROJECT_NAME.equals(propertyName)) {
            // re-compute application name, class and package
            resetProperty(APPLICATION_NAME, DEFAULT_APPLICATION_NAME);
            resetProperty(APPLICATION_PACKAGE, DEFAULT_APPLICATION_PACKAGE);
            resetProperty(APPLICATION_CLASS, DEFAULT_APPLICATION_CLASS);
        }
        // notify of valid values change
        if (VAADIN_VERSION.equals(propertyName)
                && !vaadinVersions.contains(propertyValue)) {
            try {
                vaadinVersions = getVaadinVersions();
                model.notifyPropertyChange(propertyName,
                        IDataModel.VALID_VALUES_CHG);
            } catch (CoreException e) {
                // no notification nor change of value list if listing local
                // versions failed
                VaadinPluginUtil.handleBackgroundException(IStatus.WARNING,
                        "Failed to update Vaadin version list", e);
            }
         }
        return super.propertySet(propertyName, propertyValue);
    }

    @Override
    public DataModelPropertyDescriptor[] getValidPropertyDescriptors(
            String propertyName) {
        if (VAADIN_VERSION.equals(propertyName)) {
            if (vaadinVersions == null) {
                try {
                    vaadinVersions = getVaadinVersions();
                } catch (CoreException e) {
                    // no notification nor change of value list if listing local
                    // versions failed
                    VaadinPluginUtil
                            .handleBackgroundException(
                                    IStatus.WARNING,
                                    "Failed to list the locally cached Vaadin versions",
                                    e);
                }
            }
            return DataModelPropertyDescriptor.createDescriptors(vaadinVersions
                    .toArray());
        } else {
            return super.getValidPropertyDescriptors(propertyName);
        }
    }

    private List<String> getVaadinVersions() throws CoreException {
        List<String> versions = new ArrayList<String>();
        for (Version version : DownloadUtils.getLocalVaadinJarVersions()) {
            versions.add(version.getVersionString());
        }
        return versions;
    }

    @Override
    protected int convertFacetVersionToJ2EEVersion(IProjectFacetVersion version) {
        return J2EEVersionUtil.convertVersionStringToInt(version
                .getVersionString());
    }

    private void resetProperty(String property, String defaultValue) {
        // TODO would be more complicated to "freeze" modifications when the
        // user touches the field
        setProperty(property, getDefaultProperty(property));
        model.notifyPropertyChange(property, IDataModel.VALUE_CHG);
    }

    @Override
    public IStatus validate(String name) {
        if (name.equals(APPLICATION_NAME)) {
            return validateApplicationName(getStringProperty(APPLICATION_NAME));
        } else if (name.equals(APPLICATION_PACKAGE)) {
            return validatePackageName(getStringProperty(APPLICATION_PACKAGE));
        } else if (name.equals(APPLICATION_CLASS)) {
            return validateTypeName(getStringProperty(APPLICATION_CLASS));
        }
        return super.validate(name);
    }

    private IStatus validateApplicationName(String applicationName) {
        if (!getBooleanProperty(CREATE_ARTIFACTS)) {
            return OK_STATUS;
        } else if ("".equals(applicationName)
                || !applicationName.trim().equals(applicationName)) {
            return J2EEPlugin
                    .newErrorStatus(
                            "Application name cannot be empty or start or end with whitespace",
                            null);
        } else {
            return OK_STATUS;
        }
    }

    private IStatus validatePackageName(String packageName) {
        if (!getBooleanProperty(CREATE_ARTIFACTS)) {
            return OK_STATUS;
        }
        if (packageName == null) {
            return J2EEPlugin.newErrorStatus(
                    "Application package name cannot be empty",
                    null);
        } else if (JavaConventions.validatePackageName(packageName).isOK()) {
            // have to choose between deprecated API or internal constants...
            return OK_STATUS;
        } else {
            return J2EEPlugin.newErrorStatus(
                    "Invalid application package name", null);
        }
    }

    private IStatus validateTypeName(String typeName) {
        if (!getBooleanProperty(CREATE_ARTIFACTS)) {
            return OK_STATUS;
        }
        if (typeName == null) {
            return J2EEPlugin.newErrorStatus(
                    "Application class name cannot be empty", null);
        } else if (JavaConventions.validateJavaTypeName(typeName).isOK()) {
            // have to choose between deprecated API or internal constants...
            return OK_STATUS;
        } else {
            return J2EEPlugin.newErrorStatus("Invalid application class name",
                    null);
        }
    }

}
