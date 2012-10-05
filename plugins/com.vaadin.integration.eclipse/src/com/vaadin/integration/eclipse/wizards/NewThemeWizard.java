package com.vaadin.integration.eclipse.wizards;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.StringLiteral;
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
import com.vaadin.integration.eclipse.util.ProjectUtil;

/**
 * Wizard to create new theme. If project is selected, it is created there,
 * otherwise to first web project.
 * 
 * Created with eclipse template.
 */

public class NewThemeWizard extends Wizard implements INewWizard {
    private NewThemeWizardPage page;
    private ISelection selection;
    private MethodInvocation setThemeMethod;
    private List<IFile> modifiedJavaFiles;

    /**
     * Constructor for NewThemeWizard.
     */
    public NewThemeWizard() {
        super();
        setWindowTitle("New Vaadin Theme");
        setNeedsProgressMonitor(true);
    }

    /**
     * Adding the page to the wizard.
     */

    @Override
    public void addPages() {
        page = new NewThemeWizardPage(selection);
        addPage(page);
    }

    /**
     * This method is called when 'Finish' button is pressed in the wizard. We
     * will create an operation and run it using wizard as execution context.
     */
    @Override
    public boolean performFinish() {
        // these need to be accessed in the SWT UI thread
        final String themeName = page.getThemeName();
        // this can contain Application (Vaadin 6) and/or UI (Vaadin 7)
        // instances
        final List<IType> appOrUiClassesToModify = page
                .getApplicationAndUiClassesToModify();
        IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor)
                    throws InvocationTargetException {
                try {
                    doFinish(themeName, appOrUiClassesToModify, monitor);
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
    private void doFinish(final String themeName,
            List<IType> appOrUiClassesToModify, IProgressMonitor monitor)
            throws CoreException {
        // create a sample file
        monitor.beginTask("Creating " + themeName, 2);
        IProject project = page.getProject();

        final IFile file = createTheme(project, themeName, monitor);

        // update selected application/UI classes
        modifiedJavaFiles = new ArrayList<IFile>();
        for (IType app : appOrUiClassesToModify) {
            if (null == app) {
                // should not happen
                continue;
            }
            // TODO is this a UI class or an application class? check
            // supertypes?
            boolean isUi = false;
            if (isUi) {
                modifyUiForTheme(project, app, themeName, monitor);
            } else {
                modifyApplicationForTheme(project, app, themeName, monitor);
            }
        }

        monitor.worked(1);
        monitor.setTaskName("Opening file for editing...");
        getShell().getDisplay().asyncExec(new Runnable() {
            public void run() {
                IWorkbenchPage page = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getActivePage();
                try {
                    IDE.openEditor(page, file, true);
                    for (IFile file : modifiedJavaFiles) {
                        IDE.openEditor(page, file);
                    }
                } catch (PartInitException e) {
                }
            }
        });
        monitor.worked(1);
    }

    private IFile createTheme(IProject project, final String themeName,
            IProgressMonitor monitor) throws CoreException {
        String directory = VaadinPlugin.VAADIN_RESOURCE_DIRECTORY;

        IFolder folder = ProjectUtil.getWebContentFolder(project).getFolder(
                directory);
        if (!folder.exists()) {
            folder.create(true, false, monitor);
        }
        folder = ProjectUtil.getWebContentFolder(project).getFolder(directory)
                .getFolder("themes");
        if (!folder.exists()) {
            folder.create(true, false, monitor);
        }
        folder = folder.getFolder(themeName);
        if (folder.exists()) {
            throw ErrorUtil.newCoreException("Theme already exists", null);
        } else {
            folder.create(true, false, monitor);
        }
        final IFile file = folder.getFile(new Path("styles.css"));
        try {
            InputStream stream = openContentStream(VaadinPlugin.VAADIN_DEFAULT_THEME);
            file.create(stream, true, monitor);
            stream.close();
        } catch (IOException e) {
        }

        return file;
    }

    @SuppressWarnings("unchecked")
    private void modifyApplicationForTheme(IProject project, IType app,
            final String themeName, IProgressMonitor monitor)
            throws JavaModelException {
        ICompilationUnit compilationUnit = app.getCompilationUnit();

        IFile javaFile = (IFile) compilationUnit.getCorrespondingResource();
        if (javaFile != null) {
            modifiedJavaFiles.add(javaFile);
        }

        String source = compilationUnit.getSource();
        Document document = new Document(source);

        compilationUnit.becomeWorkingCopy(monitor);
        IMethod method = app.getMethod("init", new String[] {});
        if (method != null) {
            ASTParser parser = ASTParser.newParser(AST.JLS3);
            parser.setSource(compilationUnit);
            parser.setResolveBindings(true);
            CompilationUnit astRoot = (CompilationUnit) parser
                    .createAST(monitor);

            astRoot.recordModifications();
            final AST ast = astRoot.getAST();

            setThemeMethod = null;

            astRoot.accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodInvocation node) {
                    String name = node.getName().toString();
                    if (name.equals("setTheme")) {
                        // found an existing setTheme call
                        setThemeMethod = node;
                    }
                    return super.visit(node);
                }
            });

            final StringLiteral themeString = ast.newStringLiteral();
            themeString.setLiteralValue(themeName);

            if (setThemeMethod != null) {
                // UPDATE existing
                setThemeMethod.arguments().clear();
                setThemeMethod.arguments().add(themeString);
            } else {
                // ADD setTheme method
                astRoot.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(MethodDeclaration node) {
                        String name = node.getName().toString();
                        if (name.equals("init")) {
                            Block body = node.getBody();
                            MethodInvocation setThemeInvocation = ast
                                    .newMethodInvocation();
                            setThemeInvocation.setName(ast
                                    .newSimpleName("setTheme"));
                            setThemeInvocation.arguments().add(themeString);
                            ExpressionStatement es = ast
                                    .newExpressionStatement(setThemeInvocation);

                            body.statements().add(es);
                        }
                        return super.visit(node);
                    }
                });
            }

            TextEdit rewrite = astRoot.rewrite(document, compilationUnit
                    .getJavaProject().getOptions(true));
            try {
                rewrite.apply(document);
            } catch (MalformedTreeException e) {
                ErrorUtil.handleBackgroundException(IStatus.WARNING,
                        "Failed to set the theme in the application class "
                                + app.getFullyQualifiedName(), e);
            } catch (BadLocationException e) {
                ErrorUtil.handleBackgroundException(IStatus.WARNING,
                        "Failed to set the theme in the application class "
                                + app.getFullyQualifiedName(), e);
            }
            String newSource = document.get();
            compilationUnit.getBuffer().setContents(newSource);
        }
    }

    private void modifyUiForTheme(IProject project, IType ui,
            final String themeName, IProgressMonitor monitor)
            throws JavaModelException {
        ICompilationUnit compilationUnit = ui.getCompilationUnit();

        IFile javaFile = (IFile) compilationUnit.getCorrespondingResource();
        if (javaFile != null) {
            modifiedJavaFiles.add(javaFile);
        }

        // TODO implement #8236
    }

    /**
     * We will initialize file contents with a sample text.
     */

    private InputStream openContentStream(String baseTheme) {
        String contents = "@import url(../" + baseTheme + "/styles.css);\n\n";
        return new ByteArrayInputStream(contents.getBytes());
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