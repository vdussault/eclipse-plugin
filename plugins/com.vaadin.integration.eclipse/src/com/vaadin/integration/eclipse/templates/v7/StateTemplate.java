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
public class StateTemplate implements Template {   
  protected static String nl;
  public static synchronized StateTemplate create(String lineSeparator)
  {
    nl = lineSeparator;
    StateTemplate result = new StateTemplate();
    nl = null;
    return result;
  }

  public final String NL = nl == null ? (System.getProperties().getProperty("line.separator")) : nl;
  protected final String TEXT_1 = "package ";
  protected final String TEXT_2 = ";" + NL + "" + NL + "public class ";
  protected final String TEXT_3 = " extends ";
  protected final String TEXT_4 = " {" + NL + "" + NL + "\t// TODO example state" + NL + "\tpublic String text = \"This is ";
  protected final String TEXT_5 = "\";" + NL + "" + NL + "}";

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
     typeName = componentName + "State"; 
     target =  widgetsetPackage + ".client." + componentName.toLowerCase(); 
     fileName = typeName + ".java"; 
    stringBuffer.append(TEXT_1);
    stringBuffer.append( target );
    stringBuffer.append(TEXT_2);
    stringBuffer.append( typeName );
    stringBuffer.append(TEXT_3);
    stringBuffer.append( stateExtends );
    stringBuffer.append(TEXT_4);
    stringBuffer.append( componentName );
    stringBuffer.append(TEXT_5);
    return stringBuffer.toString();
  }
}