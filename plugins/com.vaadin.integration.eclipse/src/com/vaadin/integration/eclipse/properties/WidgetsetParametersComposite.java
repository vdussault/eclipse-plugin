package com.vaadin.integration.eclipse.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;

/**
 * Widgetset compilation preferences in project properties.
 */
public class WidgetsetParametersComposite extends Composite {

    private Combo styleCombo;
    private Combo parallelismCombo;
    private IProject project = null;

    public WidgetsetParametersComposite(Composite parent, int style) {
        super(parent, style);
    }

    public void setProject(IProject project) {
        this.project = project;

        ScopedPreferenceStore prefStore = new ScopedPreferenceStore(
                new ProjectScope(project), VaadinPlugin.PLUGIN_ID);

        // get values from project or defaults if none stored

        String style = prefStore
                .getString(VaadinPlugin.PREFERENCES_WIDGETSET_STYLE);
        if (style == null || "".equals(style)) {
            style = "OBF";
        }
        styleCombo.setText(style);

        String parallelism = prefStore
                .getString(VaadinPlugin.PREFERENCES_WIDGETSET_PARALLELISM);
        if (parallelism == null) {
            parallelism = "";
        }
        parallelismCombo.setText(parallelism);
    }

    public Composite createContents() {
        setLayout(new GridLayout(1, false));
        setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

        createOptionsComposite(this);
        createHostedModeComposite(this);
        createInstructionsComposite(this);

        return this;
    }

    /**
     * Configurable options
     */
    private void createOptionsComposite(Composite parent) {
        Composite options = new Composite(parent, SWT.NULL);
        options.setLayout(new GridLayout(2, false));
        options
                .setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true,
                        false));

        // compilation style (obfuscated/pretty)
        Label label = new Label(options, SWT.NULL);
        label.setText("Javascript style:");

        styleCombo = new Combo(options, SWT.BORDER | SWT.DROP_DOWN
                | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        styleCombo.setLayoutData(gd);

        styleCombo.add("OBF");
        styleCombo.add("PRETTY");
        styleCombo.add("DETAILED");

        // compiler parallelism

        label = new Label(options, SWT.NULL);
        label.setText("Compiler threads:");

        parallelismCombo = new Combo(options, SWT.BORDER | SWT.DROP_DOWN
                | SWT.READ_ONLY);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        parallelismCombo.setLayoutData(gd);

        parallelismCombo.add("");
        for (int i = 1; i <= 8; ++i) {
            parallelismCombo.add("" + i);
        }
    }

    private void createInstructionsComposite(Composite parent) {
        Composite instructions = new Composite(parent, SWT.NULL);
        instructions.setLayout(new GridLayout(1, false));
        instructions.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true,
                false));

        // TODO label wrap would require specifying widthHint (really the
        // absolute width in pixels)!

        Label label = new Label(instructions, SWT.WRAP);
        label
                .setText("To optimize widgetset compilation time, modify the \"user.agent\" parameter in the\n"
                        + "widgetset module file (.gwt.xml).");

        label = new Label(instructions, SWT.WRAP);
        label
                .setText("To debug client-side code with hosted mode, first download a full GWT package and replace\n"
                        + "the GWT JARs on the build path with it.");

        label = new Label(instructions, SWT.WRAP);
        label
                .setText("To use OOPHM, download the OOPHM Vaadin package and use the GWT version bundled with it.\n"
                        + "Then install the appropriate browser plugin from the package.");
    }

    /**
     * Hosted mode (both normal and OOPHM) launch configuration and
     * instructions.
     */
    private void createHostedModeComposite(Composite parent) {
        Composite hosted = new Composite(parent, SWT.NULL);
        hosted.setLayout(new GridLayout(2, false));
        hosted
                .setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true,
                        false));

        // hosted mode launch creation button on the right
        Button button = new Button(hosted, SWT.NULL);
        button.setText("Create hosted mode launch");
        button
                .setLayoutData(new GridData(SWT.RIGHT, SWT.BEGINNING, true,
                        false));
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                VaadinPluginUtil.createHostedModeLaunch(project);
                VaadinPluginUtil.setWidgetsetDirty(project, true);
            }
        });
    }

    /**
     * Gets the user-selected GWT compilation style. Default is "OBF".
     *
     * @return "OBF"/"PRETTY"/"DETAILED" - never null
     */
    public String getCompilationStyle() {
        return styleCombo.getText();
    }

    /**
     * Gets the user-selected number of GWT compiler threads. Default is no
     * selection (empty string).
     *
     * @return String containing a positive number or empty string if none
     *         specified, not null
     */
    public String getParallelism() {
        return parallelismCombo.getText();
    }

}
