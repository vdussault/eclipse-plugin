package com.vaadin.integration.eclipse.refactoring;


public class WebContextRenameParticipant extends AbstractRenameParticipant {

    private WebContextRefactorer refactorer = new WebContextRefactorer();

    @Override
    public WebContextRefactorer getRefactorer() {
        return refactorer;
    }

}
