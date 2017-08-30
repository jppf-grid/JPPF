/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.node.policy;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import org.jppf.JPPFException;
import org.jppf.utils.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

/**
 * This class is a parser for XML Execution Policy documents.
 * @author Laurent Cohen
 */
public class PolicyParser {
  /**
   * List of possible rule names.
   */
  private static final List<String> RULE_NAMES = Arrays.asList("NOT", "AND", "OR", "XOR", "LessThan", "AtMost", "AtLeast", "MoreThan",
      "BetweenII", "BetweenIE", "BetweenEI", "BetweenEE", "Equal", "Contains", "OneOf", "RegExp", "CustomRule", "Script", "Preference",
      "IsInIPv4Subnet", "IsInIPv6Subnet", NodesMatching.XML_TAG);
  /**
   * The DOM parser used to build the descriptor tree.
   */
  private DocumentBuilder parser = null;

  /**
   * Initialize this parser.
   * @throws Exception if the DOM parser could not be initialized.
   */
  public PolicyParser() throws Exception {
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
  public PolicyDescriptor parse(final String docPath) throws Exception {
    InputStream is = FileUtils.getFileInputStream(docPath);
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
  public PolicyDescriptor parse(final Reader reader) throws Exception {
    InputSource is = new InputSource(reader);
    Document doc = parser.parse(is);
    return generateTree(findFirstElement(doc));
  }

  /**
   * Find the first node in a document that is an element node.
   * @param doc the document whose children are looked up.
   * @return a <code>Node</code> instance if one was found, or null otherwise.
   */
  private Node findFirstElement(final Document doc) {
    NodeList list = doc.getChildNodes();
    for (int i=0; i<list.getLength(); i++) {
      Node node = list.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) return node;
    }
    return null;
  }

  /**
   * Generate an <code>PolicyDescriptor</code> tree from a DOM subtree.
   * @param node the document to generate the tree from.
   * @return an <code>OptionDescriptor</code> instance, root of the generated tree,
   * or null if the document could not be parsed.
   */
  private PolicyDescriptor generateTree(final Node node) {
    PolicyDescriptor desc = new PolicyDescriptor();
    desc.type = node.getNodeName();
    if ("Script".equals(desc.type)) desc.script = getTextNodeValue(node);
    desc.valueType = getAttributeValue(node, "valueType", "string");
    desc.ignoreCase = getAttributeValue(node, "ignoreCase", "false");
    desc.className = getAttributeValue(node, "class", null);
    desc.language = getAttributeValue(node, "language", null);
    desc.operator = getAttributeValue(node, "operator", "EQUAL");
    desc.expected = getAttributeValue(node, "expected", "0");
    NodeList list = node.getChildNodes();
    for (int i=0; i<list.getLength(); i++) {
      Node childNode = list.item(i);
      if (childNode.getNodeType() == Node.ELEMENT_NODE) {
        String name = childNode.getNodeName();
        /*if ("Script".equals(name)) desc.script = getTextNodeValue(childNode);
        else*/ if (RULE_NAMES.contains(name)) desc.children.add(generateTree(childNode));
        else if ("Property".equals(name) || "Value".equals(name) || "Subnet".equals(name)) desc.operands.add(getTextNodeValue(childNode));
        else if ("Arg".equals(name)) desc.arguments.add(getTextNodeValue(childNode));
      }
    }
    return desc;
  }

  /**
   * Get the value of a node's text subelement.
   * @param node the node to generate whose child is a text node.
   * @return the text as a string.
   */
  private String getTextNodeValue(final Node node) {
    NodeList children = node.getChildNodes();
    for (int j=0; j<children.getLength(); j++) {
      Node childNode = children.item(j);
      int type = childNode.getNodeType();
      if ((type == Node.TEXT_NODE) || (type == Node.CDATA_SECTION_NODE)) return childNode.getNodeValue();
    }
    return null;
  }

  /**
   * Get the value of the attriobute of a node.
   * @param node the node from which to get the attribute.
   * @param name the name of the attribute to get.
   * @param def the default value to use if the attribute is not defined.
   * @return the attribute value as a string.
   */
  private String getAttributeValue(final Node node, final String name, final String def) {
    NamedNodeMap attrMap = node.getAttributes();
    Node attrNode = attrMap.getNamedItem(name);
    return attrNode == null ? def : attrNode.getNodeValue();
  }

  /**
   * Test of the parser.
   * @param args not used.
   */
  public static void main(final String...args) {
    try {
      String docPath = "ExecutionPolicy.xml";
      JPPFErrorReporter reporter = new JPPFErrorReporter(docPath);
      String schemaPath = "org/jppf/schemas/ExecutionPolicy.xsd";
      SchemaValidator validator = new SchemaValidator(reporter);
      if (!validator.validate(docPath, schemaPath)) {
        String s = "the document " + docPath;
        System.out.println(s + " has errors.");
        System.out.println("fatal errors: " + reporter.allFatalErrorsAsStrings());
        System.out.println("errors      : " + reporter.allErrorsAsStrings());
        System.out.println("warnings    : " + reporter.allWarningsAsStrings());
        return;
      }
      PolicyParser parser = new PolicyParser();
      PolicyDescriptor desc = parser.parse(docPath);
      ExecutionPolicy policy = new PolicyBuilder().buildPolicy(desc.children.get(0));
      System.out.println("Successfully build policy object:\n" + policy);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Parse an XML document representing an execution policy.
   * @param policyContent an XML string containing the policy.
   * @return an <code>ExecutionPolicy</code> instance.
   * @throws Exception if an error occurs during the validation or parsing.
   */
  public static ExecutionPolicy parsePolicy(final String policyContent) throws Exception {
    return parsePolicy(new StringReader(policyContent));
  }

  /**
   * Parse an XML document representing an execution policy from a file path.
   * @param docPath path to the XML document file.
   * @return an <code>ExecutionPolicy</code> instance.
   * @throws Exception if an error occurs during the validation or parsing.
   */
  public static ExecutionPolicy parsePolicyFile(final String docPath) throws Exception {
    return parsePolicy(FileUtils.getFileReader(docPath));
  }

  /**
   * Parse an XML document representing an execution policy from a file path.
   * @param policyFile abstract path of the XML document file.
   * @return an <code>ExecutionPolicy</code> instance.
   * @throws Exception if an error occurs during the validation or parsing.
   */
  public static ExecutionPolicy parsePolicy(final File policyFile) throws Exception {
    return parsePolicy(new BufferedReader(new FileReader(policyFile)));
  }

  /**
   * Parse an XML document representing an execution policy from an input stream.
   * @param stream an input stream from which the XML representation of the policy is read.
   * @return an <code>ExecutionPolicy</code> instance.
   * @throws Exception if an error occurs during the validation or parsing.
   */
  public static ExecutionPolicy parsePolicy(final InputStream stream) throws Exception {
    return parsePolicy(new InputStreamReader(stream));
  }

  /**
   * Parse an XML document representing an execution policy from a reader.
   * @param reader reader from which the XML representation of the policy is read.
   * @return an <code>ExecutionPolicy</code> instance.
   * @throws Exception if an error occurs during the validation or parsing.
   */
  public static ExecutionPolicy parsePolicy(final Reader reader) throws Exception {
    PolicyDescriptor desc = new PolicyParser().parse(reader);
    return desc.children.isEmpty() ? null :  new PolicyBuilder().buildPolicy(desc.children.get(0));
  }

  /**
   * Validate an XML document representing an execution policy against the
   * <a href="http://www.jppf.org/schemas/ExecutionPolicy.xsd">JPPF Execution Policy schema</a>.
   * @param policyContent the XML content of the policy document.
   * @throws JPPFException if there is a validation error. The details of the errors are included in the exception message.
   * @throws Exception if an error occurs during the validation.
   */
  public static void validatePolicy(final String policyContent) throws JPPFException, Exception {
    try (Reader reader = new StringReader(policyContent)) {
      validatePolicy(reader);
    }
  }

  /**
   * Validate an XML document representing an execution policy against the
   * <a href="http://www.jppf.org/schemas/ExecutionPolicy.xsd">JPPF Execution Policy schema</a>.
   * @param docPath path to the XML document file.
   * @throws JPPFException if there is a validation error. The details of the errors are included in the exception message.
   * @throws Exception if an error occurs during the validation.
   */
  public static void validatePolicyFile(final String docPath) throws JPPFException, Exception {
    validatePolicy(FileUtils.getFileReader(docPath));
  }

  /**
   * Validate an XML document representing an execution policy against the
   * <a href="http://www.jppf.org/schemas/ExecutionPolicy.xsd">JPPF Execution Policy schema</a>.
   * @param docPath abstract path of the XML document file.
   * @throws JPPFException if there is a validation error. The details of the errors are included in the exception message.
   * @throws Exception if an error occurs during the validation.
   */
  public static void validatePolicy(final File docPath) throws JPPFException, Exception {
    validatePolicy(new BufferedReader(new FileReader(docPath)));
  }

  /**
   * Validate an XML document representing an execution policy against the
   * <a href="http://www.jppf.org/schemas/ExecutionPolicy.xsd">JPPF Execution Policy schema</a>.
   * @param stream an input stream from which the XML representation of the policy is read.
   * @throws JPPFException if there is a validation error. The details of the errors are included in the exception message.
   * @throws Exception if an error occurs during the validation or parsing.
   */
  public static void validatePolicy(final InputStream stream) throws JPPFException, Exception {
    validatePolicy(new InputStreamReader(stream));
  }

  /**
   * Validate an XML document representing an execution policy against the
   * <a href="http://www.jppf.org/schemas/ExecutionPolicy.xsd">JPPF Execution Policy schema</a>.
   * @param reader reader from which the XML representation of the policy is read.
   * @throws JPPFException if there is a validation error. The details of the errors are included in the exception message.
   * @throws Exception if an error occurs during the validation or parsing.
   */
  public static void validatePolicy(final Reader reader) throws JPPFException, Exception {
    JPPFErrorReporter reporter = new JPPFErrorReporter("XML validator");
    String schemaPath = "org/jppf/schemas/ExecutionPolicy.xsd";
    SchemaValidator validator = new SchemaValidator(reporter);
    if (!validator.validate(reader, FileUtils.getFileReader(schemaPath))) {
      StringBuilder sb = new StringBuilder();
      //sb.append("The XML document has errors:\n");
      if (!reporter.fatalErrors.isEmpty()) sb.append("fatal errors: ").append(reporter.allFatalErrorsAsStrings()).append('\n');
      if (!reporter.errors.isEmpty())      sb.append("errors      : ").append(reporter.allErrorsAsStrings()).append('\n');
      if (!reporter.warnings.isEmpty())    sb.append("warnings    : ").append(reporter.allWarningsAsStrings()).append('\n');
      throw new JPPFException(sb.toString());
    }
  }
}
