/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.ui.options.xml;

import java.io.*;
import java.net.URL;

import javax.xml.parsers.*;

import org.jppf.ui.options.xml.OptionDescriptor.ItemDescriptor;
import org.jppf.ui.options.xml.OptionDescriptor.ListenerDescriptor;
import org.jppf.ui.options.xml.OptionDescriptor.ScriptDescriptor;
import org.jppf.utils.*;
import org.slf4j.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

/**
 * Instances of this class are used to parse an XML document, describing
 * an options page, into a tree of option descriptors.
 * @author Laurent Cohen
 */
public class OptionDescriptorParser {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(OptionDescriptorParser.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The DOM parser used to build the descriptor tree.
   */
  private DocumentBuilder parser = null;

  /**
   * Initialize this parser.
   * @throws Exception if the DOM parser could not be initialized.
   */
  public OptionDescriptorParser() throws Exception {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    parser = dbf.newDocumentBuilder();
  }

  /**
   * Parse an XML document in a file into a tree of option descriptors.
   * @param docPath the path to XML document to parse.
   * @return an <code>OptionDescriptor</code> instance, root of the generated tree,
   * or null if the document could not be parsed.
   * @throws Exception if an error occurs while parsing the document.
   */
  public OptionDescriptor parse(final String docPath) throws Exception {
    InputStream is = FileUtils.getFileInputStream(docPath);
    if (is == null) {
      URL url = this.getClass().getClassLoader().getResource(docPath);
      is = url.openStream();
    }
    if (is == null) return null;
    Document doc = parser.parse(is);
    return generateTree(findFirstElement(doc));
  }

  /**
   * Parse an XML document in a reader into a tree of option descriptors.
   * @param reader the reader providing the XML document.
   * @return an <code>OptionDescriptor</code> instance, root of the generated tree,
   * or null if the document could not be parsed.
   * @throws Exception if an error occurs while parsing the document.
   */
  public OptionDescriptor parse(final Reader reader) throws Exception {
    InputSource is = new InputSource(reader);
    Document doc = parser.parse(is);
    return generateTree(findFirstElement(doc));
  }

  /**
   * Find the first node in a document that is an element node.
   * @param doc the document whose children are looked up.
   * @return a <code>Node</code> instance if one was found, or null otherwise.
   */
  public Node findFirstElement(final Document doc) {
    NodeList list = doc.getChildNodes();
    for (int i=0; i<list.getLength(); i++) {
      Node node = list.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) return node;
    }
    return null;
  }

  /**
   * Generate an <code>OptionDescriptor</code> tree from a DOM subtree.
   * @param node the document to generate the tree from.
   * @return an <code>OptionDescriptor</code> instance, root of the generated tree,
   * or null if the document could not be parsed.
   */
  public OptionDescriptor generateTree(final Node node) {
    OptionDescriptor desc = new OptionDescriptor();
    NamedNodeMap attrMap = node.getAttributes();
    desc.type = attrMap.getNamedItem("type").getNodeValue();
    desc.name = attrMap.getNamedItem("name").getNodeValue();
    Node i18nNode = attrMap.getNamedItem("i18n");
    if (i18nNode != null) desc.i18n = i18nNode.getNodeValue();
    NodeList list = node.getChildNodes();
    for (int i=0; i<list.getLength(); i++) {
      Node childNode = list.item(i);
      if (childNode.getNodeType() == Node.ELEMENT_NODE) {
        String name = childNode.getNodeName();
        if ("child".equals(name)) desc.children.add(generateTree(childNode));
        else if ("script".equals(name)) desc.scripts.add(createScriptDescriptor(childNode));
        else if ("initializer".equals(name)) desc.initializer = createListenerDescriptor(childNode);
        else if ("finalizer".equals(name)) desc.finalizer = createListenerDescriptor(childNode);
        else if ("property".equals(name)) addProperty(desc, childNode);
        else if ("item".equals(name)) desc.items.add(createItemDescriptor(childNode));
        else if ("listener".equals(name)) desc.listeners.add(createListenerDescriptor(childNode));
        else if ("import".equals(name)) desc.children.add(loadImport(childNode));
        else if ("mouselistener".equals(name)) desc.mouseListener = createListenerDescriptor(childNode);
      }
    }
    return desc;
  }

  /**
   * Create an item descriptor from a DOM node.
   * @param node the node to generate the listener from.
   * @return a <code>ItemDescriptor</code> instance.
   */
  public ItemDescriptor createItemDescriptor(final Node node) {
    ItemDescriptor desc = new ItemDescriptor();
    NamedNodeMap attrMap = node.getAttributes();
    desc.name = attrMap.getNamedItem("name").getNodeValue();
    desc.selected = attrMap.getNamedItem("selected").getNodeValue();
    return desc;
  }

