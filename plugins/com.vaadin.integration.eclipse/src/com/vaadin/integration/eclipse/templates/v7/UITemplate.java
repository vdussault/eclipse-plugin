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
  protected final String TEXT_3 = NL + "import com.vaadin.annotations.Push;";
  protected final String TEXT_4 = NL + "import com.vaadin.annotations.Theme;";
  protected final String TEXT_5 = NL + "import com.vaadin.server.VaadinRequest;" + NL + "import com.vaadin.ui.Button;" + NL + "import com.vaadin.ui.Button.ClickEvent;" + NL + "import com.vaadin.ui.Label;" + NL + "import com.vaadin.ui.UI;" + NL + "import com.vaadin.ui.VerticalLayout;" + NL + "" + NL + "/**" + NL + " * Main UI class" + NL + " */" + NL + "@SuppressWarnings(\"serial\")";
  protected final String TEXT_6 = NL + "@Theme(\"";
  protected final String TEXT_7 = "\")";
  protected final String TEXT_8 = NL + "@Push";
  protected final String TEXT_9 = NL + "public class ";
  protected final String TEXT_10 = " extends UI {" + NL + "" + NL + "\tprivate VerticalLayout layout;" + NL + "" + NL + "\t@Override" + NL + "\tprotected void init(VaadinRequest request) {" + NL + "\t\tlayout = new VerticalLayout();" + NL + "\t\tlayout.setMargin(true);" + NL + "\t\tsetContent(layout);" + NL + "" + NL + "\t\tButton button = new Button(\"Click Me\");" + NL + "\t\tbutton.addClickListener(new Button.ClickListener() {" + NL + "\t\t\tpublic void buttonClick(ClickEvent event) {" + NL + "\t\t\t\tlayout.addComponent(new Label(\"Thank you for clicking\"));" + NL + "\t\t\t}" + NL + "\t\t});" + NL + "\t\tlayout.addComponent(button);";
  protected final String TEXT_11 = NL + NL + "\t\t// Start a background thread which will periodically push hello messages" + NL + "\t\tnew Thread(new HelloService()).start();";
  protected final String TEXT_12 = NL + "\t}" + NL;
  protected final String TEXT_13 = NL + "\tpublic class HelloService implements Runnable {" + NL + "\t\t@Override" + NL + "\t\tpublic void run() {" + NL + "\t\t\tfor (int i = 0; i < 10; i++) {" + NL + "\t\t\t\ttry {" + NL + "\t\t\t\t\tThread.sleep(1000);" + NL + "\t\t\t\t} catch (InterruptedException e) {" + NL + "\t\t\t\t\treturn;" + NL + "\t\t\t\t}" + NL + "\t\t\t\taccess(new Runnable() {" + NL + "\t\t\t\t\t@Override" + NL + "\t\t\t\t\tpublic void run() {" + NL + "\t\t\t\t\t\tlayout.addComponent(new Label(\"Server says hello!\"));" + NL + "\t\t\t\t\t}" + NL + "\t\t\t\t});" + NL + "\t\t\t}" + NL + "\t\t\taccess(new Runnable() {" + NL + "\t\t\t\t@Override" + NL + "\t\t\t\tpublic void run() {" + NL + "\t\t\t\t\tlayout.addComponent(new Label(" + NL + "\t\t\t\t\t\t\t\"Server is tired and will no longer greet you\"));" + NL + "\t\t\t\t}" + NL + "\t\t\t});" + NL + "\t\t}" + NL + "\t}" + NL;
  protected final String TEXT_14 = NL + "}";

    public String generate(String applicationPackage, String applicationName,
        String uiOrApplicationClass, String uiTheme, boolean push)
  {
    final StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append(TEXT_1);
    stringBuffer.append( applicationPackage );
    stringBuffer.append(TEXT_2);
     if (push) {
    stringBuffer.append(TEXT_3);
     } 
     if(uiTheme != null) {
    stringBuffer.append(TEXT_4);
     } 
    stringBuffer.append(TEXT_5);
     if(uiTheme != null) {
    stringBuffer.append(TEXT_6);
    stringBuffer.append( uiTheme );
    stringBuffer.append(TEXT_7);
     } 
     if (push) {
    stringBuffer.append(TEXT_8);
     } 
    stringBuffer.append(TEXT_9);
    stringBuffer.append( uiOrApplicationClass );
    stringBuffer.append(TEXT_10);
     if (push) {
    stringBuffer.append(TEXT_11);
     } 
    stringBuffer.append(TEXT_12);
     if (push) {
    stringBuffer.append(TEXT_13);
     } 
    stringBuffer.append(TEXT_14);
    return stringBuffer.toString();
  }
}