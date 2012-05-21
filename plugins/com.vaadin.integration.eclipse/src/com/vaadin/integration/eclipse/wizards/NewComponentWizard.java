package com.vaadin.integration.eclipse.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
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

import com.vaadin.integration.eclipse.builder.WidgetsetBuildManager;
import com.vaadin.integration.eclipse.templates.TEMPLATES;
import com.vaadin.integration.eclipse.templates.Template;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.ProjectDependencyManager;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.WidgetsetUtil;

public class NewComponentWizard extends Wizard implements INewWizard {
    private NewComponentWizardPage page;
    private ISelection selection;

    // created files, will be opened in the IDE
    private List<IFile> createdFiles = new LinkedList<IFile>();

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

        TEMPLATES template = page.getTemplate();
        if (template.hasClientTemplates()) {
            ProjectDependencyManager.ensureGWTLibraries(page.getProject(),
                    new SubProgressMonitor(monitor, 5));

            buildClientSideClass(template, monitor);
            monitor.worked(1);

        }

        openFiles();
        monitor.worked(1);

        // Trigger widgetset compilation dialog
        IProject project = page.getProject();
        if (WidgetsetUtil.isWidgetsetDirty(project)) {
            WidgetsetBuildManager.runWidgetSetBuildTool(project, false,
                    new NullProgressMonitor());
        }

    }

    private void buildClientSideClass(TEMPLATES template,
            IProgressMonitor monitor) throws CoreException {
        // we know that a client side widget should be built
        String widgetSetName;
        if (!template.isSuitableFor(6)) {
            // 6.2+
            // Create the widgetset if it did not exist. This way, the user can
            // immediately move the widgetset package instead of needing to
            // compile widgetset first; the package is determined based on the
            // new widget's package
            widgetSetName = WidgetsetUtil.getWidgetSet(page.getJavaProject(),
                    true, page.getPackageFragmentRoot(), page.getPackageText(),
                    monitor);
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
            if (!template.isSuitableFor(6)) {
                // 6.2+ (remove typename)
                packageName = widgetSetName.replaceAll("\\.[^\\.]+$", "");
            } else {
                widgetSet = javaProject.findType(widgetSetName);
                if (widgetSet == null) {
                    throw ErrorUtil.newCoreException("No widgetset selected",
                            null);
                }
                IPackageFragment packageFragment = widgetSet
                        .getPackageFragment();
                packageName = packageFragment.getElementName(); // + ".ui";
            }

            // Server-side component location, e.g com.example.MyComponent
            String componentPackage = page.getPackageFragment()
                    .getElementName();

            // Server-side component extends, e.g
            // com.vaadin.ui.AbstractComponent
            String componentExtends = page.getSuperClass();

            // Figure what State should extend, e.g
            // com.vaadin.terminal.gwt.client.ComponentState
            String stateExtends = null;
            if (template.hasState()) {
                IType extType = javaProject.findType(componentExtends);
                IMethod getStateMethod = extType.getMethod("getState", null);
                while (!getStateMethod.exists()) {
                    String parentName = extType.getSuperclassName();
                    if (parentName == null) {
                        break;
                    }
                    extType = javaProject.findType(parentName);
                    getStateMethod = extType.getMethod("getState", null);
                }
                if (getStateMethod.exists()) {
                    stateExtends = getStateMethod.getReturnType();
                    stateExtends = Signature.toString(stateExtends);
                } else {
                    stateExtends = "com.vaadin.terminal.gwt.client.ComponentState";
                }
            }

            // run all templates
            for (Class<Template> c : template.getClientTemplates()) {
                Template t = (Template) c.newInstance();
                String src = t.generate(typeName, componentPackage,
                        componentExtends, stateExtends, packageName, template);

                IPackageFragment targetPackage = packageFragmentRoot
                        .createPackageFragment(t.getTarget(), true, null);
                final ICompilationUnit clientSideClass = targetPackage
                        .createCompilationUnit(t.getFileName(), src, false,
                                null);

                createdFiles.add((IFile) clientSideClass
                        .getCorrespondingResource());
            }
            if (template.isSuitableFor(6.0) && template.hasWidget()) {
                // 6.0 requires widgetset class to be updated, after 6.2
                // this is handled by the compiler.
                // NOTE the name of the created client-side widget is 'guessed'
                // here! Not likely to be changed, but if you do...
                updateWidgetsetClass(monitor, widgetSet, packageName + ".ui",
                        "V" + typeName);
            }

            // refresh whole thing, as we could have created stuff anywhere
            packageFragmentRoot.getResource().refreshLocal(
                    IResource.DEPTH_INFINITE, monitor);

        } catch (JavaModelException e) {
            throw ErrorUtil.newCoreException(
                    "Failed to create client side class", e);
        } catch (InstantiationException e) {
            throw ErrorUtil.newCoreException("Failed to instantiate template",
                    e);
        } catch (IllegalAccessException e) {
            throw ErrorUtil.newCoreException("IllegalAccess (plugin problem)",
                    e);
        }

    }

    private void updateWidgetsetClass(IProgressMonitor monitor,
            IType widgetSet, final String packageName, final String simpleName)
            throws JavaModelException, CoreException {
        ICompilationUnit compilationUnit = null;
        boolean openWorkingCopy = false;
        try {

            compilationUnit = widgetSet.getCompilationUnit();

            createdFiles
                    .add((IFile) compilationUnit.getCorrespondingResource());

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
                        // SimpleType newSimpleType =
                        ast.newSimpleType(sn);

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
     * Opens created files in the IDE
     */
    private void openFiles() {
        getShell().getDisplay().asyncExec(new Runnable() {
            public void run() {
                IWorkbenchPage wbPage = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getActivePage();
                try {
                    for (IFile file : createdFiles) {
                        IDE.openEditor(wbPage, file);
                    }
                } catch (PartInitException e) {
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
