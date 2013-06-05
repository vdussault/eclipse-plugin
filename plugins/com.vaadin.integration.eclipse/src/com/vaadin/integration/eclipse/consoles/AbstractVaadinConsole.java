package com.vaadin.integration.eclipse.consoles;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;

import com.vaadin.integration.eclipse.pageparticipants.CompileConsoleParticipant;

public abstract class AbstractVaadinConsole extends MessageConsole {

    private CompileConsoleParticipant participant;

    public AbstractVaadinConsole(String consoleName) {
        super(consoleName, null);
    }

    protected static AbstractVaadinConsole findConsole(String consoleName) {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();

        for (int i = 0; i < existing.length; i++) {
            if (consoleName.equals(existing[i].getName())) {
                return (AbstractVaadinConsole) existing[i];
            }
        }
        return null;
    }

    public void setCompilationProcess(Process process) {
        if (participant != null) {
            participant.setCompilationProcess(process);
        }
    }

    public void setParticipant(
            CompileConsoleParticipant compileWidgetsetConsoleParticipant) {
        participant = compileWidgetsetConsoleParticipant;

    }
}
