package com.vaadin.integration.eclipse.refactoring;


public class WidgetSetRenameParticipant extends AbstractRenameParticipant {

    private WidgetSetRefactorer refactorer = new WidgetSetRefactorer();

    @Override
    public WidgetSetRefactorer getRefactorer() {
        return refactorer;
    }

}
