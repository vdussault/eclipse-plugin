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
public class ComponentTemplate implements Template {   
  protected static String nl;
  public static synchronized ComponentTemplate create(String lineSeparator)
  {
    nl = lineSeparator;
    ComponentTemplate result = new ComponentTemplate();
    nl = null;
    return result;
  }

  public final String NL = nl == null ? (System.getProperties().getProperty("line.separator")) : nl;
  protected final String TEXT_1 = "package ";
  protected final String TEXT_2 = ";" + NL + "" + NL + "import java.util.Map;" + NL + "" + NL + "import com.vaadin.terminal.PaintException;" + NL + "import com.vaadin.terminal.PaintTarget;" + NL + "import com.vaadin.ui.AbstractComponent;" + NL + "" + NL + "/**" + NL + " * Server side component for the V";
  protected final String TEXT_3 = " widget." + NL + " */" + NL + "@com.vaadin.ui.ClientWidget(";
  protected final String TEXT_4 = ".client.ui.V";
  protected final String TEXT_5 = ".class)" + NL + "public class ";
  protected final String TEXT_6 = " extends AbstractComponent {" + NL + "" + NL + "\tprivate String message = \"Click here.\";" + NL + "\tprivate int clicks = 0;" + NL + "" + NL + "\t@Override" + NL + "\tpublic void paintContent(PaintTarget target) throws PaintException {" + NL + "\t\tsuper.paintContent(target);" + NL + "" + NL + "\t\t// Paint any component specific content by setting attributes" + NL + "\t\t// These attributes can be read in updateFromUIDL in the widget." + NL + "\t\ttarget.addAttribute(\"clicks\", clicks);" + NL + "\t\ttarget.addAttribute(\"message\", message);" + NL + "" + NL + "\t\t// We could also set variables in which values can be returned" + NL + "\t\t// but declaring variables here is not required" + NL + "\t}" + NL + "" + NL + "\t/**" + NL + "\t * Receive and handle events and other variable changes from the client." + NL + "\t * " + NL + "\t * {@inheritDoc}" + NL + "\t */" + NL + "\t@Override" + NL + "\tpublic void changeVariables(Object source, Map<String, Object> variables) {" + NL + "\t\tsuper.changeVariables(source, variables);" + NL + "" + NL + "\t\t// Variables set by the widget are returned in the \"variables\" map." + NL + "" + NL + "\t\tif (variables.containsKey(\"click\")) {" + NL + "" + NL + "\t\t\t// When the user has clicked the component we increase the " + NL + "\t\t\t// click count, update the message and request a repaint so " + NL + "\t\t\t// the changes are sent back to the client." + NL + "\t\t\tclicks++;" + NL + "\t\t\tmessage += \"<br/>\" + variables.get(\"click\");" + NL + "" + NL + "\t\t\trequestRepaint();" + NL + "\t\t}" + NL + "\t}" + NL + "" + NL + "}" + NL;
  protected final String TEXT_7 = NL;

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
     typeName = componentName; 
     target =  componentPackage; 
     fileName = typeName + ".java"; 
    stringBuffer.append(TEXT_1);
    stringBuffer.append( target );
    stringBuffer.append(TEXT_2);
    stringBuffer.append( typeName );
    stringBuffer.append(TEXT_3);
    stringBuffer.append( widgetsetPackage );
    stringBuffer.append(TEXT_4);
    stringBuffer.append( typeName );
    stringBuffer.append(TEXT_5);
    stringBuffer.append( typeName );
    stringBuffer.append(TEXT_6);
    stringBuffer.append(TEXT_7);
    return stringBuffer.toString();
  }
}