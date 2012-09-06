package com.vaadin.integration.eclipse.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.JavaModelException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.vaadin.integration.eclipse.IVaadinFacetInstallDataModelProperties;

public class PortletConfigurationUtil {

    /**
     * Adds a portlet to the portlet configuration files.
     * 
     * The corresponding servlet should already exist in web.xml .
     * 
     * @param project
     * @param applicationName
     *            servlet name (from path) in web.xml or application classname
     *            for portlet 2.0
     * @param portletClass
     * @param portletName
     *            must be different from servlet name
     * @param portletTitle
     *            is used both as the full and the short title and
     * @param category
     *            the Liferay portlet category to add the portlet to
     * @param portletVersion
     *            {@link IVaadinFacetInstallDataModelProperties#PORTLET_VERSION10}
     *            or
     *            {@link IVaadinFacetInstallDataModelProperties#PORTLET_VERSION20}
     * @param uiMapping
     *            true to create a UI mapping (Vaadin 7) instead of an
     *            Application mapping
     * @throws CoreException
     */
    public static void addPortlet(IProject project, String applicationName,
            String portletClass, String portletName, String portletTitle,
            String category, String portletVersion, boolean uiMapping)
            throws CoreException {

        // TODO check indentation issues (first inserted line)

        // portlets.xml
        addPortletToPortletsXml(project, applicationName, portletClass,
                portletName, portletTitle, portletVersion, uiMapping);

        // liferay-portlet.xml
        addPortletToLiferayPortletXml(project, portletName);

        // liferay-display.xml
        addPortletToLiferayDisplayXml(project, portletName, category);

        // liferay-plugin-package.properties
        addPortletToLiferayPluginPackageProperties(project, portletName,
                portletTitle, category);
    }

    private static IFile getPortletConfigurationFile(IProject project,
            String filename) throws CoreException {
        return ProjectUtil.getWebInfFolder(project).getFile(filename);
    }

    private static void addPortletToPortletsXml(IProject project,
            String applicationName, String portletClass, String portletName,
            String portletTitle, String portletVersion, boolean uiMapping)
            throws CoreException {
        try {
            boolean portlet2 = IVaadinFacetInstallDataModelProperties.PORTLET_VERSION20
                    .equals(portletVersion);

            // create the file if it does not exist
            String fileTemplateName = portlet2 ? "portlet/portlet2xmlstub.txt"
                    : "portlet/portletxmlstub.txt";
            IFile portletXmlFile = VaadinPluginUtil.ensureFileFromTemplate(
                    getPortletConfigurationFile(project, "portlet.xml"),
                    fileTemplateName);

            // prepare the portlet section from template
            String sectionTemplateName = portlet2 ? "portlet/portlet2xml_portletstub.txt"
                    : "portlet/portletxml_portletstub.txt";
            String portletstub = VaadinPluginUtil
                    .readTextFromTemplate(sectionTemplateName);

            // generic portlet configuration
            portletstub = portletstub.replaceAll("STUB_PORTLETNAME",
                    portletName);
            portletstub = portletstub.replaceAll("STUB_DISPLAYNAME",
                    portletTitle);
            portletstub = portletstub.replaceAll("STUB_PORTLETCLASS",
                    portletClass);
            portletstub = portletstub.replaceAll("STUB_APPLICATION",
                    applicationName);
            portletstub = portletstub.replaceAll("STUB_INITPARAMNAME",
                    uiMapping ? WebXmlUtil.VAADIN_UI_CLASS_PARAMETER
                            : WebXmlUtil.VAADIN_APPLICATION_CLASS_PARAMETER);

            // these are for Liferay
            portletstub = portletstub.replaceAll("STUB_PORTLETTITLE",
                    portletTitle);
            portletstub = portletstub.replaceAll("STUB_SHORTTITLE",
                    portletTitle);

            // insert the portlet section in the <portlet-app> tag
            modifyXml(portletXmlFile,
                    new AddToTagXmlModifier(portletXmlFile.getName(),
                            "portlet-app", portletstub));
        } catch (IOException e) {
            throw ErrorUtil.newCoreException(
                    "Failed to add a portlet to portlets.xml", e);
        }
    }

