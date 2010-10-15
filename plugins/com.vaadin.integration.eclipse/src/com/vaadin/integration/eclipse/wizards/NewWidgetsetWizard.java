package com.vaadin.integration.eclipse.wizards;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jst.j2ee.web.componentcore.util.WebArtifactEdit;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;
import org.eclipse.ui.ide.IDE;

import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.LegacyUtil;
import com.vaadin.integration.eclipse.util.ProjectDependencyManager;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;
import com.vaadin.integration.eclipse.util.WebXmlUtil;

/**
 * Wizard to create a new Widgetset
 */

public class NewWidgetsetWizard extends Wizard implements INewWizard {
    private NewWidgetsetWizardPage page;
    private ISelection selection;
    private List<IType> applications;
    private IFile widgetSetXmlFile;

    /**
     * Constructor for NewWidgetsetWizard.
     */
    public NewWidgetsetWizard() {
        super();
        setWindowTitle("New Vaadin Widgetset");
        setNeedsProgressMonitor(true);
    }

    /**
     * Adding the page to the wizard.
     */

    @Override
    public void addPages() {
        // use currently selected project (if applicable)
        IProject project = ProjectUtil.getProject(selection);
        page = new NewWidgetsetWizardPage(project);
        addPage(page);
    }

    /**
     * This method is called when 'Finish' button is pressed in the wizard. We
     * will create an operation and run it using wizard as execution context.
     */
    @Override
    public boolean performFinish() {
        applications = page.getApplicationsToModify();
        IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor)
                    throws InvocationTargetException {
                try {
                    doFinish(monitor);
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }
        };
        try {
            getContainer().run(true, false, op);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            Throwable realException = e.getTargetException();
            MessageDialog.openError(getShell(), "Error",
                    realException.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * The worker method. It will find the container, create the file if missing
     * or just replace its contents, and open the editor on the newly created
     * file.
     */
    private void doFinish(IProgressMonitor monitor) throws CoreException {
        // create a sample file
        monitor.beginTask("Creating widgetset", 10);

        try {
            String packageText = page.getPackageText();
            // widget set goes to sub package "client"
            packageText += ".client";

            ProjectDependencyManager.ensureGWTLibraries(page.getProject(),
                    new SubProgressMonitor(monitor, 5));

            final IPackageFragment packageFragment = page.getPackageFragment();

            IFolder srcFolder = ProjectUtil.getSrcFolder(page.getProject());

            IPackageFragmentRoot root = page.getJavaProject()
                    .getPackageFragmentRoot(srcFolder);

            final IPackageFragment wspf = root.createPackageFragment(
                    packageText, true, monitor);

            // set TypeWizards package to ...client before creating type
            setTypeWizardPackageFragment(wspf);

            try {
                // let TypeWizardPage do the actual widgetset class creation
                page.createType(monitor);
            } catch (CoreException ex) {
                // if the type exists, restore the package location without
                // .client
                setTypeWizardPackageFragment(packageFragment);
                throw ex;
            }

            // create widgetset.gwt.xml file for GWT compiler
            createWidgetSetXMLFile();
            monitor.worked(1);

            // create an external launch configuration
            createBuildScript(monitor);
            monitor.worked(1);

            modifyWebXMLToUsesWidgetSet();
            monitor.worked(1);

            openFiles();
            monitor.worked(2);

        } catch (InterruptedException e) {
            ErrorUtil.handleBackgroundException(IStatus.INFO,
                    "Type creation interrupted", e);
        } finally {
            monitor.done();
        }

    }

    private void setTypeWizardPackageFragment(final IPackageFragment fragment) {
        Display.getDefault().syncExec(new Runnable() {

            public void run() {
                page.setPackageFragment(fragment, true);
            }
        });

    }

    /**
     * Create either an external launch configuration that builds a widgetset
     * and refreshes the build target directory in the workspace
     * 
     * @param monitor
     */
    private void createBuildScript(IProgressMonitor monitor) {

        IType createdType = page.getCreatedType();

        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

        // TODO should this be some other type of launch?
        ILaunchConfigurationType type = manager
                .getLaunchConfigurationType(IExternalToolConstants.ID_PROGRAM_LAUNCH_CONFIGURATION_TYPE);

        try {

            IProject project = page.getProject();

            ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(
                    project, "Compile" + createdType.getElementName());

            // get the project VM or the default java VM path from Eclipse
            IJavaProject jproject = JavaCore.create(project);
            String vmName = VaadinPluginUtil.getJvmExecutablePath(jproject);
            workingCopy.setAttribute(IExternalToolConstants.ATTR_LOCATION,
                    vmName);

            // refresh only the WebContent/VAADIN/widgetsets or
            // WebContent/ITMILL/widgetsets directory
            String resourceDirectory = LegacyUtil
                    .getVaadinResourceDirectory(project);
            IWorkingSetManager workingSetManager = PlatformUI.getWorkbench()
                    .getWorkingSetManager();
            IFolder wsDir = ProjectUtil.getWebContentFolder(project)
                    .getFolder(resourceDirectory).getFolder("widgetsets");
            // refresh this requires that the directory exists
            VaadinPluginUtil.createFolders(wsDir, monitor);

            IWorkingSet workingSet = workingSetManager
                    .createWorkingSet("launchConfigurationWorkingSet",
                            new IAdaptable[] { wsDir });
            workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_SCOPE,
                    RefreshTab.getRefreshAttribute(workingSet));
            // alternatively, could refresh the whole project
            // workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_SCOPE,
            // "${project}");

            workingCopy.setAttribute(
                    IExternalToolConstants.ATTR_WORKING_DIRECTORY,
                    "${project_loc:/" + project.getName() + "}");

            // construct the class path, including GWT JARs and project sources
            String classPath = VaadinPluginUtil.getProjectBaseClasspath(
                    jproject, false);

            // construct rest of the arguments for the launch

            String moduleName = createdType.getFullyQualifiedName();
            moduleName = moduleName.replace(".client.", ".");

            String vmargs = "-Djava.awt.headless=true -Xss8M -Xmx500M";
            if (VaadinPluginUtil.getPlatform().equals("mac")) {
                vmargs += " -XstartOnFirstThread";
            }

            String compilerClass = "com.google.gwt.dev.GWTCompiler";
            if (ProjectUtil.isVaadin6(project)) {
                compilerClass = "com.vaadin.tools.WidgetsetCompiler";
            }

            String wsDirString = wsDir.getProjectRelativePath()
                    .toPortableString();
            String arguments = vmargs + " -classpath \"" + classPath + "\" "
                    + compilerClass + " -out " + wsDirString + " " + moduleName;

            workingCopy.setAttribute(
                    IExternalToolConstants.ATTR_TOOL_ARGUMENTS, arguments);

            // save the launch
            ILaunchConfiguration conf = workingCopy.doSave();

            if (page.compileWidgetset()) {
                conf.launch(ILaunchManager.RUN_MODE, null);
            }

        } catch (CoreException e) {
            ErrorUtil
                    .displayError(
                            "Failed to create a launch configuration for compiling a widgetset",
                            e, getShell());
        }

    }

    private void modifyWebXMLToUsesWidgetSet() {
        /* Update web.xml */

        IProject project = page.getProject();
        WebArtifactEdit artifact = WebArtifactEdit
                .getWebArtifactEditForWrite(project);
        if (artifact == null) {
            ErrorUtil.logWarning("Could not open web.xml for edit.");
            return;
        }

        try {
            String fullyQualifiedName = page.getCreatedType()
                    .getFullyQualifiedName();
            // by convention module name don't include "client" package
            String wsName = fullyQualifiedName.replace(".client.", ".");

            WebXmlUtil.setWidgetSet(artifact, wsName, applications);
            artifact.saveIfNecessary(null);
        } finally {
            artifact.dispose();
        }
    }

    /**
     * Opens gwt module descriptor and widgetset java file
     */
    private void openFiles() {
        getShell().getDisplay().asyncExec(new Runnable() {
            public void run() {
                IWorkbenchPage wbPage = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getActivePage();
                try {
                    // open widget set
                    // if (widgetSetXmlFile != null) {
                    // IDE.openEditor(wbPage, widgetSetXmlFile);
                    // }

                    // open server side widgetset class
                    IType type = page.getCreatedType();
                    if (type != null) {
                        ICompilationUnit compilationUnit = type
                                .getCompilationUnit();
                        IFile javaFile = (IFile) compilationUnit
                                .getCorrespondingResource();
                        IDE.openEditor(wbPage, javaFile, true);
                    }
                } catch (PartInitException e) {
                    ErrorUtil.handleBackgroundException(IStatus.WARNING,
                            "Failed to open created files in editor", e);
                } catch (JavaModelException e) {
                    ErrorUtil.handleBackgroundException(IStatus.WARNING,
                            "Failed to open created files in editor", e);
                }
            }
        });
    }

    private void createWidgetSetXMLFile() throws CoreException {
        IType wsClass = page.getCreatedType();
        try {
            IContainer wsRoot = wsClass.getPackageFragment()
                    .getUnderlyingResource().getParent();

            String gwtxmlstub = VaadinPluginUtil
                    .readTextFromTemplate("widgetsetxmlstub.txt");

            gwtxmlstub = gwtxmlstub.replace("WS_NAME",
                    wsClass.getFullyQualifiedName());

            String superClass = page.getSuperClass();
            String superModudle = superClass.replace(".client.", ".");

            gwtxmlstub = gwtxmlstub.replace("SUPER_WS", superModudle);

            ByteArrayInputStream xmlstream = new ByteArrayInputStream(
                    gwtxmlstub.getBytes());

            String elementName = wsClass.getElementName();

            Path xmlfilepath = new Path(elementName + ".gwt.xml");
            widgetSetXmlFile = wsRoot.getFile(xmlfilepath);
            widgetSetXmlFile.create(xmlstream, true, null);

        } catch (JavaModelException e) {
            throw ErrorUtil.newCoreException(
                    "Failed to create widgetset XML file", e);
        } catch (IOException e) {
            throw ErrorUtil.newCoreException(
                    "Failed to create widgetset XML file", e);
        }
    }

    /**
     * We will accept the selection in the workbench to see if we can initialize
     * from it.
     * 
     * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
     */
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.selection = selection;
    }

    @Override
    public boolean canFinish() {
        // disallow default package
        return super.canFinish() && page.getProject() != null
                && !page.getPackageFragment().isDefaultPackage();
    }
}