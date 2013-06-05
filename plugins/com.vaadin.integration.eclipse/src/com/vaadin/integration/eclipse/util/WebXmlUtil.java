package com.vaadin.integration.eclipse.util;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jdt.core.IType;
import org.eclipse.jst.j2ee.common.CommonFactory;
import org.eclipse.jst.j2ee.common.Description;
import org.eclipse.jst.j2ee.common.ParamValue;
import org.eclipse.jst.j2ee.internal.J2EEVersionConstants;
import org.eclipse.jst.j2ee.web.componentcore.util.WebArtifactEdit;
import org.eclipse.jst.j2ee.webapplication.ContextParam;
import org.eclipse.jst.j2ee.webapplication.InitParam;
import org.eclipse.jst.j2ee.webapplication.Servlet;
import org.eclipse.jst.j2ee.webapplication.ServletMapping;
import org.eclipse.jst.j2ee.webapplication.ServletType;
import org.eclipse.jst.j2ee.webapplication.WebApp;
import org.eclipse.jst.j2ee.webapplication.WebapplicationFactory;

import com.vaadin.integration.eclipse.util.data.AbstractVaadinVersion;

@SuppressWarnings("restriction")
public class WebXmlUtil {

    public static final String VAADIN_SERVLET_CLASS = "terminal.gwt.server.ApplicationServlet";
    public static final String VAADIN_GAE_SERVLET_CLASS = "terminal.gwt.server.GAEApplicationServlet";
    public static final String VAADIN_PORTLET_CLASS = "terminal.gwt.server.ApplicationPortlet";
    public static final String VAADIN_PORTLET2_CLASS = "terminal.gwt.server.ApplicationPortlet2";

    public static final String VAADIN_APPLICATION_CLASS_PARAMETER = "application";

    public static final String VAADIN7_SERVLET_CLASS = "server.VaadinServlet";
    public static final String VAADIN7_GAE_SERVLET_CLASS = "server.GAEVaadinServlet";
    public static final String VAADIN7_PORTLET2_CLASS = "server.VaadinPortlet";

    public static final String VAADIN_UI_CLASS_PARAMETER = "UI";

    public static final String VAADIN_LEGACY_TOSTRING_PARAMETER = "legacyPropertyToString";

    public static final String VAADIN_WIDGETSET_PARAMETER = "widgetset";

    /**
     * Adds a servlet and its mapping to web.xml.
     * 
     * @param artifact
     * @param applicationName
     * @param applicationClass
     *            application or ui class name
     * @param urlPattern
     * @param servletClassName
     * @param addVaadinMapping
     * @param vaadinVersion
     */
    @SuppressWarnings("unchecked")
    public static void addServlet(WebApp webApp, String applicationName,
            String applicationClass, String urlPattern,
            String servletClassName, boolean addVaadinMapping,
            AbstractVaadinVersion vaadinVersion) {

        /* Create servlet type compatible with Vaadin */
        ServletType servletType = WebapplicationFactory.eINSTANCE
                .createServletType();
        servletType.setClassName(servletClassName);

        /* Create servlet definition */
        Servlet servlet = WebapplicationFactory.eINSTANCE.createServlet();
        servlet.setServletName(applicationName);
        servlet.setWebType(servletType);

        boolean uiMapping = VersionUtil.isVaadin7(vaadinVersion);
        if (uiMapping) {
            // Vaadin 7 UI instead of a Vaadin 6 Application
            addServletInitParameter(webApp, servlet, VAADIN_UI_CLASS_PARAMETER,
                    applicationClass, "Vaadin UI class to use");

            if (VersionUtil.isVaadin71(vaadinVersion)) {
                addServletInitParameter(
                        webApp,
                        servlet,
                        VAADIN_LEGACY_TOSTRING_PARAMETER,
                        "false",
                        "Legacy mode to return the value of the property as a string from AbstractProperty.toString()");
            }
        } else {
            // Vaadin 6 Application
            addServletInitParameter(webApp, servlet,
                    VAADIN_APPLICATION_CLASS_PARAMETER, applicationClass,
                    "Vaadin application class to start");
        }

        /* Set up servlet mapping */
        ServletMapping servletMapping = WebapplicationFactory.eINSTANCE
                .createServletMapping();
        servletMapping.setUrlPattern(urlPattern);
        servletMapping.setServlet(servlet);

        /* Add the servlet and mapping to webapp */
        webApp.getServlets().add(servlet);
        webApp.getServletMappings().add(servletMapping);

        if (addVaadinMapping) {
            /* Set up /VAADIN mapping if requested */
            servletMapping = WebapplicationFactory.eINSTANCE
                    .createServletMapping();
            servletMapping.setUrlPattern("/VAADIN/*");
            servletMapping.setServlet(servlet);

            /* Add the mapping to webapp */
            webApp.getServletMappings().add(servletMapping);
        }
    }

