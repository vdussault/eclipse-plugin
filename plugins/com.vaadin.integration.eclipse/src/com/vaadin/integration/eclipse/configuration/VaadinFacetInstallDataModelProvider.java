package com.vaadin.integration.eclipse.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jst.common.project.facet.IJavaFacetInstallDataModelProperties;
import org.eclipse.jst.common.project.facet.JavaFacetUtils;
import org.eclipse.jst.j2ee.internal.J2EEConstants;
import org.eclipse.jst.j2ee.internal.common.J2EEVersionUtil;
import org.eclipse.jst.j2ee.internal.plugin.J2EEPlugin;
import org.eclipse.jst.j2ee.internal.plugin.J2EEPreferences;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.jst.j2ee.project.facet.J2EEModuleFacetInstallDataModelProvider;
import org.eclipse.jst.j2ee.web.project.facet.IWebFacetInstallDataModelProperties;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetProjectCreationDataModelProperties;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetProjectCreationDataModelProperties.FacetDataModelMap;
import org.eclipse.wst.common.frameworks.datamodel.DataModelPropertyDescriptor;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

import com.vaadin.integration.eclipse.IVaadinFacetInstallDataModelProperties;
import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.VersionUtil;
import com.vaadin.integration.eclipse.util.data.LocalVaadinVersion;
import com.vaadin.integration.eclipse.util.files.LocalFileManager;

/**
 * This data model provider is used whenever installing the Vaadin facet to a
 * project, whether at project creation or for an existing project.
 */
