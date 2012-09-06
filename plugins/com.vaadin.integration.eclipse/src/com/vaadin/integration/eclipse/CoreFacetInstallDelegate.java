package com.vaadin.integration.eclipse;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.j2ee.web.componentcore.util.WebArtifactEdit;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

import com.vaadin.integration.eclipse.builder.WidgetsetNature;
import com.vaadin.integration.eclipse.configuration.VaadinFacetInstallDataModelProvider;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.GaeConfigurationUtil;
import com.vaadin.integration.eclipse.util.PortletConfigurationUtil;
import com.vaadin.integration.eclipse.util.PreferenceUtil;
import com.vaadin.integration.eclipse.util.ProjectDependencyManager;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;
import com.vaadin.integration.eclipse.util.VersionUtil;
import com.vaadin.integration.eclipse.util.WebXmlUtil;
import com.vaadin.integration.eclipse.util.WidgetsetUtil;
import com.vaadin.integration.eclipse.util.data.LocalVaadinVersion;
import com.vaadin.integration.eclipse.util.files.LocalFileManager;
import com.vaadin.integration.eclipse.util.network.DownloadManager;

public class CoreFacetInstallDelegate implements IDelegate,
        IVaadinFacetInstallDataModelProperties {

    private static final String VAADIN_PRODUCTION_MODE = "productionMode";
    private static final String VAADIN_PRODUCTION_MODE_DESCRIPTION = "Vaadin production mode";

    public void execute(IProject project, IProjectFacetVersion fv,
            Object configObject, IProgressMonitor monitor) throws CoreException {

        if (!(configObject instanceof IDataModel)) {
            throw ErrorUtil.newCoreException(
                    "Config object is of invalid type", null);
        }
        IDataModel model = (IDataModel) configObject;

        monitor.beginTask("Setting up Vaadin project", 11);

        String projectType = model.getStringProperty(VAADIN_PROJECT_TYPE);
        boolean gaeProject = VaadinFacetInstallDataModelProvider.PROJECT_TYPE_GAE
                .equals(projectType);
        String portletVersion = model.getStringProperty(PORTLET_VERSION);
        boolean createPortlet = !PORTLET_VERSION_NONE.equals(portletVersion);
        boolean useLatestNightly = model.getBooleanProperty(USE_LATEST_NIGHTLY);

        // Reference to the local Vaadin JAR that we should use
        LocalVaadinVersion localVaadinVersion = null;

        /*
         * Find the latest local version. If the model has a Vaadin version
         * specified, use it instead.
         */
        monitor.subTask("Checking for locally cached Vaadin");
        if (model.isPropertySet(VAADIN_VERSION)
                && model.getProperty(VAADIN_VERSION) != null) {
            // A version was specified on the configuration page - use that
            String versionString = model.getStringProperty(VAADIN_VERSION);
            try {
                localVaadinVersion = LocalFileManager
                        .getLocalVaadinVersion(versionString);
            } catch (CoreException ex) {
                throw ErrorUtil.newCoreException(
                        "Failed to use the requested Vaadin version ("
                                + versionString + ")", ex);
            }
        } else {
            // No version was specified on the configuration page. Use the
            // newest local.
            localVaadinVersion = LocalFileManager
                    .getNewestLocalVaadinJarVersion();
        }
        monitor.worked(1);

        if (localVaadinVersion == null) {
            /*
             * No Vaadin jar has been fetched - we must fetch one before
             * continuing.
             */
            monitor.subTask("Checking for latest available Vaadin version");
            String latestVaadinVersion = DownloadManager
                    .getLatestVaadinVersion();
            monitor.worked(1);
            monitor.subTask("Downloading Vaadin JAR (" + latestVaadinVersion
                    + ")");
            // In this case, reload list of Vaadin versions prior to downloading
            // Vaadin itself to make sure the latest version is on the cached
            // list.
            DownloadManager.flushCache();
            DownloadManager.downloadVaadinJar(latestVaadinVersion,
                    new SubProgressMonitor(monitor, 2));

            localVaadinVersion = LocalFileManager
                    .getNewestLocalVaadinJarVersion();
        } else {
            monitor.worked(3);
        }

        try {
            monitor.subTask("Setting up project preferences");

            /*
             * Save the information about project type to the project settings
             */
            try {
                PreferenceUtil preferences = PreferenceUtil.get(project);
                preferences.setProjectTypeGae(gaeProject);
                preferences.setUsingLatestNightly(useLatestNightly);
                preferences.persist();
            } catch (IOException e) {
                throw ErrorUtil.newCoreException(
                        "Failed to save project preferences", e);
            }

            monitor.worked(1);
        } catch (Exception e) {
            monitor.done();
            throw ErrorUtil.newCoreException(
                    "Setting up Vaadin project preferences failed", e);
        }

        try {
            monitor.subTask("Installing libraries");

            /* Copy Vaadin JAR to project's WEB-INF/lib folder */
            ProjectDependencyManager.ensureVaadinLibraries(project,
                    localVaadinVersion, new SubProgressMonitor(monitor, 5));

            // do not create project artifacts if adding the facet to an
            // existing project or if the user has chosen not to create them
            // when creating the project (e.g. SVN checkout)
            if (model.getBooleanProperty(CREATE_ARTIFACTS)) {
                // Name of Application (Vaadin 6) or UI (Vaadin 7) class
                String applicationClass = model
                        .getStringProperty(APPLICATION_CLASS);
                String applicationPackage = model
                        .getStringProperty(APPLICATION_PACKAGE);
                String applicationName = model
                        .getStringProperty(APPLICATION_NAME);

                // this is only used when "create portlet" is selected
                // TODO better alternative? must be different from application
                // name
                String servletName = model.getStringProperty(APPLICATION_CLASS)
                        + "Servlet";

                String applicationFileName = applicationClass + ".java";

                /* Create the application class */
                IJavaProject jProject = JavaCore.create(project);
                IPackageFragmentRoot rootPackage = jProject
                        .getPackageFragmentRoot(ProjectUtil
                                .getSrcFolder(project));

                /* Create the package if it does not exist */
                IPackageFragment appPackage = rootPackage
                        .createPackageFragment(applicationPackage, true,
                                monitor);

                // special case as the Vaadin JAR is not yet in the classpath
                String vaadinPackagePrefix = VaadinPlugin.VAADIN_PACKAGE_PREFIX;
                String servletClassName;
                String portletClassName = null;

                boolean vaadin7 = VersionUtil.isVaadin7(localVaadinVersion);
                if (vaadin7) {
                    // Vaadin 7 or newer: create a UI instead of an
                    // application
                    String uiCode = VaadinPluginUtil.createUiClassSource(
                            applicationPackage, applicationName,
                            applicationClass, vaadinPackagePrefix);

                    /* Create the application class if it does not exist */
                    appPackage.createCompilationUnit(applicationFileName,
                            uiCode, false, monitor);

                    servletClassName = vaadinPackagePrefix
                            + (gaeProject ? WebXmlUtil.VAADIN7_GAE_SERVLET_CLASS
                                    : WebXmlUtil.VAADIN7_SERVLET_CLASS);

                    if (createPortlet) {
                        // Vaadin 7 only supports portlet 2.0
                        portletClassName = vaadinPackagePrefix
                                + WebXmlUtil.VAADIN7_PORTLET2_CLASS;
                    }
                } else {
                    // Vaadin 6: create an Application class
                    String applicationCode = VaadinPluginUtil
                            .createApplicationClassSource(applicationPackage,
                                    applicationName, applicationClass,
                                    vaadinPackagePrefix);

                    /* Create the application class if it does not exist */
                    appPackage.createCompilationUnit(applicationFileName,
                            applicationCode, false, monitor);

                    servletClassName = vaadinPackagePrefix
                            + (gaeProject ? WebXmlUtil.VAADIN_GAE_SERVLET_CLASS
                                    : WebXmlUtil.VAADIN_SERVLET_CLASS);

                    if (createPortlet) {
                        if (PORTLET_VERSION20.equals(portletVersion)) {
                            portletClassName = vaadinPackagePrefix
                                    + WebXmlUtil.VAADIN_PORTLET2_CLASS;
                        } else {
                            portletClassName = vaadinPackagePrefix
                                    + WebXmlUtil.VAADIN_PORTLET_CLASS;
                        }
                    }
                }

                /* Update web.xml */
                WebArtifactEdit artifact = WebArtifactEdit
                        .getWebArtifactEditForWrite(project);
                try {
                    String servletPath = "/*";
                    // TODO check; could also skip web.xml creation for portlet
                    // 2.0 - creating to help testing portlets as servlets
                    if (createPortlet) {
                        servletPath = "/" + servletName + servletPath;
                    }
                    // For Vaadin 7, use a UI instead of an Application
                    WebXmlUtil.addServlet(artifact.getWebApp(),
                            applicationName, applicationPackage + "."
                                    + applicationClass, servletPath,
                            servletClassName, createPortlet, vaadin7);
                    WebXmlUtil.addContextParameter(artifact.getWebApp(),
                            VAADIN_PRODUCTION_MODE, "false",
                            VAADIN_PRODUCTION_MODE_DESCRIPTION);

                    artifact.save(monitor);
                } finally {
                    artifact.dispose();
                }

                if (createPortlet) {
                    // update portlet.xml, liferay-portlet.xml and
                    // liferay-display.xml
                    String portletName = applicationName + " portlet";
                    String portletTitle = model
                            .getStringProperty(PORTLET_TITLE);
                    String category = "Vaadin";
                    String portletApplicationName = null;
                    if (PORTLET_VERSION20.equals(portletVersion)) {
                        portletApplicationName = applicationPackage + "."
                                + applicationClass;
                    } else {
                        portletApplicationName = servletName;
                    }
                    PortletConfigurationUtil.addPortlet(project,
                            portletApplicationName, portletClassName,
                            portletName, portletTitle, category,
                            portletVersion, vaadin7);
                }

                // create appengine-web.xml
                if (gaeProject) {
                    GaeConfigurationUtil.createAppEngineWebXml(project);
                }

                // TODO true for 6.2 and later
                boolean isNewWidgetSetStyleProject = true;
                if (isNewWidgetSetStyleProject
                        && WidgetsetUtil.isWidgetsetManagedByPlugin(project)) {
                    WidgetsetNature.addWidgetsetNature(project);
                }
            }
            monitor.worked(1);
        } catch (Exception e) {
            throw ErrorUtil.newCoreException(
                    "Vaadin libraries installation failed", e);
        } finally {
            monitor.done();
        }

    }
}
