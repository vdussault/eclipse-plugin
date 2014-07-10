package com.vaadin.integration.eclipse.wizards;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.common.project.facet.core.IDynamicPreset;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectBase;
import org.eclipse.wst.common.project.facet.core.IPresetFactory;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.PresetDefinition;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.internal.DefaultFacetsExtensionPoint;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;

import com.vaadin.integration.eclipse.util.ErrorUtil;

/**
 * Vaadin preset factory
 * 
 * This class provides the basic functionality for the dynamic presets for
 * Vaadin projects.
 * 
 */

public abstract class AbstractVaadinPresetFactory implements IPresetFactory {
    private static final String javaFacet = "java";
    private static final String servletFacet = "jst.web";
    private static final String vaadinFacet = "com.vaadin.integration.eclipse.core";

    public PresetDefinition createPreset(String presetId,
            Map<String, Object> context) throws CoreException {
        IRuntime runtime = (IRuntime) context
                .get(IDynamicPreset.CONTEXT_KEY_PRIMARY_RUNTIME);

        final IFacetedProjectBase fproj = (IFacetedProjectBase) context
                .get(IDynamicPreset.CONTEXT_KEY_FACETED_PROJECT);

        @SuppressWarnings("restriction")
        final Set<IProjectFacetVersion> defaultFacets = DefaultFacetsExtensionPoint
                .getDefaultFacets(fproj);

        final Set<IProjectFacetVersion> facets = buildPreferredFacetSet(
                defaultFacets, runtime);

        String javaVersion = "undefined";
        String servletVersion = "undefined";

        for (IProjectFacetVersion version : facets) {
            if (version.getProjectFacet().getId().equals(javaFacet)) {
                javaVersion = version.getVersionString();
            }
            if (version.getProjectFacet().getId().equals(servletFacet)) {
                servletVersion = version.getVersionString();
            }
        }

        return new PresetDefinition(getLabelString(), String.format(
                getDescriptionString(), javaVersion, servletVersion), facets);
    }

    private Set<IProjectFacetVersion> buildPreferredFacetSet(
            Set<IProjectFacetVersion> defaultFacets, IRuntime runtime)
            throws CoreException {
        Set<IProjectFacetVersion> facets = new HashSet<IProjectFacetVersion>();
        try {
            IProjectFacetVersion vaadinVersion = ProjectFacetsManager
                    .getProjectFacet(vaadinFacet).getVersion(
                            getVaadinCoreVersion());
            IProjectFacetVersion javaVersion;
            IProjectFacetVersion servletVersion;
            if (runtime == null) {
                // If no runtime has been selected we use the preferred defaults
                // if they have been defined, and use the latest version if they
                // have not.

                String preferredJavaVersion = getPreferredJavaVersionString();
                if (preferredJavaVersion == null) {
                    javaVersion = ProjectFacetsManager.getProjectFacet(
                            javaFacet).getLatestVersion();
                } else {
                    javaVersion = ProjectFacetsManager.getProjectFacet(
                            javaFacet).getVersion(preferredJavaVersion);
                }
                String preferredServletVersion = getPreferredServletVersionString();
                if (preferredServletVersion == null) {
                    servletVersion = ProjectFacetsManager.getProjectFacet(
                            servletFacet).getLatestVersion();
                } else {
                    servletVersion = ProjectFacetsManager.getProjectFacet(
                            servletFacet).getVersion(preferredServletVersion);
                }

            } else {
                // If a runtime has been selected we use the latest versions
                // that are supported by the runtime.
                javaVersion = ProjectFacetsManager.getProjectFacet(javaFacet)
                        .getLatestSupportedVersion(runtime);
                servletVersion = ProjectFacetsManager.getProjectFacet(
                        servletFacet).getLatestSupportedVersion(runtime);
            }

            // As a fallback we use the default facet version. These settings
            // are the same as the ones provided by default.configuration
            if (javaVersion == null) {
                javaVersion = ProjectFacetsManager.getProjectFacet(javaFacet)
                        .getDefaultVersion();
            }
            if (servletVersion == null) {
                servletVersion = ProjectFacetsManager.getProjectFacet(
                        servletFacet).getDefaultVersion();
            }
            facets.add(vaadinVersion);
            facets.add(javaVersion);
            facets.add(servletVersion);
            if (defaultFacets != null) {
                for (IProjectFacetVersion defaultFacet : defaultFacets) {
                    if (!defaultFacet.conflictsWith(javaVersion)
                            && !defaultFacet.conflictsWith(servletVersion)
                            && !defaultFacet.conflictsWith(vaadinVersion)) {
                        facets.add(defaultFacet);
                    }
                }
            }
        } catch (CoreException e) {
            ErrorUtil.displayErrorFromBackgroundThread(
                    "Error calculating preferred defaults",
                    "Error calculating preferred defaults for "
                            + getLabelString() + ":\n" + e.getMessage()
                            + "\n\nSee error log for details.");
            throw e;
        }

        return facets;
    }

    protected abstract String getPreferredServletVersionString();

    protected abstract String getLabelString();

    protected abstract String getDescriptionString();

    protected abstract String getPreferredJavaVersionString();

    protected abstract String getVaadinCoreVersion();
}
