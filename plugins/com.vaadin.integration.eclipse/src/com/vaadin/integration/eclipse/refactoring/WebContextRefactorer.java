package com.vaadin.integration.eclipse.refactoring;

import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.WebXmlUtil;

public class WebContextRefactorer extends VaadinTextFileRefactorer {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant#
     * createChange(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public Change createChange(RefactoringParticipant refactoringParticipant,
            IProgressMonitor pm) throws CoreException,
            OperationCanceledException {
        if (getSourceProject() == null) {
            return null;
        }
        IFile webXml = ProjectUtil.getWebInfFolder(getSourceProject()).getFile(
                "web.xml");
        if (webXml == null) {
            return null;
        }

        // TODO could also handle widgetset XML file name change - other
        // refactoring API?

        TextChange change = refactoringParticipant.getTextChange(webXml);
        boolean createdChange = false;
        if (change == null) {
            createdChange = true;
            change = new TextFileChange("web.xml", webXml);
            change.setEdit(new MultiTextEdit());
        }

        TextSearchEngine textSearchEngine = TextSearchEngine.create();
        IFile[] scope = new IFile[] { webXml };

        for (String from : getClassRenames().keySet()) {
            String to = getClassRenames().get(from);

            // System.err.println("Renaming class: " + from + " -> " + to);
            String escapedFrom = from.replace(".", "\\.").replace("$", "\\$");

            /*
             * Match either the class name only, or className$something (for
             * inner classes)
             */
            String fromRegexp = escapedFrom + "(\\$([^\\.]*))?";
            Pattern pattern = getApplicationParamValuePattern(fromRegexp);

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
            Pattern pattern = getApplicationParamValuePattern(fromRegexp);

            TextReplacer replacer = new TextReplacer(from, to);
            textSearchEngine.search(scope, replacer, pattern, pm);

            for (TextEdit edit : replacer.getEdits()) {
                change.getEdit().addChild(edit);
            }

        }

        if (createdChange && change.getEdit().getChildrenSize() > 0) {
            return change;
        } else {
            return null;
        }
    }

    /**
     * Returns a pattern that matches a servlet definition for the
     * ApplicationServlet containing an application value which matches the
     * applicationString parameter.
     * 
     * @param applicationString
     * @return
     */
    private static Pattern getApplicationParamValuePattern(
            String applicationString) {
        String applicationServletClassRegexp = "(("
                + WebXmlUtil.VAADIN_SERVLET_CLASS.replace(".", "\\.") + ")|("
                + WebXmlUtil.VAADIN_GAE_SERVLET_CLASS.replace(".", "\\.")
                + "))";
        Pattern pattern = Pattern.compile("<servlet>"
                + "(.*?)<servlet-class>(\\s*)" + "(com\\.vaadin\\.)"
                + applicationServletClassRegexp + "(\\s*)</servlet-class>"
                + "(.*?)<param-value>(\\s*)" + applicationString
                + "(\\s*)</param-value>" + "(.*?)</servlet>", Pattern.DOTALL);

        return pattern;

    }

}
