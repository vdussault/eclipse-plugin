package com.vaadin.integration.eclipse.viewers;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;

/**
 * List of Vaadin Application classes that supports multiple selection. For
 * Vaadin 7, also lists UI classes.
 */
public class ApplicationList extends Composite {

    private CheckboxTableViewer viewer;

    public ApplicationList(Composite parent, int style) {
        super(parent, SWT.NULL);
        setLayout(new FillLayout());
        viewer = CheckboxTableViewer.newCheckList(this, style);
        viewer.setLabelProvider(new JavaElementLabelProvider(
                JavaElementLabelProvider.SHOW_POST_QUALIFIED));
        viewer.setContentProvider(new ArrayContentProvider());

        Table table = viewer.getTable();
        TableColumn column = new TableColumn(table, SWT.NONE);
        column.setWidth(300);
        column.setText("Application class");
        table.setHeaderVisible(true);
        table.setMenu(getContextMenu());

        viewer.setInput(new IType[0]);
    }

    public void update(IProject project, boolean includeUis) {
        // update application list
        IType[] applications = getApplications(project);
        IType[] input = applications;
        if (includeUis) {
            IType[] uis = getUis(project);
            input = new IType[applications.length + uis.length];
            System.arraycopy(applications, 0, input, 0, applications.length);
            System.arraycopy(uis, 0, input, applications.length, uis.length);
        }
        viewer.setInput(input);
    }

    public void clear() {
        // clear the application list
        viewer.setInput(new IType[0]);
    }

    public void selectAll() {
        viewer.setAllChecked(true);
    }

    public java.util.List<IType> getSelectedApplications() {
        Object[] selected = viewer.getCheckedElements();
        java.util.List<IType> apps = new ArrayList<IType>();
        for (Object o : selected) {
            if (o instanceof IType) {
                // only Vaadin applications were added to the list
                apps.add((IType) o);
            }
        }
        return apps;
    }

    private IType[] getApplications(IProject project) {
        IType[] applications = new IType[0];
        try {
            // no progress monitor
            applications = VaadinPluginUtil
                    .getApplicationClasses(project, null);
        } catch (JavaModelException e) {
            // do not list the applications to modify
            // TODO should this be displayed?
            // ErrorUtil.displayError(
            // "Failed to list Application classes in the project", e);
            ErrorUtil.handleBackgroundException(IStatus.WARNING,
                    "Failed to list the Application classes in the project", e);
        }

        return applications;
    }

    private IType[] getUis(IProject project) {
        IType[] applications = new IType[0];
        try {
            // no progress monitor
            applications = VaadinPluginUtil.getUiClasses(project, null);
        } catch (JavaModelException e) {
            // do not list the applications to modify
            // TODO should this be displayed?
            // ErrorUtil.displayError(
            // "Failed to list Application classes in the project", e);
            ErrorUtil.handleBackgroundException(IStatus.WARNING,
                    "Failed to list the UI classes in the project", e);
        }

        return applications;
    }

    private Menu getContextMenu() {
        Menu menu = new Menu(viewer.getTable());
        MenuItem selectItem = new MenuItem(menu, SWT.PUSH);
        selectItem.setText("Select all");
        selectItem.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event arg0) {
                viewer.setAllChecked(true);
            }
        });
        MenuItem deselectItem = new MenuItem(menu, SWT.PUSH);
        deselectItem.setText("Deselect all");
        deselectItem.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event arg0) {
                viewer.setAllChecked(false);
            }
        });

        return menu;
    }

}
