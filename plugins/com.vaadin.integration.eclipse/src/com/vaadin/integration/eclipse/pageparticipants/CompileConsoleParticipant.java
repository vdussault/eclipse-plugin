package com.vaadin.integration.eclipse.pageparticipants;

import java.io.IOException;

import org.eclipse.core.runtime.Status;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.consoles.AbstractVaadinConsole;

@SuppressWarnings("restriction")
public class CompileConsoleParticipant implements IConsolePageParticipant {

    private AbstractVaadinConsole console;
    private Process process;
    private TerminateButton terminateButton;

    @SuppressWarnings("rawtypes")
    public Object getAdapter(Class arg0) {
        return null;
    }

    public void activated() {
    }

    public void deactivated() {
    }

    public void dispose() {
    }

    public void init(IPageBookViewPage page, IConsole iconsole) {
        if (!(iconsole instanceof AbstractVaadinConsole)) {
            return;
        }

        console = (AbstractVaadinConsole) iconsole;
        console.setParticipant(this);
        IActionBars actionBars = page.getSite().getActionBars();
        IToolBarManager manager = actionBars.getToolBarManager();
        terminateButton = new TerminateButton();
        manager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, terminateButton);

    }

    public class TerminateButton extends org.eclipse.jface.action.Action {

        public TerminateButton() {
            setImageDescriptor(DebugPluginImages
                    .getImageDescriptor(IInternalDebugUIConstants.IMG_LCL_TERMINATE));
            setDisabledImageDescriptor(DebugPluginImages
                    .getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_TERMINATE));
            setHoverImageDescriptor(DebugPluginImages
                    .getImageDescriptor(IInternalDebugUIConstants.IMG_LCL_TERMINATE));

            setToolTipText("Terminate compilation");
        }

        @Override
        public void run() {
            terminateCompilation();
        }

    }

    public void setCompilationProcess(Process process) {
        this.process = process;
        terminateButton.setEnabled(this.process != null);
    }

    public void terminateCompilation() {
        if (process != null) {
            process.destroy();
            try {
                console.newMessageStream().write("\nCompilation terminated\n");
            } catch (IOException e) {
            }
            setCompilationProcess(null);
        } else {
            VaadinPlugin
                    .getInstance()
                    .getLog()
                    .log(new Status(Status.ERROR, VaadinPlugin.PLUGIN_ID,
                            "Terminate compilation called when no compilation was active"));
        }

    }

}
