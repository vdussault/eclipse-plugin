package com.vaadin.integration.eclipse.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;

import com.vaadin.integration.eclipse.VaadinPlugin;

public class ThemesUtil {

    /**
     * Creates a new theme
     * 
     * @param jproject
     * @param themeName
     * @param scssTheme
     * @param monitor
     * @return
     * @throws CoreException
     */
    public static IFile[] createTheme(IJavaProject jproject,
            final String themeName, boolean scssTheme,
            IProgressMonitor monitor, boolean addonStylesSupported)
            throws CoreException {

        IProject project = jproject.getProject();

        try {
            // very short operations so if some skipped, handled by
            // monitor.done()
            monitor.beginTask("Creating theme " + themeName, 5);

            String VAADIN = VaadinPlugin.VAADIN_RESOURCE_DIRECTORY;
            String themes = VaadinPlugin.THEME_FOLDER_NAME;
            IFolder webContent = ProjectUtil.getWebContentFolder(project);

            // Ensure theme does not already exist
            IFolder themeFolder = webContent.getFolder(VAADIN)
                    .getFolder(themes).getFolder(themeName);
            if (themeFolder.exists()) {
                throw ErrorUtil.newCoreException("Theme already exists", null);
            }

            // Create folders
            themeFolder.getLocation().toFile().mkdirs();
            webContent.refreshLocal(IResource.DEPTH_INFINITE,
                            new SubProgressMonitor(monitor, 1));


            if (scssTheme) {
                IFile stylesFile = themeFolder.getFile(new Path("styles.scss"));
                IFile themeFile = themeFolder.getFile(new Path(themeName
                        + ".scss"));

                try {
                    String stylesContent = ThemesUtil.getScssStylesContent(
                            themeName, VaadinPlugin.VAADIN_DEFAULT_THEME,
                            addonStylesSupported);
                    InputStream stream = openStringStream(stylesContent);
                    stylesFile.create(stream, true, new SubProgressMonitor(
                            monitor, 1));
                    stream.close();

                    String themeContent = getScssThemeContent(themeName,
                            VaadinPlugin.VAADIN_DEFAULT_THEME);
                    stream = openStringStream(themeContent);
                    themeFile.create(stream, true, new SubProgressMonitor(
                            monitor, 1));
                    stream.close();

                } catch (IOException e) {
                }
                return new IFile[] { stylesFile, themeFile };
            } else {
                IFile file = themeFolder.getFile(new Path("styles.css"));
                String cssContent = getCssContent(themeName,
                        VaadinPlugin.VAADIN_DEFAULT_THEME);
                InputStream stream = openStringStream(cssContent);
                try {
                    file.create(stream, true,
                            new SubProgressMonitor(monitor, 2));
                    stream.close();
                } catch (IOException e) {
                } finally {
                    file.refreshLocal(IResource.DEPTH_INFINITE,
                            new SubProgressMonitor(monitor, 1));
                }
                return new IFile[] { file };
            }
        } finally {
            monitor.done();
        }
    }

    /**
     * We will initialize file contents with a sample text.
     */
    private static String getCssContent(String themeName, String baseTheme) {
        StringBuilder sb = new StringBuilder();
        sb.append("@import url(../" + baseTheme + "/styles.css);\n\n");
        return sb.toString();
    }

    /**
     * We will initialize file contents with a sample text.
     */
    private static String getScssStylesContent(String themeName,
            String baseTheme, boolean supportsAddonStyles) {
        StringBuilder sb = new StringBuilder();

        if (supportsAddonStyles) {
            sb.append("@import \"addons.scss\";\n");
        }

        sb.append("@import \"" + themeName + ".scss\";\n\n");
        sb.append("/* This file prefixes all rules with the theme name to avoid causing conflicts with other themes. */\n");
        sb.append("/* The actual styles should be defined in " + themeName
                + ".scss */\n");
        sb.append("." + themeName + " {\n");
        sb.append("  @include " + themeName + ";\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * We will initialize file contents with a sample text.
     */
    private static String getScssThemeContent(String themeName, String baseTheme) {
        StringBuilder sb = new StringBuilder();
        sb.append("/* Import the " + baseTheme + " theme.*/\n");
        sb.append("/* This only allows us to use the mixins defined in it and does not add any styles by itself. */\n");
        sb.append("@import \"../" + baseTheme + "/" + baseTheme
                + ".scss\";\n\n");
        sb.append("/* This contains all of your theme.*/\n");
        sb.append("/* If somebody wants to extend the theme she will include this mixin. */\n");
        sb.append("@mixin " + themeName + " {\n");
        sb.append("  /* Include all the styles from the " + baseTheme
                + " theme */\n");
        sb.append("  @include " + baseTheme + ";\n\n");
        sb.append("  /* Insert your theme rules here */\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static InputStream openStringStream(String contents) {
        return new ByteArrayInputStream(contents.getBytes());
    }

}
