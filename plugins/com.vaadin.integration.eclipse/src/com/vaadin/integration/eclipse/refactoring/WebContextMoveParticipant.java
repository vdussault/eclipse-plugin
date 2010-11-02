package com.vaadin.integration.eclipse.refactoring;

public class WebContextMoveParticipant extends AbstractMoveParticipant {

    private WebContextRefactorer refactorer = new WebContextRefactorer();

    @Override
    public WebContextRefactorer getRefactorer() {
        return refactorer;
    }

}
