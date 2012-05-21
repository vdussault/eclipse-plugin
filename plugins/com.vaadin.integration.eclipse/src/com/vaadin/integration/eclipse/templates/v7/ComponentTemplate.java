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
  protected final String TEXT_8 = "ServerRpc;" + NL + "import com.vaadin.terminal.gwt.client.MouseEventDetails;";
  protected final String TEXT_9 = NL + "import ";
  protected final String TEXT_10 = ".";
  protected final String TEXT_11 = "State;";
  protected final String TEXT_12 = NL + NL + "public class ";
  protected final String TEXT_13 = " extends ";
  protected final String TEXT_14 = " {" + NL;
  protected final String TEXT_15 = NL + "    private ";
  protected final String TEXT_16 = "ServerRpc rpc = new ";
  protected final String TEXT_17 = "ServerRpc() {" + NL + "        private int clickCount = 0;" + NL + "        " + NL + "        public void clicked(MouseEventDetails mouseDetails) {";
  protected final String TEXT_18 = NL + "            // nag every 5:th click using RPC" + NL + "            if (++clickCount % 5 == 0) {" + NL + "                getRpcProxy(";
  protected final String TEXT_19 = "ClientRpc.class).alert(" + NL + "                        \"Ok, that's enough!\");" + NL + "            }";
  protected final String TEXT_20 = NL + "            // update shared state" + NL + "            getState().setText(\"You have clicked \" + clickCount + \" times\");" + NL + "            requestRepaint();" + NL + "        }" + NL + "    };";
  protected final String TEXT_21 = "  ";
  protected final String TEXT_22 = NL + NL + "    public ";
  protected final String TEXT_23 = "() {";
  protected final String TEXT_24 = NL + "        getState().setText(\"This is ";
  protected final String TEXT_25 = "\");";
  protected final String TEXT_26 = NL + "        registerRpc(rpc);";
  protected final String TEXT_27 = NL + "    }" + NL;
  protected final String TEXT_28 = NL + "    @Override" + NL + "    public ";
  protected final String TEXT_29 = "State getState() {" + NL + "        return (";
  protected final String TEXT_30 = "State) super.getState();" + NL + "    }";
  protected final String TEXT_31 = NL + "}";
  protected final String TEXT_32 = NL;

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
     if (t.hasState()) { 
    stringBuffer.append(TEXT_24);
    stringBuffer.append( componentName );
    stringBuffer.append(TEXT_25);
     } 
     if (t.hasServerRpc()) { 
    stringBuffer.append(TEXT_26);
     } 
    stringBuffer.append(TEXT_27);
     if (t.hasState()) { 
    stringBuffer.append(TEXT_28);
    stringBuffer.append( typeName );
    stringBuffer.append(TEXT_29);
    stringBuffer.append( typeName );
    stringBuffer.append(TEXT_30);
     } 
    stringBuffer.append(TEXT_31);
    stringBuffer.append(TEXT_32);
    return stringBuffer.toString();
  }
}