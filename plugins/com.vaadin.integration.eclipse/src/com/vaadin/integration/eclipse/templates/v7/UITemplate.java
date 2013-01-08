package com.vaadin.integration.eclipse.templates.v7;

import com.vaadin.integration.eclipse.templates.*;

/*
 * JET GENERATED do not edit!
 * The source templates are in the templates folder (note: not package).
 *
 * The JET source templates can be edited. They are then transformed into java
 * template classes by the JET plugin. To use the generated java templates, no 
 * dependencies are required.
 */
public class UITemplate {   
  protected static String nl;
  public static synchronized UITemplate create(String lineSeparator)
  {
    nl = lineSeparator;
    UITemplate result = new UITemplate();
    nl = null;
    return result;
  }

  public final String NL = nl == null ? (System.getProperties().getProperty("line.separator")) : nl;
  protected final String TEXT_1 = "package ";
  protected final String TEXT_2 = ";" + NL + "" + NL + "import com.vaadin.server.VaadinRequest;" + NL + "import com.vaadin.ui.Button;" + NL + "import com.vaadin.ui.Button.ClickEvent;" + NL + "import com.vaadin.ui.Label;" + NL + "import com.vaadin.ui.UI;" + NL + "import com.vaadin.ui.VerticalLayout;" + NL + "" + NL + "/**" + NL + " * Main UI class" + NL + " */" + NL + "@SuppressWarnings(\"serial\")" + NL + "public class ";
  protected final String TEXT_3 = " extends UI {" + NL + "" + NL + "\t@Override" + NL + "\tprotected void init(VaadinRequest request) {" + NL + "\t\tfinal VerticalLayout layout = new VerticalLayout();" + NL + "\t\tlayout.setMargin(true);" + NL + "\t\tsetContent(layout);" + NL + "" + NL + "\t\tButton button = new Button(\"Click Me\");" + NL + "\t\tbutton.addClickListener(new Button.ClickListener() {" + NL + "\t\t\tpublic void buttonClick(ClickEvent event) {" + NL + "\t\t\t\tlayout.addComponent(new Label(\"Thank you for clicking\"));" + NL + "\t\t\t}" + NL + "\t\t});" + NL + "\t\tlayout.addComponent(button);" + NL + "\t}" + NL + "" + NL + "}";

    public String generate(String applicationPackage, String applicationName,
        String uiOrApplicationClass)
  {
    final StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append(TEXT_1);
    stringBuffer.append( applicationPackage );
    stringBuffer.append(TEXT_2);
    stringBuffer.append( uiOrApplicationClass );
    stringBuffer.append(TEXT_3);
    return stringBuffer.toString();
  }
}