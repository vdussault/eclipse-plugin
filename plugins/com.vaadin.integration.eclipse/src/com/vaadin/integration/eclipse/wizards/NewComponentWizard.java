package com.vaadin.integration.eclipse.wizards;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.LegacyUtil;
import com.vaadin.integration.eclipse.util.ProjectDependencyManager;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;
import com.vaadin.integration.eclipse.wizards.NewComponentWizardPage.TEMPLATE;

public class NewComponentWizard extends Wizard implements INewWizard {
    private NewComponentWizardPage page;
    private ISelection selection;
    private IFile widgetSetJavaFile;
    private IFile clientSideJavaFile;

    /**
     * Constructor for new Component wizard.
     */
    public NewComponentWizard() {
        super();
        setWindowTitle("New Vaadin Widget");
        setNeedsProgressMonitor(true);
    }

    /**
     * Adding the page to the wizard.
     */

    @Override
    public void addPages() {
        IProject project = ProjectUtil.getProject(selection);
        page = new NewComponentWizardPage(project);
        addPage(page);
    }

    /**
     * This method is called when 'Finish' button is pressed in the wizard. We
     * will create an operation and run it using wizard as execution context.
     */
    @Override
    public boolean performFinish() {
        IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor)
                    throws InvocationTargetException {
                try {
                    doFinish(monitor);
                } catch (CoreException e) {
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
        monitor.beginTask("Creating widget", 10);

        try {

            TEMPLATE template = page.getTemplate();
            if (template.hasClientWidget()) {
                ProjectDependencyManager.ensureGWTLibraries(page.getProject(),
                        new SubProgressMonitor(monitor, 5));

                buildClientSideClass(template, monitor);
                monitor.worked(1);

            }

            // let TypeWizardPage do the actual widget class creation
            page.createType(monitor);
            monitor.worked(2);

            openFiles();
            monitor.worked(2);

        } catch (InterruptedException e) {
            ErrorUtil.displayError("Failed to create widget", e, getShell());
        }

    }

    private void buildClientSideClass(TEMPLATE template,
            IProgressMonitor monitor) throws CoreException {
        // we know that a client side widget should be built
        String widgetSetName;
        if (template.isVaadin62()) {
            // Create the widgetset if it did not exist. This way, the user can
            // immediately move the widgetset package instead of needing to
            // compile widgetset first; the package is determined based on the
            // new widget's package
            widgetSetName = VaadinPluginUtil.getWidgetSet(
                    page.getJavaProject(), true, page.getPackageFragmentRoot(),
                    page.getPackageText(), monitor);
        } else {
            widgetSetName = page.getWidgetSetName();
        }
        IJavaProject javaProject = page.getJavaProject();

        String typeName = page.getTypeName();

        try {
            IPackageFragmentRoot packageFragmentRoot = page
                    .getPackageFragmentRoot();
            IType widgetSet = null;
            final String packageName;
            if (template.isVaadin62()) {
                packageName = widgetSetName.replaceAll("\\.[^\\.]+$",
                        ".client.ui");
            } else {
                widgetSet = javaProject.findType(widgetSetName);

                if (widgetSet == null) {
                    throw ErrorUtil.newCoreException("No widgetset selected",
                            null);
                }

                IPackageFragment packageFragment = widgetSet
                        .getPackageFragment();
                packageName = packageFragment.getElementName() + ".ui";

            }

            IPackageFragment uiPackage = packageFragmentRoot
                    .createPackageFragment(packageName, true, null);

            String vaadinPackagePrefix = LegacyUtil
                    .getVaadinPackagePrefix(javaProject.getProject());

            String iComponentStub = VaadinPluginUtil
                    .readTextFromTemplate("component/"
                            + template.getClientTemplate() + ".txt");

            // choose "V" or "I" based on project type (Vaadin or IT Mill
            // Toolkit)
            String clientSidePrefix;
            if (vaadinPackagePrefix.equals(VaadinPlugin.TOOLKIT_PACKAGE_PREFIX)) {
                clientSidePrefix = "I";
            } else {
                clientSidePrefix = "V";
            }
            final String simpleName = clientSidePrefix + typeName;

            iComponentStub = iComponentStub.replaceAll("STUB_CLASSNAME",
                    simpleName);

            iComponentStub = iComponentStub.replaceAll("STUB_PACKAGE",
                    packageName);

            iComponentStub = iComponentStub.replaceAll("STUB_VAADIN_PREFIX",
                    vaadinPackagePrefix);

            iComponentStub = iComponentStub.replaceAll(
                    "STUB_CLIENT_SIDE_PREFIX", clientSidePrefix.toLowerCase());

            iComponentStub = iComponentStub.replaceAll("STUB_TAGNAME",
                    typeName.toLowerCase());

            final ICompilationUnit clientSideClass = uiPackage
                    .createCompilationUnit(simpleName + ".java",
                            iComponentStub, false, null);
            page.setCreatedClientSideClass(clientSideClass);
            clientSideJavaFile = (IFile) clientSideClass
                    .getCorrespondingResource();

            if (!template.isVaadin62()) {
                // if old style widgetset,
                // modify widgetset to include newly created class

                updateWidgetsetClass(monitor, widgetSet, packageName,
                        simpleName);
            } else {
                // the widgetset compiler (generator) takes care of this
            }

            // refresh subtree
            if (uiPackage.getParent() != null
                    && uiPackage.getParent().getParent() != null) {
                IJavaElement toRefresh = uiPackage.getParent().getParent();
                toRefresh.getResource().refreshLocal(IResource.DEPTH_INFINITE,
                        monitor);
            } else {
                packageFragmentRoot.getResource().refreshLocal(
                        IResource.DEPTH_INFINITE, monitor);
            }

        } catch (JavaModelException e) {
            throw ErrorUtil.newCoreException(
                    "Failed to create client side class", e);
        } catch (IOException e) {
            throw ErrorUtil.newCoreException(
                    "Failed to create client side class", e);
        }

    }

    private void updateWidgetsetClass(IProgressMonitor monitor,
            IType widgetSet, final String packageName, final String simpleName)
            throws JavaModelException, CoreException {
        ICompilationUnit compilationUnit = null;
        boolean openWorkingCopy = false;
        try {

            IMethod[] methods = widgetSet.getMethods();
            // FIXME: What is the point of this variable as it is never read?
            boolean hasAutogeneratedBlock = false;
            for (int i = 0; i < methods.length; i++) {
                IMethod m = methods[i];
                if (m.getElementName().equals("resolveWidgetType")) {
                    String source = m.getSource();
                    if (source.contains("AUTOGENERATED")) {
                        hasAutogeneratedBlock = true;
                        break;
                    }
                }
            }

            compilationUnit = widgetSet.getCompilationUnit();

            widgetSetJavaFile = (IFile) compilationUnit
                    .getCorrespondingResource();

            final String source = compilationUnit.getSource();
            Document document = new Document(source);

            compilationUnit.becomeWorkingCopy(monitor);
            openWorkingCopy = true;
            ASTParser parser = ASTParser.newParser(AST.JLS3);
            parser.setSource(compilationUnit);
            parser.setResolveBindings(true);
            final CompilationUnit astRoot = (CompilationUnit) parser
                    .createAST(monitor);

            astRoot.recordModifications();
            final AST ast = astRoot.getAST();

            astRoot.accept(new ASTVisitor() {

                @SuppressWarnings({ "unchecked", "rawtypes" })
                @Override
                public boolean visit(MethodDeclaration node) {
                    SimpleName name = node.getName();
                    String fullyQualifiedName = name.getFullyQualifiedName();
                    if (fullyQualifiedName.equals("createWidgetByClass")) {
                        List statements = node.getBody().statements();
                        boolean emptyIfElseClausule = statements.size() == 0;

                        IfStatement createWidgetStatement = ast
                                .newIfStatement();

                        if (!emptyIfElseClausule) {
                            assert statements.size() == 1;
                            Statement oldStatement = (Statement) statements
                                    .get(0);
                            // remove the old statement from dom before
                            // adding it again
                            statements.clear();
                            createWidgetStatement
                                    .setElseStatement(oldStatement);
                        }
                        statements.add(createWidgetStatement);

                        InfixExpression expression = ast.newInfixExpression();
                        expression.setOperator(Operator.EQUALS);
                        SimpleName classType = ast.newSimpleName("classType");
                        expression.setLeftOperand(classType);
                        Name clientSideWidgetName = ast.newName(simpleName);
                        SimpleType icomponenttype = ast
                                .newSimpleType(clientSideWidgetName);

                        TypeLiteral componentTypeClass = ast.newTypeLiteral();
                        componentTypeClass.setType(icomponenttype);

                        expression.setRightOperand(componentTypeClass);
                        createWidgetStatement.setExpression(expression);
                        ReturnStatement statement = ast.newReturnStatement();
                        ClassInstanceCreation retExpr = ast
                                .newClassInstanceCreation();

                        SimpleName sn = ast.newSimpleName(simpleName);
                        SimpleType newSimpleType = ast.newSimpleType(sn);

                        retExpr.setType(newSimpleType);
                        statement.setExpression(retExpr);
                        Block newBlock = ast.newBlock();
                        newBlock.statements().add(statement);
                        createWidgetStatement.setThenStatement(newBlock);

                        return false;
                    } else if (fullyQualifiedName.equals("resolveWidgetByTag")) {
                        List statements = node.getBody().statements();
                        boolean emptyIfElseClausule = statements.size() == 0;

                        IfStatement createWidgetStatement = ast
                                .newIfStatement();

                        if (!emptyIfElseClausule) {
                            assert statements.size() == 1;
                            Statement oldStatement = (Statement) statements
                                    .get(0);
                            // remove the old statement from dom before
                            // adding it again
                            statements.clear();
                            createWidgetStatement
                                    .setElseStatement(oldStatement);
                        }
                        statements.add(createWidgetStatement);

                        MethodInvocation expression = ast.newMethodInvocation();
                        SimpleName classType = ast.newSimpleName(simpleName);
                        FieldAccess fieldAccess = ast.newFieldAccess();
                        fieldAccess.setExpression(classType);
                        fieldAccess.setName(ast.newSimpleName("TAGNAME"));
                        expression.setExpression(fieldAccess);
                        expression.setName(ast.newSimpleName("equals"));
                        expression.arguments().add(ast.newSimpleName("tag"));

                        createWidgetStatement.setExpression(expression);

                        SimpleName sn = ast.newSimpleName(simpleName);
                        // FIXME: Is this needed? newSimpleType is never used
                        SimpleType newSimpleType = ast.newSimpleType(sn);

                        ReturnStatement statement = ast.newReturnStatement();

                        Name clientSideWidgetName = ast.newName(simpleName);
                        SimpleType icomponenttype = ast
                                .newSimpleType(clientSideWidgetName);

                        TypeLiteral componentTypeClass = ast.newTypeLiteral();
                        componentTypeClass.setType(icomponenttype);

                        statement.setExpression(componentTypeClass);

                        Block newBlock = ast.newBlock();
                        newBlock.statements().add(statement);
                        createWidgetStatement.setThenStatement(newBlock);

                        return false;
                    }

                    return super.visit(node);
                }
            });

            TextEdit rewrite = astRoot.rewrite(document, compilationUnit
                    .getJavaProject().getOptions(true));

            try {
                rewrite.apply(document);

                ImportRewrite iRewrite = ImportRewrite.create(astRoot, true);

                iRewrite.addImport(packageName + "." + simpleName);

                TextEdit rewriteImports = iRewrite.rewriteImports(monitor);
                rewriteImports.apply(document);

            } catch (MalformedTreeException e) {
                throw ErrorUtil.newCoreException(
                        "Failed to create client side class", e);
            } catch (BadLocationException e) {
                throw ErrorUtil.newCoreException(
                        "Failed to create client side class", e);
            }
            String newSource = document.get();
            compilationUnit.getBuffer().setContents(newSource);

            // this also saves the file
            compilationUnit.commitWorkingCopy(false, monitor);

        } finally {
            if (openWorkingCopy) {
                compilationUnit.discardWorkingCopy();
            }
        }
    }

    /**
     * Opens component and optionally client side stub java files
     */
    private void openFiles() {
        getShell().getDisplay().asyncExec(new Runnable() {
            public void run() {
                IWorkbenchPage wbPage = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getActivePage();
                try {
                    // open client side stub
                    if (clientSideJavaFile != null) {
                        IDE.openEditor(wbPage, clientSideJavaFile);
                    }

                    // open widget set
                    if (widgetSetJavaFile != null) {
                        IDE.openEditor(wbPage, widgetSetJavaFile);
                    }

                    // open server side component class
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
        return super.canFinish() && page.getProject() != null;
    }
}
