/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.ui.options.xml;

import java.io.InputStream;
import javax.xml.parsers.*;
import org.jppf.ui.options.xml.OptionDescriptor.*;
import org.jppf.utils.FileUtils;
import org.w3c.dom.*;

/**
 * Instances of this class are used to parse an XML document, describing
 * an options page, into a tree of option descriptors. 
 * @author Laurent Cohen
 */
public class OptionDescriptorParser
{
	/**
	 * The DOM parser used to build the descriptor tree.
	 */
	private DocumentBuilder parser = null;

	/**
	 * Initialize this parser.
	 * @throws Exception if the DOM parser could not be initialized.
	 */
	public OptionDescriptorParser() throws Exception
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		parser = dbf.newDocumentBuilder();
	}

	/**
	 * Parse an XML document into a tree of option descriptors. 
	 * @param docPath the path to XML document to parse.
	 * @return an <code>OptionDescriptor</code> instance, root of the generated tree,
	 * or null if the docuement could not be parsed.
	 * @throws Exception if an error occurs while parsing the document.
	 */
	public OptionDescriptor parse(String docPath) throws Exception
	{
		InputStream is = FileUtils.findFile(docPath);
		if (is == null) return null;
		Document doc = parser.parse(is);
		return generateTree(doc.getFirstChild());
	}

	/**
	 * Generate an <code>OptionDescriptor</code> tree from a DOM subtree.
	 * @param node the document to generate the tree from.
	 * @return an <code>OptionDescriptor</code> instance, root of the generated tree,
	 * or null if the docuement could not be parsed.
	 */
	public OptionDescriptor generateTree(Node node)
	{
		OptionDescriptor desc = new OptionDescriptor();
		NamedNodeMap attrMap = node.getAttributes();
		desc.type = attrMap.getNamedItem("type").getNodeValue();
		desc.name = attrMap.getNamedItem("name").getNodeValue();
		NodeList list = node.getChildNodes();
		for (int i=0; i<list.getLength(); i++)
		{
			Node childNode = list.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE)
			{
				String name = childNode.getNodeName();
				if ("child".equals(name))
					desc.children.add(generateTree(childNode));
				else if ("property".equals(name))
					addProperty(desc, childNode);
				else if ("item".equals(name))
					desc.items.add(createItemDescriptor(childNode));
				else if ("listener".equals(name))
					desc.listeners.add(createListenerDescriptor(childNode));
			}
		}
		return desc;
	}

	/**
	 * Create an item descriptor from a DOM node.
	 * @param node the node to generate the listener from.
	 * @return a <code>ItemDescriptor</code> instance.
	 */
	public ItemDescriptor createItemDescriptor(Node node)
	{
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
	public ListenerDescriptor createListenerDescriptor(Node node)
	{
		ListenerDescriptor desc = new ListenerDescriptor();
		NamedNodeMap attrMap = node.getAttributes();
		desc.type = attrMap.getNamedItem("type").getNodeValue();
		desc.className = attrMap.getNamedItem("class").getNodeValue();
		return desc;
	}

	/**
	 * Add a property to an option descriptor from a DOM node.
	 * @param desc the option descriptor to add the property to.
	 * @param node the node to get the property name and value from.
	 */
	public void addProperty(OptionDescriptor desc, Node node)
	{
		NamedNodeMap attrMap = node.getAttributes();
		String name = attrMap.getNamedItem("name").getNodeValue();
		String value = attrMap.getNamedItem("value").getNodeValue();
		desc.setProperty(name, value);
	}
}
