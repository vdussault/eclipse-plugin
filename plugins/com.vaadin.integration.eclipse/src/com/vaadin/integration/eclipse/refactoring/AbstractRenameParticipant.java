package com.vaadin.integration.eclipse.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;

public abstract class AbstractRenameParticipant extends RenameParticipant {
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

        return getRefactorer().createChange(this, pm);
    }

    protected abstract VaadinTextFileRefactorer getRefactorer();

    @Override
    public String getName() {
        return "Vaadin " + getClass().getSimpleName();
    }

    @Override
    protected boolean initialize(Object element) {
        // Do not update references if not requested
        if (!getArguments().getUpdateReferences()) {
            return false;
        }

        return getRefactorer().initializeRename(element,
                getArguments().getNewName(), false);
    }

}