    /**
     * Adds the given context parameter to web.xml
     * 
     * @param webApp
     * @param name
     * @param value
     * @param desc
     */
    @SuppressWarnings("unchecked")
    public static void addContextParameter(WebApp webApp, String name,
            String value, String desc) {
        if (webApp.getJ2EEVersionID() >= J2EEVersionConstants.J2EE_1_4_ID) {
            ParamValue contextParam = createParameter_2_4(name, value, desc);
            webApp.getContextParams().add(contextParam);
        } else {
            ContextParam contextParam = createContextParameter_2_3(name, value,
                    desc);
            contextParam.setWebApp(webApp);
        }
    }

    @SuppressWarnings("unchecked")
    private static void addServletInitParameter(WebApp webApp, Servlet servlet,
            String name, String value, String desc) {
        if (webApp.getJ2EEVersionID() >= J2EEVersionConstants.J2EE_1_4_ID) {
            ParamValue param = createParameter_2_4(name, value, desc);
            servlet.getInitParams().add(param);
        } else {
            InitParam param = createInitParameter_2_3(name, value, desc);
            servlet.getParams().add(param);
        }
    }

    private static void addOrUpdateServletInitParameter(WebApp webApp,
            Servlet servlet, String name, String value, String desc) {
        if (webApp.getJ2EEVersionID() >= J2EEVersionConstants.J2EE_1_4_ID) {
            ParamValue param = getInitParameter_2_4(servlet, name);
            if (param != null) {
                param.setValue(value);
            } else {
                addServletInitParameter(webApp, servlet, name, value, desc);
            }
        } else {
            InitParam param = getInitParameter_2_3(servlet, name);
            if (param != null) {
                param.setParamValue(value);
            } else {
                addServletInitParameter(webApp, servlet, name, value, desc);
            }
        }

    }

    @SuppressWarnings("rawtypes")
    private static InitParam getInitParameter_2_3(Servlet servlet, String name) {
        EList params = servlet.getParams();
        for (Object object : params) {
            if (object instanceof InitParam) {
                InitParam param = (InitParam) object;
                if (param.getParamName().equals(name)) {
                    return param;
                }
            }
        }

        return null;
    }

