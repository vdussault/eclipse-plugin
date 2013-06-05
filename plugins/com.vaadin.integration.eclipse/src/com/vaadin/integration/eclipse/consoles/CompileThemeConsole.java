package com.vaadin.integration.eclipse.consoles;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;

public class CompileThemeConsole extends AbstractVaadinConsole {

    private static final String THEME_COMPILATION_CONSOLE_NAME = "Vaadin Theme Compilation";

    public CompileThemeConsole() {
        super(THEME_COMPILATION_CONSOLE_NAME);
    }

    public static CompileThemeConsole get() {
        AbstractVaadinConsole console = findConsole(THEME_COMPILATION_CONSOLE_NAME);

        if (console == null) {
            ConsolePlugin plugin = ConsolePlugin.getDefault();
            IConsoleManager conMan = plugin.getConsoleManager();

            // no console found, so create a new one
            console = new CompileThemeConsole();
            conMan.addConsoles(new IConsole[] { console });
        }

        return (CompileThemeConsole) console;
    }
}
