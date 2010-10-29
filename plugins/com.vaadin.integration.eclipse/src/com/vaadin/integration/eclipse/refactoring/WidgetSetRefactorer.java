package com.vaadin.integration.eclipse.refactoring;

import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

public class WidgetSetRefactorer extends VaadinTextFileRefactorer {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant#
     * createChange(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public Change createChange(RefactoringParticipant refactoringParticipant,
            final IProgressMonitor pm) throws CoreException,
            OperationCanceledException {
        if (getSourceProject() == null) {
            return null;
        }

        final CompositeChange result = new CompositeChange(
                "Widgetset refactoring");

        // iterate over widgetsets (*.gwt.xml) in the source project
        getSourceProject().accept(new IResourceProxyVisitor() {

            public boolean visit(IResourceProxy resourceProxy)
                    throws CoreException {
                if (resourceProxy.isDerived()) {
                    return false;
                }
                switch (resourceProxy.getType()) {
                case IResource.PROJECT:
                    return true;
                case IResource.FOLDER:
                    return true;
                case IResource.FILE:
                    if (resourceProxy.getName().endsWith(".gwt.xml")) {
                        // widgetset file - refactoring of entry point
                        Change widgetSetChange = createWidgetSetChange(
                                (IFile) resourceProxy.requestResource(), pm);
                        if (widgetSetChange != null) {
                            result.add(widgetSetChange);
                        }
                    }
                    break;
                default:
                    break;
                }
                return false;
            }
        }, IResource.NONE);

        if (result.getChildren().length > 0) {
            return result;
        } else {
            return null;
        }
    }

    private Change createWidgetSetChange(IFile widgetSetXmlFile,
            IProgressMonitor pm) {
        // update the widgetset path if the package name changed, treating the
        // widgetset XML as a text file

        // TODO should use RefactoringParticipant,getTextChange() if it is
        // possible that multiple refactoring participants change the same
        // launch configuration.

        TextChange change = new TextFileChange(widgetSetXmlFile.getName()
                .replaceAll("\\.gwt.xml$", ""), widgetSetXmlFile);
        change.setEdit(new MultiTextEdit());

        TextSearchEngine textSearchEngine = TextSearchEngine.create();
        IFile[] scope = new IFile[] { widgetSetXmlFile };

        for (String from : getClassRenames().keySet()) {
            String to = getClassRenames().get(from);
            // System.err.println("Renaming class: " + from + " -> " + to);

            String escapedFrom = from.replace(".", "\\.").replace("$", "\\$");

            /*
             * Match either the class name only, or className$something (for
             * inner classes)
             */
            String fromRegexp = escapedFrom + "(\\$([^\\.]*))?";
            Pattern pattern = getEntryPointPattern(fromRegexp);

            TextReplacer replacer = new TextReplacer(from, to);
            textSearchEngine.search(scope, replacer, pattern, pm);

            for (TextEdit edit : replacer.getEdits()) {
                change.getEdit().addChild(edit);
            }

        }

        for (String from : getPackageRenames().keySet()) {
            String to = getPackageRenames().get(from);
            // System.err.println("Renaming package: " + from + " -> " + to);

            // Rename all application classes of the type "from.<class>"
            String escapedFrom = from.replace(".", "\\.");
            /*
             * Only match strings that starts with "from" and continues with a
             * dot
             */
            String fromRegexp = escapedFrom + "\\.([^\\.]*)";
            Pattern pattern = getEntryPointPattern(fromRegexp);

            TextReplacer replacer = new TextReplacer(from, to);
            textSearchEngine.search(scope, replacer, pattern, pm);

            for (TextEdit edit : replacer.getEdits()) {
                change.getEdit().addChild(edit);
            }

        }

        if (change.getEdit().getChildrenSize() > 0) {
            return change;
        } else {
            return null;
        }
    }

    /**
     * Returns a pattern that matches the widgetset entry point class name that
     * contains a string matching pattern inside double quotes. This can be e.g.
     * in the entry point or inherits tags.
     * 
     * @param classNamePattern
     *            class name pattern for entry point class, matching the whole
     *            fully qualified class name
     * @return
     */
    private static Pattern getEntryPointPattern(String classNamePattern) {
        Pattern pattern = Pattern.compile("\"" + classNamePattern + "\"",
                Pattern.DOTALL);

        return pattern;

    }

}
