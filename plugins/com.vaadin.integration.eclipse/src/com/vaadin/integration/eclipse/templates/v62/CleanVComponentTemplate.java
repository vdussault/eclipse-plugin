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
public class CleanVComponentTemplate implements Template {   
  protected static String nl;
  public static synchronized CleanVComponentTemplate create(String lineSeparator)
  {
    nl = lineSeparator;
    CleanVComponentTemplate result = new CleanVComponentTemplate();
    nl = null;
    return result;
  }

  public final String NL = nl == null ? (System.getProperties().getProperty("line.separator")) : nl;
  protected final String TEXT_1 = "package ";
  protected final String TEXT_2 = ";" + NL + "" + NL + "import com.vaadin.terminal.gwt.client.ApplicationConnection;" + NL + "import com.vaadin.terminal.gwt.client.Paintable;" + NL + "import com.vaadin.terminal.gwt.client.UIDL;" + NL + "import com.google.gwt.dom.client.Document;" + NL + "import com.google.gwt.user.client.ui.Widget;" + NL + "" + NL + "/**" + NL + " * Client side widget which communicates with the server. Messages from the" + NL + " * server are shown as HTML and mouse clicks are sent to the server." + NL + " */" + NL + "public class ";
  protected final String TEXT_3 = " extends Widget implements Paintable {" + NL + "" + NL + "\t/** Set the CSS class name to allow styling. */" + NL + "\tpublic static final String CLASSNAME = \"v-";
  protected final String TEXT_4 = "\";" + NL + "" + NL + "\t/** The client side widget identifier */" + NL + "\tprotected String paintableId;" + NL + "" + NL + "\t/** Reference to the server connection object. */" + NL + "\tprotected ApplicationConnection client;" + NL + "" + NL + "\t/**" + NL + "\t * The constructor should first call super() to initialize the component and" + NL + "\t * then handle any initialization relevant to Vaadin." + NL + "\t */" + NL + "\tpublic ";
  protected final String TEXT_5 = "() {" + NL + "\t\t// TODO Example code is extending GWT Widget so it must set a root element." + NL + "\t\t// Change to proper element or remove if extending another widget" + NL + "\t\tsetElement(Document.get().createDivElement());" + NL + "\t\t" + NL + "\t\t// This method call of the Paintable interface sets the component" + NL + "\t\t// style name in DOM tree" + NL + "\t\tsetStyleName(CLASSNAME);" + NL + "\t}" + NL + "" + NL + "\t/**" + NL + "\t * Called whenever an update is received from the server " + NL + "\t */" + NL + "\tpublic void updateFromUIDL(UIDL uidl, ApplicationConnection client) {" + NL + "\t\t// This call should be made first. " + NL + "\t\t// It handles sizes, captions, tooltips, etc. automatically." + NL + "\t\tif (client.updateComponent(this, uidl, true)) {" + NL + "\t\t\t// If client.updateComponent returns true there has been no changes and we" + NL + "\t\t\t// do not need to update anything." + NL + "\t\t\treturn;" + NL + "\t\t}" + NL + "" + NL + "\t\t// Save reference to server connection object to be able to send" + NL + "\t\t// user interaction later" + NL + "\t\tthis.client = client;" + NL + "" + NL + "\t\t// Save the client side identifier (paintable id) for the widget" + NL + "\t\tpaintableId = uidl.getId();" + NL + "" + NL + "\t\t// TODO replace dummy code with actual component logic" + NL + "\t\tgetElement().setInnerHTML(\"It works!\");" + NL + "\t\t" + NL + "\t}" + NL + "}";

    private String target = null;
    private String fileName = null;
    private String typeName = null;
	
	public String getTarget() {
		return target;
	}
 
 	public String getFileName() {
 		return fileName;
 	}
 	
 	public String getTypeName() {
 		return typeName;
 	}
 
   public String generate(String componentName, String componentPackage, 
   			String componentExtends, String stateExtends, String widgetsetPackage, TEMPLATES t)
  {
    final StringBuffer stringBuffer = new StringBuffer();
     typeName = "V" + componentName; 
     target =  widgetsetPackage + ".client.ui"; 
     fileName = typeName + ".java"; 
    stringBuffer.append(TEXT_1);
    stringBuffer.append( target );
    stringBuffer.append(TEXT_2);
    stringBuffer.append( typeName );
    stringBuffer.append(TEXT_3);
    stringBuffer.append( componentName.toLowerCase() );
    stringBuffer.append(TEXT_4);
    stringBuffer.append( typeName );
    stringBuffer.append(TEXT_5);
    return stringBuffer.toString();
  }
}