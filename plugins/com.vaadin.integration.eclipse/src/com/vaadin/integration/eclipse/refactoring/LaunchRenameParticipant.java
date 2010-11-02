package com.vaadin.integration.eclipse.refactoring;


public class LaunchRenameParticipant extends AbstractRenameParticipant {

    private LaunchRefactorer refactorer = new LaunchRefactorer();

    @Override
    public LaunchRefactorer getRefactorer() {
        return refactorer;
    }

}
