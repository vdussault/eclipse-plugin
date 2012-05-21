package com.vaadin.integration.eclipse.templates.v60;

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
  protected final String TEXT_2 = ";" + NL + "" + NL + "import com.vaadin.terminal.PaintException;" + NL + "import com.vaadin.terminal.PaintTarget;" + NL + "import com.vaadin.ui.AbstractComponent;" + NL + "" + NL + "public class ";
  protected final String TEXT_3 = " extends AbstractComponent {" + NL + "" + NL + "    @Override" + NL + "    public String getTag() {" + NL + "        return \"";
  protected final String TEXT_4 = "\";" + NL + "    }" + NL + "" + NL + "    @Override" + NL + "    public void paintContent(PaintTarget target) throws PaintException {" + NL + "        super.paintContent(target);" + NL + "" + NL + "        // TODO Paint any component specific content by setting attributes" + NL + "        // These attributes can be read in updateFromUIDL in the widget." + NL + "    }" + NL + "" + NL + "}";
  protected final String TEXT_5 = NL;

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
    stringBuffer.append( componentName.toLowerCase() );
    stringBuffer.append(TEXT_4);
    stringBuffer.append(TEXT_5);
    return stringBuffer.toString();
  }
}