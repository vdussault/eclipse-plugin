package com.vaadin.integration.eclipse.properties;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.AbstractElementListSelectionDialog;

import com.vaadin.integration.eclipse.util.DownloadUtils;
import com.vaadin.integration.eclipse.util.DownloadUtils.Version;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;

/**
 * Project property page for selecting an Vaadin version and updating the JAR in
 * the project.
 */
public class VaadinVersionComposite extends Composite {

    private Label liferayPathLabel;
    private Text liferayPathField;
    private Button liferayPathButton;
    private Combo versionCombo;
    private Map<String, Version> versionMap = new HashMap<String, Version>();
    private Button downloadButton;
    private IProject project = null;

    private static class DownloadVaadinDialog extends
            AbstractElementListSelectionDialog {

        /**
         * This is an ugly and inefficient hack: as sorting cannot be disabled
         * for AbstractElementListSelectionDialog, use a comparator that
         * compares the positions of the elements in a reference list.
         *
         * Cannot do real version comparison as no information is available
         * about which builds are official releases, pre-releases or release
         * candidates, nightly builds etc.
         */
        private static class VersionStringComparator implements
                Comparator<String> {
            private Map<String, Integer> positions = new HashMap<String, Integer>();

            public VersionStringComparator(List<Version> versionList) {
                for (int i = 0; i < versionList.size(); ++i) {
                    positions.put(versionList.get(i).toString(), i);
                }
            }

            public int compare(String o1, String o2) {
                return positions.get(o1) - positions.get(o2);
            }
        }

        public DownloadVaadinDialog(Shell parent) {
            super(parent, new LabelProvider());

            setTitle("Select Vaadin Version to Download");
            setMessage("Select a Vaadin library version (* = any string, ? = any char):");
            setMultipleSelection(false);
        }

        /*
         * @see SelectionStatusDialog#computeResult()
         */
        @Override
        protected void computeResult() {
            setResult(Arrays.asList(getSelectedElements()));
        }

        /**
         * Creates the checkbox to show or hide development versions and nightly
         * builds.
         *
         * @param composite
         *            the parent composite of the message area.
         */
        protected Button createDevelopmentCheckbox(Composite composite) {
            GridData data = new GridData();
            data.grabExcessVerticalSpace = false;
            data.grabExcessHorizontalSpace = true;
            data.horizontalAlignment = GridData.FILL;
            data.verticalAlignment = GridData.BEGINNING;

            final Button developmentCheckbox = new Button(composite, SWT.CHECK);
            developmentCheckbox
                    .setText("Show pre-release versions and nightly builds");
            developmentCheckbox.setSelection(false);
            developmentCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateVersionList(developmentCheckbox.getSelection());
                }
            });
            developmentCheckbox.setLayoutData(data);

            return developmentCheckbox;
        }

        // fetch the list of available versions, including or excluding
        // development versions and nightly builds
        protected void updateVersionList(boolean development) {
            try {
                Version selected = getSelectedVersion();

                List<Version> available = DownloadUtils
                        .listDownloadableVaadinVersions(development);
                available.removeAll(DownloadUtils.getLocalVaadinJarVersions());

                Version[] versions = available.toArray(new Version[0]);
                fFilteredList.setComparator(new VersionStringComparator(
                        available));
                setListElements(versions);

                // try to preserve selection
                if (selected != null) {
                    setSelection(new Version[] { selected });
                } else {
                    setSelection(getInitialElementSelections().toArray());
                }

            } catch (CoreException ex) {
                // TODO handle exceptions: message to user?
                VaadinPluginUtil
                        .handleBackgroundException(
                                IStatus.WARNING,
                                "Failed to update the list of available Vaadin versions",
                                ex);
            }
        }

        /*
         * @see Dialog#createDialogArea(Composite)
         */
        @Override
        protected Control createDialogArea(Composite parent) {
            Composite contents = (Composite) super.createDialogArea(parent);

            createMessageArea(contents);
            createFilterText(contents);
            createFilteredList(contents);

            createDevelopmentCheckbox(contents);

            updateVersionList(false);

            return contents;
        }

        /**
         * Gets the selected version string or null if none.
         *
         * @return String version or null
         */
        public Version getSelectedVersion() {
            return (Version) getFirstResult();
        }

        @Override
        protected void handleEmptyList() {
            updateOkState();
        }
    }

    public VaadinVersionComposite(Composite parent, int style) {
        super(parent, style);

        setLayout(new GridLayout(3, false));
        GridData data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        setLayoutData(data);
    }

    private void addVersionSelectionSection() {
        Label label = new Label(this, SWT.NULL);
        label.setText("Vaadin version:");

        // Vaadin version selection combo
        versionCombo = new Combo(this, SWT.BORDER | SWT.DROP_DOWN
                | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        versionCombo.setLayoutData(gd);

        // list available versions not yet downloaded
        downloadButton = new Button(this, SWT.NULL);
        downloadButton.setText("Download...");
        gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        downloadButton.setLayoutData(gd);
        downloadButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                downloadVaadin();
            }
        });
    }

    private void addLiferayPathSection() {
        liferayPathLabel = new Label(this, SWT.NULL);
        liferayPathLabel.setText("Liferay server path:");

        liferayPathField = new Text(this, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        liferayPathField.setLayoutData(gd);

        liferayPathButton = new Button(this, SWT.NULL);
        liferayPathButton.setText("Browse...");
        gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        liferayPathButton.setLayoutData(gd);
        liferayPathButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                File f = new File(liferayPathField.getText());
                if (!f.exists()) {
                    f = null;
                }
                File d = getDirectory(f);
                if (d != null) {
                    liferayPathField.setText(d.getAbsolutePath());
                }
            }
        });
    }

    /**
     * Helper that opens the directory chooser dialog.
     * 
     * @param startingDirectory
     *            The directory the dialog will open in.
     * @return File File or <code>null</code>.
     */
    private File getDirectory(File startingDirectory) {

        DirectoryDialog fileDialog = new DirectoryDialog(getShell(), SWT.OPEN
                | SWT.SHEET);
        if (startingDirectory != null) {
            fileDialog.setFilterPath(startingDirectory.getPath());
        }
        String dir = fileDialog.open();
        if (dir != null) {
            dir = dir.trim();
            if (dir.length() > 0) {
                return new File(dir);
            }
        }

        return null;
    }

    private void updateVersionCombo() {
        try {
            versionCombo.removeAll();
            versionMap.clear();
            for (Version version : DownloadUtils.getLocalVaadinJarVersions()) {
                versionMap.put(version.getVersionString(), version);
                versionCombo.add(version.getVersionString());
            }
            try {
                // select current version (if any)
                if (project != null) {
                    Version currentVaadinVersion = VaadinPluginUtil
                            .getVaadinLibraryVersion(project, true);
                    if (currentVaadinVersion != null) {
                        // #3863 add custom Vaadin version in project if any
                        String versionString = currentVaadinVersion
                                .getVersionString();
                        if (!versionMap.containsKey(versionString)) {
                            versionMap.put(versionString, currentVaadinVersion);
                            versionCombo.add(versionString, 0);
                        }
                        versionCombo.setText(versionString);
                    }
                }
            } catch (CoreException ce) {
                // ignore if cannot select current version
                VaadinPluginUtil
                        .handleBackgroundException(
                                IStatus.WARNING,
                                "Failed to select the Vaadin version used in the project",
                                ce);
            }
        } catch (CoreException ex) {
            // leave the combo empty and show an error message
            VaadinPluginUtil
                    .displayError("Failed to list downloaded Vaadin versions",
                            ex, getShell());
        }
    }

    // list available versions not yet downloaded and let the user download one
    private void downloadVaadin() {
        try {
            // let the user choose the version to download in a dialog
            DownloadVaadinDialog dialog = new DownloadVaadinDialog(getShell());

            if (dialog.open() == Window.OK) {
                final Version version = dialog.getSelectedVersion();
                if (version != null) {
                    IRunnableWithProgress op = new IRunnableWithProgress() {
                        public void run(IProgressMonitor monitor)
                                throws InvocationTargetException {
                            try {
                                DownloadUtils.fetchVaadinJar(version, monitor);
                            } catch (CoreException e) {
                                throw new InvocationTargetException(e);
                            } finally {
                                monitor.done();
                            }
                        }
                    };
                    // ProgressService would not show the progress dialog if in
                    // a modal dialog
                    new ProgressMonitorDialog(getShell()).run(true, true, op);

                    updateVersionCombo();
                    versionCombo.setText(version.getVersionString());
                }
            }
        } catch (InterruptedException e) {
            return;
        } catch (InvocationTargetException e) {
            VaadinPluginUtil.displayError(
                    "Failed to download selected Vaadin version", e
                            .getTargetException(), getShell());
        }
    }

    /**
     * This method exists only to enable automatic synchronization with a model.
     * The combo box value is the selected version string.
     *
     * @return Combo
     */
    public Combo getVersionCombo() {
        return versionCombo;
    }

    public Text getLiferayPathField() {
        return liferayPathField;
    }

    public Composite createContents() {
        addVersionSelectionSection();
        addLiferayPathSection();

        liferayPathField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                // update shown version number
                setLiferayPath(liferayPathField.getText());

                // TODO link to validation?
            }
        });

        return this;
    }

    public Version getSelectedVersion() {
        Version newVaadinVersion = versionMap.get(versionCombo.getText());
        if ("".equals(newVaadinVersion)) {
            newVaadinVersion = null;
        }
        return newVaadinVersion;
    }

    public void setProject(IProject project) {
        this.project = project;

        // checks if a Liferay project or not
        updateView(VaadinPluginUtil.isLiferayProject(project),
                VaadinPluginUtil.getLiferayWebInfPath(project));
    }

    public void setNewProject() {
        project = null;

        setLiferayMode(false);

        selectLatestLocalVersion();
    }

    public void setLiferayMode(boolean liferayMode) {
        // keep path value if already set
        updateView(liferayMode, null);
    }

    public void enablePluginManagedVaadin(boolean useVaadin) {
        versionCombo.setEnabled(useVaadin);
        downloadButton.setEnabled(useVaadin);
    }

    protected void updateView(boolean liferayProject, String liferayPath) {
        // TODO what to do/show in a Liferay project?
        liferayPathLabel.setVisible(liferayProject);
        liferayPathField.setVisible(liferayProject);
        liferayPathButton.setVisible(liferayProject);

        if (liferayProject) {
            setLiferayPath(liferayPath);
        } else {
            updateVersionCombo();
        }

        enablePluginManagedVaadin(!liferayProject);
    }

    // should only be called for a Liferay project
    protected void setLiferayPath(String liferayPath) {
        // just show current version (if any), from the JAR in Liferay
        versionCombo.removeAll();
        versionMap.clear();
        if (liferayPath != null
                && !liferayPath.equals(liferayPathField.getText())) {
            liferayPathField.setText(liferayPath);
        }
        try {
            Version currentVaadinVersion = VaadinPluginUtil
                    .getVaadinLibraryVersionInLiferay(liferayPathField
                            .getText());
            if (currentVaadinVersion != null) {
                String versionString = currentVaadinVersion
                        .getVersionString();
                versionMap.put(versionString, currentVaadinVersion);
                versionCombo.add(versionString, 0);
                versionCombo.setText(versionString);
            }
        } catch (CoreException e) {
            // ignore:
            // failed to obtain information about current Vaadin version
        }
    }

    protected void selectLatestLocalVersion() {
        try {
            Version latestVaadinVersion = DownloadUtils
                    .getLatestLocalVaadinJarVersion();
            if (latestVaadinVersion != null) {
                versionCombo.setText(latestVaadinVersion.getVersionString());
            }
        } catch (CoreException e) {
            // maybe there is no version downloaded - ignore
            VaadinPluginUtil
                    .handleBackgroundException(
                            IStatus.WARNING,
                            "Failed to select the most recent cached Vaadin version, probably no versions in cache yet",
                            e);
        }
    }
}