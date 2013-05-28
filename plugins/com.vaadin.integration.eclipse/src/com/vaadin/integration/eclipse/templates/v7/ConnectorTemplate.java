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
public class ConnectorTemplate implements Template {   
  protected static String nl;
  public static synchronized ConnectorTemplate create(String lineSeparator)
  {
    nl = lineSeparator;
    ConnectorTemplate result = new ConnectorTemplate();
    nl = null;
    return result;
  }

  public final String NL = nl == null ? (System.getProperties().getProperty("line.separator")) : nl;
  protected final String TEXT_1 = "package ";
  protected final String TEXT_2 = ";" + NL + "" + NL + "import com.google.gwt.core.client.GWT;" + NL + "import com.google.gwt.user.client.Window;" + NL + "import com.google.gwt.user.client.ui.Widget;" + NL + "" + NL + "import com.vaadin.client.ui.AbstractComponentConnector;" + NL + "import com.vaadin.shared.ui.Connect;" + NL + "" + NL + "import ";
  protected final String TEXT_3 = ".";
  protected final String TEXT_4 = ";";
  protected final String TEXT_5 = NL + "import ";
  protected final String TEXT_6 = ";";
  protected final String TEXT_7 = NL + "import com.google.gwt.user.client.ui.Label;";
  protected final String TEXT_8 = NL + "import ";
  protected final String TEXT_9 = ".";
  protected final String TEXT_10 = ";" + NL + "import com.vaadin.client.communication.RpcProxy;" + NL + "import com.google.gwt.event.dom.client.ClickEvent;" + NL + "import com.google.gwt.event.dom.client.ClickHandler;" + NL + "import com.vaadin.shared.MouseEventDetails;" + NL + "import com.vaadin.client.MouseEventDetailsBuilder;";
  protected final String TEXT_11 = NL + "import ";
  protected final String TEXT_12 = ".";
  protected final String TEXT_13 = ";";
  protected final String TEXT_14 = NL + "import ";
  protected final String TEXT_15 = ".";
  protected final String TEXT_16 = ";" + NL + "import com.vaadin.client.communication.StateChangeEvent;";
  protected final String TEXT_17 = NL + NL + "@Connect(";
  protected final String TEXT_18 = ".class)" + NL + "public class ";
  protected final String TEXT_19 = " extends AbstractComponentConnector {" + NL;
  protected final String TEXT_20 = NL + "\t";
  protected final String TEXT_21 = " rpc = RpcProxy" + NL + "\t\t\t.create(";
  protected final String TEXT_22 = ".class, this);";
  protected final String TEXT_23 = NL + "\t" + NL + "\tpublic ";
  protected final String TEXT_24 = "() {";
  protected final String TEXT_25 = NL + "\t\tregisterRpc(";
  protected final String TEXT_26 = ".class, new ";
  protected final String TEXT_27 = "() {" + NL + "\t\t\tpublic void alert(String message) {" + NL + "\t\t\t\t// TODO Do something useful" + NL + "\t\t\t\tWindow.alert(message);" + NL + "\t\t\t}" + NL + "\t\t});";
  protected final String TEXT_28 = NL + NL + "\t\t// TODO ServerRpc usage example, do something useful instead" + NL + "\t\tgetWidget().addClickHandler(new ClickHandler() {" + NL + "\t\t\tpublic void onClick(ClickEvent event) {" + NL + "\t\t\t\tfinal MouseEventDetails mouseDetails = MouseEventDetailsBuilder" + NL + "\t\t\t\t\t.buildMouseEventDetails(event.getNativeEvent()," + NL + "\t\t\t\t\t\t\t\tgetWidget().getElement());" + NL + "\t\t\t\trpc.clicked(mouseDetails);" + NL + "\t\t\t}" + NL + "\t\t});";
  protected final String TEXT_29 = NL + NL + "\t}" + NL + "" + NL + "\t@Override" + NL + "\tprotected Widget createWidget() {" + NL + "\t\treturn GWT.create(";
  protected final String TEXT_30 = ".class);" + NL + "\t}" + NL + "" + NL + "\t@Override" + NL + "\tpublic ";
  protected final String TEXT_31 = " getWidget() {" + NL + "\t\treturn (";
  protected final String TEXT_32 = ") super.getWidget();" + NL + "\t}" + NL;
  protected final String TEXT_33 = NL + "\t@Override" + NL + "\tpublic ";
  protected final String TEXT_34 = " getState() {" + NL + "\t\treturn (";
  protected final String TEXT_35 = ") super.getState();" + NL + "\t}" + NL + "" + NL + "\t@Override" + NL + "\tpublic void onStateChanged(StateChangeEvent stateChangeEvent) {" + NL + "\t\tsuper.onStateChanged(stateChangeEvent);" + NL + "" + NL + "\t\t// TODO do something useful" + NL + "\t\tfinal String text = getState().text;" + NL + "\t\tgetWidget().setText(text);" + NL + "\t}";
  protected final String TEXT_36 = NL + NL + "}" + NL;
  protected final String TEXT_37 = NL;

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
     typeName = componentName + "Connector"; 
     target =  widgetsetPackage + ".client." + componentName.toLowerCase(); 
     fileName = typeName + ".java"; 
     String widgetName = (t.hasWidget()? componentName + "Widget" : "Label"); 
     String stateName = componentName + "State"; 
     String serverRpcName = componentName + "ServerRpc"; 
     String clientRpcName = componentName + "ClientRpc"; 
    stringBuffer.append(TEXT_1);
    stringBuffer.append( target );
    stringBuffer.append(TEXT_2);
    stringBuffer.append( componentPackage );
    stringBuffer.append(TEXT_3);
    stringBuffer.append( componentName );
    stringBuffer.append(TEXT_4);
     if (t.hasWidget()) { 
    stringBuffer.append(TEXT_5);
    stringBuffer.append( target + "." + widgetName );
    stringBuffer.append(TEXT_6);
     } else { 
    stringBuffer.append(TEXT_7);
     } 
     if (t.hasServerRpc()) { 
    stringBuffer.append(TEXT_8);
    stringBuffer.append( target );
    stringBuffer.append(TEXT_9);
    stringBuffer.append( serverRpcName );
    stringBuffer.append(TEXT_10);
     } 
     if (t.hasClientRpc()) { 
    stringBuffer.append(TEXT_11);
    stringBuffer.append( target );
    stringBuffer.append(TEXT_12);
    stringBuffer.append( clientRpcName );
    stringBuffer.append(TEXT_13);
     } 
     if (t.hasState()) { 
    stringBuffer.append(TEXT_14);
    stringBuffer.append( target );
    stringBuffer.append(TEXT_15);
    stringBuffer.append( stateName );
    stringBuffer.append(TEXT_16);
     } 
    stringBuffer.append(TEXT_17);
    stringBuffer.append( componentName );
    stringBuffer.append(TEXT_18);
    stringBuffer.append( typeName );
    stringBuffer.append(TEXT_19);
     if (t.hasServerRpc()) { 
    stringBuffer.append(TEXT_20);
    stringBuffer.append( serverRpcName );
    stringBuffer.append(TEXT_21);
    stringBuffer.append( serverRpcName );
    stringBuffer.append(TEXT_22);
     } 
    stringBuffer.append(TEXT_23);
    stringBuffer.append( typeName );
    stringBuffer.append(TEXT_24);
     if (t.hasClientRpc()) { 
    stringBuffer.append(TEXT_25);
    stringBuffer.append( clientRpcName );
    stringBuffer.append(TEXT_26);
    stringBuffer.append( clientRpcName );
    stringBuffer.append(TEXT_27);
     } 
     if (t.hasServerRpc()) { 
    stringBuffer.append(TEXT_28);
     } 
    stringBuffer.append(TEXT_29);
    stringBuffer.append( widgetName );
    stringBuffer.append(TEXT_30);
    stringBuffer.append( widgetName );
    stringBuffer.append(TEXT_31);
    stringBuffer.append( widgetName );
    stringBuffer.append(TEXT_32);
     if (t.hasState()) { 
    stringBuffer.append(TEXT_33);
    stringBuffer.append( stateName );
    stringBuffer.append(TEXT_34);
    stringBuffer.append( stateName );
    stringBuffer.append(TEXT_35);
     } 
    stringBuffer.append(TEXT_36);
    stringBuffer.append(TEXT_37);
    return stringBuffer.toString();
  }
}