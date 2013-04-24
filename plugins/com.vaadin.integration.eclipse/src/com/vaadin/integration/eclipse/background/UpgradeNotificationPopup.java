package com.vaadin.integration.eclipse.background;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.mylyn.internal.provisional.commons.ui.AbstractNotificationPopup; 
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import com.vaadin.integration.eclipse.util.data.DownloadableVaadinVersion;

public class UpgradeNotificationPopup extends AbstractNotificationPopup {

    private final Map<IProject, DownloadableVaadinVersion> upgrades;

    private MouseAdapter closeListener = new MouseAdapter() {
        @Override
        public void mouseUp(MouseEvent e) {
            close();
        }
    };

    public UpgradeNotificationPopup(Display display,
            Map<IProject, DownloadableVaadinVersion> upgrades) {
        super(display);
        this.upgrades = upgrades;

        // keep open indefinitely (until user clicks)
        setDelayClose(-1);
    }

    @Override
    protected void createTitleArea(Composite parent) {
        ((GridData) parent.getLayoutData()).heightHint = 24;

        Label titleCircleLabel = new Label(parent, SWT.NONE);
        titleCircleLabel.setText("Upgrading Vaadin Nightly Builds");
        titleCircleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
                true));
        titleCircleLabel.setCursor(parent.getDisplay().getSystemCursor(
                SWT.CURSOR_HAND));

        titleCircleLabel.addMouseListener(closeListener);
        parent.addMouseListener(closeListener);
    }

    @Override
    protected void createContentArea(Composite parent) {
        for (IProject project : upgrades.keySet()) {
            Label l = new Label(parent, SWT.None);
            l.setText("Project " + project + ": " + upgrades.get(project));
            l.setBackground(parent.getBackground());
            l.addMouseListener(closeListener);
        }

        parent.addMouseListener(closeListener);
    }

    @Override
    protected String getPopupShellTitle() {
        return "Upgrading Vaadin Nightly Builds";
    }
}
