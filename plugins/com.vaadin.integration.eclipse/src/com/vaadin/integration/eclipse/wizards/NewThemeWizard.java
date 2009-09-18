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

import com.vaadin.integration.eclipse.util.VaadinPluginUtil;

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
        final String thmeName = page.getThemeName();
        final List<IType> appClassesToModify = page
                .getApplicationClassesToModify();
        IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor)
                    throws InvocationTargetException {
                try {
                    doFinish(thmeName, appClassesToModify, monitor);
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
            MessageDialog.openError(getShell(), "Error", realException
                    .getMessage());
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
            List<IType> appClassesToModify, IProgressMonitor monitor)
            throws CoreException {
        // create a sample file
        monitor.beginTask("Creating " + themeName, 2);
        IProject root = page.getProject();

        String directory = VaadinPluginUtil.getVaadinResourceDirectory(root);

        IFolder folder = VaadinPluginUtil.getWebContentFolder(root).getFolder(
                directory);
        if (!folder.exists()) {
            folder.create(true, false, monitor);
        }
        folder = VaadinPluginUtil.getWebContentFolder(root)
                .getFolder(directory).getFolder("themes");
        if (!folder.exists()) {
            folder.create(true, false, monitor);
        }
        folder = folder.getFolder(themeName);
        if (folder.exists()) {
            throw VaadinPluginUtil.newCoreException("Theme already exists",
                    null);
        } else {
            folder.create(true, false, monitor);
        }
        final IFile file = folder.getFile(new Path("styles.css"));
        try {
            InputStream stream = openContentStream(VaadinPluginUtil
                    .isVaadin6(root) ? "reindeer" : "default");
            file.create(stream, true, monitor);
            stream.close();
        } catch (IOException e) {
        }

        // update selected application classes
        modifiedJavaFiles = new ArrayList<IFile>();
        for (IType app : appClassesToModify) {
            // should not happen
            if (app != null) {
                ICompilationUnit compilationUnit = app.getCompilationUnit();

                IFile javaFile = (IFile) compilationUnit
                        .getCorrespondingResource();
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
                                    setThemeInvocation.arguments().add(
                                            themeString);
                                    ExpressionStatement es = ast
                                            .newExpressionStatement(setThemeInvocation);

                                    body.statements().add(es);
                                }
                                return super.visit(node);
                            }
                        });
                    }

                    TextEdit rewrite = astRoot.rewrite(document,
                            compilationUnit.getJavaProject().getOptions(true));
                    try {
                        rewrite.apply(document);
                    } catch (MalformedTreeException e) {
                        VaadinPluginUtil.handleBackgroundException(
                                IStatus.WARNING,
                                "Failed to set the theme in the application class "
                                        + app.getFullyQualifiedName(), e);
                    } catch (BadLocationException e) {
                        VaadinPluginUtil.handleBackgroundException(
                                IStatus.WARNING,
                                "Failed to set the theme in the application class "
                                        + app.getFullyQualifiedName(), e);
                    }
                    String newSource = document.get();
                    compilationUnit.getBuffer().setContents(newSource);
                }
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