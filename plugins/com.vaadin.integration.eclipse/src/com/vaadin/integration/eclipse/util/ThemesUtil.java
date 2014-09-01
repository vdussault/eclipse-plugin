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
            IProgressMonitor monitor, boolean addonStylesSupported,
            boolean valoThemeSupported) throws CoreException {

        IProject project = jproject.getProject();

        try {
            // very short operations so if some skipped, handled by
            // monitor.done()
            monitor.beginTask("Creating theme " + themeName, 5);

            String VAADIN = VaadinPlugin.VAADIN_RESOURCE_DIRECTORY;
            String themes = VaadinPlugin.THEME_FOLDER_NAME;
            String baseThemeName = VaadinPlugin.VAADIN_DEFAULT_THEME;
            if (valoThemeSupported) {
                baseThemeName = VaadinPlugin.VAADIN_73_DEFAULT_THEME;
            }

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
                IFile addonsFile = themeFolder.getFile(new Path("addons.scss"));

                try {

                    String stylesContent = ThemesUtil.getScssStylesContent(
                            themeName, baseThemeName, addonStylesSupported);
                    InputStream stream = openStringStream(stylesContent);
                    stylesFile.create(stream, true, new SubProgressMonitor(
                            monitor, 1));
                    stream.close();

                    String themeContent;
                    if (valoThemeSupported) {
                        themeContent = getValoScssThemeContent(themeName,
                                baseThemeName);
                    } else {
                        themeContent = getScssThemeContent(themeName,
                                baseThemeName);
                    }

                    stream = openStringStream(themeContent);
                    themeFile.create(stream, true, new SubProgressMonitor(
                            monitor, 1));
                    stream.close();

                    String addonsContent = getAdddonsScssContent();
                    stream = openStringStream(addonsContent);
                    addonsFile.create(stream, true, new SubProgressMonitor(
                            monitor, 1));

                } catch (IOException e) {
                }
                return new IFile[] { stylesFile, themeFile };
            } else {
                IFile file = themeFolder.getFile(new Path("styles.css"));
                String cssContent = getCssContent(themeName, baseThemeName);
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

        if (supportsAddonStyles) {
            sb.append("  @include addons;\n");
        }

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

    private static String getValoScssThemeContent(String themeName,
            String baseTheme) {
        StringBuilder sb = new StringBuilder();
        sb.append("// Global variable overrides. Must be declared before importing Valo.\n");
        sb.append("\n");
        sb.append("// Defines the plaintext font size, weight and family. Font size affects general component sizing.\n");
        sb.append("//$v-font-size: 16px;\n");
        sb.append("//$v-font-weight: 300;\n");
        sb.append("//$v-font-family: \"Open Sans\", sans-serif;\n");
        sb.append("\n");
        sb.append("// Defines the border used by all components.\n");
        sb.append("//$v-border: 1px solid (v-shade 0.7);\n");
        sb.append("//$v-border-radius: 4px;\n");
        sb.append("\n");
        sb.append("// Affects the color of some component elements, e.g Button, Panel title, etc\n");
        sb.append("//$v-background-color: hsl(210, 0%, 98%);\n");
        sb.append("// Affects the color of content areas, e.g  Panel and Window content, TextField input etc\n");
        sb.append("//$v-app-background-color: $v-background-color;\n");
        sb.append("\n");
        sb.append("// Affects the visual appearance of all components\n");
        sb.append("//$v-gradient: v-linear 8%;\n");
        sb.append("//$v-bevel-depth: 30%;\n");
        sb.append("//$v-shadow-opacity: 5%;\n");
        sb.append("\n");
        sb.append("// Defines colors for indicating status (focus, success, failure)\n");
        sb.append("//$v-focus-color: valo-focus-color(); // Calculates a suitable color automatically\n");
        sb.append("//$v-friendly-color: #2c9720;\n");
        sb.append("//$v-error-indicator-color: #ed473b;\n");
        sb.append("\n");
        sb.append("// For more information, see: https://vaadin.com/book/-/page/themes.valo.html\n");
        sb.append("// Example variants can be copy/pasted from https://vaadin.com/wiki/-/wiki/Main/Valo+Examples\n");
        sb.append("\n");
        sb.append("@import \"../" + baseTheme + "/" + baseTheme
                + ".scss\";\n\n");
        sb.append("@mixin " + themeName + " {\n");
        sb.append("  @include " + baseTheme + ";\n\n");
        sb.append("  // Insert your own theme rules here\n");
        sb.append("}\n");

        return sb.toString();

    }

    private static String getAdddonsScssContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("/*This file is automatically managed and "
                + "will be overwritten from time to time.*/\n");
        sb.append("/* Do not manually edit this file. */\n");
        sb.append("\n");
        sb.append("/* Import and include this mixin into your project theme to include the addon themes */\n");
        sb.append("@mixin addons {");
        sb.append("}");

        return sb.toString();
    }

    private static InputStream openStringStream(String contents) {
        return new ByteArrayInputStream(contents.getBytes());
    }

}
