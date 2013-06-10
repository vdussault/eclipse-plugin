package com.vaadin.integration.eclipse.templates.v62;

import com.vaadin.integration.eclipse.templates.*;

/*
 * JET GENERATED do not edit!
 * The source templates are in the templates folder (note: not package).
 *
 * The JET source templates can be edited. They are then transformed into java
 * template classes by the JET plugin. To use the generated java templates, no 
 * dependencies are required.
 */
public class ApplicationTemplate {   
  protected static String nl;
  public static synchronized ApplicationTemplate create(String lineSeparator)
  {
    nl = lineSeparator;
    ApplicationTemplate result = new ApplicationTemplate();
    nl = null;
    return result;
  }

  public final String NL = nl == null ? (System.getProperties().getProperty("line.separator")) : nl;
  protected final String TEXT_1 = "package ";
  protected final String TEXT_2 = ";" + NL + "" + NL + "import com.vaadin.Application;" + NL + "import com.vaadin.ui.*;" + NL + "" + NL + "/**" + NL + " * Main application class." + NL + " */" + NL + "public class ";
  protected final String TEXT_3 = " extends Application {" + NL + "" + NL + "\t@Override" + NL + "\tpublic void init() {" + NL + "\t\tWindow mainWindow = new Window(\"";
  protected final String TEXT_4 = "\");" + NL + "\t\tLabel label = new Label(\"Hello Vaadin user\");" + NL + "\t\tmainWindow.addComponent(label);" + NL + "\t\tsetMainWindow(mainWindow);" + NL + "\t}" + NL + "" + NL + "}" + NL;
  protected final String TEXT_5 = NL;

    public String generate(String applicationPackage, String applicationName,
        String uiOrApplicationClass, String uiTheme, boolean servlet30, boolean vaadin71)
  {
    final StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append(TEXT_1);
    stringBuffer.append( applicationPackage );
    stringBuffer.append(TEXT_2);
    stringBuffer.append( uiOrApplicationClass );
    stringBuffer.append(TEXT_3);
    stringBuffer.append( applicationName );
    stringBuffer.append(TEXT_4);
    stringBuffer.append(TEXT_5);
    return stringBuffer.toString();
  }
}