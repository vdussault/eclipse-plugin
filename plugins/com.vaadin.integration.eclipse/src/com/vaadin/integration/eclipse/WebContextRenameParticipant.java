package com.vaadin.integration.eclipse;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;

public class WebContextRenameParticipant extends
        org.eclipse.ltk.core.refactoring.participants.RenameParticipant {

    private WebContextRefactorer refactorer = new WebContextRefactorer();

    @Override
    public RefactoringStatus checkConditions(IProgressMonitor pm,
            CheckConditionsContext context) throws OperationCanceledException {
        return new RefactoringStatus();
    }

    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException,
            OperationCanceledException {
        if (!getArguments().getUpdateReferences()) {
            return null;
        }

        return refactorer.createChange(pm);
    }

    @Override
    public String getName() {
        return "Vaadin Web Context Rename Participant";
    }

    @Override
    protected boolean initialize(Object element) {
        // Do not update references if not requested
        if (!getArguments().getUpdateReferences()) {
            return false;
        }

        return refactorer.initializeRename(element,
                getArguments().getNewName(), false);
    }

}
