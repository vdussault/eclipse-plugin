package com.vaadin.integration.eclipse.refactoring;


public class WidgetSetMoveParticipant extends AbstractMoveParticipant {

    private WidgetSetRefactorer refactorer = new WidgetSetRefactorer();

    @Override
    public WidgetSetRefactorer getRefactorer() {
        return refactorer;
    }

}
