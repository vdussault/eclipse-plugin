package com.vaadin.integration.eclipse;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
import com.vaadin.integration.eclipse.util.DownloadUtils;
import com.vaadin.integration.eclipse.util.GaeConfigurationUtil;
import com.vaadin.integration.eclipse.util.PortletConfigurationUtil;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;
import com.vaadin.integration.eclipse.util.WebXmlUtil;
import com.vaadin.integration.eclipse.util.DownloadUtils.Version;

public class CoreFacetInstallDelegate implements IDelegate,
        IVaadinFacetInstallDataModelProperties {

    private static final String VAADIN_PRODUCTION_MODE = "productionMode";
    private static final String VAADIN_PRODUCTION_MODE_DESCRIPTION = "Vaadin production mode";

    public void execute(IProject project, IProjectFacetVersion fv,
            Object configObject, IProgressMonitor monitor) throws CoreException {

        if (!(configObject instanceof IDataModel)) {
            throw VaadinPluginUtil.newCoreException(
                    "Config object is of invalid type", null);
        }
        IDataModel model = (IDataModel) configObject;

        monitor.beginTask("Setting up Vaadin project", 10);

        /*
         * Find the latest local version. If the model has a Vaadin version
         * specified, use it instead.
         */
        monitor.subTask("Checking for locally cached Vaadin");
        Version vaadinVersion;
        if (model.isPropertySet(VAADIN_VERSION)
                && model.getProperty(VAADIN_VERSION) != null) {
            // get the version specified on the configuration page
            String versionString = model.getStringProperty(VAADIN_VERSION);
            try {
                vaadinVersion = DownloadUtils
                        .getLocalVaadinVersion(versionString);
            } catch (CoreException ex) {
                // default to the latest downloaded version
                VaadinPluginUtil
                        .handleBackgroundException(
                                IStatus.WARNING,
                                "Failed to get the requested Vaadin version, using the most recent cached version",
                                ex);
                vaadinVersion = DownloadUtils.getLatestLocalVaadinJarVersion();
            }
        } else {
            vaadinVersion = DownloadUtils.getLatestLocalVaadinJarVersion();
        }
        monitor.worked(1);
        if (vaadinVersion == null) {
            /*
             * No Vaadin jar has been fetched - we must fetch one before
             * continuing.
             */
            monitor.subTask("Checking for latest Vaadin version available");
            vaadinVersion = DownloadUtils.getLatestVaadinVersion();
            monitor.worked(1);
            monitor.subTask("Downloading Vaadin "
                    + vaadinVersion.getVersionString());
            DownloadUtils.fetchVaadinJar(vaadinVersion, new SubProgressMonitor(
                    monitor, 2));
        } else {
            monitor.worked(3);
        }

        try {
            monitor.subTask("Installing libraries");
            /* Copy Vaadin JAR to project's WEB-INF/lib folder */
            VaadinPluginUtil.ensureVaadinLibraries(project, vaadinVersion,
                    new SubProgressMonitor(monitor, 5));

            // do not create project artifacts if adding the facet to an
            // existing project or if the user has chosen not to create them
            // when creating the project (e.g. SVN checkout)
            if (model.getBooleanProperty(CREATE_ARTIFACTS)) {
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
                        .getPackageFragmentRoot(VaadinPluginUtil
                                .getSrcFolder(project));

                /* Create the package if it does not exist */
                IPackageFragment appPackage = rootPackage
                        .createPackageFragment(applicationPackage, true,
                                monitor);

                // special case as the Vaadin JAR is not yet in the classpath
                String vaadinPackagePrefix;
                if (vaadinVersion.getVersionString().startsWith("5")) {
                    vaadinPackagePrefix = VaadinPlugin.TOOLKIT_PACKAGE_PREFIX;
                } else {
                    vaadinPackagePrefix = VaadinPlugin.VAADIN_PACKAGE_PREFIX;
                }

                String applicationCode = VaadinPluginUtil
                        .createApplicationClassSource(applicationPackage,
                                applicationName, applicationClass,
                                vaadinPackagePrefix);

                /* Create the application class if it does not exist */
                appPackage.createCompilationUnit(applicationFileName,
                        applicationCode, false, monitor);

                String projectType = model
                        .getStringProperty(VAADIN_PROJECT_TYPE);
                boolean gaeProject = VaadinFacetInstallDataModelProvider.PROJECT_TYPE_GAE
                        .equals(projectType);

                String portletVersion = model
                        .getStringProperty(PORTLET_VERSION);
                boolean createPortlet = !PORTLET_VERSION_NONE.equals(portletVersion);

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
                    String servletClassName = vaadinPackagePrefix
                            + (gaeProject ? WebXmlUtil.VAADIN_GAE_SERVLET_CLASS
                                    : WebXmlUtil.VAADIN_SERVLET_CLASS);
                    WebXmlUtil.addServlet(artifact.getWebApp(),
                            applicationName, applicationPackage + "."
                                    + applicationClass, servletPath,
                            servletClassName, createPortlet);
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
                    if (PORTLET_VERSION20.equals(portletVersion)) {
                        PortletConfigurationUtil
                                .addPortlet(
                                        project,
                                        applicationPackage + "."
                                                + applicationClass,
                                        vaadinPackagePrefix
                                                + "terminal.gwt.server.ApplicationPortlet2",
                                        portletName, portletTitle, category,
                                        portletVersion);
                    } else {
                        PortletConfigurationUtil.addPortlet(project, servletName,
                                vaadinPackagePrefix
                                        + "terminal.gwt.server.ApplicationPortlet",
                                        portletName, portletTitle, category,
                                        portletVersion);
                    }
                }

                // create appengine-web.xml
                if (gaeProject) {
                    GaeConfigurationUtil.createAppEngineWebXml(project);
                }

                // TODO true for 6.2 and later
                boolean isNewWidgetSetStyleProject = true;
                if (isNewWidgetSetStyleProject) {
                    WidgetsetNature.addWidgetsetNature(project);
                }
            }
            monitor.worked(1);
        } catch (Exception e) {
            throw VaadinPluginUtil.newCoreException(
                    "Vaadin libraries installation failed", e);
        } finally {
            monitor.done();
        }

    }
}
