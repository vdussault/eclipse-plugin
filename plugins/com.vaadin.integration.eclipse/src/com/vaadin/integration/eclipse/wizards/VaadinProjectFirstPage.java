package com.vaadin.integration.eclipse.wizards;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jst.common.project.facet.IJavaFacetInstallDataModelProperties;
import org.eclipse.jst.common.project.facet.JavaFacetUtils;
import org.eclipse.jst.j2ee.internal.J2EEConstants;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.jst.j2ee.web.project.facet.IWebFacetInstallDataModelProperties;
import org.eclipse.jst.servlet.ui.project.facet.WebFacetInstallPage;
import org.eclipse.jst.servlet.ui.project.facet.WebProjectFirstPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.internal.datamodel.ui.DataModelSynchHelper;

import com.vaadin.integration.eclipse.IVaadinFacetInstallDataModelProperties;
import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.properties.VaadinVersionComposite;

/**
 * The first (main) page of the Vaadin top-level project creation wizard.
 *
 * This replaces WebProjectFirstPage when creating a Vaadin project through its
 * own wizard, and collects the key configuration items on the first page so
 * that the user can typically just click Finish after the first page.
 *
 * Some settings are omitted when creating a project through the Vaadin wizard.
 */
public class VaadinProjectFirstPage extends WebProjectFirstPage implements
        IVaadinFacetInstallDataModelProperties {

    // private Label contextRootLabel;
    // private Text contextRoot;
    private Button useMavenLayoutCheckBox;
    // private Label contentDirLabel;
    private Text contentDir;

    // private Text applicationNameField;
    // private Text applicationClassField;
    // private Text applicationPackageField;

    // private Button applicationCreatePortlet;

    public VaadinProjectFirstPage(IDataModel model, String pageName) {
        super(model, pageName);

        setTitle("Vaadin Project");
        setDescription("Create a Vaadin Dynamic Web project.");
        // setImageDescriptor(J2EEUIPlugin.getDefault().getImageDescriptor(
        // J2EEUIPluginIcons.WEB_PROJECT_WIZARD_BANNER));
    }

    @Override
    protected String getModuleFacetID() {
        return VaadinFacetUtils.VAADIN_FACET_ID;
    }

    @Override
    protected Composite createTopLevelComposite(Composite parent) {
        Composite top = new Composite(parent, SWT.NONE);
        top.setLayout(new GridLayout());
        top.setLayoutData(new GridData(GridData.FILL_BOTH));
        createProjectGroup(top);
        createServerTargetComposite(top);
        createPrimaryFacetComposite(top);
        createPresetPanel(top);

        // Vaadin key settings on the first page
        createVaadinComposite(top);

        // Context root settings
        createContextRootComposite(top);

        // createEarComposite(top);
        // createWorkingSetGroupPanel(top, new String[] { RESOURCE_WORKING_SET,
        // JAVA_WORKING_SET });

        return top;
    }

    // this is partly duplicated in VaadinCoreFacetInstallPage
    protected Composite createVaadinComposite(final Composite parent) {
        final Group group = new Group(parent, SWT.NONE);
        group.setLayoutData(gdhfill());
        group.setLayout(new GridLayout(1, false));
        group.setText("Vaadin"); //$NON-NLS-1$

        // synchronize fields with the Vaadin facet data model instead of the
        // project data model
        FacetDataModelMap map = (FacetDataModelMap) model
                .getProperty(FACET_DM_MAP);
        IDataModel vaadinFacetDataModel = map
                .getFacetDataModel(VaadinFacetUtils.VAADIN_FACET_ID);
        DataModelSynchHelper vaadinFacetSynchHelper = new DataModelSynchHelper(
                vaadinFacetDataModel);

        // Label label = new Label(group, SWT.NONE);
        // label.setLayoutData(gdhfill());
        // label.setText("Application name:");
        //
        // applicationNameField = new Text(group, SWT.BORDER);
        // applicationNameField.setLayoutData(gdhfill());
        // vaadinFacetSynchHelper.synchText(applicationNameField,
        // APPLICATION_NAME, new Control[] { label });
        //
        // label = new Label(group, SWT.NONE);
        // label.setLayoutData(gdhfill());
        // label.setText("Base package name:");
        //
        // applicationPackageField = new Text(group, SWT.BORDER);
        // applicationPackageField.setLayoutData(gdhfill());
        // vaadinFacetSynchHelper.synchText(applicationPackageField,
        // APPLICATION_PACKAGE, new Control[] { label });
        //
        // label = new Label(group, SWT.NONE);
        // label.setLayoutData(gdhfill());
        // label.setText("Application class name:");
        //
        // applicationClassField = new Text(group, SWT.BORDER);
        // applicationClassField.setLayoutData(gdhfill());
        // vaadinFacetSynchHelper.synchText(applicationClassField,
        // APPLICATION_CLASS, new Control[] { label });

        // Vaadin version selection
        VaadinVersionComposite versionComposite = new VaadinVersionComposite(
                group, SWT.NULL);
        versionComposite.createContents();

        versionComposite.setProject(null);
        versionComposite.selectLatestLocalVersion();

        // synch version string to model
        synchHelper.synchCombo(versionComposite.getVersionCombo(),
                VAADIN_VERSION, new Control[] {});

        // portlet creation
        // applicationCreatePortlet = new Button(group, SWT.CHECK);
        // applicationCreatePortlet.setText("Create portlet configuration");
        // applicationCreatePortlet.setLayoutData(gdhfill());
        // vaadinFacetSynchHelper.synchCheckbox(applicationCreatePortlet,
        // CREATE_PORTLET, null);

        return group;
    }

    protected Composite createContextRootComposite(final Composite parent) {
        final Group group = new Group(parent, SWT.NONE);
        group.setLayoutData(gdhfill());
        group.setLayout(new GridLayout(2, false));
        group.setText(WebFacetResources.pageTitle);

        FacetDataModelMap map = (FacetDataModelMap) model
                .getProperty(FACET_DM_MAP);
        IDataModel webFacetDataModel = map
                .getFacetDataModel(IJ2EEFacetConstants.DYNAMIC_WEB);
        DataModelSynchHelper webFacetSynchHelper = new DataModelSynchHelper(
                webFacetDataModel);

        // contextRootLabel = new Label(group, SWT.NONE);
        // contextRootLabel.setText(WebFacetResources.contextRootLabel);
        // contextRootLabel.setLayoutData(new GridData());
        //
        // contextRoot = new Text(group, SWT.BORDER);
        // contextRoot.setLayoutData(gdhfill());
        //        contextRoot.setData("label", contextRootLabel); //$NON-NLS-1$
        // webFacetSynchHelper.synchText(contextRoot,
        // IWebFacetInstallDataModelProperties.CONTEXT_ROOT,
        // new Control[] { contextRootLabel });

        useMavenLayoutCheckBox = new Button(group, SWT.CHECK);
        useMavenLayoutCheckBox.setLayoutData(hspan(gdhfill(), 2));
        useMavenLayoutCheckBox.setText("Use Maven/Google directory layout");
        useMavenLayoutCheckBox.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                useMavenLayout(useMavenLayoutCheckBox.getSelection());
                contentDir.setEnabled(!useMavenLayoutCheckBox.getSelection());
            }

            public void widgetSelected(SelectionEvent e) {
                useMavenLayout(useMavenLayoutCheckBox.getSelection());
                contentDir.setEnabled(!useMavenLayoutCheckBox.getSelection());
            }
        });

        // contentDirLabel = new Label(group, SWT.NONE);
        // contentDirLabel.setText(WebFacetResources.contentDirLabel);
        // contentDirLabel.setLayoutData(new GridData());
        //
        // contentDir = new Text(group, SWT.BORDER);
        // contentDir.setLayoutData(gdhfill());
        //        contentDir.setData("label", contentDirLabel); //$NON-NLS-1$
        // webFacetSynchHelper.synchText(contentDir,
        // IWebFacetInstallDataModelProperties.CONFIG_FOLDER, null);

        return group;
    }

    private void useMavenLayout(boolean mavenLayout) {
        // use Maven/GAE directory layout:
        // - context root dir: war
        // - default output dir: war/WEB-INF/classes
        // - src dir: src/main/java

        FacetDataModelMap map = (FacetDataModelMap) model
                .getProperty(FACET_DM_MAP);

        IDataModel webFacet = map
                .getFacetDataModel(IJ2EEFacetConstants.DYNAMIC_WEB);
        IDataModel javaFacet = map.getFacetDataModel(JavaFacetUtils.JAVA_FACET
                .getId());

        // set the content root directory for Maven or the Google plugin
        String webRoot = mavenLayout ? "war" : "WebContent";
        webFacet.setStringProperty(
                IWebFacetInstallDataModelProperties.CONFIG_FOLDER, webRoot);

        // Maven convention, which also works for Google GWT/GAE projects
        String webSrc = mavenLayout ? "src/main/java" : "src";
        webFacet.setStringProperty(
                IWebFacetInstallDataModelProperties.SOURCE_FOLDER, webSrc);
        javaFacet
                .setProperty(
                        IJavaFacetInstallDataModelProperties.SOURCE_FOLDER_NAME,
                        webSrc);

        // set the output folder to "<content folder>/WEB-INF/classes"
        String outputDir = mavenLayout ? webRoot + "/"
                + J2EEConstants.WEB_INF_CLASSES : "build/classes";
        javaFacet
                .setProperty(
                        IJavaFacetInstallDataModelProperties.DEFAULT_OUTPUT_FOLDER_NAME,
                        outputDir); //$NON-NLS-1$
    }

    @Override
    protected String[] getValidationPropertyNames() {
        String[] superProperties = super.getValidationPropertyNames();
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(Arrays.asList(superProperties));
        // validation of these relies on nested models in the project level
        // model - see VaadinProjectCreationDataModelProvider
        arrayList.add(APPLICATION_NAME);
        arrayList.add(APPLICATION_PACKAGE);
        arrayList.add(APPLICATION_CLASS);
        arrayList.add(IWebFacetInstallDataModelProperties.CONTEXT_ROOT);
        // validating this leads to strange behavior for Finish button
        // enabling/disabling when changing the value
        // arrayList.add(IWebFacetInstallDataModelProperties.CONFIG_FOLDER);
        return (String[]) arrayList.toArray(new String[0]);
    }

    private static final class WebFacetResources extends NLS {
        public static String pageTitle;
        // public static String pageDescription;
        public static String contextRootLabel;
        public static String contextRootLabelInvalid;
        public static String contentDirLabel;
        public static String contentDirLabelInvalid;

        static {
            initializeMessages(WebFacetInstallPage.class.getName(),
                    WebFacetResources.class);
        }
    }

}
