package com.vaadin.integration.eclipse.consoles;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;

import com.vaadin.integration.eclipse.pageparticipants.CompileWidgetsetConsoleParticipant;

public class CompileWidgetsetConsole extends MessageConsole {

    private static final String WS_COMPILATION_CONSOLE_NAME = "Vaadin Widgetset Compilation";
    private CompileWidgetsetConsoleParticipant participant;

    public CompileWidgetsetConsole() {
        super(WS_COMPILATION_CONSOLE_NAME, null);
    }

    public static CompileWidgetsetConsole get() {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();

        for (int i = 0; i < existing.length; i++) {
            if (WS_COMPILATION_CONSOLE_NAME.equals(existing[i].getName())) {
                return (CompileWidgetsetConsole) existing[i];
            }
        }
        // no console found, so create a new one
        CompileWidgetsetConsole console = new CompileWidgetsetConsole();
        conMan.addConsoles(new IConsole[] { console });

        return console;

    }

    public void setCompilationProcess(Process process) {
        if (participant != null) {
            participant.setCompilationProcess(process);
        }
    }

    public void setParticipant(
            CompileWidgetsetConsoleParticipant compileWidgetsetConsoleParticipant) {
        this.participant = compileWidgetsetConsoleParticipant;

    }
}