    private static void addPortletToLiferayPortletXml(IProject project,
            String portletName) throws CoreException {
        try {
            // create the file if it does not exist
            IFile portletXmlFile = VaadinPluginUtil
                    .ensureFileFromTemplate(
                            getPortletConfigurationFile(project,
                                    "liferay-portlet.xml"),
                            "portlet/liferayportletxmlstub.txt");

            // prepare the portlet section from template
            String portletstub = VaadinPluginUtil
                    .readTextFromTemplate("portlet/liferayportletxml_portletstub.txt");

            portletstub = portletstub.replaceAll("STUB_PORTLETNAME",
                    portletName);

            // insert the portlet section in the <liferay-portlet-app> tag
            modifyXml(portletXmlFile,
                    new AddToTagXmlModifier(portletXmlFile.getName(),
                            "liferay-portlet-app", portletstub));
        } catch (IOException e) {
            throw ErrorUtil.newCoreException(
                    "Failed to add a portlet to liferay-portlet.xml", e);
        }
    }

    private static void addPortletToLiferayDisplayXml(IProject project,
            String portletName, String category) throws CoreException {
        // create the file if it does not exist
        IFile portletXmlFile = VaadinPluginUtil.ensureFileFromTemplate(
                getPortletConfigurationFile(project, "liferay-display.xml"),
                "portlet/liferaydisplayxmlstub.txt");

        // add the portlet section to the category
        // create the category section if does not exist
        modifyXml(portletXmlFile,
                new LiferayDisplayXmlModifier(portletXmlFile.getName(),
                        category, portletName));
    }

    private static void addPortletToLiferayPluginPackageProperties(
            IProject project, String pluginName, String shortDescription,
            String moduleGroup) throws CoreException {
        IFile file = getPortletConfigurationFile(project,
                "liferay-plugin-package.properties");
        // create the file if it does not exist
        try {
            if (!file.exists()) {
                String stub = VaadinPluginUtil
                        .readTextFromTemplate("portlet/liferay-plugin-package.properties.txt");

                stub = stub.replaceAll("STUB_PLUGINNAME", pluginName);
                stub = stub.replaceAll("STUB_SHORTDESCRIPTION",
                        shortDescription);
                stub = stub.replaceAll("STUB_MODULEGROUP", moduleGroup);

                ByteArrayInputStream stubstream = new ByteArrayInputStream(
                        stub.getBytes());

                file.create(stubstream, true, null);
            }
        } catch (JavaModelException e) {
            throw ErrorUtil.newCoreException(
                    "Failed to create file " + file.getName(), e);
        } catch (IOException e) {
            throw ErrorUtil.newCoreException(
                    "Failed to create file " + file.getName(), e);
        }
    }

