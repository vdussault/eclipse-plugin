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
public class WidgetTemplate implements Template {   
  protected static String nl;
  public static synchronized WidgetTemplate create(String lineSeparator)
  {
    nl = lineSeparator;
    WidgetTemplate result = new WidgetTemplate();
    nl = null;
    return result;
  }

  public final String NL = nl == null ? (System.getProperties().getProperty("line.separator")) : nl;
  protected final String TEXT_1 = "package ";
  protected final String TEXT_2 = ";" + NL + "" + NL + "import com.google.gwt.user.client.ui.Label;" + NL + "" + NL + "// TODO extend any GWT Widget" + NL + "public class ";
  protected final String TEXT_3 = " extends Label {" + NL + "" + NL + "\tpublic static final String CLASSNAME = \"";
  protected final String TEXT_4 = "\";" + NL + "" + NL + "\tpublic ";
  protected final String TEXT_5 = "() {" + NL + "" + NL + "\t\t// setText(\"";
  protected final String TEXT_6 = " sets the text via ";
  protected final String TEXT_7 = "Connector using ";
  protected final String TEXT_8 = "State\");" + NL + "\t\tsetStyleName(CLASSNAME);" + NL + "" + NL + "\t}" + NL + "" + NL + "}";

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
     typeName = componentName + "Widget"; 
     target =  widgetsetPackage + ".client." + componentName.toLowerCase(); 
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
    stringBuffer.append( componentName );
    stringBuffer.append(TEXT_6);
    stringBuffer.append( componentName );
    stringBuffer.append(TEXT_7);
    stringBuffer.append( componentName );
    stringBuffer.append(TEXT_8);
    return stringBuffer.toString();
  }
}