package com.vaadin.integration.eclipse.properties;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * Property page grouping Vaadin related project properties.
 * 
 * This page is mostly empty, and the subpages contain the actual settings.
 */
public class VaadinProjectPropertyPage extends PropertyPage {

    public VaadinProjectPropertyPage() {
        super();
        noDefaultAndApplyButton();
    }

    /**
     * @see PreferencePage#createContents(Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout());
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
        composite.setLayoutData(data);

        Label vaadinLabel = new Label(composite, SWT.NONE);
        vaadinLabel
                .setText("Expand the tree to configure your Vaadin project properties.");

        return composite;
    }
}