    /**
     * Modify an XML file with a given modifier.
     * 
     * @param portletsXmlFile
     *            the file to modify
     * @param Modifier
     *            the class performing the actual modification
     * @throws CoreException
     */
    private static void modifyXml(IFile portletsXmlFile, XmlModifier modifier)
            throws CoreException {
        // TODO should use Eclipse facilities to handle open editors better
        InputStream input = null;
        OutputStream output = null;
        try {
            // read and parse the original file
            DocumentBuilderFactory docFactory = DocumentBuilderFactory
                    .newInstance();
            docFactory.setValidating(false);
            docFactory.setSchema(null);
            docFactory.setAttribute(
                    "http://apache.org/xml/features/dom/defer-node-expansion",
                    Boolean.FALSE);
            docFactory
                    .setAttribute(
                            "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                            Boolean.FALSE);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            input = portletsXmlFile.getContents();
            Document doc = docBuilder.parse(input);
            DocumentType docType = doc.getDoctype();

            modifier.performModification(docBuilder, doc);

            try {
                // write out the modified file
                TransformerFactory transFactory = TransformerFactory
                        .newInstance();
                transFactory.setURIResolver(null);
                transFactory.setAttribute("indent-number", 4);
                Transformer transformer = transFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                if (docType != null) {
                    transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
                            docType.getSystemId());
                    transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC,
                            docType.getPublicId());
                }
                // this is a trick to get the indentations to almost work;
                // nevertheless, some tricks are needed below for first lines of
                // added sections
                ByteArrayOutputStream memoryOutputStream = new ByteArrayOutputStream(
                        1024);
                StreamResult result = new StreamResult(new OutputStreamWriter(
                        memoryOutputStream));
                // StreamResult result = new StreamResult(portletsXmlFile
                // .getLocation().toFile());
                DOMSource source = new DOMSource(doc);
                transformer.transform(source, result);

                if (input != null) {
                    input.close();
                    input = null;
                }

                output = new FileOutputStream(portletsXmlFile.getLocation()
                        .toFile());
                output.write(memoryOutputStream.toByteArray());
                memoryOutputStream.close();
            } catch (Exception ex) {
                ErrorUtil
                        .handleBackgroundException(
                                IStatus.ERROR,
                                "Failed to transform the XML document, should retry without indentation transformation",
                                ex);
                if (output != null) {
                    output.close();
                }
            }

            // tell Eclipse that the file has been modified
            portletsXmlFile.refreshLocal(IResource.DEPTH_ZERO, null);

        } catch (IOException e) {
            throw ErrorUtil.newCoreException("Failed to modify XML file "
                    + portletsXmlFile.getName(), e);
        } catch (ParserConfigurationException e) {
            throw ErrorUtil.newCoreException("Failed to initialize XML parser",
                    e);
        } catch (SAXException e) {
            throw ErrorUtil.newCoreException("Failed to parse XML file "
                    + portletsXmlFile.getName(), e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private static interface XmlModifier {
        public void performModification(DocumentBuilder docBuilder, Document doc)
                throws SAXException, IOException, CoreException;
    }

    /**
     * Modify an XML document by adding given String content to the first
     * instance of the named tag.
     * 
     * The named parent tag must exist in the document.
     */
    private static class AddToTagXmlModifier implements XmlModifier {
        private final String filename;
        private final String tagname;
        private final String content;

        public AddToTagXmlModifier(String filename, String tagname,
                String content) {
            this.filename = filename;
            this.tagname = tagname;
            this.content = content;
        }

        public void performModification(DocumentBuilder docBuilder, Document doc)
                throws SAXException, IOException, CoreException {
            // parse the content to add
            Document contentDoc = docBuilder.parse(new ByteArrayInputStream(
                    content.getBytes()));
            NodeList newChildren = contentDoc.getChildNodes();

            // add content
            // note that this requires that the <tagname> element exists
            NodeList nodeList = doc.getElementsByTagName(tagname);

            addWhitespace(doc, nodeList.item(0), "    ");

            if (nodeList.getLength() > 0) {
                for (int i = 0; i < newChildren.getLength(); ++i) {
                    Node newChild = newChildren.item(i);
                    nodeList.item(0).appendChild(doc.adoptNode(newChild));
                }
            } else {
                throw ErrorUtil.newCoreException("The XML file " + filename
                        + " is missing the tag " + tagname, null);
            }
        }
    }

    /**
     * Modify an XML document for liferay-display.xml by adding a portlet
     * description to a category, creating the category section if it does not
     * exist.
     */
    private static class LiferayDisplayXmlModifier implements XmlModifier {
        private final String filename;
        private final String category;
        private final String portletName;

        public LiferayDisplayXmlModifier(String filename, String category,
                String portletName) {
            this.filename = filename;
            this.category = category;
            this.portletName = portletName;
        }

        public void performModification(DocumentBuilder docBuilder, Document doc)
                throws SAXException, IOException, CoreException {
            // find the category node
            Node categoryNode = null;
            NodeList nodeList = doc.getElementsByTagName("category");
            for (int i = 0; i < nodeList.getLength(); ++i) {
                Node node = nodeList.item(i);
                if (category.equals(node.getAttributes().getNamedItem("name"))) {
                    categoryNode = node;
                }
            }

            // create the category node if it does not exist
            if (categoryNode == null) {
                NodeList topNodes = doc.getElementsByTagName("display");
                if (topNodes.getLength() < 1) {
                    throw ErrorUtil.newCoreException("The XML file " + filename
                            + " is missing the display tag", null);
                }
                categoryNode = doc.createElement("category");

                addWhitespace(doc, topNodes.item(0), "    ");
                topNodes.item(0).appendChild(categoryNode);

                Attr attribute = doc.createAttribute("name");
                attribute.setValue(category);
                categoryNode.getAttributes().setNamedItem(attribute);
            }

            // add portlet info to categoryNode
            Node portletNode = doc.createElement("portlet");
            categoryNode.appendChild(portletNode);

            Attr idAttribute = doc.createAttribute("id");
            idAttribute.setValue(portletName);
            portletNode.getAttributes().setNamedItem(idAttribute);
        }
    }

    private static void addWhitespace(Document doc, Node node, String content) {
        // indentation/formatting hack
        DocumentFragment indent = doc.createDocumentFragment();
        indent.setTextContent(content);
        node.appendChild(indent);
    }
}
