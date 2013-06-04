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
  protected final String TEXT_2 = ";" + NL;
  protected final String TEXT_3 = NL + "import javax.servlet.annotation.WebInitParam;" + NL + "import javax.servlet.annotation.WebServlet;" + NL;
  protected final String TEXT_4 = NL + "import com.vaadin.annotations.Theme;";
  protected final String TEXT_5 = NL + "import com.vaadin.server.VaadinRequest;";
  protected final String TEXT_6 = NL + "import com.vaadin.server.VaadinServlet;";
  protected final String TEXT_7 = NL + "import com.vaadin.ui.Button;" + NL + "import com.vaadin.ui.Button.ClickEvent;" + NL + "import com.vaadin.ui.Label;" + NL + "import com.vaadin.ui.UI;" + NL + "import com.vaadin.ui.VerticalLayout;" + NL + "" + NL + "@SuppressWarnings(\"serial\")";
  protected final String TEXT_8 = NL + "@Theme(\"";
  protected final String TEXT_9 = "\")";
  protected final String TEXT_10 = NL + "public class ";
  protected final String TEXT_11 = " extends UI {" + NL;
  protected final String TEXT_12 = NL + "\t@WebServlet(value = \"/*\", asyncSupported = true, initParams = {" + NL + "\t\t\t@WebInitParam(name = \"ui\", value = \"";
  protected final String TEXT_13 = ".";
  protected final String TEXT_14 = "\")," + NL + "\t\t\t@WebInitParam(name = \"productionMode\", value = \"false\") })" + NL + "\tpublic static class Servlet extends VaadinServlet {" + NL + "\t}" + NL;
  protected final String TEXT_15 = NL + "\t@Override" + NL + "\tprotected void init(VaadinRequest request) {" + NL + "\t\tfinal VerticalLayout layout = new VerticalLayout();" + NL + "\t\tlayout.setMargin(true);" + NL + "\t\tsetContent(layout);" + NL + "" + NL + "\t\tButton button = new Button(\"Click Me\");" + NL + "\t\tbutton.addClickListener(new Button.ClickListener() {" + NL + "\t\t\tpublic void buttonClick(ClickEvent event) {" + NL + "\t\t\t\tlayout.addComponent(new Label(\"Thank you for clicking\"));" + NL + "\t\t\t}" + NL + "\t\t});" + NL + "\t\tlayout.addComponent(button);" + NL + "\t}" + NL + "" + NL + "}";

    public String generate(String applicationPackage, String applicationName,
        String uiOrApplicationClass, String uiTheme, boolean servlet30)
  {
    final StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append(TEXT_1);
    stringBuffer.append( applicationPackage );
    stringBuffer.append(TEXT_2);
     if(servlet30) {
    stringBuffer.append(TEXT_3);
     } 
     if(uiTheme != null) {
    stringBuffer.append(TEXT_4);
     } 
    stringBuffer.append(TEXT_5);
     if(servlet30) {
    stringBuffer.append(TEXT_6);
     } 
    stringBuffer.append(TEXT_7);
     if(uiTheme != null) {
    stringBuffer.append(TEXT_8);
    stringBuffer.append( uiTheme );
    stringBuffer.append(TEXT_9);
     } 
    stringBuffer.append(TEXT_10);
    stringBuffer.append( uiOrApplicationClass );
    stringBuffer.append(TEXT_11);
     if(servlet30) {
    stringBuffer.append(TEXT_12);
    stringBuffer.append( applicationPackage );
    stringBuffer.append(TEXT_13);
    stringBuffer.append( uiOrApplicationClass );
    stringBuffer.append(TEXT_14);
     } 
    stringBuffer.append(TEXT_15);
    return stringBuffer.toString();
  }
}