  /**
   * Create a listener descriptor from a DOM node.
   * @param node the node to generate the listener from.
   * @return a <code>ListenerDescriptor</code> instance.
   */
  public ListenerDescriptor createListenerDescriptor(final Node node) {
    ListenerDescriptor desc = new ListenerDescriptor();
    NamedNodeMap attrMap = node.getAttributes();
    desc.type = attrMap.getNamedItem("type").getNodeValue();
    setListenerAttributes(node, desc);
    return desc;
  }

  /**
   * Set the attributes of a listener descriptor.
   * @param node the parent listener node.
   * @param desc the listener descriptor whose attributes have to be set.
   */
  public void setListenerAttributes(final Node node, final ListenerDescriptor desc) {
    NodeList list = node.getChildNodes();
    for (int i=0; i<list.getLength(); i++) {
      Node childNode = list.item(i);
      if (childNode.getNodeType() == Node.ELEMENT_NODE) {
        String name = childNode.getNodeName();
        if ("class".equals(name)) desc.className = getTextNodeValue(childNode);
        else if ("script".equals(name)) desc.script = createScriptDescriptor(childNode);
        else continue;
        break;
      }
    }
    getClass();
  }

  /**
   * Get the value of a node's text subelement.
   * @param node the node to generate whose child is a text node.
   * @return the text as a string.
   */
  public String getTextNodeValue(final Node node) {
    NodeList children = node.getChildNodes();
    for (int j=0; j<children.getLength(); j++) {
      Node tmpNode = children.item(j);
      if (tmpNode.getNodeType() == Node.TEXT_NODE) return tmpNode.getNodeValue();
    }
    return null;
  }

  /**
   * Create a script descriptor from a DOM node.
   * @param node the node to generate the listener from.
   * @return a <code>ScriptDescriptor</code> instance.
   */
  public ScriptDescriptor createScriptDescriptor(final Node node) {
    ScriptDescriptor desc = new ScriptDescriptor();
    NamedNodeMap attrs = node.getAttributes();
    desc.language = attrs.getNamedItem("language").getNodeValue();
    Node source = attrs.getNamedItem("source");
    if (source != null) desc.source = source.getNodeValue();
    if ((desc.source == null) || "inline".equalsIgnoreCase(desc.source)) {
      NodeList children = node.getChildNodes();
      for (int j=0; j<children.getLength(); j++) {
        Node tmpNode = children.item(j);
        if ((tmpNode.getNodeType() == Node.CDATA_SECTION_NODE) || (tmpNode.getNodeType() == Node.TEXT_NODE)) {
          desc.content = tmpNode.getNodeValue();
          break;
        }
      }
    } else {
      InputStream is = null;
      try {
        try {
          is = new URL(desc.source).openStream();
        } catch(Exception e) {
          if (debugEnabled) log.debug(e.getMessage());
        }
        try {
          if (is == null) is = FileUtils.getFileInputStream(desc.source);
        } catch(Exception e) {
          if (debugEnabled) log.debug(e.getMessage(), e);
        }
        if (is != null) {
          try (InputStreamReader reader = new InputStreamReader(is)) {
            desc.content = FileUtils.readTextFile(reader);
          }
        }
      } catch(Exception e) {
      }
    }
    return desc;
  }

  /**
   * Add a property to an option descriptor from a DOM node.
   * @param desc the option descriptor to add the property to.
   * @param node the node to get the property name and value from.
   */
  public void addProperty(final OptionDescriptor desc, final Node node) {
    NamedNodeMap attrMap = node.getAttributes();
    String name = attrMap.getNamedItem("name").getNodeValue();
    String value = attrMap.getNamedItem("value").getNodeValue();
    if ((name != null) && (value != null)) desc.setProperty(name, value);
  }

  /**
   * Import an external XML descriptor.
   * @param node the node to get the import description from.
   * @return a <code>OptionDescriptor</code> instance.
   */
  public OptionDescriptor loadImport(final Node node) {
    NamedNodeMap attrMap = node.getAttributes();
    OptionDescriptor desc = new OptionDescriptor();
    desc.type = "import";
    Node attr = attrMap.getNamedItem("pluggableView");
    desc.setProperty("pluggableView", (attr != null) ? attr.getNodeValue() : "");
    desc.setProperty("source", attrMap.getNamedItem("source").getNodeValue());
    if (attrMap.getNamedItem("location") != null)  desc.setProperty("location", attrMap.getNamedItem("location").getNodeValue());
    Node debugNode = attrMap.getNamedItem("debug");
    if (debugNode != null) desc.setProperty("debug", debugNode.getNodeValue());
    NodeList children = node.getChildNodes();
    for (int j=0; j<children.getLength(); j++) {
      Node child = children.item(j);
      if ("script".equals(child.getNodeName())) {
        desc.scripts.add(createScriptDescriptor(child));
        break;
      }
    }
    return desc;
  }
}
