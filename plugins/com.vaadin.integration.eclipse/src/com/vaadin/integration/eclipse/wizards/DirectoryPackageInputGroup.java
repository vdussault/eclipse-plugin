package com.vaadin.integration.eclipse.wizards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.filters.EmptyInnerPackageFilter;
import org.eclipse.jdt.internal.ui.jarpackager.CheckboxTreeAndListGroup;
import org.eclipse.jdt.ui.JavaElementComparator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Composite;

import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.ProjectUtil;

/**
 * Input group view: tree and list side by side.
 * 
 * Filtering etc. is mostly taken care of the view. A single
 * {@link IJavaProject} can be selected as the initial selection, selecting a
 * default set of elements in it and hiding other projects.
 */
@SuppressWarnings("restriction")
class DirectoryPackageInputGroup extends CheckboxTreeAndListGroup {

    private boolean initiallySelecting = true;

    public DirectoryPackageInputGroup(Composite parent, Object rootObject,
            ITreeContentProvider treeContentProvider,
            ILabelProvider treeLabelProvider,
            IStructuredContentProvider listContentProvider,
            ILabelProvider listLabelProvider, int style, int width, int height) {
        super(parent, rootObject, treeContentProvider, treeLabelProvider,
                listContentProvider, listLabelProvider, style, width, height);

        addTreeFilter(new EmptyInnerPackageFilter());
        setTreeComparator(new JavaElementComparator());
        setListComparator(new JavaElementComparator());
        addTreeFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object p, Object element) {
                if (element instanceof IPackageFragmentRoot) {
                    IPackageFragmentRoot root = (IPackageFragmentRoot) element;
                    return isContainer(element) && !root.isArchive()
                            && !root.isExternal();
                }
                return isContainer(element);
            }
        });
        addListFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement,
                    Object element) {
                return !isContainer(element) && !isAddonStyleFile(element);
            }
        });

    }

    private boolean isContainer(Object element) {
        boolean isContainer = element instanceof IContainer;
        if (!isContainer && element instanceof IJavaElement) {
            int type = ((IJavaElement) element).getElementType();
            isContainer = type == IJavaElement.JAVA_MODEL
                    || type == IJavaElement.JAVA_PROJECT
                    || type == IJavaElement.PACKAGE_FRAGMENT
                    || type == IJavaElement.PACKAGE_FRAGMENT_ROOT;
        }
        return isContainer;
    }

    private boolean isAddonStyleFile(Object element) {
        if (element instanceof IFile) {
            IFile file = (IFile) element;
            String filename = file.getName().toLowerCase();
            if (filename.equals("addons.scss")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void populateListViewer(Object treeElement) {
        super.populateListViewer(treeElement);
    }

    private IFolder getFolder(IProject project, IPath path) {
        if (project.exists(path)) {
            return project.getFolder(path);
        } else {
            // assume it is relative to the workspace - folders outside the
            // project will not be exported
            return project.getWorkspace().getRoot().getFolder(path);
        }
    }

    /**
     * Get the elements to select by default for a Java project.
     * 
     * These include the source directories of the project, the class
     * directories of the project, WebContent/META-INF, WebContent/VAADIN/themes
     * and WebContent/VAADIN/addons.
     * 
     * @param javaProject
     * @return List of elements to select (IFolder, IPackageFragmentRoot etc.)
     */
    private List<Object> getDefaultElements(IJavaProject javaProject) {
        List<Object> result = new ArrayList<Object>();
        try {
            IProject project = javaProject.getProject();

            // default output location
            result.add(getFolder(project, javaProject.getOutputLocation()));

            // classpath entries for source and the corresponding output
            // locations (if specified)
            for (IClasspathEntry entry : javaProject.getResolvedClasspath(true)) {
                if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    // in practice, this is the package fragment root;
                    // this needs to be selected as a IJavaElement and not
                    // as an IFolder
                    IJavaElement root = JavaCore.create(getFolder(project,
                            entry.getPath()));
                    result.add(root);
                    if (entry.getOutputLocation() != null) {
                        result.add(getFolder(project, entry.getOutputLocation()));
                    }
                }
            }

            IFolder webContentFolder = ProjectUtil
                    .getWebContentFolder(javaProject.getProject());

            IFolder themesFolder = ProjectUtil.getThemesFolder(project);
            if (themesFolder.exists()) {
                result.add(themesFolder);
            }

            IFolder addonsFolder = ProjectUtil.getAddonsFolder(project);
            if (addonsFolder.exists()) {
                result.add(addonsFolder);
            }

            // Select META-INF to include also everything else than the manifest
            // by default (e.g. Directory add-on pom.xml).
            // The manifest itself is not added as it is generated and updated
            // separately.
            IFolder metainfFolder = webContentFolder.getFolder("META-INF");
            if (metainfFolder.exists()) {
                IResource[] members = metainfFolder.members();
                for (IResource member : members) {
                    if (!"MANIFEST.MF".equals(member.getName())) {
                        result.add(member);
                    }
                }
            }

            return result;
        } catch (JavaModelException e) {
            ErrorUtil.handleBackgroundException(IStatus.WARNING,
                    "Could not select contents of project "
                            + javaProject.getProject().getName(), e);
            return Collections.emptyList();
        } catch (CoreException e) {
            ErrorUtil.handleBackgroundException(IStatus.ERROR,
                    "Could not make default selections for project "
                            + javaProject.getProject().getName(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Make default selections in the tree based on a Java project and hide
     * other projects from the view.
     * 
     * @see #getDefaultElements(IJavaProject)
     * 
     * @param javaProject
     */
    public void selectProject(final IJavaProject javaProject) {
        // select a part of the project contents by default
        for (Object child : getDefaultElements(javaProject)) {
            if (child instanceof IFile) {
                initialCheckListItem(child);
            } else {
                initialCheckTreeItem(child);
            }
        }

        // hide other projects in the tree
        addTreeFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object p, Object element) {
                if (element instanceof IJavaProject) {
                    return javaProject.equals(element);
                }
                return true;
            }
        });
    }

    @Override
    protected void setTreeChecked(final Object element, final boolean state) {
        if (isInitiallySelecting() && element instanceof IResource) {
            final IResource resource = (IResource) element;
            if (resource.getName().charAt(0) == '.') {
                return;
            }       
        }
        super.setTreeChecked(element, state);
    }

    public void setInitiallySelecting(boolean initiallySelecting) {
        this.initiallySelecting = initiallySelecting;
    }

    public boolean isInitiallySelecting() {
        return initiallySelecting;
    }

    /**
     * Return the elements which will be exported.
     * 
     * {@see
     * org.eclipse.jdt.ui.jarpackager.JarPackageData.setElements(Object[])}
     * 
     * Everything under the returned elements will be exported and the elements
     * should correspond to entities in the file system, so the elements should
     * be mostly leaf elements (e.g. IFile), not their parent containers.
     * 
     * @return Object[] elements to export
     */
    @SuppressWarnings("unchecked")
    // adapted from JarPackageWizardPage
    Object[] getSelectedElementsWithoutContainedChildren() {
        Set<Object> closure = removeContainedChildren(getWhiteCheckedTreeItems());
        closure.addAll(getExportedNonContainers());
        return closure.toArray();
    }

    // adapted from JarPackageWizardPage
    private Set<Object> removeContainedChildren(Set<Object> elements) {
        Set<Object> newList = new HashSet<Object>(elements.size());
        Set<IResource> javaElementResources = getCorrespondingContainers(elements);
        Iterator<Object> iter = elements.iterator();
        boolean removedOne = false;
        while (iter.hasNext()) {
            Object element = iter.next();
            Object parent;
            if (element instanceof IResource) {
                parent = ((IResource) element).getParent();
            } else if (element instanceof IJavaElement) {
                parent = ((IJavaElement) element).getParent();
                if (parent instanceof IPackageFragmentRoot) {
                    IPackageFragmentRoot pkgRoot = (IPackageFragmentRoot) parent;
                    try {
                        if (pkgRoot.getCorrespondingResource() instanceof IProject) {
                            parent = pkgRoot.getJavaProject();
                        }
                    } catch (JavaModelException ex) {
                        // leave parent as is
                    }
                }
            } else {
                // unknown type
                newList.add(element);
                continue;
            }
            if (element instanceof IJavaModel
                    || ((!(parent instanceof IJavaModel)) && (elements
                            .contains(parent) || javaElementResources
                            .contains(parent)))) {
                removedOne = true;
            } else {
                newList.add(element);
            }
        }
        if (removedOne) {
            return removeContainedChildren(newList);
        } else {
            return newList;
        }
    }

    // adapted from JarPackageWizardPage
    @SuppressWarnings("unchecked")
    private Set<Object> getExportedNonContainers() {
        Set<Object> whiteCheckedTreeItems = getWhiteCheckedTreeItems();
        Set<Object> exportedNonContainers = new HashSet<Object>(
                whiteCheckedTreeItems.size());
        Set<IResource> javaElementResources = getCorrespondingContainers(whiteCheckedTreeItems);
        Iterator<Object> iter = getAllCheckedListItems();
        while (iter.hasNext()) {
            Object element = iter.next();
            Object parent = null;
            if (element instanceof IResource) {
                parent = ((IResource) element).getParent();
            } else if (element instanceof IJavaElement) {
                parent = ((IJavaElement) element).getParent();
            }
            if (!whiteCheckedTreeItems.contains(parent)
                    && !javaElementResources.contains(parent)) {
                exportedNonContainers.add(element);
            }
        }
        return exportedNonContainers;
    }

    /**
     * Create a list with {@link IResource} instances for the folders / projects
     * that correspond to the Java elements (Java project, package, package
     * root) in a set.
     * 
     * The set can contain objects of different types, but only
     * {@link IJavaElement} instances are taken into account.
     */
    // adapted from JarPackageWizardPage
    private Set<IResource> getCorrespondingContainers(Set<Object> elements) {
        Set<IResource> javaElementResources = new HashSet<IResource>(
                elements.size());
        Iterator<Object> iter = elements.iterator();
        while (iter.hasNext()) {
            Object element = iter.next();
            if (element instanceof IJavaElement) {
                IJavaElement je = (IJavaElement) element;
                int type = je.getElementType();
                if (type == IJavaElement.JAVA_PROJECT
                        || type == IJavaElement.PACKAGE_FRAGMENT
                        || type == IJavaElement.PACKAGE_FRAGMENT_ROOT) {
                    // exclude default package since it is covered by the root
                    if (!(type == IJavaElement.PACKAGE_FRAGMENT && ((IPackageFragment) element)
                            .isDefaultPackage())) {
                        IResource resource;
                        try {
                            resource = je.getCorrespondingResource();
                        } catch (JavaModelException ex) {
                            resource = null;
                        }
                        if (resource != null) {
                            javaElementResources.add(resource);
                        }
                    }
                }
            }
        }
        return javaElementResources;
    }

}