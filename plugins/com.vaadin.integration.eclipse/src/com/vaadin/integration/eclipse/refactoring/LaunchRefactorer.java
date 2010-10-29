package com.vaadin.integration.eclipse.refactoring;

import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;

@SuppressWarnings("restriction")
public class LaunchRefactorer extends VaadinTextFileRefactorer {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant#
     * createChange(org.eclipse.core.runtime.IProgressMonitor)
     */
    @SuppressWarnings("deprecation")
    @Override
    public Change createChange(RefactoringParticipant refactoringParticipant,
            IProgressMonitor pm) throws CoreException,
            OperationCanceledException {
        if (getSourceProject() == null) {
            return null;
        }

        CompositeChange result = new CompositeChange(
                "Launch configuration refactoring");

        // iterate over external launch configuration files in the source
        // project
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType type = manager
                .getLaunchConfigurationType(IExternalToolConstants.ID_PROGRAM_LAUNCH_CONFIGURATION_TYPE);

        // limit to launches that are top-level resources in the project of
        // interest, anything else is up to the user
        for (IResource resource : getSourceProject().members()) {
            // identify the external launches of the correct type
            if (resource instanceof IFile
                    && "launch".equals(resource.getFileExtension())) {
                ILaunchConfiguration launchConfiguration = manager
                        .getLaunchConfiguration((IFile) resource);
                if (launchConfiguration != null && launchConfiguration.exists()
                        && type.equals(launchConfiguration.getType())) {
                    // create a change for the launch configuration file and add
                    // the resulting change (if not empty) to overall composite
                    // change

                    Change launchChange = createLaunchChange((IFile) resource,
                            pm);
                    if (launchChange != null) {
                        result.add(launchChange);
                    }
                }
            }
        }

        if (result.getChildren().length > 0) {
            return result;
        } else {
            return null;
        }
    }

    private Change createLaunchChange(IFile launchFile, IProgressMonitor pm) {
        // update the widgetset path if the package name changed, treating the
        // launch as a text file

        // TODO should use RefactoringParticipant,getTextChange() if it is
        // possible that multiple refactoring participants change the same
        // launch configuration.

        // TODO does not handle class name change nor widgetset XML name change

        TextChange change = new TextFileChange(launchFile.getName(), launchFile);
        change.setEdit(new MultiTextEdit());

        TextSearchEngine textSearchEngine = TextSearchEngine.create();
        IFile[] scope = new IFile[] { launchFile };

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
            Pattern pattern = getArgumentsParamValuePattern(fromRegexp);

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
     * Returns a pattern that matches an external launch arguments definition
     * containing a package name which matches the packageNameString parameter.
     * 
     * @param packageNameString
     * @return
     */
    private static Pattern getArgumentsParamValuePattern(
            String packageNameString) {
        Pattern pattern = Pattern
                .compile(
                        "<stringAttribute(\\s*)key=\"org.eclipse.ui.externaltools.ATTR_TOOL_ARGUMENTS\"(\\s*)value=\"[^\"]*"
                                + packageNameString + "[^\"]*/>",
                        Pattern.DOTALL);

        return pattern;

    }

}
