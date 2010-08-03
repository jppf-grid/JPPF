/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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
package org.jppf.ui.options.factory;

import java.util.*;
import java.util.prefs.Preferences;

import org.apache.commons.logging.*;
import org.jppf.ui.options.*;
import org.jppf.ui.options.xml.OptionsPageBuilder;

/**
 * This class handles the persistence of the dynamic UI com
 * @author Laurent Cohen
 */
public final class OptionsHandler
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(OptionsHandler.class);
	/**
	 * The root of the preferences subtree in which the chart configurations are saved.
	 */
	private static final Preferences PREFERENCES = Preferences.userRoot().node("jppf");
	/**
	 * The list of option pages managed by this handler.
	 */
	private static List<OptionElement> pageList = new Vector<OptionElement>();
	/**
	 * A mapping of option pages to their name.
	 */
	private static Map<String, OptionElement> pageMap = new Hashtable<String, OptionElement>();
	/**
	 * The page builder used to instantiate pages from XML descriptors.
	 */
	private static OptionsPageBuilder builder = new OptionsPageBuilder();

	/**
	 * Get the list of option pages managed by this handler.
	 * @return a list of <code>OptionsPage</code> instances.
	 */
	public static List<OptionElement> getPageList()
	{
		return pageList;
	}

	/**
	 * Retrieve a page from its name.
	 * @param name the name of the page to retrieve.
	 * @return an <code>OptionsPage</code> instance.
	 */
	public static synchronized OptionElement getPage(String name)
	{
		return pageMap.get(name);
	}

	/**
	 * Add a page to the list of pages managed by this handler.
	 * @param page an <code>OptionsPage</code> instance.
	 * @return the page that was added.
	 */
	public static synchronized OptionElement addPage(OptionElement page)
	{
		pageList.add(page);
		try
		{
			pageMap.put(page.getName(), page);
		}
		catch(RuntimeException e)
		{
			int breakpoint = 0;
			log.info("Exception for page = \"" + page + "\" : " + e.getMessage());
			throw e;
		}
		return page;
	}

	/**
	 * Remove a page from the list of pages managed by this handler.
	 * @param page an <code>OptionsPage</code> instance.
	 */
	public static synchronized void removePage(OptionsPage page)
	{
		pageList.remove(page);
		pageMap.remove(page.getName());
	}

	/**
	 * Load a page built from an xml document.
	 * @param xmlPath the path to the xml document.
	 * @return the page that was added.
	 */
	public static synchronized OptionElement loadPageFromXml(String xmlPath)
	{
		try
		{
			return builder.buildPage(xmlPath, null);
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Add a page built from an xml document.
	 * @param xmlPath the path to the xml document.
	 * @return the page that was added.
	 */
	public static synchronized OptionElement addPageFromXml(String xmlPath)
	{
		return addPage(loadPageFromXml(xmlPath));
	}

	/**
	 * Load a page built from an xml document.
	 * @param xmlPath the path to the xml document.
	 * @param baseName base name for resource bundle lookup.
	 * @return the page that was added.
	 */
	public static synchronized OptionElement loadPageFromURL(String xmlPath, String baseName)
	{
		try
		{
			return builder.buildPageFromURL(xmlPath, baseName);
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Add a page built from an xml document.
	 * @param xmlPath the path to the xml document.
	 * @param baseName base name for resource bundle lookup.
	 * @return the page that was added.
	 */
	public static synchronized OptionElement addPageFromURL(String xmlPath, String baseName)
	{
			return addPage(loadPageFromURL(xmlPath, baseName));
	}

	/**
	 * Save the value of all persistent options in the preferences store.
	 */
	public static void savePreferences()
	{
		try
		{
			for (OptionElement elt: pageList)
			{
				OptionNode node = buildPersistenceGraph(elt);
				savePreferences(node, getPreferences());
			}
			getPreferences().flush();
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Save the value of all persistent options in the preferences store.
	 * @param node the root of the options subtree to save.
	 * @param prefs the preferences node in which to save the vaues.
	 */
	public static void savePreferences(OptionNode node, Preferences prefs)
	{
		if (!node.children.isEmpty())
		{
			Preferences p = prefs.node(node.elt.getName());
			for (OptionNode child: node.children) savePreferences(child, p);
		}
		else
		{
			Option option = (Option) node.elt;
			if (option.isPersistent()) prefs.put(option.getName(), ""+option.getValue());
		}
	}

	/**
	 * Load the value of all persistent options in the preferences store.
	 */
	public static void loadPreferences()
	{
		for (OptionElement elt: pageList)
		{
			OptionNode node = buildPersistenceGraph(elt);
			loadPreferences(node, getPreferences());
		}
	}

	/**
	 * Save the value of all persistent options in the preferences store.
	 * @param node the root of the options subtree to save.
	 * @param prefs the preferences node in which to save the vaues.
	 */
	public static void loadPreferences(OptionNode node, Preferences prefs)
	{
		if (!node.children.isEmpty())
		{
			Preferences p = prefs.node(node.elt.getName());
			for (OptionNode child: node.children) loadPreferences(child, p);
		}
		else
		{
			AbstractOption option = (AbstractOption) node.elt;
			Object def = option.getValue();
			String val = prefs.get(option.getName(), def == null ? null : def.toString());
			option.setValue(val);
		}
	}

	/**
	 * Get the page builder used to instantiate pages from XML descriptors.
	 * @return an <code>OptionsPageBuilder</code> instance.
	 */
	public static OptionsPageBuilder getBuilder()
	{
		return builder;
	}

	/**
	 * Build a graph of the persistent elements.
	 * @param elt the root of the current subgraph.
	 * @return an <code>OptionNode</code> instance.
	 */
	public static OptionNode buildPersistenceGraph(OptionElement elt)
	{
		OptionNode node = null;
		if (elt instanceof OptionsPage)
		{
			OptionsPage page = (OptionsPage) elt;
			for (OptionElement child: page.getChildren())
			{
				OptionNode childNode = buildPersistenceGraph(child);
				if (childNode != null)
				{
					if (node == null) node = new OptionNode(elt);
					node.children.add(childNode);
				}
			}
		}
		else if (elt instanceof AbstractOption)
		{
			if (((AbstractOption) elt).isPersistent()) node = new OptionNode(elt);
		}
		return node;
	}

	/**
	 * Get the root of the preferences subtree in which the chart configurations are saved.
	 * @return a {@link Preferences} instance.
	 */
	public static Preferences getPreferences()
	{
		return PREFERENCES;
	}

	/**
	 * A graph of the persistent options. 
	 */
	public static class OptionNode
	{
		/**
		 * The correponding option element.
		 */
		public OptionElement elt = null;
		/**
		 * The children of the corresponding option element.
		 */
		public List<OptionNode> children = new ArrayList<OptionNode>();

		/**
		 * Initilaize this node.
		 * @param elt the correponding option element.
		 */
		public OptionNode(OptionElement elt)
		{
			this.elt = elt;
		}
	}
}
