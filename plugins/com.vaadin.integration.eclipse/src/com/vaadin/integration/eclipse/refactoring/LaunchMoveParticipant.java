package com.vaadin.integration.eclipse.refactoring;


public class LaunchMoveParticipant extends AbstractMoveParticipant {

    private LaunchRefactorer refactorer = new LaunchRefactorer();

    @Override
    public LaunchRefactorer getRefactorer() {
        return refactorer;
    }

}