    @SuppressWarnings("rawtypes")
    private static ParamValue getInitParameter_2_4(Servlet servlet, String name) {
        EList params = servlet.getInitParams();
        for (Object object : params) {
            ParamValue v = (ParamValue) object;
            if (v.getName().equals(name)) {
                return v;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static ParamValue createParameter_2_4(String name, String value,
            String desc) {

        /* Set up application class parameter */
        ParamValue paramValue = CommonFactory.eINSTANCE.createParamValue();
        paramValue.setName(name);
        paramValue.setValue(value);

        Description description = CommonFactory.eINSTANCE.createDescription();
        description.setValue(desc);
        paramValue.getDescriptions().add(description);

        return paramValue;
    }

    private static InitParam createInitParameter_2_3(String name, String value,
            String desc) {

        /* Set up application class parameter */
        InitParam initParam = WebapplicationFactory.eINSTANCE.createInitParam();
        initParam.setParamName(name);
        initParam.setParamValue(value);
        initParam.setDescription(desc);

        return initParam;
    }

    private static ContextParam createContextParameter_2_3(String name,
            String value, String desc) {

        /* Set up application class parameter */
        ContextParam param = WebapplicationFactory.eINSTANCE
                .createContextParam();
        param.setParamName(name);
        param.setParamValue(value);
        param.setDescription(desc);

        return param;
    }

    /**
     * Updates the widgetset parameter for all Vaadin applications in web.xml
     * 
     * @param artifact
     * @param widgetsetname
     */
    @SuppressWarnings("rawtypes")
    public static void setWidgetSet(WebArtifactEdit artifact,
            String widgetsetname, List<IType> applications) {
        final WebApp root = artifact.getWebApp();

        Set<String> appNames = new HashSet<String>();
        for (IType app : applications) {
            appNames.add(app.getFullyQualifiedName());
        }

        EList servlets = root.getServlets();
        for (Object o : servlets) {
            Servlet servlet = (Servlet) o;

            // update selected applications only
            if (appNames.contains(getVaadinApplicationOrUiClass(root, servlet))) {
                addOrUpdateServletInitParameter(root, servlet,
                        VAADIN_WIDGETSET_PARAMETER, widgetsetname,
                        "Application widgetset");
            }
        }
    }

    /**
     * Returns a map from application names to the corresponding widgetset names
     * in web.xml. If no widgetset is configured for an application, the tuple
     * (application, null) is returned for it.
     * 
     * @param artifact
     * @return map from application name to its configured widgetset name or to
     *         null if no widgetset configured
     */
    @SuppressWarnings("rawtypes")
    @Deprecated
    // TODO now unused but kept as a reference until setWidgetSet() is updated
    private static Map<String, String> getWidgetSetsFromWebXml(
            WebArtifactEdit artifact) {
        Map<String, String> widgetsets = new LinkedHashMap<String, String>();

        final WebApp root = artifact.getWebApp();

        EList servlets = root.getServlets();
        for (Object o : servlets) {
            Servlet servlet = (Servlet) o;

            String appName = getVaadinApplicationOrUiClass(root, servlet);
            if (appName != null) {
                String widgetset = null;
                if (root.getJ2EEVersionID() >= J2EEVersionConstants.J2EE_1_4_ID) {
                    ParamValue param = getInitParameter_2_4(servlet,
                            VAADIN_WIDGETSET_PARAMETER);
                    if (param != null) {
                        widgetset = param.getValue();
                    }
                } else {
                    InitParam param = getInitParameter_2_3(servlet,
                            VAADIN_WIDGETSET_PARAMETER);
                    if (param != null) {
                        widgetset = param.getParamValue();
                    }
                }
                widgetsets.put(appName, widgetset);
            }
        }

        return widgetsets;
    }

    /**
     * Checks if the servlet has an application or UI init parameter and returns
     * the application or UI class name or null if not a Vaadin application.
     * 
     * @param webApp
     * @param servlet
     * @return
     */
    private static String getVaadinApplicationOrUiClass(WebApp webApp,
            Servlet servlet) {
        if (webApp.getJ2EEVersionID() >= J2EEVersionConstants.J2EE_1_4_ID) {
            // application
            ParamValue value = getInitParameter_2_4(servlet,
                    VAADIN_APPLICATION_CLASS_PARAMETER);
            if (value != null) {
                return value.getValue();
            }
            // UI
            value = getInitParameter_2_4(servlet, VAADIN_UI_CLASS_PARAMETER);
            // TODO also support lower case
            if (value != null) {
                return value.getValue();
            }
        } else {
            // application
            InitParam value = getInitParameter_2_3(servlet,
                    VAADIN_APPLICATION_CLASS_PARAMETER);
            if (value != null) {
                return value.getParamValue();
            }
            // UI
            value = getInitParameter_2_3(servlet, VAADIN_UI_CLASS_PARAMETER);
            // TODO also support lower case
            if (value != null) {
                return value.getParamValue();
            }
        }
        return null;
    }
}
