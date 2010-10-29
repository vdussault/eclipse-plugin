package com.vaadin.integration.eclipse.refactoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.search.core.text.TextSearchMatchAccess;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import com.vaadin.integration.eclipse.VaadinFacetUtils;

public abstract class VaadinTextFileRefactorer {

    private Map<String, String> packageRenames = new HashMap<String, String>();
    private Map<String, String> classRenames = new HashMap<String, String>();
    private IProject sourceProject = null;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant#
     * createChange(org.eclipse.core.runtime.IProgressMonitor)
     */
    public abstract Change createChange(
            RefactoringParticipant refactoringParticipant, IProgressMonitor pm)
            throws CoreException, OperationCanceledException;

    /**
     * Initialize a move operation of "element" to "destination". No operation
     * is performed if element and destination belong to separate projects.
     * 
     * @param element
     * @param arguments
     * @return
     */
    public boolean initializeMove(Object element, Object destination) {
        if (destination instanceof IPackageFragment) {
            IPackageFragment fragment = (IPackageFragment) destination;
            String newName = getName(fragment);

            if (getSourceProject() == null) {
                sourceProject = getProject(element);
                if (getSourceProject() == null) {
                    return false;
                }
            }
            IProject targetProject = getProject(destination);

            if (targetProject != getSourceProject()) {
                // TODO Always flag as not initialized in this case?
                // Show warning that an element is moved away?
                // Remove servlet context if application moved out?
                return false;
            }
            return initializeRename(element, newName, true);
        }

        return false;
    }

    private IProject getProject(Object element) {
        IProject proj = null;
        if (element instanceof IPackageFragment) {
            IPackageFragment fragment = (IPackageFragment) element;
            proj = fragment.getJavaProject().getProject();
        } else if (element instanceof IType) {
            IType type = (IType) element;
            proj = type.getJavaProject().getProject();
        }
        if (VaadinFacetUtils.isVaadinProject(proj)) {
            return proj;
        }
        return null;
    }

    private String getName(IPackageFragment fragment) {
        return fragment.getElementName();
    }

    /**
     * Initialize a rename operation for the element. The newName parameter is
     * the new package name for the class (if newNameIsPackage is true) or the
     * new simple class name (no package information, should be placed in the
     * same package as the element).
     * 
     * @param element
     * @param newName
     * @param newNameIsPackage
     * @return true if a change is needed, false otherwise
     */
    public boolean initializeRename(Object element, String newName,
            boolean newNameIsPackage) {
        if (element instanceof IPackageFragment) {
            // Renaming a package
            IPackageFragment fragment = (IPackageFragment) element;
            if (getSourceProject() == null) {
                sourceProject = getProject(fragment);
            }
            String oldName = getName(fragment);
            if (newNameIsPackage) {
                newName = newName + "." + oldName;
            }

            getPackageRenames().put(oldName, newName);

            return true;
        }

        if (element instanceof IType) {
            // Sub type
            IType type = (IType) element;
            if (getSourceProject() == null) {
                sourceProject = getProject(type);
            }

            String oldName = getName(type);
            if (oldName == null) {
                return false;
            }
            if (newNameIsPackage) {
                newName = newName + "." + type.getElementName();
            } else {
                // Prefix new name with package
                String base = getTypeClassName(type, false);
                newName = base + newName;
            }
            getClassRenames().put(oldName, newName);

            return true;
        }

        return false;
    }

    /**
     * Returns the full class name for the type.
     * 
     * @param type
     * @return
     */
    private String getName(IType type) {
        try {
            if (!type.isStructureKnown()) {
                return null;
            }
        } catch (JavaModelException e) {
            return null;
        }

        String base = getTypeClassName(type, false);
        String name = base + type.getElementName();

        return name;
    }

    /**
     * Returns the name of the type if includeType is true. If includeType is
     * false it returns the name of the parent type (package or class).
     * 
     * @param type
     * @param includeType
     * @return
     */
    private String getTypeClassName(IType type, boolean includeType) {
        String typeName = type.getElementName();
        IJavaElement parent = type.getParent();

        if (parent instanceof IType) {
            // This is an inner class
            return getTypeClassName((IType) parent, true) + "$";
        } else if (parent instanceof ICompilationUnit) {
            String base = getCompilationUnitBase((ICompilationUnit) parent);
            if (includeType) {
                return base + typeName;
            } else {
                return base;
            }
        } else {
            return null;
        }
    }

    private String getCompilationUnitBase(ICompilationUnit unit) {
        String packageName = ((IPackageFragment) unit.getParent())
                .getElementName();
        return packageName + ".";
    }

    protected IProject getSourceProject() {
        return sourceProject;
    }

    protected Map<String, String> getPackageRenames() {
        return packageRenames;
    }

    protected Map<String, String> getClassRenames() {
        return classRenames;
    }

    public class TextReplacer extends TextSearchRequestor {

        private List<TextEdit> edits = new ArrayList<TextEdit>();

        private String from;
        private String to;

        public TextReplacer(String from, String to) {
            this.from = from;
            this.to = to;
        }

        public List<TextEdit> getEdits() {
            return edits;
        }

        @Override
        public boolean acceptPatternMatch(TextSearchMatchAccess matchAccess)
                throws CoreException {

            /*
             * The used pattern might contain a large chunk to exclude unwanted
             * blocks. Here we make sure that we only replace "from" with "to".
             */

            int offset = matchAccess.getMatchOffset();

            String matchedString = matchAccess.getFileContent(offset,
                    matchAccess.getMatchLength());

            int subOffset = matchedString.indexOf(from);
            if (subOffset >= 0) {
                offset += subOffset;
                int length = from.length();
                edits.add(new ReplaceEdit(offset, length, to));
            }
            return true;
        }

    }

}
