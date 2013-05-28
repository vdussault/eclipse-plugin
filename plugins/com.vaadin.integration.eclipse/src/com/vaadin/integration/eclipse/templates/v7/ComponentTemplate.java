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
  protected final String TEXT_2 = ";" + NL;
  protected final String TEXT_3 = NL + "import ";
  protected final String TEXT_4 = ".";
  protected final String TEXT_5 = "ClientRpc;";
  protected final String TEXT_6 = NL + "import ";
  protected final String TEXT_7 = ".";
  protected final String TEXT_8 = "ServerRpc;" + NL + "import com.vaadin.shared.MouseEventDetails;";
  protected final String TEXT_9 = NL + "import ";
  protected final String TEXT_10 = ".";
  protected final String TEXT_11 = "State;";
  protected final String TEXT_12 = NL + NL + "public class ";
  protected final String TEXT_13 = " extends ";
  protected final String TEXT_14 = " {" + NL;
  protected final String TEXT_15 = NL + "\tprivate ";
  protected final String TEXT_16 = "ServerRpc rpc = new ";
  protected final String TEXT_17 = "ServerRpc() {" + NL + "\t\tprivate int clickCount = 0;" + NL + "" + NL + "\t\tpublic void clicked(MouseEventDetails mouseDetails) {";
  protected final String TEXT_18 = NL + "\t\t\t// nag every 5:th click using RPC" + NL + "\t\t\tif (++clickCount % 5 == 0) {" + NL + "\t\t\t\tgetRpcProxy(";
  protected final String TEXT_19 = "ClientRpc.class).alert(" + NL + "\t\t\t\t\t\t\"Ok, that's enough!\");" + NL + "\t\t\t}";
  protected final String TEXT_20 = NL + "\t\t\t// update shared state" + NL + "\t\t\tgetState().text = \"You have clicked \" + clickCount + \" times\";" + NL + "\t\t}" + NL + "\t};";
  protected final String TEXT_21 = "  ";
  protected final String TEXT_22 = NL + NL + "\tpublic ";
  protected final String TEXT_23 = "() {";
  protected final String TEXT_24 = NL + "\t\tregisterRpc(rpc);";
  protected final String TEXT_25 = NL + "\t}" + NL;
  protected final String TEXT_26 = NL + "\t@Override" + NL + "\tpublic ";
  protected final String TEXT_27 = "State getState() {" + NL + "\t\treturn (";
  protected final String TEXT_28 = "State) super.getState();" + NL + "\t}";
  protected final String TEXT_29 = NL + "}";
  protected final String TEXT_30 = NL;

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
     String clientPackage = widgetsetPackage + ".client." + componentName.toLowerCase(); 
    stringBuffer.append(TEXT_1);
    stringBuffer.append( target );
    stringBuffer.append(TEXT_2);
     if (t.hasClientRpc()) { 
    stringBuffer.append(TEXT_3);
    stringBuffer.append( clientPackage );
    stringBuffer.append(TEXT_4);
    stringBuffer.append( typeName );
    stringBuffer.append(TEXT_5);
     } 
     if (t.hasServerRpc()) { 
    stringBuffer.append(TEXT_6);
    stringBuffer.append( clientPackage );
    stringBuffer.append(TEXT_7);
    stringBuffer.append( typeName );
    stringBuffer.append(TEXT_8);
     } 
     if (t.hasState()) { 
    stringBuffer.append(TEXT_9);
    stringBuffer.append( clientPackage );
    stringBuffer.append(TEXT_10);
    stringBuffer.append( typeName );
    stringBuffer.append(TEXT_11);
     } 
    stringBuffer.append(TEXT_12);
    stringBuffer.append( typeName );
    stringBuffer.append(TEXT_13);
    stringBuffer.append( componentExtends );
    stringBuffer.append(TEXT_14);
     if (t.hasServerRpc()) { 
    stringBuffer.append(TEXT_15);
    stringBuffer.append( typeName );
    stringBuffer.append(TEXT_16);
    stringBuffer.append( typeName );
    stringBuffer.append(TEXT_17);
     if (t.hasClientRpc()) { 
    stringBuffer.append(TEXT_18);
    stringBuffer.append( typeName );
    stringBuffer.append(TEXT_19);
     } 
     if (t.hasState()) { 
    stringBuffer.append(TEXT_20);
     } 
    stringBuffer.append(TEXT_21);
     } 
    stringBuffer.append(TEXT_22);
    stringBuffer.append( typeName );
    stringBuffer.append(TEXT_23);
     if (t.hasServerRpc()) { 
    stringBuffer.append(TEXT_24);
     } 
    stringBuffer.append(TEXT_25);
     if (t.hasState()) { 
    stringBuffer.append(TEXT_26);
    stringBuffer.append( typeName );
    stringBuffer.append(TEXT_27);
    stringBuffer.append( typeName );
    stringBuffer.append(TEXT_28);
     } 
    stringBuffer.append(TEXT_29);
    stringBuffer.append(TEXT_30);
    return stringBuffer.toString();
  }
}