@SuppressWarnings({ "restriction", "deprecation" })
public class VaadinFacetInstallDataModelProvider extends
        J2EEModuleFacetInstallDataModelProvider implements
        IVaadinFacetInstallDataModelProperties {

    // these project type constants also serve as the labels for the project
    // types, and the selected value goes into the project creation data model
    // - these are Strings to be able to use them in SWT Combo widgets
    public static final String PROJECT_TYPE_SERVLET = "Servlet (default)";
    public static final String PROJECT_TYPE_GAE = "Google App Engine servlet";
    public static final String PROJECT_TYPE_PORTLET20 = "Generic portlet (Portlet 2.0)";
    public static final String PROJECT_TYPE_PORTLET10 = "Old portlet (Portlet 1.0)";

    // allowed project types in display order, default first
    public static final String[] PROJECT_TYPES = new String[] {
            PROJECT_TYPE_SERVLET, PROJECT_TYPE_GAE, PROJECT_TYPE_PORTLET20,
            PROJECT_TYPE_PORTLET10 };

    private static final String BASE_PACKAGE_NAME = "com.example";

    public static final String DEFAULT_APPLICATION_NAME = "Vaadin Application";
    public static final String DEFAULT_APPLICATION_PACKAGE = BASE_PACKAGE_NAME;
    public static final String DEFAULT_APPLICATION_CLASS_PREFIX = "Vaadin";

    private List<String> vaadinVersions;

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getPropertyNames() {
        Set<String> names = super.getPropertyNames();
        names.add(APPLICATION_NAME);
        names.add(APPLICATION_PACKAGE);
        names.add(APPLICATION_CLASS);
        names.add(CREATE_ARTIFACTS);
        names.add(PORTLET_VERSION);
        names.add(PORTLET_TITLE);
        names.add(VAADIN_VERSION);
        names.add(USE_LATEST_NIGHTLY);
        names.add(VAADIN_PROJECT_TYPE);
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
            return J2EEPlugin.getDefault().getJ2EEPreferences()
                    .getString(J2EEPreferences.Keys.WEB_CONTENT_FOLDER);
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
            String suffix = getApplicationClassSuffix();

            if (projectName == null) {
                return DEFAULT_APPLICATION_CLASS_PREFIX + suffix;
            } else {
                return projectName + suffix;
            }
        } else if (propertyName.equals(CREATE_ARTIFACTS)) {
            // by default, do not create artifacts if the configuration page is
            // not shown (e.g. when importing a project from version control or
            // adding the facet to an existing project)
            return Boolean.FALSE;
        } else if (propertyName.equals(PORTLET_VERSION)) {
            return PORTLET_VERSION_NONE;
        } else if (propertyName.equals(PORTLET_TITLE)) {
            Object projectName = getProperty(FACET_PROJECT_NAME);
            if (projectName == null) {
                return "Portlet Title";
            } else {
                return projectName.toString();
            }
        } else if (propertyName.equals(USE_LATEST_NIGHTLY)) {
            return Boolean.FALSE;
        } else if (propertyName.equals(VAADIN_VERSION)) {
            try {
                LocalVaadinVersion latestLocal = LocalFileManager
                        .getNewestLocalVaadinJarVersion();
                return (latestLocal != null) ? latestLocal.getVersionNumber()
                        : null;
            } catch (CoreException ex) {
                ErrorUtil
                        .handleBackgroundException(
                                IStatus.WARNING,
                                "Checking the latest locally cached Vaadin version failed",
                                ex);
                return null;
            }
        } else if (propertyName.equals(FACET_ID)) {
            return VaadinFacetUtils.VAADIN_FACET_ID;
        } else if (propertyName.equals(VAADIN_PROJECT_TYPE)) {
            return PROJECT_TYPE_SERVLET;
        }
        return super.getDefaultProperty(propertyName);
    }

    /**
     * Returns "Application" (for Vaadin 6 or unknown) or "Root" (for other
     * Vaadin versions - 7 or higher).
     * 
     * @return
     */
    private String getApplicationClassSuffix() {
        String suffix = VaadinPlugin.APPLICATION_CLASS_NAME;
        Object versionObject = getProperty(VAADIN_VERSION);
        if (null != versionObject && !"".equals(versionObject)) {
            boolean useRoot = VersionUtil.isVaadin7VersionString(String
                    .valueOf(versionObject));
            if (useRoot) {
                suffix = VaadinPlugin.ROOT_CLASS_NAME;
            }
        }
        return suffix;
    }

    @Override
    public boolean propertySet(String propertyName, Object propertyValue) {
        if (FACET_PROJECT_NAME.equals(propertyName)) {
            // re-compute application name, class and package
            resetProperty(APPLICATION_NAME, DEFAULT_APPLICATION_NAME);
            resetProperty(APPLICATION_PACKAGE, DEFAULT_APPLICATION_PACKAGE);
            resetProperty(APPLICATION_CLASS, DEFAULT_APPLICATION_CLASS_PREFIX
                    + getApplicationClassSuffix());
            resetProperty(PORTLET_TITLE, null);
        }
        // notify of valid values change
        if (VAADIN_VERSION.equals(propertyName)) {
            if (!vaadinVersions.contains(propertyValue)) {
                try {
                    vaadinVersions = getVaadinVersions();
                    model.notifyPropertyChange(propertyName,
                            IDataModel.VALID_VALUES_CHG);
                } catch (CoreException e) {
                    // no notification nor change of value list if listing local
                    // versions failed
                    ErrorUtil.handleBackgroundException(IStatus.WARNING,
                            "Failed to update Vaadin version list", e);
                }
            }
            // update application class name (*Application/*Root) if necessary
            if (null != propertyValue && !"".equals(propertyValue)) {
                boolean useRoot = VersionUtil.isVaadin7VersionString(String
                        .valueOf(propertyValue));
                Object classNameObject = getProperty(APPLICATION_CLASS);
                if (null != classNameObject) {
                    String className = classNameObject.toString();
                    if (useRoot) {
                        if (className
                                .endsWith(VaadinPlugin.APPLICATION_CLASS_NAME)) {
                            className = className
                                    .substring(
                                            0,
                                            className
                                                    .lastIndexOf(VaadinPlugin.APPLICATION_CLASS_NAME))
                                    + VaadinPlugin.ROOT_CLASS_NAME;
                            setProperty(APPLICATION_CLASS, className);
                        }
                    } else {
                        if (className.endsWith(VaadinPlugin.ROOT_CLASS_NAME)) {
                            className = className.substring(0, className
                                    .lastIndexOf(VaadinPlugin.ROOT_CLASS_NAME))
                                    + VaadinPlugin.APPLICATION_CLASS_NAME;
                            setProperty(APPLICATION_CLASS, className);
                        }
                    }
                }
            }
        } else if (PORTLET_VERSION.equals(propertyName)) {
            if (PORTLET_VERSION20.equals(propertyValue)
                    && !PROJECT_TYPE_PORTLET20
                            .equals(getProperty(VAADIN_PROJECT_TYPE))) {
                setProperty(VAADIN_PROJECT_TYPE, PROJECT_TYPE_PORTLET20);
            } else if (PORTLET_VERSION10.equals(propertyValue)
                    && !PROJECT_TYPE_PORTLET10
                            .equals(getProperty(VAADIN_PROJECT_TYPE))) {
                setProperty(VAADIN_PROJECT_TYPE, PROJECT_TYPE_PORTLET10);
            }
            // else do not change anything - multiple possible project types

            // notify about a change of enablement for sub-properties
            model.notifyPropertyChange(PORTLET_TITLE, IDataModel.ENABLE_CHG);
        } else if (VAADIN_PROJECT_TYPE.equals(propertyName)) {
            // set directory structure based on the selected project type
            useGaeDirectoryStructure(PROJECT_TYPE_GAE.equals(propertyValue));

            // set portlet creation flag
            if (PROJECT_TYPE_PORTLET20.equals(propertyValue)) {
                setProperty(PORTLET_VERSION, PORTLET_VERSION20);
            } else if (PROJECT_TYPE_PORTLET10.equals(propertyValue)) {
                setProperty(PORTLET_VERSION, PORTLET_VERSION10);
            } else {
                setProperty(PORTLET_VERSION, PORTLET_VERSION_NONE);
            }
            model.notifyPropertyChange(PORTLET_VERSION, IDataModel.VALUE_CHG);

        }
        return super.propertySet(propertyName, propertyValue);
    }

    /**
     * Set up the directory layout (context root dir and output folder) for
     * default or Google App Engine configuration.
     * 
     * @param useGae
     */
    private void useGaeDirectoryStructure(boolean useGae) {
        // obtain the master model which has links to other facet models
        IDataModel masterModel = (IDataModel) model
                .getProperty(MASTER_PROJECT_DM);
        FacetDataModelMap map = (FacetDataModelMap) masterModel
                .getProperty(IFacetProjectCreationDataModelProperties.FACET_DM_MAP);

        IDataModel webFacet = map
                .getFacetDataModel(IJ2EEFacetConstants.DYNAMIC_WEB);
        IDataModel javaFacet = map.getFacetDataModel(JavaFacetUtils.JAVA_FACET
                .getId());

        // context root dir: war
        String webRoot = useGae ? "war" : "WebContent";
        webFacet.setStringProperty(
                IWebFacetInstallDataModelProperties.CONFIG_FOLDER, webRoot);

        // output folder: "<content folder>/WEB-INF/classes"
        String outputDir = useGae ? webRoot + "/"
                + J2EEConstants.WEB_INF_CLASSES : "build/classes";
        javaFacet
                .setProperty(
                        IJavaFacetInstallDataModelProperties.DEFAULT_OUTPUT_FOLDER_NAME,
                        outputDir);
    }

    @Override
    public boolean isPropertyEnabled(String propertyName) {
        if (PORTLET_TITLE.equals(propertyName)
                && PORTLET_VERSION_NONE
                        .equals(getStringProperty(PORTLET_VERSION))) {
            return false;
        }
        return super.isPropertyEnabled(propertyName);
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
                    ErrorUtil
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
        for (LocalVaadinVersion version : LocalFileManager
                .getLocalVaadinJarVersions()) {
            versions.add(version.getVersionNumber());
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
                    "Application package name cannot be empty", null);
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
