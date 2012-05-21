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
public class CleanComponentTemplate implements Template {   
  protected static String nl;
  public static synchronized CleanComponentTemplate create(String lineSeparator)
  {
    nl = lineSeparator;
    CleanComponentTemplate result = new CleanComponentTemplate();
    nl = null;
    return result;
  }

  public final String NL = nl == null ? (System.getProperties().getProperty("line.separator")) : nl;
  protected final String TEXT_1 = "package ";
  protected final String TEXT_2 = ";" + NL + "" + NL + "import java.util.Map;" + NL + "" + NL + "import com.vaadin.terminal.PaintException;" + NL + "import com.vaadin.terminal.PaintTarget;" + NL + "import com.vaadin.ui.AbstractComponent;" + NL + "" + NL + "/**" + NL + " * Server side component for the V";
  protected final String TEXT_3 = " widget." + NL + " */" + NL + " @com.vaadin.ui.ClientWidget(";
  protected final String TEXT_4 = ".client.ui.V";
  protected final String TEXT_5 = ".class)" + NL + "public class ";
  protected final String TEXT_6 = " extends AbstractComponent {" + NL + " " + NL + "    @Override" + NL + "    public void paintContent(PaintTarget target) throws PaintException {" + NL + "        super.paintContent(target);" + NL + "" + NL + "    }" + NL + "" + NL + "    /**" + NL + "     * Receive and handle events and other variable changes from the client." + NL + "     * " + NL + "     * {@inheritDoc}" + NL + "     */" + NL + "    @Override" + NL + "    public void changeVariables(Object source, Map<String, Object> variables) {" + NL + "        super.changeVariables(source, variables);" + NL + "" + NL + "    }" + NL + "" + NL + "}" + NL;
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