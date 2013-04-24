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
public class ServerRpcTemplate implements Template {   
  protected static String nl;
  public static synchronized ServerRpcTemplate create(String lineSeparator)
  {
    nl = lineSeparator;
    ServerRpcTemplate result = new ServerRpcTemplate();
    nl = null;
    return result;
  }

  public final String NL = nl == null ? (System.getProperties().getProperty("line.separator")) : nl;
  protected final String TEXT_1 = "package ";
  protected final String TEXT_2 = ";" + NL + "" + NL + "import com.vaadin.shared.MouseEventDetails;" + NL + "import com.vaadin.shared.communication.ServerRpc;" + NL + "" + NL + "public interface ";
  protected final String TEXT_3 = " extends ServerRpc {" + NL + "" + NL + "\t// TODO example API" + NL + "\tpublic void clicked(MouseEventDetails mouseDetails);" + NL + "" + NL + "}";
  protected final String TEXT_4 = NL;

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
     typeName = componentName + "ServerRpc"; 
     target =  widgetsetPackage + ".client." + componentName.toLowerCase(); 
     fileName = typeName + ".java"; 
    stringBuffer.append(TEXT_1);
    stringBuffer.append( target );
    stringBuffer.append(TEXT_2);
    stringBuffer.append( typeName );
    stringBuffer.append(TEXT_3);
    stringBuffer.append(TEXT_4);
    return stringBuffer.toString();
  }
}